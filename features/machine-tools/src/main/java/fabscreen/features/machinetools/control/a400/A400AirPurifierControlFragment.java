package fabscreen.features.machinetools.control.a400;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.control.common.S30AirPurifierControlViewModel;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 1. Power on/off;
 * 2. Tune fan speed;
 * 3. Display filter life.
 */
public class A400AirPurifierControlFragment extends BaseFragment {

    @BindView(R2.id.ab_purifier_power)
    Button mAbPower;
    //    @BindView(R2.id.sbg_fan_speed)
//    SegmentedButtonGroup mSbgFanSpeed;
    @BindView(R2.id.tab_layout)
    TabLayout mTlFanSpeed;
    @BindView(R2.id.pb_lifetime)
    ProgressBar mPbLifeTime;
    @BindView(R2.id.tv_no_power_bg)
    TextView mTvNotPower;
    @BindView(R2.id.ll_no_power_tip)
    LinearLayout mLlNotPower;
    boolean isChangeFanSpeed;
    @BindView(R2.id.tv_air_purifier_switch_title)
    TextView mTvSwitchTitle;
    private S30AirPurifierControlViewModel mViewModel;
    private String[] mTabs;

    public static Fragment newInstance() {
        return new A400AirPurifierControlFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
    }

    private void initView() {
        mPbLifeTime.setMax(100);
        setLinTabValue();

        mTlFanSpeed.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
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
                    mTvSwitchTitle.setText(isFanOn ? R.string.a400_control_air_purifier_turn_off_title : R.string.a400_control_air_purifier_turn_on_title);
                    mAbPower.setActivated(isFanOn);
                }, LogHelper::log);

        mViewModel.getFanSpeedObservable()
                .filter(level -> !isChangeFanSpeed)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(level -> {
                    mTlFanSpeed.selectTab(level == 0 ? mTlFanSpeed.getTabAt(1) : mTlFanSpeed.getTabAt(level - 1));
                }, LogHelper::log);

        mViewModel.getFilterLifeObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(life -> mPbLifeTime.setProgress(life == 2 ? 100 : (life + 1) * 33), LogHelper::log);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_control_air_purifier;
    }

    @Override
    protected S30AirPurifierControlViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(S30AirPurifierControlViewModel.class);
    }

    @OnClick(R2.id.btn_more_settings)
    void onMoreSettingsClick() {
        playNormalClickSound();
        ((A400ControlActivity) requireActivity()).goToAirPurifierSettings();
    }

    @OnClick(R2.id.ab_purifier_power)
    void onPurifierPowerClicked() {
        if (!mAbPower.isPressed()) return;
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

    public void setLinTabValue() {
        mTabs = new String[]{getString(R.string.all_low)
                , getString(R.string.all_medium), getString(R.string.all_High)};
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
}
