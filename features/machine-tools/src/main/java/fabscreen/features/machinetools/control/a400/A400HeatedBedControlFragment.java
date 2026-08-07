package fabscreen.features.machinetools.control.a400;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.control.a400.viewmodel.A400HeatedBedViewModel;
import fabscreen.platform.base.service.machine.entity.module.HeatedBed;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.RotateButtonView;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400HeatedBedControlFragment extends BaseFragment {

    @BindView(R2.id.tv_switch_whole)
    TextView mTvSwitchWhole;
    @BindView(R2.id.tv_switch_inner)
    TextView mTvSwitchInner;

    @BindView(R2.id.tv_target_temp_heated_bed)
    TextView mTvZone0TargetTemp;
    @BindView(R2.id.tv_cur_temp_heated_bed)
    TextView mTvZone0CurTemp;

    @BindView(R2.id.rbv_control_heated_bed)
    RotateButtonView mCasHeatedBed;
    @BindView(R2.id.btn_heated_bed_control_switch)
    Button mHeatedBedHeating;

    private A400HeatedBedViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400HeatedBedControlFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(A400HeatedBedViewModel.class);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_control_heated_bed;
    }

    private void initView() {
        mViewModel.geZoneStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(heatedBedStatus -> {
                    HeatedBed.ZoneInfo zoneInfo = heatedBedStatus.getZoneList().get(0);
                    float currentTemperature = zoneInfo.getCurrentTemperature();
                    int targetTemperature = zoneInfo.getTargetTemperature();
                    mTvZone0TargetTemp.setText(String.format(Locale.getDefault(), "%d", targetTemperature));
                    mTvZone0CurTemp.setText(String.format(Locale.getDefault(), "%d", (int) currentTemperature));
                    mCasHeatedBed.setColor1Progress(targetTemperature);
                    mCasHeatedBed.setColor2Progress(currentTemperature);
                    mHeatedBedHeating.setText(targetTemperature != 0 ?
                            R.string.a400_3dp_filament_action_stop_heating : R.string.a400_3dp_filament_action_heat);
                    mHeatedBedHeating.setBackgroundResource(targetTemperature != 0 ? R.drawable.pic_a400_cnc_on_bg : R.drawable.pic_a400_cnc_off_bg);

                    int workMode = heatedBedStatus.getWorkMode();
                    if (workMode == HeatedBed.HeatedBedStatus.HEATED_BED_STATUS_WORK_MODE_WHOLE) {
                        mTvSwitchWhole.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_gradient_background_m);
                        mTvSwitchInner.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_background_m);
                        mCasHeatedBed.setMax(mViewModel.A400_BED_WHOLE_MODE_MAX_VALUE);
                    } else {
                        mTvSwitchWhole.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_background_m);
                        mTvSwitchInner.setBackgroundResource(R.drawable.a400_bg_rounded_rectangle_gradient_background_m);
                        mCasHeatedBed.setMax(mViewModel.A400_BED_INNER_MODE_MAX_VALUE);
                    }
                }, LogHelper::log);

        mCasHeatedBed.setMin(mViewModel.A400_BED_ALL_MODE_MIN_VALUE);
        mCasHeatedBed.setMax(mViewModel.A400_BED_WHOLE_MODE_MAX_VALUE);
        mCasHeatedBed.setIncrementalInterval(5);
        mCasHeatedBed.setCrollerChangeListener(new RotateButtonView.OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(RotateButtonView croller, float progress) {
                mViewModel.setTargetChange(-1, (int) progress);
            }

            @Override
            public void onStartTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setTargetChange(-1, (int) progress);
            }

            @Override
            public void onStopTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setTargetChange(-1, (int) progress);
            }
        });
    }

    @OnClick(R2.id.btn_heated_bed_control_switch)
    void onSwitch0Heating() {
        playNormalClickSound();
        boolean isHeating = mViewModel.geZoneStatevable().getZoneList().get(0).getTargetTemperature() != 0;
        if (isHeating) {
            mViewModel.setTargetChange(-1, 0);
        } else {
            mViewModel.setTargetChange(-1, 60);
        }
    }

    @OnClick({R2.id.tv_switch_whole, R2.id.tv_switch_inner})
    void onClickSwitch(View view) {
        playNormalClickSound();
        int id = view.getId();
        if (id == R.id.tv_switch_whole) {
            mViewModel.setHeatedBedWorkMode(HeatedBed.HeatedBedStatus.HEATED_BED_STATUS_WORK_MODE_WHOLE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        /**/
                    }, LogHelper::log);
        } else if (id == R.id.tv_switch_inner) {
            mViewModel.setHeatedBedWorkMode(HeatedBed.HeatedBedStatus.HEATED_BED_STATUS_WORK_MODE_INNER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        /**/
                    }, LogHelper::log);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.subscribeTemperatureChange();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.unSubscribeTemperatureChange();
    }
}
