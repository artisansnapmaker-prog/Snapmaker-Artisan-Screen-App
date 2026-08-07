package fabscreen.features.machinetools.calibration.j1Platform.calibrationCheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.CalibrationPrintViewModel;
import fabscreen.features.machinetools.calibration.J1CalibrationBaseFragment;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.print.PrintListener;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.entity.module.HeatedBed;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

import static fabscreen.platform.base.RoutePath.PRINT_PRINT_J1_AJUSTMENT_MENT_CONTAINER;
import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_PAUSED;

public class CalibrationCheckPrintFragment extends J1CalibrationBaseFragment {
    @BindView(R2.id.tv_calibration_instructions_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_calibration_instructions_content)
    TextView mTvContent;
    @BindView(R2.id.tv_j1_leveling_xy_calibration_progress)
    TextView mTvProgress;
    @BindView(R2.id.tv_j1_leveling_xy_calibration_time)
    TextView mTvRemainingTime;
    @BindView(R2.id.btn_resume)
    Button mBtResume;
    @BindView(R2.id.btn_pause)
    Button mBtPause;
    @BindView(R2.id.top_bar_back)
    Button mBtBack;
    private final BehaviorSubject<Boolean> mWaitingSubject = BehaviorSubject.createDefault(false);
    @BindView(R2.id.tv_nozzle_state_left)
    TextView mShowStateLeft;
    @BindView(R2.id.iv_nozzle_state_left)
    ImageView mIvShowStateLeft;
    @BindView(R2.id.tv_nozzle_state_right)
    TextView mShowStateRight;
    @BindView(R2.id.iv_nozzle_state_right)
    ImageView mIvShowStateRight;
    @BindView(R2.id.tv_bed_state)
    TextView mShowStateBed;
    @BindView(R2.id.iv_bed_state)
    ImageView mIvShowStateBed;
    @BindView(R2.id.rectangle_2)
    ImageView mIvRectangle;

    private CalibrationPrintViewModel mViewModel;
    boolean mIsFirstLeftExtruderTemp = false;
    boolean mIsFirstRightExtruderTemp = false;
    boolean mIsFirstHeatedBedTemp = false;

    public static Fragment newInstance() {
        return new CalibrationCheckPrintFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        setNewPrintControllerListener();
        mViewModel.startPrint(R.raw.scp_p1);
    }

    private void initView() {
        mBtPause.setBackgroundResource(R.drawable.j1_btn_round_secondary);
        mBtPause.setTextColor(requireContext().getColorStateList(R.color.j1_btn_second_txt));
        Glide.with(this).asGif().load(R.drawable.gif_j1_calibration_check_printing).into(mIvRectangle);
        mTvTitle.setText(R.string.j1_calibration_calibration_check_print_check_model);
//        mTvContent.setText("You can check if the XY Offset Calibration is successful by observing the printed check model with the prompts on the Touchscreen.");
        mViewModel.getProgress()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(integer -> {
                    mTvProgress.setText(String.format("%d", integer) + "%");
                });
        mTvRemainingTime.setText("15 min");
//        mViewModel.getRemaining()
//                .observeOn(AndroidSchedulers.mainThread())
//                .as(bindToLifecycle())
//                .subscribe(str -> {
//                    mTvRemainingTime.setText(str);
//                });
        mWaitingSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(waiting -> {
                    mBtBack.setEnabled(!waiting);
                    mBtPause.setEnabled(!waiting);
                    mBtResume.setEnabled(!waiting);
                });

        mViewModel.getToolheadStatusObservable(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolHeadInfo -> {
                            Extruder extruder = fdmToolHeadInfo.getExtruderList().get(0);
                            float currentTemperature = extruder.getTemperature();
                            float targetTemperature = extruder.getTargetTemperature();
                            mShowStateLeft.setText(String.format("%.0f/%.0f℃", currentTemperature, targetTemperature));
                            if (currentTemperature <= targetTemperature && targetTemperature != 0 && !mIsFirstLeftExtruderTemp) {
                                mIvShowStateLeft.setImageResource(R.drawable.icon_nozzle_left_yellow_64x64);
                            } else {
                                if (targetTemperature != 0) {
                                    mIsFirstLeftExtruderTemp = true;
                                }
                                mIvShowStateLeft.setImageResource(R.drawable.icon_nozzle_left_normal_64x64);
                            }
                        }
                );

        mViewModel.getToolheadStatusObservable(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolHeadInfo -> {
                            Extruder extruder = fdmToolHeadInfo.getExtruderList().get(0);
                            float currentTemperature = extruder.getTemperature();
                            float targetTemperature = extruder.getTargetTemperature();
                            mShowStateRight.setText(currentTemperature + "/" + targetTemperature + "°C");
                            if (currentTemperature <= targetTemperature && targetTemperature != 0 && !mIsFirstRightExtruderTemp) {
                                mIvShowStateRight.setImageResource(R.drawable.icon_nozzle_right_yellow_64x64);
                            } else {
                                if (targetTemperature != 0) {
                                    mIsFirstRightExtruderTemp = true;
                                }
                                mIvShowStateRight.setImageResource(R.drawable.icon_nozzle_right_normal_64x64);
                            }
                            mShowStateRight.setText(String.format("%.0f/%.0f℃", currentTemperature, targetTemperature));
                        }
                );


        mViewModel.getHeatedBedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(bedStatus -> {
                            HeatedBed.ZoneInfo zoneInfo = bedStatus.getZoneList().get(0);
                            float targetTemperature = zoneInfo.getTargetTemperature();
                            float currentTemperature = zoneInfo.getCurrentTemperature();
                            if (currentTemperature <= targetTemperature && targetTemperature != 0 && !mIsFirstHeatedBedTemp) {
                                mIvShowStateBed.setImageResource(R.drawable.icon_heated_bed_yellow_64x64);
                            } else {
                                if (targetTemperature != 0) {
                                    mIsFirstHeatedBedTemp = true;
                                }
                                mIvShowStateBed.setImageResource(R.drawable.icon_heated_bed_normal_64x64);
                            }
                            mShowStateBed.setText(String.format("%.0f/%.0f℃", currentTemperature, targetTemperature));

                        }
                );

        mViewModel.getNewPrintControllerStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(integer -> {
                            mBtResume.setVisibility(SYSTEM_STATUS_PAUSED.valueEquals(integer) ? View.VISIBLE : View.INVISIBLE);
                            mBtPause.setVisibility(SYSTEM_STATUS_PAUSED.valueEquals(integer) ? View.INVISIBLE : View.VISIBLE);

                        }
                );

        mViewModel.getFilamentSubjectObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(triggered -> {
                    if (triggered) {
                        DecisionDialog.create(requireContext())
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                                .setContent(getString(R.string.calibration_check_print_filament_run_out_desc))
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                                    mViewModel.setFilament(true);
                                    dialog.dismiss();
                                }).show();
                    }
                });
        mViewModel.getFilamentSubjectObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(triggered -> {
                    if (triggered) {
                        int sumFilamentStatus = 0;
                        sumFilamentStatus += mViewModel.getToolheadFilamentStatus(0) ? 1 : 0;
                        sumFilamentStatus += mViewModel.getToolheadFilamentStatus(1) ? 2 : 0;

                        DecisionDialog decisionDialog = DecisionDialog.create(requireContext())
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, true, true)
                                .setTitle(R.string.control_load_filament_failed)
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setFirstTv(R.string.all_ok, R.color.select_dialog_orange_txt, (dialog, which) -> {
                                    mViewModel.setFilament(true);
                                    dialog.dismiss();
                                });
                        switch (sumFilamentStatus) {
                            case 1:
                                decisionDialog.setContent(R.string.error_left_extruder_unable_discharge);
                                break;
                            case 2:
                                decisionDialog.setContent(R.string.error_right_extruder_unable_discharge);
                                break;
                            case 3:
                            default:
                                decisionDialog.setContent(R.string.error_double_extruder_unable_discharge);
                                break;
                        }
                        decisionDialog.show();
                    }
                });

    }

    private void setNewPrintControllerListener() {
        mViewModel.setListener(new PrintListener() {
            @Override
            public void onStartSuccess() {
                mWaitingSubject.onNext(false);
            }

            @Override
            public void onStartFailed(int retCode) {
                mWaitingSubject.onNext(false);
                Logger.w("Unable to start printing, ret code %d", retCode);
                String str = "";
                switch (retCode) {
                    //FIXME:
                    case 256:
                        str = getString(R.string.calibration_check_print_file_init_error_desc);
                        break;
                    default: {
                        str = getString(R.string.print_warning_start_unable) + retCode;
                        break;
                    }
                }
                DecisionDialog.create(requireContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setContent(str)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                            exitAndFinish();
                        }).show();
            }

            @Override
            public void onPauseSuccess() {
                mWaitingSubject.onNext(false);
            }

            @Override
            public void onPauseFailed(int retCode) {
                mWaitingSubject.onNext(false);
                DecisionDialog.create(requireContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setContent(getString(R.string.print_warning_pause_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                        }).show();
            }

            @Override
            public void onResumeSuccess() {
                mWaitingSubject.onNext(false);
            }

            @Override
            public void onResumeFailed(int retCode) {
                mWaitingSubject.onNext(false);
                DecisionDialog.create(requireContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setContent(getString(R.string.print_warning_resume_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                            mViewModel.stop();
                        }).show();
            }

            @Override
            public void onResumeFromPowerOutageSuccess() {
                mWaitingSubject.onNext(false);
            }

            @Override
            public void onResumeFromPowerOutageFailed(int retCode) {
                mWaitingSubject.onNext(false);
                DecisionDialog.create(requireContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setContent(getString(R.string.print_warning_resume_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                            mViewModel.stop();
                        }).show();
            }

            @Override
            public void onStopSuccess() {
                fabMoving.dismiss();
                mWaitingSubject.onNext(false);
                exitAndFinish();
            }

            @Override
            public void onStopFailed(int retCode) {
                fabMoving.dismiss();
                mWaitingSubject.onNext(false);
                DecisionDialog.create(requireContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setContent(getString(R.string.print_warning_stop_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                            exitAndFinish();
                        }).show();
            }

            @Override
            public void onFinishSuccess() {
                mWaitingSubject.onNext(false);
                ((CalibrationCheckCalibrationActivity) requireActivity()).gotoCheckZOffsetCalibration();
            }

            @Override
            public void onFinishFailed(int retCode) {
                mWaitingSubject.onNext(false);
                DecisionDialog.create(requireContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setContent(getString(R.string.print_warning_finish_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                            mViewModel.stop();
                        }).show();
            }
        });
    }

    @Override
    protected CalibrationPrintViewModel getViewModel() {
        return getViewModelProvider().get(CalibrationPrintViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_leveling_xy_calibration_print;
    }

    boolean skip = false;

    @OnClick({R2.id.top_bar_back})
    public void onChickStop() {
        playNormalClickSound();
        DecisionDialog.create(getContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, false, false, false, true)
                .setType(DecisionDialog.WARMING_TYPE)
                .setContent(R.string.j1_calibration_quit_msg)
                .setContentColor(R.color.palette_grey_french)
                .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_left_text_color, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setSecondTv(getString(R.string.all_quit), R.color.palette_red_sunset, ((dialog, which) -> {
                    mWaitingSubject.onNext(true);
                    dialog.dismiss();
                    fabMoving.show();
                    ServiceContainer.getInstance().getService(IMachine.class).getFDMController()
                            .exitCalibration(false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(success -> {
                                if (success.isSuccess()) {
                                    mViewModel.stop();
                                }
                            });
                }))
                .show();
    }

    @OnClick(R2.id.btn_pause)
    public void onChickPause() {
        mWaitingSubject.onNext(true);
        mViewModel.pause();
    }

    @OnClick(R2.id.btn_resume)
    public void onChickResume() {
        mWaitingSubject.onNext(true);
        mViewModel.resume();
    }


    private void exitAndFinish() {
        if (skip == true) {
            ((CalibrationCheckCalibrationActivity) requireActivity()).gotoCheckZOffsetCalibration();
            return;
        }
        requireActivity().finish();
//        ServiceContainer.getInstance().getService(IMachine.class).getFDMController()
//                .exitCalibration(false)
//                .observeOn(AndroidSchedulers.mainThread())
//                .as(bindToLifecycle())
//                .subscribe(success -> {
//                    if (success.isSuccess()) {
//                        requireActivity().finish();
//                    }
//                }, LogHelper::log);
    }

    @OnClick(R2.id.btn_skip)
    void onClickAdjustment() {
        ServiceContainer.getInstance().getService(IRouter.class)
                .routeWithClassPath(PRINT_PRINT_J1_AJUSTMENT_MENT_CONTAINER)
                .start(getContext());
    }

    @OnClick(R2.id.rl_j1_print_details_adjustment)
    public void onChickSkip() {
        FabConfirm.create(getContext())
                .setDescription("这只是一个临时跳过按钮")
                .setConfirm(R.string.all_yes, (dialog, which) -> {
                    dialog.dismiss();
                    skip = true;
                    mViewModel.stop();
                })
                .setCancel(R.string.all_cancel, ((dialog, which) -> dialog.dismiss()))
                .show();

    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.unSubscribeTemperature();
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.subscribeTemperature();
    }
}
