package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.features.print.a400platform.viewmodel.A400AirPurifierControlViewModel;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 1. Power on/off;
 * 2. Tune fan speed;
 * 3. Display filter life.
 */
public class PrintA400AdjustmentAirPurifierFragment extends BaseFragment {

    @BindView(R2.id.ab_purifier_power)
    Button mAbPower;
    //    @BindView(R2.id.sbg_fan_speed)
//    SegmentedButtonGroup mSbgFanSpeed;
    @BindView(R2.id.tab_layout)
    TabLayout mTlFanSpeed;
    boolean isChangeFanSpeed;
    @BindView(R2.id.tv_no_power_bg)
    TextView mTvNotPower;
    @BindView(R2.id.ll_no_power_tip)
    LinearLayout mLlNotPower;
    @BindView(R2.id.tv_air_purifier_switch_title)
    TextView mTvSwitchTitle;
    @BindView(R2.id.tv_print_setting_name)
    TextView mTvName;
    private A400AirPurifierControlViewModel mViewModel;
    private String[] mTabs;

    public static Fragment newInstance() {
        return new PrintA400AdjustmentAirPurifierFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
    }

    private void initView() {
        mTvName.setText(R.string.a400_print_print_setting_air_purifier);
        setLinTabValue();
        mTlFanSpeed.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
//                if (!mTlFanSpeed.isPressed()) return;
                playSwitchSound();
                isChangeFanSpeed = true;
                mViewModel.setPurifierFanSpeed(tab.getPosition() + 1)
                        .flatMap(responseStructure -> responseStructure.isSuccess() ? Observable.timer(2, TimeUnit.SECONDS).flatMap(delay -> Observable.just(responseStructure)) : Observable.just(responseStructure))
                        .doOnError(e -> isChangeFanSpeed = false)
                        .as(bindToLifecycle())
                        .subscribe(result -> isChangeFanSpeed = false, LogHelper::log);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mViewModel.getPowerStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isPowerOn -> {
                    mAbPower.setEnabled(isPowerOn);
                    mLlNotPower.setVisibility(isPowerOn ? View.GONE : View.VISIBLE);
                    mTvNotPower.setVisibility(isPowerOn ? View.GONE : View.VISIBLE);
                    mTvSwitchTitle.setTextColor(ContextCompat.getColor(requireContext(), isPowerOn ? R.color.palette_white_pure : R.color.palette_grey_dim));
                }, LogHelper::log);

        mViewModel.getFanOnOffObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isFanOn -> {
                    mTvSwitchTitle.setText(isFanOn ? R.string.a400_print_settings_air_purifier_turn_off_title : R.string.a400_print_settings_air_purifier_turn_on_title);
                    mAbPower.setActivated(isFanOn);
                }, LogHelper::log);
        mViewModel.getFanSpeedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(level -> {
                    if (!isChangeFanSpeed) {
                        mTlFanSpeed.selectTab(level == 0 ? mTlFanSpeed.getTabAt(1) : mTlFanSpeed.getTabAt(level - 1));
                    }
                }, LogHelper::log);

    }

    public void setLinTabValue() {
        mTabs = new String[]{getString(R.string.all_low), getString(R.string.all_medium), getString(R.string.all_High)};
        if (mTlFanSpeed.getTabCount() > 0) {
            for (int i = 0; i < mTlFanSpeed.getTabCount(); i++) {
                mTlFanSpeed.getTabAt(i).setText(mTabs[i]);
            }
        } else {
            for (int i = 0; i < mTabs.length; i++) {
                mTlFanSpeed.addTab(mTlFanSpeed.newTab().setText(mTabs[i]));
            }
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print_adjustment_air_purifier;
    }

    @Override
    protected A400AirPurifierControlViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(A400AirPurifierControlViewModel.class);
    }

    @OnClick(R2.id.ab_purifier_power)
    void onPurifierPowerClicked() {
        playNormalClickSound();
        mViewModel.switchPurifierPower();
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.subscribePurifierStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.unsubscribePurifierStatus();
    }
}
