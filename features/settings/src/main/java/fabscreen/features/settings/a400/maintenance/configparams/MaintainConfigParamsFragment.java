package fabscreen.features.settings.a400.maintenance.configparams;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_20W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_2W_INFRARED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_40W;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.helper.EditTextHelper;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@SuppressLint("NonConstantResourceId")
public class MaintainConfigParamsFragment extends BaseFragment {

    @BindView(R2.id.cv_single_single_initial_z)
    ConfigParamView mCvSingleSingleInitialZ;
    @BindView(R2.id.cv_single_dual_left_z)
    ConfigParamView mCvSingleDualLeftZ;
    @BindView(R2.id.cv_single_dual_right_z)
    ConfigParamView mCvSingleDualRightZ;
    @BindView(R2.id.cv_single_dual_x_offset)
    ConfigParamView mCvSingleDualXOffset;
    @BindView(R2.id.cv_single_dual_y_offset)
    ConfigParamView mCvSingleDualYOffset;
    @BindView(R2.id.cv_laser_focal_length)
    ConfigParamView mCvLaserFocalLength;
    @BindView(R2.id.cv_laser_platform_height)
    ConfigParamView mCvLaserPlatformHeight;
    @BindView(R2.id.cv_laser_4axis_center_height)
    ConfigParamView mCvLaser4AxisCenterHeight;
    @BindView(R2.id.cv_laser_fire_sensor_sensitivity)
    ConfigParamView mCvLaserFireSensorSensitivity;
    @BindView(R2.id.cv_laser_cross_line_indicator_x_offset)
    ConfigParamView mCvLaserCrossLineIndicatorXOffset;
    @BindView(R2.id.cv_laser_cross_line_indicator_y_offset)
    ConfigParamView mCvLaserCrossLineIndicatorYOffset;
    @BindView(R2.id.cv_laser_indicator_power)
    ConfigParamView mCvLaserIndicatorPower;

    private MaintainConfigParamsViewModel mViewModel;
    private CustomKeyboardUtil mCustomKeyboardUtil;

    public static Fragment newInstance() {
        return new MaintainConfigParamsFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_maintenance_config_params;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(MaintainConfigParamsViewModel.class);
        mCustomKeyboardUtil = new CustomKeyboardUtil(requireContext());
        initView();
    }

    private void initView() {
        switch (mViewModel.getWorkType()) {

            case FDM:
                setTitle(getString(R.string.a400_maintenance_config_params_fdm_title));
                break;
            case LASER:
                setTitle(getString(R.string.a400_maintenance_config_params_laser_title));
                break;
        }
        mCvSingleSingleInitialZ.setTitle(getString(R.string.a400_maintenance_config_single_extruder_initial_z_title));
        mCvSingleDualLeftZ.setTitle(getString(R.string.a400_maintenance_config_left_extruder_initial_z_title));
        mCvSingleDualRightZ.setTitle(getString(R.string.a400_maintenance_config_right_extruder_initial_z_title));
        mCvSingleDualXOffset.setTitle(getString(R.string.a400_maintenance_config_double_extruder_x_offset_title));
        mCvSingleDualYOffset.setTitle(getString(R.string.a400_maintenance_config_double_extruder_y_offset_title));
        mCvLaserFocalLength.setTitle(getString(R.string.a400_maintenance_config_laser_focal_len_title));
        mCvLaserPlatformHeight.setTitle(getString(R.string.a400_maintenance_config_platform_height_title));
        mCvLaser4AxisCenterHeight.setTitle(getString(R.string.a400_maintenance_config_rotary_central_axis_height_title));
        mCvLaserFireSensorSensitivity.setTitle(getString(R.string.a400_maintenance_config_fire_sensor_sensitivity_title));
        mCvLaserCrossLineIndicatorXOffset.setTitle(getString(R.string.a400_maintenance_config_indicator_x_offset_title));
        mCvLaserCrossLineIndicatorYOffset.setTitle(getString(R.string.a400_maintenance_config_indicator_y_offset_title));
        mCvLaserIndicatorPower.setTitle(getString(R.string.a400_maintenance_config_laser_indicator_power));

        switch (mViewModel.getHeadType()) {
            case HEAD_3DP:
                mCvSingleSingleInitialZ.setVisibility(View.VISIBLE);
                mCvSingleSingleInitialZ.setOnEditClickListener(() -> editInitialZOffset(mCvSingleSingleInitialZ, 0));
                mCustomKeyboardUtil.bindKeyboardListener(mCvSingleSingleInitialZ, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setZOffset(0, Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }

                    }
                });
                mViewModel.fetchZOffset();
                break;
            case HEAD_3DP_DOUBLE_EXTRUDER:
                mCvSingleDualLeftZ.setVisibility(View.VISIBLE);
                mCvSingleDualLeftZ.setOnEditClickListener(() -> editInitialZOffset(mCvSingleDualLeftZ, 0));
                mCustomKeyboardUtil.bindKeyboardListener(mCvSingleDualLeftZ, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setZOffset(0, Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchZOffset();
                mCvSingleDualRightZ.setVisibility(View.VISIBLE);
                mCvSingleDualRightZ.setOnEditClickListener(() -> editInitialZOffset(mCvSingleDualRightZ, 1));
                mCustomKeyboardUtil.bindKeyboardListener(mCvSingleDualRightZ, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setZOffset(1, Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchZOffset();
                mCvSingleDualXOffset.setVisibility(View.VISIBLE);
                mCvSingleDualXOffset.setOnEditClickListener(() -> editSingleDualX(mCvSingleDualXOffset));
                mCustomKeyboardUtil.bindKeyboardListener(mCvSingleDualXOffset, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setXOffset(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mCvSingleDualYOffset.setVisibility(View.VISIBLE);
                mCvSingleDualYOffset.setOnEditClickListener(() -> editSingleDualY(mCvSingleDualYOffset));
                mCustomKeyboardUtil.bindKeyboardListener(mCvSingleDualYOffset, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setYOffset(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchXYOffset();
                break;

            case HEAD_LASER:
            case HEAD_LASER_10W:
                mCvLaserFocalLength.setVisibility(View.VISIBLE);
                mCvLaserFocalLength.setOnEditClickListener(() -> editFocalLength(mCvLaserFocalLength));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserFocalLength, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setFocalLen(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchFocalLength();
                if (mViewModel.isRotary()) {
                    mCvLaser4AxisCenterHeight.setVisibility(View.VISIBLE);
                    mCvLaser4AxisCenterHeight.setOnEditClickListener(() -> edit4AxisCenterHeight(mCvLaser4AxisCenterHeight));
                    mCustomKeyboardUtil.bindKeyboardListener(mCvLaser4AxisCenterHeight, new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (!TextUtils.isEmpty(s.toString())) {
                                mViewModel.setRotaryCenterHeight(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                            }
                        }
                    });
                    mViewModel.fetch4AxisCenterHeight();
                } else {
                    mCvLaserPlatformHeight.setVisibility(View.VISIBLE);
                    mCvLaserPlatformHeight.setOnEditClickListener(() -> editPlatformHeight(mCvLaserPlatformHeight));
                    mCustomKeyboardUtil.bindKeyboardListener(mCvLaserPlatformHeight, new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (!TextUtils.isEmpty(s.toString())) {
                                mViewModel.setPlatformHeight(Float.parseFloat(s.toString()));
                            }
                        }
                    });
                    mViewModel.fetchPlatformHeight();
                }
                mCvLaserIndicatorPower.setVisibility(View.VISIBLE);
                mCvLaserIndicatorPower.setOnEditClickListener(() -> editLaserIndicatorPower(mCvLaserIndicatorPower));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserIndicatorPower, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            float value = Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString());
                            float minLimit = 0f;
                            int headType = mViewModel.getHeadType();
                            switch (headType) {
                                case Module.ModuleType.HEAD_LASER:
                                case Module.ModuleType.HEAD_LASER_10W:
                                    minLimit = 0.5f;
                                    break;
                                case Module.ModuleType.HEAD_LASER_20W:
                                case Module.ModuleType.HEAD_LASER_40W:
                                    minLimit = 0.2f;
                                    break;
                                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                                    minLimit = 0f;
                                    break;
                                default:
                                    break;
                            }
                            value = Math.max(minLimit, Math.min(value, 3f));
                            mViewModel.setLaserIndicatorPower(value)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .as(bindToLifecycle())
                                    .subscribe(response -> {
                                        if (response.isSuccess()) return;

                                        String msg = getString(R.string.a400_settings_set_laser_indicator_power_failed_default_msg_format, response.resultProp.getValue());
                                        if (response.isTimeOut()) {
                                            msg = getString(R.string.a400_settings_set_laser_indicator_power_failed_timeout_msg_format, response.resultProp.getValue());
                                        }
                                        DecisionDialog.create(requireContext()).setDialogStatus(1, true, false, true, false)
                                                .setPic(R.drawable.ic_pic_a400_error_112x112)
                                                .setTitle(R.string.a400_settings_set_laser_indicator_power_failed_title)
                                                .setContent(msg)
                                                .setFirstTv(R.string.all_confirm, R.color.select_dialog_red_txt, (dialog, which) -> {
                                                    dialog.dismiss();
                                                })
                                                .show();
                                    }, LogHelper::log);
                        }
                    }
                });
                mViewModel.fetchLaserIndicatorPower();
                break;
            case HEAD_LASER_20W:
            case HEAD_LASER_40W:
                mCvLaserFocalLength.setVisibility(View.VISIBLE);
                mCvLaserFocalLength.setOnEditClickListener(() -> editFocalLength(mCvLaserFocalLength));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserFocalLength, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setFocalLen(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchFocalLength();
                if (mViewModel.isRotary()) {
                    mCvLaser4AxisCenterHeight.setVisibility(View.VISIBLE);
                    mCvLaser4AxisCenterHeight.setOnEditClickListener(() -> edit4AxisCenterHeight(mCvLaser4AxisCenterHeight));
                    mCustomKeyboardUtil.bindKeyboardListener(mCvLaser4AxisCenterHeight, new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (!TextUtils.isEmpty(s.toString())) {
                                mViewModel.setRotaryCenterHeight(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                            }
                        }
                    });
                    mViewModel.fetch4AxisCenterHeight();
                } else {
                    mCvLaserPlatformHeight.setVisibility(View.VISIBLE);
                    mCvLaserPlatformHeight.setOnEditClickListener(() -> editPlatformHeight(mCvLaserPlatformHeight));
                    mCustomKeyboardUtil.bindKeyboardListener(mCvLaserPlatformHeight, new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (!TextUtils.isEmpty(s.toString())) {
                                mViewModel.setPlatformHeight(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                            }
                        }
                    });
                    mViewModel.fetchPlatformHeight();
                }
                mCvLaserFireSensorSensitivity.setVisibility(View.VISIBLE);
                mCvLaserFireSensorSensitivity.setOnEditClickListener(() -> editFireSensorSensitivity(mCvLaserFireSensorSensitivity));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserFireSensorSensitivity, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setFireSensorSensitivity(Integer.parseInt(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchFireSensorSensitivity();

                mCvLaserCrossLineIndicatorXOffset.setVisibility(View.VISIBLE);
                mCvLaserCrossLineIndicatorXOffset.setOnEditClickListener(() -> editCrossLineIndicatorXOffset(mCvLaserCrossLineIndicatorXOffset));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserCrossLineIndicatorXOffset, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setCrossLineIndicatorXOffset(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mCvLaserCrossLineIndicatorYOffset.setVisibility(View.VISIBLE);
                mCvLaserCrossLineIndicatorYOffset.setOnEditClickListener(() -> editCrossLineIndicatorYOffset(mCvLaserCrossLineIndicatorYOffset));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserCrossLineIndicatorYOffset, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setCrossLineIndicatorYOffset(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchCrossLineIndicatorOffset();

                mCvLaserIndicatorPower.setVisibility(View.VISIBLE);
                mCvLaserIndicatorPower.setOnEditClickListener(() -> editLaserIndicatorPower(mCvLaserIndicatorPower));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserIndicatorPower, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            float value = Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString());
                            float minLimit = 0f;
                            int headType = mViewModel.getHeadType();
                            switch (headType) {
                                case Module.ModuleType.HEAD_LASER:
                                case Module.ModuleType.HEAD_LASER_10W:
                                    minLimit = 0.5f;
                                    break;
                                case Module.ModuleType.HEAD_LASER_20W:
                                case Module.ModuleType.HEAD_LASER_40W:
                                    minLimit = 0.2f;
                                    break;
                                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                                    minLimit = 0f;
                                    break;
                                default:
                                    break;
                            }
                            value = Math.max(minLimit, Math.min(value, 3f));
                            mViewModel.setLaserIndicatorPower(value)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .as(bindToLifecycle())
                                    .subscribe(response -> {
                                        if (response.isSuccess()) return;

                                        String msg = getString(R.string.a400_settings_set_laser_indicator_power_failed_default_msg_format, response.resultProp.getValue());
                                        if (response.isTimeOut()) {
                                            msg = getString(R.string.a400_settings_set_laser_indicator_power_failed_timeout_msg_format, response.resultProp.getValue());
                                        }
                                        DecisionDialog.create(requireContext()).setDialogStatus(1, true, false, true, false)
                                                .setPic(R.drawable.ic_pic_a400_error_112x112)
                                                .setTitle(R.string.a400_settings_set_laser_indicator_power_failed_title)
                                                .setContent(msg)
                                                .setFirstTv(R.string.all_confirm, R.color.select_dialog_red_txt, (dialog, which) -> {
                                                    dialog.dismiss();
                                                })
                                                .show();
                                    }, LogHelper::log);
                        }
                    }
                });
                mViewModel.fetchLaserIndicatorPower();
                break;
            case HEAD_LASER_2W_INFRARED:
                if (mViewModel.isRotary()) {
                    mCvLaser4AxisCenterHeight.setVisibility(View.VISIBLE);
                    mCvLaser4AxisCenterHeight.setOnEditClickListener(() -> edit4AxisCenterHeight(mCvLaser4AxisCenterHeight));
                    mCustomKeyboardUtil.bindKeyboardListener(mCvLaser4AxisCenterHeight, new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (!TextUtils.isEmpty(s.toString())) {
                                mViewModel.setRotaryCenterHeight(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                            }
                        }
                    });
                    mViewModel.fetch4AxisCenterHeight();
                } else {
                    mCvLaserPlatformHeight.setVisibility(View.VISIBLE);
                    mCvLaserPlatformHeight.setOnEditClickListener(() -> editPlatformHeight(mCvLaserPlatformHeight));
                    mCustomKeyboardUtil.bindKeyboardListener(mCvLaserPlatformHeight, new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (!TextUtils.isEmpty(s.toString())) {
                                mViewModel.setPlatformHeight(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                            }
                        }
                    });
                    mViewModel.fetchPlatformHeight();
                }
                mCvLaserCrossLineIndicatorXOffset.setVisibility(View.VISIBLE);
                mCvLaserCrossLineIndicatorXOffset.setOnEditClickListener(() -> editCrossLineIndicatorXOffset(mCvLaserCrossLineIndicatorXOffset));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserCrossLineIndicatorXOffset, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setCrossLineIndicatorXOffset(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mCvLaserCrossLineIndicatorYOffset.setVisibility(View.VISIBLE);
                mCvLaserCrossLineIndicatorYOffset.setOnEditClickListener(() -> editCrossLineIndicatorYOffset(mCvLaserCrossLineIndicatorYOffset));
                mCustomKeyboardUtil.bindKeyboardListener(mCvLaserCrossLineIndicatorYOffset, new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            mViewModel.setCrossLineIndicatorYOffset(Float.parseFloat(EditTextHelper.fixNumberInputSinglePoint(s).toString()));
                        }
                    }
                });
                mViewModel.fetchCrossLineIndicatorOffset();
            default:
                break;
        }

        observeValues();
    }

    private void observeValues() {
        mViewModel.getZOffset0Observable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> {
                    mCvSingleSingleInitialZ.setValue(value);
                    mCvSingleDualLeftZ.setValue(value);
                }, LogHelper::log);

        mViewModel.getZOffset1Observable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvSingleDualRightZ.setValue(value), LogHelper::log);

        mViewModel.getXOffsetObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvSingleDualXOffset.setValue(value), LogHelper::log);

        mViewModel.getYOffsetObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvSingleDualYOffset.setValue(value), LogHelper::log);

        mViewModel.getFocalLenObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvLaserFocalLength.setValue(value), LogHelper::log);

        mViewModel.getPlatformHeightObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvLaserPlatformHeight.setValue(value), LogHelper::log);

        mViewModel.getRotaryCenterHeightObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvLaser4AxisCenterHeight.setValue(value), LogHelper::log);

        mViewModel.getFireSensorSensorSensitivityObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvLaserFireSensorSensitivity.setValue(value), LogHelper::log);

        mViewModel.getCrossLineIndicatorOffsetXObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvLaserCrossLineIndicatorXOffset.setValue(value), LogHelper::log);

        mViewModel.getCrossLineIndicatorOffsetYObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvLaserCrossLineIndicatorYOffset.setValue(value), LogHelper::log);

        mViewModel.getLaserIndicatorPowerObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> mCvLaserIndicatorPower.setValue(value), LogHelper::log);
    }

    private void edit4AxisCenterHeight(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getCurrentRotaryCenterHeight());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editPlatformHeight(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getCurrentPlatformHeight());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editFocalLength(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getCurrentFocalLength());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editFireSensorSensitivity(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getFireSensorSensitivity());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
    }

    private void editCrossLineIndicatorXOffset(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getCrossLineIndicatorXOffset());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editCrossLineIndicatorYOffset(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getCrossLineIndicatorYOffset());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editLaserIndicatorPower(View v) {
        String indicatorPowerValue = mViewModel.getLaserIndicatorPower();
        mCustomKeyboardUtil.setMaxLength(4);
        mCustomKeyboardUtil.setPreInputText(String.valueOf(indicatorPowerValue));
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editSingleDualY(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getCurrentYOffset());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editSingleDualX(View v) {
        mCustomKeyboardUtil.setPreInputText(mViewModel.getCurrentXOffset());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }

    private void editInitialZOffset(View v, int index) {
        mCustomKeyboardUtil.setPreInputText(String.valueOf(mViewModel.getCurrentZOffset(index)));
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    }
}
