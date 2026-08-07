package fabscreen.features.machinetools.control.a400;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.control.a400.viewmodel.A400FilamentControlViewModel;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.core.ui.view.RotateButtonView;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400FilamentControlFragment extends BaseFragment {
    private static final int A400_EXTRUDER_MIN_VALUE = 160;
    private static final int A400_DOUBLE_EXTRUDER_MAX_VALUE = 300;
    private static final int A400_EXTRUDER_MAX_VALUE = 275;
    protected A400FilamentControlViewModel mViewModel;
    @BindView(R2.id.li_a400_control_filament)
    LinearLayout mLiSwitchFilament;
    @BindView(R2.id.tv_switch_l)
    TextView mTvSwitchL;
    @BindView(R2.id.tv_switch_r)
    TextView mTvSwitchR;
    @BindView(R2.id.rl_a400_control_filament_left)
    RelativeLayout mRLFilamentLeft;
    @BindView(R2.id.rbv_control_filament_left)
    RotateButtonView mRbvFilamentLeft;
    @BindView(R2.id.btn_l_hotend_control_switch)
    Button mBtnControlSwitchL;
    @BindView(R2.id.tv_cur_temp_l)
    TextView mTvCurTempL;
    @BindView(R2.id.tv_target_temp_l)
    TextView mTvTarTempL;
    @BindView(R2.id.btn_filament_out_l)
    Button mBtnFilamentUnloadL;
    @BindView(R2.id.tv_filament_out_l)
    TextView mTvFilamentUnloadL;
    @BindView(R2.id.btn_filament_in_l)
    Button mBtnFilamentLoadL;
    @BindView(R2.id.tv_filament_in_l)
    TextView mTvFilamentLoadL;
    @BindView(R2.id.rl_a400_control_filament_right)
    RelativeLayout mRLFilamentRight;
    @BindView(R2.id.rbv_control_filament_right)
    RotateButtonView mRbvFilamentRight;
    @BindView(R2.id.btn_r_hotend_control_switch)
    Button mBtnControlSwitchR;
    @BindView(R2.id.tv_cur_temp_r)
    TextView mTvCurTempR;
    @BindView(R2.id.tv_target_temp_r)
    TextView mTvTarTempR;
    @BindView(R2.id.btn_filament_out_r)
    Button mBtnFilamentUnloadR;
    @BindView(R2.id.tv_filament_out_r)
    TextView mTvFilamentUnloadR;
    @BindView(R2.id.btn_filament_in_r)
    Button mBtnFilamentLoadR;
    @BindView(R2.id.tv_filament_in_r)
    TextView mTvFilamentLoadR;
    @BindView(R2.id.btn_filament_out_select)
    Button mBtnFilamentUnloadSelect;
    @BindView(R2.id.tv_filament_out_select)
    TextView mTvFilamentUnloadSelect;
    @BindView(R2.id.btn_filament_in_select)
    Button mBtnFilamentLoadSelect;
    @BindView(R2.id.tv_filament_in_select)
    TextView mTvFilamentLoadSelect;

    FileParsingDialog mPdSwitchExtruder;
    private boolean isFilamentMove;

    public static Fragment newInstance() {
        return new A400FilamentControlFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(A400FilamentControlViewModel.class);
        initView();
    }

    private void initView() {
        mPdSwitchExtruder = FileParsingDialog.create(requireContext());
        mPdSwitchExtruder.setContent(R.string.a400_control_switching_extruder);

        if (mViewModel.isDoubleExtruder()) {
            initLeftFilamentView();
            initRightFilamentView();
            mViewModel.getActivateNozzle()
                    .distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(i -> {
                        switch (i) {
                            case 0:
                                mTvSwitchL.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_gradient_background_m);
                                mTvSwitchR.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_background_m);
                                mRLFilamentLeft.setVisibility(View.VISIBLE);
                                mRLFilamentRight.setVisibility(View.INVISIBLE);
                                break;
                            case 1:
                                mTvSwitchL.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_background_m);
                                mTvSwitchR.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_gradient_background_m);
                                mRLFilamentLeft.setVisibility(View.INVISIBLE);
                                mRLFilamentRight.setVisibility(View.VISIBLE);
                                break;
                            case -1:
                            default:
                                mTvSwitchL.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_background_m);
                                mTvSwitchR.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_background_m);
                                mRLFilamentLeft.setVisibility(View.INVISIBLE);
                                mRLFilamentRight.setVisibility(View.INVISIBLE);
                                break;
                        }
                    });
        } else {
            mLiSwitchFilament.setVisibility(View.INVISIBLE);
            initLeftFilamentView();
        }
    }

    private void initLeftFilamentView() {
        mRbvFilamentLeft.setMin(A400_EXTRUDER_MIN_VALUE);
        mRbvFilamentLeft.setMax(mViewModel.isDoubleExtruder() ? A400_DOUBLE_EXTRUDER_MAX_VALUE : A400_EXTRUDER_MAX_VALUE);
        mRbvFilamentLeft.setIncrementalInterval(5);
        mViewModel.getToolheadStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolheadStatus -> {
                    Extruder extruder = fdmToolheadStatus.getExtruderList().get(0);
                    float targetTemperature = extruder.getTargetTemperature();
                    float temperature = extruder.getTemperature();
                    mTvCurTempL.setText(String.format(Locale.ENGLISH, "%3d", (int) temperature));
                    mTvTarTempL.setText(String.format(Locale.ENGLISH, "%3d", (int) targetTemperature));
                    mRbvFilamentLeft.setColor1Progress(targetTemperature);
                    mRbvFilamentLeft.setColor2Progress(temperature);
                    mBtnControlSwitchL.setText(targetTemperature != 0 ?
                            R.string.a400_3dp_filament_action_stop_heating : R.string.a400_3dp_filament_action_heat);
                    mBtnControlSwitchL.setBackgroundResource(targetTemperature != 0 ? R.drawable.pic_a400_cnc_on_bg : R.drawable.pic_a400_cnc_off_bg);
                    boolean filamentState = targetTemperature != 0 && temperature >= targetTemperature - mViewModel.A400_DEFAULT_TEMPERATURE_FLUCTUATION && temperature <= targetTemperature + mViewModel.A400_DEFAULT_TEMPERATURE_FLUCTUATION;
                    setFilamentState(0, filamentState);
                }, LogHelper::log);
        mRbvFilamentLeft.setCrollerChangeListener(new RotateButtonView.OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(RotateButtonView croller, float progress) {
                mViewModel.setTargetTemp(0, (int) progress);
            }

            @Override
            public void onStartTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setTargetTemp(0, (int) progress);
            }

            @Override
            public void onStopTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setTargetTemp(0, (int) progress);
            }
        });
    }

    private void initRightFilamentView() {
        mRbvFilamentRight.setMin(A400_EXTRUDER_MIN_VALUE);
        mRbvFilamentRight.setMax(mViewModel.isDoubleExtruder() ? A400_DOUBLE_EXTRUDER_MAX_VALUE : A400_EXTRUDER_MAX_VALUE);
        mRbvFilamentRight.setIncrementalInterval(5);
        mViewModel.getToolheadStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolheadStatus -> {
                    Extruder extruder = fdmToolheadStatus.getExtruderList().get(1);
                    float targetTemperature = extruder.getTargetTemperature();
                    float temperature = extruder.getTemperature();
                    mTvCurTempR.setText(String.format(Locale.ENGLISH, "%3d", (int) temperature));
                    mTvTarTempR.setText(String.format(Locale.ENGLISH, "%3d", (int) targetTemperature));
                    mRbvFilamentRight.setColor1Progress(targetTemperature);
                    mRbvFilamentRight.setColor2Progress(temperature);
                    mBtnControlSwitchR.setText(targetTemperature != 0 ?
                            R.string.a400_3dp_filament_action_stop_heating : R.string.a400_3dp_filament_action_heat);
                    mBtnControlSwitchR.setBackgroundResource(targetTemperature != 0 ? R.drawable.pic_a400_cnc_on_bg : R.drawable.pic_a400_cnc_off_bg);
                    boolean filamentState = targetTemperature != 0 && temperature >= targetTemperature - mViewModel.A400_DEFAULT_TEMPERATURE_FLUCTUATION && temperature <= targetTemperature + mViewModel.A400_DEFAULT_TEMPERATURE_FLUCTUATION;
                    setFilamentState(1, filamentState);
                }, LogHelper::log);
        mRbvFilamentRight.setCrollerChangeListener(new RotateButtonView.OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(RotateButtonView croller, float progress) {
                mViewModel.setTargetTemp(1, (int) progress);
            }

            @Override
            public void onStartTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setTargetTemp(1, (int) progress);
            }

            @Override
            public void onStopTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setTargetTemp(1, (int) progress);
            }
        });
    }

    @OnClick(R2.id.tv_switch_l)
    public void checkSwitchL() {
        if (mViewModel.getNowActivateNozzle() == 0) return;
        playNormalClickSound();
        mPdSwitchExtruder.show();
        Logger.d("Switching extruder L...");
        mViewModel.switchExtruder(0, 0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    mPdSwitchExtruder.dismiss();
                }, e -> {
                    mPdSwitchExtruder.dismiss();
                    LogHelper.log(e);
                });
    }

    @OnClick(R2.id.tv_switch_r)
    public void checkSwitchR() {
        if (mViewModel.getNowActivateNozzle() == 1) return;
        playNormalClickSound();
        mPdSwitchExtruder.show();
        Logger.d("Switching extruder R...");
        mViewModel.switchExtruder(0, 1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    mPdSwitchExtruder.dismiss();
                }, e -> {
                    mPdSwitchExtruder.dismiss();
                    LogHelper.log(e);
                });
    }


    @OnClick(R2.id.btn_l_hotend_control_switch)
    public void checkHeatingL() {
        playNormalClickSound();
        boolean isHeating = mViewModel.getToolheadStatusValue().getExtruderList().get(0).getTargetTemperature() != 0;
        if (isHeating) {
            mViewModel.setTargetTemp(0, 0);
        } else {
            mViewModel.setTargetTemp(0, mViewModel.A400_DEFAULT_HEATING);
        }
    }

    @OnClick(R2.id.btn_r_hotend_control_switch)
    public void checkHeatingR() {
        playNormalClickSound();
        boolean isHeating = mViewModel.getToolheadStatusValue().getExtruderList().get(1).getTargetTemperature() != 0;
        if (isHeating) {
            mViewModel.setTargetTemp(1, 0);
        } else {
            mViewModel.setTargetTemp(1, mViewModel.A400_DEFAULT_HEATING);
        }
    }

    @OnClick({R2.id.btn_filament_out_r, R2.id.btn_filament_out_l})
    protected void onFilamentRUnloadClicked() {
        playNormalClickSound();
        setFilamentState(false, true);
        mBtnFilamentUnloadSelect.setVisibility(View.VISIBLE);
        mTvFilamentUnloadSelect.setVisibility(View.VISIBLE);
        mViewModel.FilamentMove(false)
                .as(bindToLifecycle())
                .subscribe(structure -> {
                    mBtnFilamentUnloadSelect.setVisibility(View.INVISIBLE);
                    mTvFilamentUnloadSelect.setVisibility(View.INVISIBLE);
                    setFilamentState(true, false);
                    if (structure.isSuccess()) {
                        Logger.d("Filament unloaded.");
                    } else {
                        Logger.d("Filament unload fail.");
                    }
                }, log -> {
                    setFilamentState(true, false);
                    LogHelper.log(log);
                });
    }

    @OnClick({R2.id.btn_filament_in_r, R2.id.btn_filament_in_l})
    protected void onFilamentRLoadClicked() {
        playNormalClickSound();
        setFilamentState(false, true);
        mBtnFilamentLoadSelect.setVisibility(View.VISIBLE);
        mTvFilamentLoadSelect.setVisibility(View.VISIBLE);
        mViewModel.FilamentMove(true)
                .as(bindToLifecycle())
                .subscribe(structure -> {
                    mBtnFilamentLoadSelect.setVisibility(View.INVISIBLE);
                    mTvFilamentLoadSelect.setVisibility(View.INVISIBLE);
                    setFilamentState(true, false);
                    if (structure.isSuccess()) {
                        Logger.d("Filament Loaded.");
                    } else {
                        Logger.d("Filament load fail.");
                    }
                }, log -> {
                    setFilamentState(true, false);
                    LogHelper.log(log);
                });
    }

    @OnClick(R2.id.view_control_move_to_proper_position)
    void onClick() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.pic_toolhead_run_boundary)
                .setType(DecisionDialog.TIP_TYPE)
                .setTitle(requireContext().getString(R.string.a400_settings_replace_module_machine_move_title))
                .setContent(R.string.a400_settings_replace_module_machine_move_desc)
                .setSecondTv(requireContext().getString(R.string.all_confirm),
                        R.color.select_dialog_yellow_txt, (dialog, which) -> {
                            dialog.dismiss();
                            // show loading dialog
                            WarmTipDialog movingDialog = WarmTipDialog.create(requireContext())
                                    .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                                    .setPic(R.drawable.ic_block_setup)
                                    .setTitle(R.string.all_move_show)
                                    .setContent(R.string.all_move_show_content);
                            movingDialog.show();
                            mViewModel.moveToProperPosition()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .as(bindToLifecycle())
                                    .subscribe(result -> {
                                        movingDialog.dismiss();
                                    }, e -> {
                                        movingDialog.dismiss();
                                        LogHelper.log(e);
                                    });
                        }).
                setFirstTv(requireContext().getString(R.string.all_cancel),
                        R.color.select_dialog_white_txt, (dialog, which) -> {
                            dialog.dismiss();
                        })
                .show();
    }

    private void setFilamentState(int index, boolean enable) {
        if (isFilamentMove) return;
        if (index == 0) {
            mTvFilamentUnloadL.setEnabled(enable);
            mBtnFilamentUnloadL.setEnabled(enable);
            mBtnFilamentLoadL.setEnabled(enable);
            mTvFilamentLoadL.setEnabled(enable);
        } else if (index == 1) {
            mTvFilamentUnloadR.setEnabled(enable);
            mBtnFilamentUnloadR.setEnabled(enable);
            mBtnFilamentLoadR.setEnabled(enable);
            mTvFilamentLoadR.setEnabled(enable);
        } else {
            mTvFilamentUnloadL.setEnabled(enable);
            mTvFilamentLoadL.setEnabled(enable);
            mTvFilamentUnloadR.setEnabled(enable);
            mTvFilamentLoadR.setEnabled(enable);
            mBtnFilamentUnloadL.setEnabled(enable);
            mBtnFilamentUnloadR.setEnabled(enable);
            mBtnFilamentLoadL.setEnabled(enable);
            mBtnFilamentLoadR.setEnabled(enable);
        }
    }

    private void setFilamentState(boolean enable, boolean isMove) {
        setFilamentState(-1, enable);
        isFilamentMove = isMove;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_control_filament;
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.subscribeDataChange();
        Observable<ResponseStructure> requestActivatedExtrusion = mViewModel.getRequestActivatedExtrusion();
        if (requestActivatedExtrusion == null) {
            setFilamentState(true, false);
            mBtnFilamentUnloadSelect.setVisibility(View.INVISIBLE);
            mTvFilamentUnloadSelect.setVisibility(View.INVISIBLE);
            mBtnFilamentLoadSelect.setVisibility(View.INVISIBLE);
            mTvFilamentLoadSelect.setVisibility(View.INVISIBLE);
        } else {
            setFilamentState(false, true);
            requestActivatedExtrusion
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(structure -> {
                        mBtnFilamentUnloadSelect.setVisibility(View.INVISIBLE);
                        mTvFilamentUnloadSelect.setVisibility(View.INVISIBLE);
                        mBtnFilamentLoadSelect.setVisibility(View.INVISIBLE);
                        mTvFilamentLoadSelect.setVisibility(View.INVISIBLE);
                        setFilamentState(true, false);
                        if (structure.isSuccess()) {
                            Logger.d("Filament unloaded.");
                        } else {
                            Logger.d("Filament unload fail.");
                        }
                    }, log -> {
                        mBtnFilamentUnloadSelect.setVisibility(View.INVISIBLE);
                        mTvFilamentUnloadSelect.setVisibility(View.INVISIBLE);
                        mBtnFilamentLoadSelect.setVisibility(View.INVISIBLE);
                        mTvFilamentLoadSelect.setVisibility(View.INVISIBLE);
                        setFilamentState(true, false);
                        LogHelper.log(log);
                    });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.unSubscribeDataChange();
    }
}
