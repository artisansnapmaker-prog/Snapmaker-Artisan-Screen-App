package fabscreen.features.settings.a400.maintenance.machineinfo;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import java.util.List;

import butterknife.BindView;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.entity.module.AirPurifier;
import fabscreen.platform.base.service.machine.entity.module.DryBox;
import fabscreen.platform.base.service.machine.entity.module.Enclosure;
import fabscreen.platform.base.service.machine.entity.parts.LinearLimit;
import fabscreen.platform.base.service.machine.entity.toolhead.CNCToolhead;
import fabscreen.platform.base.service.machine.entity.toolhead.FdmToolhead;
import fabscreen.platform.base.service.machine.entity.toolhead.LaserToolhead;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_AIR_PURIFIER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_DRY_BOX;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_ENCLOSURE;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_ENCLOSURE_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC_200W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_20W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_2W_INFRARED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_40W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.LINEAR_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ROTARY_MODULE;

public class A400MachineInfoFragment extends BaseFragment {
    private static final String TAG = "A400MachineInfoFragment";

    // ToolHead - Dual Extrusion Module
    @BindView(R2.id.ll_module_dual_extrusion)
    LinearLayout mLlModuleDualExtrusion;
    @BindView(R2.id.miiv_dual_nozzle_temp_l)
    MachineInfoItemView mMiivNozzleTempL;
    @BindView(R2.id.miiv_dual_nozzle_temp_r)
    MachineInfoItemView mMiivNozzleTempR;
    @BindView(R2.id.miiv_filament_detect_l)
    MachineInfoItemView mMiivFilamentDetectL;
    @BindView(R2.id.miiv_filament_detect_r)
    MachineInfoItemView mMiivFilamentDetectR;
    @BindView(R2.id.miiv_cooling_l)
    MachineInfoItemView mMiivCoolingL;
    @BindView(R2.id.miiv_cooling_r)
    MachineInfoItemView mMiivCoolingR;
    @BindView(R2.id.miiv_heat_dissipation_dual)
    MachineInfoItemView mMiivHeatDissipationDual;

    // ToolHead - Single Extrusion Module
    @BindView(R2.id.ll_module_single_extrusion)
    LinearLayout mLlModuleSingleExtrusion;
    @BindView(R2.id.miiv_single_nozzle_temp)
    MachineInfoItemView mMiivSingleNozzleTemp;
    @BindView(R2.id.miiv_filament_detect)
    MachineInfoItemView mMiivFilamentDetect;
    @BindView(R2.id.miiv_cooling)
    MachineInfoItemView mMiivCooling;
    @BindView(R2.id.miiv_dissipation_single)
    MachineInfoItemView mMiivDissipationSingle;

    // ToolHead - 10w Laser Module
    @BindView(R2.id.ll_module_laser_10w)
    LinearLayout mLlModuleLaser10w;
    @BindView(R2.id.miiv_laser_power)
    MachineInfoItemView mMiivLaserPower;
    @BindView(R2.id.miiv_orientation_detect)
    MachineInfoItemView mMiivOrientationDetect;
    @BindView(R2.id.miiv_laser_emitter_temp)
    MachineInfoItemView mMiivLaserEmitterTemp;
    @BindView(R2.id.miiv_camera_10w_connection)
    MachineInfoItemView mMiiv10WLaserCameraConnection;

    // ToolHead - 1600mw Laser Module
    @BindView(R2.id.ll_module_laser_1600mw)
    LinearLayout mLlModuleLaser1600mw;

    // ToolHead - 20w Laser Module
    @BindView(R2.id.ll_module_laser_20w)
    LinearLayout mLlModuleLaser20w;
    @BindView(R2.id.miiv_laser_20w_power)
    MachineInfoItemView mMiivLaser20wPower;
    @BindView(R2.id.miiv_20w_orientation_detect)
    MachineInfoItemView mMiiv20wOrientationDetect;
    @BindView(R2.id.miiv_laser_20w_emitter_temp)
    MachineInfoItemView mMiivLaser20wEmitterTemp;
    @BindView(R2.id.miiv_laser_20w_fire_sensor_sensitivity)
    MachineInfoItemView mMiiv20wFireSensorSensitivity;

    // ToolHead - 40w Laser Module
    @BindView(R2.id.ll_module_laser_40w)
    LinearLayout mLlModuleLaser40w;
    @BindView(R2.id.miiv_laser_40w_power)
    MachineInfoItemView mMiivLaser40wPower;
    @BindView(R2.id.miiv_40w_orientation_detect)
    MachineInfoItemView mMiiv40wOrientationDetect;
    @BindView(R2.id.miiv_laser_40w_emitter_temp)
    MachineInfoItemView mMiivLaser40wEmitterTemp;
    @BindView(R2.id.miiv_laser_40w_fire_sensor_sensitivity)
    MachineInfoItemView mMiiv40wFireSensorSensitivity;

    // ToolHead - 2w Laser Module
    @BindView(R2.id.ll_module_laser_2w)
    LinearLayout mLlModuleLaser2w;
    @BindView(R2.id.miiv_laser_2w_power)
    MachineInfoItemView mMiivLaser2wPower;
    @BindView(R2.id.miiv_2w_orientation_detect)
    MachineInfoItemView mMiiv2wOrientationDetect;
    @BindView(R2.id.miiv_laser_2w_emitter_temp)
    MachineInfoItemView mMiivLaser2wEmitterTemp;

    // ToolHead - 200w CNC Module
    @BindView(R2.id.ll_module_cnc)
    LinearLayout mLlModuleCNC;
    @BindView(R2.id.miiv_cnc_speed)
    MachineInfoItemView mMiivCncSpeed;

    // Addon - Rotary Module
    @BindView(R2.id.ll_module_rotary)
    LinearLayout mLlModuleRotary;

    // Addon - Dryer Module
    @BindView(R2.id.ll_module_dryer)
    LinearLayout mLlModuleDryer;
    @BindView(R2.id.miiv_dryer_temp)
    MachineInfoItemView mMiivDryerTemp;
    @BindView(R2.id.miiv_dryer_rh)
    MachineInfoItemView mMiivDryerRH;
    @BindView(R2.id.miiv_dryer_fan)
    MachineInfoItemView mMiivDryerFan;
    @BindView(R2.id.miiv_dryer_power)
    MachineInfoItemView mMiivDryerIsDrying;

    // Addon - Enclosure Module
    @BindView(R2.id.ll_module_enclosure)
    LinearLayout mLlModuleEnclosure;
    @BindView(R2.id.miiv_enclosure_led)
    MachineInfoItemView mMiivEnclosureLed;
    @BindView(R2.id.miiv_enclosure_fan)
    MachineInfoItemView mMiivEnclosureFan;
    @BindView(R2.id.miiv_enclosure_door)
    MachineInfoItemView mMiivEnclosureDoor;

    // Addon - Air Purifier Module
    @BindView(R2.id.ll_module_purifier)
    LinearLayout mLlModulePurifier;
    @BindView(R2.id.miiv_purifier_fan)
    MachineInfoItemView mMiivPurifierFan;
    @BindView(R2.id.miiv_purifier_cover)
    MachineInfoItemView mMiivPurifierCover;
    @BindView(R2.id.miiv_purifier_filter_detection)
    MachineInfoItemView mMiivPurifierFilterDetection;

    // Linear Module
    @BindView(R2.id.liv_module_x)
    LinearInfoView mLivModuleX;
    @BindView(R2.id.liv_module_y1)
    LinearInfoView mLivModuleY1;
    @BindView(R2.id.liv_module_y2)
    LinearInfoView mLivModuleY2;
    @BindView(R2.id.liv_module_z1)
    LinearInfoView mLivModuleZ1;
    @BindView(R2.id.liv_module_z2)
    LinearInfoView mLivModuleZ2;

    private A400MachineInfoViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400MachineInfoFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_maintenance_machine_info;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(A400MachineInfoViewModel.class);
        initView();
    }

    private void initView() {
        setTitle(getString(R.string.a400_maintenance_machine_info_title));
        showModules();
    }

    private void showModules() {
        mViewModel.getModuleListObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::showDetectedModules, LogHelper::log);

        mViewModel.getFdmToolheadObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshFdmToolheadStatus, LogHelper::log);

        mViewModel.getLaserToolheadObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshLaserToolheadStatus, LogHelper::log);

        mViewModel.getLaserSafetyInfoObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(safetyInfo -> {
                    mMiivLaserEmitterTemp.setContent(safetyInfo.getTubeTemperature() + "℃");
                    mMiivLaser20wEmitterTemp.setContent(safetyInfo.getTubeTemperature() + "℃");
                    mMiivLaser40wEmitterTemp.setContent(safetyInfo.getTubeTemperature() + "℃");
                    mMiivLaser2wEmitterTemp.setContent(safetyInfo.getTubeTemperature() + "℃");
                }, LogHelper::log);

        mViewModel.getLaserCameraOnlineObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(online -> mMiiv10WLaserCameraConnection.setContent(online ? getString(R.string.all_on) : getString(R.string.all_off)));

        mViewModel.getCncToolheadObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshCncToolheadStatus, LogHelper::log);

        mViewModel.getDryerObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshDryerStatus, LogHelper::log);

        mViewModel.getAirPurifierObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshAirPurifierStatus, LogHelper::log);

        mViewModel.getEnclosureObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshEnclosureStatus, LogHelper::log);

        mViewModel.getLinearLimitObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshLinearStatus, LogHelper::log);

        mViewModel.getLaserFireSensorSensitivityObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> {
                    String valueContent = String.valueOf(value);
                    mMiiv20wFireSensorSensitivity.setContent(valueContent);
                    mMiiv40wFireSensorSensitivity.setContent(valueContent);
                });
    }

    private void refreshLinearStatus(List<LinearLimit> linearLimits) {
        for (LinearLimit limit : linearLimits) {
            switch (limit.getIndex()) {
                case 0:
                    mLivModuleX.setAttributeContent(limit.getTrigger() ? getString(R.string.all_on) : getString(R.string.all_off));
                    break;
                case 1:
                    mLivModuleY1.setAttributeContent(limit.getTrigger() ? getString(R.string.all_on) : getString(R.string.all_off));
                    break;
                case 2:
                    mLivModuleZ1.setAttributeContent(limit.getTrigger() ? getString(R.string.all_on) : getString(R.string.all_off));
                    break;
                case 4:
                    mLivModuleY2.setAttributeContent(limit.getTrigger() ? getString(R.string.all_on) : getString(R.string.all_off));
                    break;
                case 5:
                    mLivModuleZ2.setAttributeContent(limit.getTrigger() ? getString(R.string.all_on) : getString(R.string.all_off));
                    break;
            }
        }
    }

    private void refreshEnclosureStatus(Enclosure.EnclosureStatus status) {
        mMiivEnclosureLed.setContent(status.getLedValue() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        mMiivEnclosureFan.setContent(status.getFanSpeed() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        mMiivEnclosureDoor.setContent(status.isDoorOpen() ? getString(R.string.all_open) : getString(R.string.all_closed));
    }

    private void refreshAirPurifierStatus(AirPurifier.AirPurifierStatus status) {
        mMiivPurifierFan.setContent(status.getFanSpeedLevel() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        mMiivPurifierCover.setContent(getString(R.string.all_open));
        mMiivPurifierFilterDetection.setContent(getString(R.string.all_on));
    }

    private void refreshDryerStatus(DryBox.DryBoxInfo info) {
        mMiivDryerTemp.setContent(info.getDryBoxStatus().getTempCurrentChamber() + "℃");
        mMiivDryerRH.setContent(info.getDryBoxStatus().getCurrentHumidity() + "%");
        mMiivDryerIsDrying.setContent(info.getDryBoxStatus().getDryState() == 1 ? getString(R.string.all_on) : getString(R.string.all_off));
    }

    private void refreshCncToolheadStatus(CNCToolhead.CNCToolheadInfo info) {
        mMiivCncSpeed.setContent(info.getCurrentSpeed() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
    }

    private void refreshLaserToolheadStatus(LaserToolhead.LaserToolheadInfo info) {
//        Logger.t(TAG).d("Laser tube info: %s", info);
        mMiivLaserPower.setContent(info.getLaserTube().getCurrentPower() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        mMiivLaser20wPower.setContent(info.getLaserTube().getCurrentPower() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        mMiivLaser40wPower.setContent(info.getLaserTube().getCurrentPower() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        mMiivLaser2wPower.setContent(info.getLaserTube().getCurrentPower() +  requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        mMiivOrientationDetect.setContent(getString(R.string.all_on));
        mMiiv20wOrientationDetect.setContent(getString(R.string.all_on));
        mMiiv40wOrientationDetect.setContent(getString(R.string.all_on));
        mMiiv2wOrientationDetect.setContent(getString(R.string.all_on));
        mMiiv10WLaserCameraConnection.setContent(getString(R.string.all_online));
    }

    private void refreshFdmToolheadStatus(FdmToolhead.FdmToolheadStatus status) {
        // TODO: 2022/7/7 fan should get with type
        Logger.t(TAG).d("FDM status: %s", status);
        if (mLlModuleDualExtrusion.getVisibility() == View.VISIBLE) {
            mMiivNozzleTempL.setContent(status.getExtruderList().get(0).getTemperature() + "℃");
            mMiivNozzleTempR.setContent(status.getExtruderList().get(1).getTemperature() + "℃");
            mMiivFilamentDetectL.setContent(status.getExtruderList().get(0).getFilamentStatus() ? getString(R.string.all_off) : getString(R.string.all_on));
            mMiivFilamentDetectR.setContent(status.getExtruderList().get(1).getFilamentStatus() ? getString(R.string.all_off) : getString(R.string.all_on));
            mMiivCoolingL.setContent(status.getFanList().get(0).getSpeedLevel() + requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
            mMiivCoolingR.setContent(status.getFanList().get(1).getSpeedLevel() + requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
            mMiivHeatDissipationDual.setContent(status.getFanList().get(2).getSpeedLevel() + requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
        } else if (mLlModuleSingleExtrusion.getVisibility() == View.VISIBLE) {
            mMiivSingleNozzleTemp.setContent(status.getExtruderList().get(0).getTemperature() + "℃");
            mMiivFilamentDetect.setContent(status.getExtruderList().get(0).getFilamentDetectionStatus() == 1 ? getString(R.string.all_on) : getString(R.string.all_off));
            mMiivCooling.setContent(status.getFanList().get(0).getSpeedLevel() + requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
            if (status.getFanList().size() > 1) {
                mMiivDissipationSingle.setContent(status.getFanList().get(1).getSpeedLevel() + requireContext().getString(R.string.a400_bracket_has_date, "PWM"));
            }
        }
    }

    private void showDetectedModules(List<Module> modules) {
        for (Module module : modules) {
            switch (module.getModuleInfo().getModuleId()) {
                case HEAD_3DP:
                    show3dpSingleStats();
                    break;
                case HEAD_3DP_DOUBLE_EXTRUDER:
                    show3dpDualStats();
                    break;
                case HEAD_LASER:
                    show1600mWLaserStats();
                    break;
                case HEAD_LASER_10W:
                    show10WLaserStats();
                    break;
                case HEAD_LASER_20W:
                    show20WLaserStats();
                    break;
                case HEAD_LASER_40W:
                    show40WLaserStats();
                    break;
                case HEAD_LASER_2W_INFRARED:
                    show2WLaserStats();
                    break;
                case HEAD_CNC:
                case HEAD_CNC_200W:
                    showCncStats();
                    break;
                case ROTARY_MODULE:
                    mLlModuleRotary.setVisibility(View.VISIBLE);
                    break;
                case ADDON_DRY_BOX:
                    showDryerStats();
                    break;
                case ADDON_ENCLOSURE:
                case ADDON_ENCLOSURE_A400:
                    showEnclosureStats();
                    break;
                case ADDON_AIR_PURIFIER:
                    showAirPurifierStats();
                    break;
                case LINEAR_A400:
                    handleLinearModuleVisibility(module.getModuleInfo().getModuleIndex());
                    break;
            }
        }
    }

    private void showAirPurifierStats() {
        mLlModulePurifier.setVisibility(View.VISIBLE);
        mMiivPurifierFan.setTitle(R.string.all_fan_speed);
        mMiivPurifierCover.setTitle(R.string.all_cover);
        mMiivPurifierFilterDetection.setTitle(R.string.all_filter_detection_title);
    }

    private void showEnclosureStats() {
        mLlModuleEnclosure.setVisibility(View.VISIBLE);
        mMiivEnclosureLed.setTitle(R.string.all_led_strip_title);
        mMiivEnclosureFan.setTitle(R.string.all_exhaust_fan_title);
        mMiivEnclosureDoor.setTitle(R.string.all_enclosure_door_title);
    }

    private void showDryerStats() {
        mLlModuleDryer.setVisibility(View.VISIBLE);
        mMiivDryerTemp.setTitle(R.string.all_temperature_title);
        mMiivDryerRH.setTitle(R.string.all_rh_title);
        mMiivDryerFan.setTitle(R.string.a400_machine_info_heat_circulation_fan_title);
        mMiivDryerIsDrying.setTitle(R.string.a400_machine_info_is_drying_title);
    }

    private void showCncStats() {
        mLlModuleCNC.setVisibility(View.VISIBLE);
        mMiivCncSpeed.setTitle(R.string.a400_machine_info_spindle_speed_title);
    }

    private void show1600mWLaserStats() {
        mLlModuleLaser1600mw.setVisibility(View.VISIBLE);
        mMiivLaserPower.setTitle(R.string.a400_machine_info_laser_power_title);
        mMiivOrientationDetect.setTitle(R.string.a400_machine_info_orientation_detect_title);
        mMiivLaserEmitterTemp.setTitle(R.string.a400_machine_info_laser_emitter_temp_title);
        mMiiv10WLaserCameraConnection.setTitle(R.string.a400_machine_info_camera_connection_title);
    }

    private void show10WLaserStats() {
        mLlModuleLaser10w.setVisibility(View.VISIBLE);
        mMiivLaserPower.setTitle(R.string.a400_machine_info_laser_power_title);
        mMiivOrientationDetect.setTitle(R.string.a400_machine_info_orientation_detect_title);
        mMiivLaserEmitterTemp.setTitle(R.string.a400_machine_info_laser_emitter_temp_title);
        mMiiv10WLaserCameraConnection.setTitle(R.string.a400_machine_info_camera_connection_title);
    }

    private void show20WLaserStats() {
        mLlModuleLaser20w.setVisibility(View.VISIBLE);
        mMiivLaser20wPower.setTitle(R.string.a400_machine_info_laser_power_title);
        mMiiv20wOrientationDetect.setTitle(R.string.a400_machine_info_orientation_detect_title);
        mMiivLaser20wEmitterTemp.setTitle(R.string.a400_machine_info_laser_emitter_temp_title);
        mMiiv20wFireSensorSensitivity.setTitle(R.string.a400_machine_info_fire_sensor_title);
    }

    private void show40WLaserStats() {
        mLlModuleLaser40w.setVisibility(View.VISIBLE);
        mMiivLaser40wPower.setTitle(R.string.a400_machine_info_laser_power_title);
        mMiiv40wOrientationDetect.setTitle(R.string.a400_machine_info_orientation_detect_title);
        mMiivLaser40wEmitterTemp.setTitle(R.string.a400_machine_info_laser_emitter_temp_title);
        mMiiv40wFireSensorSensitivity.setTitle(R.string.a400_machine_info_fire_sensor_title);
    }

    private void show2WLaserStats() {
        mLlModuleLaser2w.setVisibility(View.VISIBLE);
        mMiivLaser2wPower.setTitle(R.string.a400_machine_info_laser_power_title);
        mMiiv2wOrientationDetect.setTitle(R.string.a400_machine_info_orientation_detect_title);
        mMiivLaser2wEmitterTemp.setTitle(R.string.a400_machine_info_laser_emitter_temp_title);
    }

    private void show3dpSingleStats() {
        mLlModuleSingleExtrusion.setVisibility(View.VISIBLE);
        mMiivSingleNozzleTemp.setTitle(R.string.a400_machine_info_nozzle_temp_title);
        mMiivFilamentDetect.setTitle(R.string.a400_machine_info_filament_detect_title);
        mMiivCooling.setTitle(R.string.a400_machine_info_part_cooling_fan_title);
        mMiivDissipationSingle.setTitle(R.string.a400_machine_info_heat_dissipation_fan);
    }

    private void show3dpDualStats() {
        mLlModuleDualExtrusion.setVisibility(View.VISIBLE);
        mMiivNozzleTempL.setTitle(R.string.a400_machine_info_left_nozzle_temp_title);
        mMiivNozzleTempR.setTitle(R.string.a400_machine_info_right_nozzle_temp_title);
        mMiivFilamentDetectL.setTitle(R.string.a400_machine_info_filament_detect_l_title);
        mMiivFilamentDetectR.setTitle(R.string.a400_machine_info_filament_detect_r_title);
        mMiivCoolingL.setTitle(R.string.a400_machine_info_left_part_cooling_fan_title);
        mMiivCoolingR.setTitle(R.string.a400_machine_info_right_part_cooling_fan_title);
        mMiivHeatDissipationDual.setTitle(getString(R.string.a400_machine_info_heat_dissipation_fan));
    }

    private void handleLinearModuleVisibility(int moduleIndex) {
        switch (moduleIndex) {
            case 0:
                mLivModuleX.setVisibility(View.VISIBLE);
                mLivModuleX.setLinearTitle(R.string.all_linear_module_x_title);
                break;
            case 1:
                mLivModuleY1.setVisibility(View.VISIBLE);
                mLivModuleY1.setLinearTitle(R.string.all_linear_module_y1_title);
                break;
            case 2:
                mLivModuleZ1.setVisibility(View.VISIBLE);
                mLivModuleZ1.setLinearTitle(R.string.all_linear_module_z1_title);
                break;
            case 4:
                mLivModuleY2.setVisibility(View.VISIBLE);
                mLivModuleY2.setLinearTitle(R.string.all_linear_module_y2_title);
            case 5:
                mLivModuleZ2.setVisibility(View.VISIBLE);
                mLivModuleZ2.setLinearTitle(R.string.all_linear_module_z2_title);
                break;
        }
    }
}
