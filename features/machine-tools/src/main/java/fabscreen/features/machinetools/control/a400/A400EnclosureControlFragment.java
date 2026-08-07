package fabscreen.features.machinetools.control.a400;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400EnclosureControlFragment extends BaseFragment {

    @BindView(R2.id.btn_fan)
    Button mAbFan;
    @BindView(R2.id.btn_led_strip)
    Button mAbLed;
    @BindView(R2.id.tv_door_status)
    TextView mTvDoorStatus;
    @BindView(R2.id.tv_led_title)
    TextView mTvLedTitle;
    @BindView(R2.id.tv_fan_title)
    TextView mTvFanTitle;
    private A400EnclosureControlViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400EnclosureControlFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
        mViewModel.subscribeEnclosureStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.unSubscribeEnclosureStatus();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        mViewModel.getEnclosureStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(info -> {
                    int status = info.getStatus();
                    mAbLed.setEnabled(status == 2);
                    mAbFan.setEnabled(status == 2);
                    mAbLed.setActivated(info.isLedOn());
                    mAbFan.setActivated(info.isFanOn());
                    mTvLedTitle.setTextColor(ContextCompat.getColor(requireContext(),
                            info.isLedOn() ? R.color.palette_white_pure : R.color.palette_grey_dim));
                    mTvFanTitle.setTextColor(ContextCompat.getColor(requireContext(),
                            info.isFanOn() ? R.color.palette_white_pure : R.color.palette_grey_dim));
//                    mTvDoorStatus.setText(info.isDoorOpen() ? "外罩门状态：开" : "外罩门状态：关");
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_control_enclosure;
    }

    @OnClick(R2.id.btn_led_strip)
    void onClickLed() {
        if (!mAbLed.isPressed()) return;
        playNormalClickSound();
        boolean isLedOn = mViewModel.isEnclosureLedOn();
        mAbLed.setActivated(!isLedOn);
        mTvLedTitle.setTextColor(ContextCompat.getColor(requireContext(),
                !isLedOn ? R.color.palette_white_pure : R.color.palette_grey_dim));
        // We define level 0 means close the led, 1-100 means open the led (Max value 100).
        mViewModel.setLedLevel(isLedOn ? 0 : 100)
                .as(bindToLifecycle())
                .subscribe(ret -> {/**/}, LogHelper::log);
    }

    @OnClick(R2.id.btn_fan)
    void onClickFan() {
        if (!mAbFan.isPressed()) return;
        playNormalClickSound();
        boolean isFanOn = mViewModel.isEnclosureFanOn();
        mAbFan.setActivated(!isFanOn);
        mTvFanTitle.setTextColor(ContextCompat.getColor(requireContext(),
                !isFanOn ? R.color.palette_white_pure : R.color.palette_grey_dim));
        // We define level 0 means close the fan, 1-100 means open the fan (Max value 100).
        mViewModel.setFanLevel(isFanOn ? 0 : 100)
                .as(bindToLifecycle())
                .subscribe(ret -> {/**/}, LogHelper::log);
    }

    @OnClick(R2.id.btn_more_settings)
    void onClickMoreSettings() {
        playNormalClickSound();
        ((A400ControlActivity) requireActivity()).goToEnclosureSettings();
    }

    @Override
    protected A400EnclosureControlViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(A400EnclosureControlViewModel.class);
    }
}
