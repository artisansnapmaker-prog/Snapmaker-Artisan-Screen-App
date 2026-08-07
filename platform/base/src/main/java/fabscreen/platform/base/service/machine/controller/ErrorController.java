package fabscreen.platform.base.service.machine.controller;

import static fabscreen.platform.base.service.machine.controller.ErrorController.EmergencyStopState.EMERGENCY_STOP_STATE_PRESS;
import static fabscreen.platform.base.service.machine.controller.ErrorController.EmergencyStopState.EMERGENCY_STOP_STATE_RELEASE;

import android.content.Intent;
import android.util.SparseArray;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.R;
import fabscreen.platform.base.helper.SoundUtil;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.MachineConnectionController;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.MachineFault;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.prop.ArrayProp;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class ErrorController {
    private final IMachine mMachine;
    private final IAppService mAppService;
    private final IRouter mRouter;

    private final MachineConnectionController mConnectionController;
    private final FDMController mFDMController;
    private final LaserController mLaserController;
    private final CNCController mCNCController;

    private boolean mIsEmergencyStopState = false;

    int mSoundSteamId = -1;

    private final SparseArray<DecisionDialog> mErrorViewsSparseArray = new SparseArray<>();

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    private PublishSubject<AbnormalState> mAbnormalTriggerShowSubject = PublishSubject.create();
    private PublishSubject<AbnormalState> mAbnormalReturnShowSubject = PublishSubject.create();

    public ErrorController(IMachine iMachine, IAppService appService, MachineConnectionController cc, IRouter iRouter, FDMController fdmController, LaserController laserController, CNCController cncController) {
        mMachine = iMachine;
        mConnectionController = cc;
        mAppService = appService;
        mRouter = iRouter;
        mFDMController = fdmController;
        mLaserController = laserController;
        mCNCController = cncController;

        bindEvents();
    }

    private void bindEvents() {
        mDisposables.add(mAbnormalTriggerShowSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(abnormalState -> {
                    Logger.w("Receiving error state: " + abnormalState.toString());
                    if (mAppService.getNowViewContext() == null) {
                        Logger.e("Could not get current view context, went silent.");
                        return;
                    }

                    // return if error state was showing dialog already.
                    DecisionDialog errorDialog = mErrorViewsSparseArray.get(abnormalState.getIndex());
                    if (errorDialog != null && errorDialog.isShowing()) {
                        return;
                    }

                    DecisionDialog decisionDialog;
                    if (mMachine.getMachineInfoSubjectHolder().getValue().seriesId == IMachine.MachineSeries.J) {
                        decisionDialog = DecisionDialog.create(mAppService.getNowViewContext())
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true);

                        if (!abnormalState.check.isEmpty()) {
                            decisionDialog.setFirstTv(abnormalState.check, R.color.select_dialog_orange_txt, ((dialog, which) -> dialog.dismiss()));
                        } else {
                            decisionDialog.setFirstTv(R.string.all_j1_i_know, R.color.select_dialog_orange_txt, ((dialog, which) -> dialog.dismiss()));
                        }

                        if (!abnormalState.title.isEmpty()) {
                            decisionDialog.setTitle(abnormalState.title);
                        }

                        if (!abnormalState.content.isEmpty()) {
                            decisionDialog.setContent(abnormalState.content);
                        }
                    } else {

                        decisionDialog = DecisionDialog.create(mAppService.getNowViewContext())
                                .setPic(abnormalState.level < 2 ? R.drawable.ic_pic_a400_error_68x68 : R.drawable.pic_a400_warning_68x68)
                                .setTitle(abnormalState.title.isEmpty() ? mAppService.getAppContext().getString(R.string.a400_dialog_general_error_warning_title) : abnormalState.title)
                                .setContent(abnormalState.content)
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setDialogStatus(DecisionDialog.BTN_ONE, true, false, true, false)
                                .setFirstTv(R.string.all_confirm, abnormalState.level < 2 ? R.color.select_dialog_red_txt : R.color.select_dialog_white_txt, ((dialog, which) -> {
                                    if (abnormalState.isStateNeedRestart()) {
                                        stopSound();
                                        DecisionDialog.getsInstance().setCanceledOnTouchOutSide(false);
                                        DecisionDialog.getsInstance().setContent(R.string.a400_error_restarting_machine);
                                        DecisionDialog.getsInstance().mCancelBtn.setEnabled(false);
                                        Disposable disposable = mMachine.getMachineController().restartMachine()
                                                .throttleLast(20, TimeUnit.SECONDS)
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe((responseStructure -> {
                                                    if (!responseStructure.isSuccess()) {
                                                        Logger.e("restart machine controller failed!");
                                                    }
                                                    dialog.dismiss();
                                                    ServiceContainer.getInstance().getService(IAppService.class).restart();
                                                }), LogHelper::log);

                                    } else {
                                        dialog.dismiss();
                                    }
                                }));
                        if (abnormalState.isStateNeedRestart()) {
                            IAppService appService = ServiceContainer.getInstance().getService(IAppService.class);
                            mSoundSteamId = SoundUtil.playSoundLoop(appService.getSoundPool(), appService.getSoundIdByResourceId(R.raw.sound_emergency_stop));
                        }
                    }
                    decisionDialog.show();
                    mErrorViewsSparseArray.put(abnormalState.getIndex(), decisionDialog);
                }));

        mDisposables.add(mAbnormalReturnShowSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(abnormalState -> {
                    DecisionDialog fabConfirm = mErrorViewsSparseArray.get(abnormalState.getIndex());
                    if (fabConfirm != null && fabConfirm.isShowing()) {
                        fabConfirm.dismiss();
                    }
                    mErrorViewsSparseArray.remove(abnormalState.getIndex());
                }));
    }

    private void stopSound() {
        if (mSoundSteamId != -1) {
            SoundUtil.stopSound(ServiceContainer.getInstance().getService(IAppService.class).getSoundPool(), mSoundSteamId);
        }
    }

    public void onAbnormalTrigger(MachineFault machineFault, List<Integer> machineBehavior) {
        // Emergency Stop triggered, set state, route to page and return
        if (machineFault.isEmergencyStop()) {
            mAppService.setEmergencyStop(EMERGENCY_STOP_STATE_PRESS);
            mIsEmergencyStopState = true;
            mRouter.routeToEmergencyStopPage(false).start(mAppService.getNowViewContext(), Intent.FLAG_ACTIVITY_SINGLE_TOP);
            return;
        }

        // If emergency stop was pressed, other error state should not be triggered,
        // because the only way we can recover is restart machine.
        if (mIsEmergencyStopState) return;

        StringBuilder content = new StringBuilder();
        String title = "";
        String check = "";
        AbnormalState a400AbnormalState = new AbnormalState(machineFault, "", "", "");
        switch (machineFault.getOwner()) {
            case Module.ModuleType.J1_CONTROL: {
                switch (machineFault.getValue()) {
//                case 1:
//                    content.append(mAppService.getAppContext().getString(R.string.error_abnormal_motor_drive));
//                    break;
                    case 2:
                        content.append(mAppService.getAppContext().getString(R.string.error_hot_bed_not_connected));
                        break;
                    case 3:
                        content.append(mAppService.getAppContext().getString(R.string.error_hot_bed_not_connected));
                        break;
                    case 4:
                        content.append(mAppService.getAppContext().getString(R.string.error_unable_identify_left_extruder));
                        break;
                    case 5:
                        content.append(mAppService.getAppContext().getString(R.string.error_unable_identify_right_extruder));
                        break;
                    case 6:
                        content.append(mAppService.getAppContext().getString(R.string.error_unable_identify_double_extruder));
                        break;
                    case 7:
                        content.append(mAppService.getAppContext().getString(R.string.error_left_extruder_abnormal_temperature));
                        break;
                    case 8:
                        content.append(mAppService.getAppContext().getString(R.string.error_right_extruder_abnormal_temperature));
                        break;
                    case 9:
                        content.append(mAppService.getAppContext().getString(R.string.error_double_extruder_abnormal_temperature));
                        break;
                    case 10:
                        content.append(mAppService.getAppContext().getString(R.string.error_hot_bed_abnormal_temperature));
                        break;
                    case 11:
                        content.append(mAppService.getAppContext().getString(R.string.error_motor_lose_step));
                        title = mAppService.getAppContext().getString(R.string.error_motor_lose_step_title);
                        break;
                    case 12:
                        content.append(mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_left_extruder));
                        title = mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_left_extruder_title);
                        check = mAppService.getAppContext().getString(R.string.error_all_aware_danger);
                        break;
                    case 13:
                        content.append(mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_right_extruder));
                        title = mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_right_extruder_title);
                        check = mAppService.getAppContext().getString(R.string.error_all_aware_danger);
                        break;
                    case 14:
                        content.append(mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_hot_bed));
                        title = mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_hot_bed_title);
                        check = mAppService.getAppContext().getString(R.string.error_all_aware_danger);
                        break;
                    case 15:
                        content.append(mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_double_extruder));
                        title = mAppService.getAppContext().getString(R.string.error_no_feedback_during_heating_double_extruder_title);
                        check = mAppService.getAppContext().getString(R.string.error_all_aware_danger);
                        break;
                    case 16:
                        content.append(mAppService.getAppContext().getString(R.string.error_abnormal_x_motor_drive));
                        break;
                    case 17:
                        content.append(mAppService.getAppContext().getString(R.string.error_abnormal_y_motor_drive));
                        break;
                    case 18:
                        content.append(mAppService.getAppContext().getString(R.string.error_abnormal_z_motor_drive));
                        break;
                    case 19:
                        content.append(mAppService.getAppContext().getString(R.string.error_abnormal_left_motor_drive));
                        break;
                    case 20:
                        content.append(mAppService.getAppContext().getString(R.string.error_abnormal_right_motor_drive));
                        break;
                    case 21:
                        content.append(mAppService.getAppContext().getString(R.string.error_abnormal_double_motor_drive));
                        break;
                    default:
                        content.append("未定义异常编码：" + machineFault.getValue());
                        break;
                }
                mAbnormalTriggerShowSubject.onNext(new AbnormalState(machineFault, title, content.toString(), check));
                break;
            }
            case Module.ModuleType.A400_CONTROL: {
                a400AbnormalState = buildControllerErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.LINEAR_A400: {
                a400AbnormalState = buildLinearModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.HEAD_3DP:
            case Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER: {
                if (machineFault.getValue() == 11) {
                    mMachine.getNewPrintController().setFilament(false);
                    return;
                }

                a400AbnormalState = buildDualExtruderModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.HEAD_LASER:
            case Module.ModuleType.HEAD_LASER_10W: {
                a400AbnormalState = buildLaser10wModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.HEAD_LASER_20W: {
                a400AbnormalState = buildLaser20wModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.HEAD_LASER_40W: {
                a400AbnormalState = buildLaser40wModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.HEAD_LASER_2W_INFRARED: {
                a400AbnormalState = buildLaser2wModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.HEAD_CNC: {
                a400AbnormalState = build50wCNCModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.HEAD_CNC_200W: {
                a400AbnormalState = build200wCNCModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.ADDON_ENCLOSURE_A400: {
                a400AbnormalState = buildArtisanEnclosureErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.ADDON_AIR_PURIFIER: {
                a400AbnormalState = buildAirPurifierModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.ROTARY_MODULE: {
                a400AbnormalState = buildRotaryModuleErrorMessage(machineFault);
                break;
            }
            case Module.ModuleType.ADDON_HEATED_BED_A400: {
                a400AbnormalState = buildArtisanHeatedBedErrorMessage(machineFault);
                break;
            }
            default:
                String errorCode = machineFault.getOwner() + "-" + machineFault.getValue();
                content.append(mAppService.getAppContext().getString(R.string.a400_dialog_error_code_desc));
                content.append(errorCode);
//                content.append(machineFault).append("\n" + mAppService.getAppContext().getString(R.string.error_error_state));
//                for (int i = 0; i < machineBehavior.size(); i++) {
//                    if (machineBehavior.get(i) == -1) continue;
//                    content.append(machineBehavior.get(i)).append(" ");
//                }
                a400AbnormalState.content = content.toString();
                break;
        }
        mAbnormalTriggerShowSubject.onNext(a400AbnormalState);

    }

    AbnormalState buildControllerErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_controller_tool_head_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_controller_tool_head_not_detected_content));
                break;
            case 2:
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_controller_mainboard_overheated_title);
                content.append(getStringFromContext(R.string.a400_error_controller_mainboard_overheated_content));
                break;
            case 4:
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_controller_failed_to_home_title);
                content.append(getStringFromContext(R.string.a400_error_controller_failed_to_home_content));
                break;
            case 6:
                break;
            case 7:
                title = getStringFromContext(R.string.a400_error_controller_system_power_supply_abnormal_title);
                content.append(getStringFromContext(R.string.a400_error_controller_system_power_supply_abnormal_content));
                break;
            case 8:
                title = getStringFromContext(R.string.a400_error_controller_motion_power_supply_abnormal_title);
                content.append(getStringFromContext(R.string.a400_error_controller_motion_power_supply_abnormal_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState buildLinearModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_linear_module_disconnected_title);
                content.append(getStringFromContext(R.string.a400_error_linear_module_disconnected_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_linear_module_all_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_linear_module_all_not_detected_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_linear_module_x_axis_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_linear_module_x_axis_not_detected_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_linear_module_y_axis_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_linear_module_y_axis_not_detected_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_linear_module_z_axis_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_linear_module_z_axis_not_detected_content));
                break;
            case 6:
                title = getStringFromContext(R.string.a400_error_linear_module_z_axis_lowest_limit_title);
                content.append(getStringFromContext(R.string.a400_error_linear_module_z_axis_lowest_limit_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState buildRotaryModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_rotary_module_wrong_socket_plugged_title);
                content.append(getStringFromContext(R.string.a400_error_rotary_module_wrong_socket_plugged_content));
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState buildDualExtruderModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        int level = 2;
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_abnormal_heating_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_abnormal_heating_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_abnormal_heating_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_abnormal_heating_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_abnormal_temp_control_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_abnormal_temp_control_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_abnormal_temp_control_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_abnormal_temp_control_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_low_temp_limit_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_low_temp_limit_content));
                break;
            case 6:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_low_temp_limit_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_low_temp_limit_content));
                break;
            case 7:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_high_temp_limit_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_left_extruder_high_temp_limit_content));
                break;
            case 8:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_high_temp_limit_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_high_temp_limit_content));
                break;
            case 9:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_tool_head_disconnected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_tool_head_disconnected_content));
                break;
            case 10:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_content));
                break;
            case 11:
                break;
            case 12:
                level = 1;
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_calibrate_sensor_abnormally_triggered_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_calibrate_sensor_abnormally_triggered_content));
                break;
            case 13:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_calibration_failed_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_calibration_failed_content));
                break;
            case 14:
                break;
            case 15:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_fail_to_home_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_right_extruder_fail_to_home_content));
                break;
            case 16:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_tool_head_communication_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_tool_head_communication_content));
                break;
            case 17:
            case 18:
                level = 1;
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_extruder_being_pull_up_constantly_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_extruder_being_pull_up_constantly_content));
                break;
            default:
                break;
        }
        AbnormalState abnormalState = new AbnormalState(machineFault, title, content.toString(), "");
        abnormalState.level = level;
        return abnormalState;
    }

    AbnormalState buildLaser10wModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_content));
                break;
            case 9:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_content));
                break;
            case 10:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_content));
                break;
            case 11:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_content));
                break;
            case 12:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_content));
                break;
            case 13:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState buildLaser20wModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        int level = 2;
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_content));
                break;
            case 6:
                level = 0;
                title = getStringFromContext(R.string.a400_error_tool_head_laser_visible_flames_detected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_visible_flames_detected_content));
                break;
            case 9:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_content));
                break;
            case 10:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_content));
                break;
            case 11:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_content));
                break;
            case 12:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_content));
                break;
            case 13:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_content));
                break;
            default:
                break;
        }
        AbnormalState abnormalState = new AbnormalState(machineFault, title, content.toString(), "");
        abnormalState.level = level;
        return abnormalState;
    }

    AbnormalState buildLaser40wModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        int level = 2;
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_content));
                break;
            case 6:
                level = 0;
                title = getStringFromContext(R.string.a400_error_tool_head_laser_visible_flames_detected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_visible_flames_detected_content));
                break;
            case 9:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_content));
                break;
            case 10:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_content));
                break;
            case 11:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_content));
                break;
            case 12:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_content));
                break;
            case 13:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_content));
                break;
            default:
                break;
        }
        AbnormalState abnormalState = new AbnormalState(machineFault, title, content.toString(), "");
        abnormalState.level = level;
        return abnormalState;
    }

    AbnormalState buildLaser2wModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        int level = 2;
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_orientation_detection_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_overheat_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_dual_extruder_abnormal_hot_end_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_slanting_toolhead_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_emitter_abnormal_content));
                break;
            case 6:
                level = 0;
                title = getStringFromContext(R.string.a400_error_tool_head_laser_visible_flames_detected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_visible_flames_detected_content));
                break;
            case 9:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_heat_dissipation_content));
                break;
            case 10:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_abnormal_temp_sensor_content));
                break;
            case 11:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_wrong_socket_plugged_content));
                break;
            case 12:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_tool_head_disconnect_content));
                break;
            case 13:
                title = getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_laser_enclosure_not_detected_content));
                break;
            default:
                break;
        }
        AbnormalState abnormalState = new AbnormalState(machineFault, title, content.toString(), "");
        abnormalState.level = level;
        return abnormalState;
    }

    AbnormalState build50wCNCModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_tool_head_50w_cnc_locked_protection_trigger_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_50w_cnc_locked_protection_trigger_content));
                break;
            case 7:
                title = getStringFromContext(R.string.a400_error_tool_head_50w_cnc_wrong_socket_plugged_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_50w_cnc_wrong_socket_plugged_content));
                break;
            case 8:
                title = getStringFromContext(R.string.a400_error_tool_head_50w_cnc_tool_head_disconnected_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_50w_cnc_head_disconnected_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState build200wCNCModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_locked_protection_trigger_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_locked_protection_trigger_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_motor_driver_protection_triggered_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_motor_driver_protection_triggered_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_software_overcurrent_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_software_overcurrent_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_drive_board_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_drive_board_overheat_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_spindle_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_spindle_overheat_content));
                break;
            case 6:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_abnormal_spindle_voltage_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_abnormal_spindle_voltage_content));
                break;
            case 7:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_wrong_socket_plugged_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_wrong_socket_plugged_content));
                break;
            case 8:
                title = getStringFromContext(R.string.a400_error_tool_head_200w_cnc_toolhead_disconnect_title);
                content.append(getStringFromContext(R.string.a400_error_tool_head_200w_cnc_toolhead_disconnect_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState buildArtisanEnclosureErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_add_on_artisan_enclosure_disconnect_title);
                content.append(getStringFromContext(R.string.a400_error_add_on_artisan_enclosure_disconnect_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState buildAirPurifierModuleErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_add_on_air_purifier_disconnect_title);
                content.append(getStringFromContext(R.string.a400_error_add_on_air_purifier_disconnect_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }

    AbnormalState buildArtisanHeatedBedErrorMessage(MachineFault machineFault) {
        String title = "";
        StringBuilder content = new StringBuilder();
        content = appendErrorCode(machineFault, content);
        switch (machineFault.getValue()) {
            case 1:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_abnormal_inner_bed_heating_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_abnormal_inner_bed_heating_content));
                break;
            case 2:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_abnormal_outer_bed_heating_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_abnormal_outer_bed_heating_content));
                break;
            case 3:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_inner_abnormal_heated_bed_temp_control_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_inner_abnormal_heated_bed_temp_control_content));
                break;
            case 4:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_outer_abnormal_heated_bed_temp_control_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_outer_abnormal_heated_bed_temp_control_content));
                break;
            case 5:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_inner_heated_bed_underheat_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_inner_heated_bed_underheat_content));
                break;
            case 6:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_outer_heated_bed_underheat_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_outer_heated_bed_underheat_content));
                break;
            case 7:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_inner_heated_bed_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_inner_heated_bed_overheat_content));
                break;
            case 8:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_outer_heated_bed_overheat_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_outer_heated_bed_overheat_title));
                break;
            case 9:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_heated_bed_platform_mismatched_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_heated_bed_platform_mismatched_content));
                break;
            case 10:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_not_detected_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_not_detected_content));
                break;
            case 11:
                title = getStringFromContext(R.string.a400_error_artisan_heated_bed_abnormal_bed_self_check_title);
                content.append(getStringFromContext(R.string.a400_error_artisan_heated_bed_abnormal_bed_self_check_content));
                break;
            default:
                break;
        }
        return new AbnormalState(machineFault, title, content.toString(), "");
    }


    StringBuilder appendErrorCode(MachineFault machineFault, StringBuilder stringBuilder) {
        String errorCode = machineFault.getOwner() + "-" + machineFault.getValue();
        stringBuilder.append(mAppService.getAppContext().getString(R.string.a400_dialog_error_code_desc));
        stringBuilder.append(errorCode + "\n");
        return stringBuilder;
    }

    String getStringFromContext(int resId) {
        return mAppService.getAppContext().getString(resId);
    }

    public void onAbnormalReturn(MachineFault machineFault, List<Integer> machineBehavior) {
        if (machineFault.isEmergencyStop()) {
            mAppService.setEmergencyStop(EMERGENCY_STOP_STATE_RELEASE);
            mIsEmergencyStopState = false;
            mRouter.routeToEmergencyStopPage(true).start(mAppService.getNowViewContext(), Intent.FLAG_ACTIVITY_SINGLE_TOP);
            return;
        } else if (machineFault.is3DP() && machineFault.getValue() == 11) {
            mMachine.getNewPrintController().setFilament(true);
            return;
        }
        mAbnormalReturnShowSubject.onNext(new AbnormalState(machineFault, "", "", ""));
    }

    public Observable<ResponseStructure> queryException() {
        ResponseStructure iStructureResponseStructure = new ResponseStructure();
        BaseStructure baseStructure = new BaseStructure() {
            @Override
            protected void init() {
                addProp("exceptionInfos", new ArrayProp<>(new MachineFault()));
                addProp("machineBehaviorStates", new ArrayProp<>(new UInt8Prop(-1)));
            }
        };
        iStructureResponseStructure.dataProp = baseStructure;
        return mConnectionController.request(0x04, 0x02, null, iStructureResponseStructure)
                .doOnNext(responseStructure -> {
                    BaseStructure baseStructure1 = (BaseStructure) responseStructure.dataProp;
                    final List<MachineFault> exceptionInfos = ((ArrayProp<MachineFault>) baseStructure1.getProp("exceptionInfos")).getValue();
                    final List<UInt8Prop> machineBehaviorStates = ((ArrayProp<UInt8Prop>) baseStructure1.getProp("machineBehaviorStates")).getValue();
                    final List<Integer> machineBehaviors = new ArrayList<>();
                    if (exceptionInfos.size() == 1 && exceptionInfos.get(0).getOwner() == -1) {
                        return;
                    }
                    for (int i = 0; i < machineBehaviorStates.size(); i++) {
                        machineBehaviors.add(machineBehaviorStates.get(i).getValue());
                    }
                    for (int i = 0; i < exceptionInfos.size(); i++) {
                        onAbnormalTrigger(exceptionInfos.get(i), machineBehaviors);
                    }
                });
    }

    @Deprecated
    public void onEmergencyStop(boolean onStart) {
        if (onStart == mIsEmergencyStopState) return;
        mIsEmergencyStopState = onStart;
        mRouter.routeToEmergencyStopPage(!onStart).start(mAppService.getNowViewContext(), Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    public Observable<ResponseStructure> TestException() {
        return mConnectionController.request(0x04, 0x0a, null, new ResponseStructure());
    }

    static class AbnormalState {
        MachineFault machineFault;
        String title;
        String content;
        String check;
        int level = -1;

//        public AbnormalState(MachineFault machineFault, String content) {
//            this.machineFault = machineFault;
//            this.content = content;
//        }

        public AbnormalState(MachineFault machineFault, String title, String content, String check) {
            this.machineFault = machineFault;
            this.title = title;
            this.content = content;
            this.check = check;
            this.level = 2;
        }

        public int getIndex() {
            return machineFault.getOwner() << 24 | machineFault.getLevel() << 8 | machineFault.getValue();
        }

        public boolean isStateBlocked() {
            return level == 1;
        }

        public boolean isStateNeedRestart() {
            return level == 0;
        }
    }

    public enum EmergencyStopState {
        EMERGENCY_STOP_STATE_NORMAL, EMERGENCY_STOP_STATE_PRESS, EMERGENCY_STOP_STATE_RELEASE
    }
}
