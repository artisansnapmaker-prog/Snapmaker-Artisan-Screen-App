package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.features.print.a400platform.viewmodel.A400EnclosureControlViewModel;
import fabscreen.platform.base.view.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintA400AdjustmentEnclosureControlFragment extends BaseFragment {

    @BindView(R2.id.btn_fan)
    Button mAbFan;
    @BindView(R2.id.btn_led_strip)
    Button mAbLed;
    @BindView(R2.id.tv_door_status)
    TextView mTvDoorStatus;
    @BindView(R2.id.tv_print_setting_name)
    TextView mTvName;
    private A400EnclosureControlViewModel mViewModel;

    public static Fragment newInstance() {
        return new PrintA400AdjustmentEnclosureControlFragment();
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
        mTvName.setText(R.string.a400_print_print_setting_enclosure);

        mViewModel.getEnclosureStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(info -> {
                    int status = info.getStatus();
                    mAbLed.setEnabled(status == 2);
                    mAbFan.setEnabled(status == 2);
                    mAbLed.setActivated(info.isLedOn());
                    mAbFan.setActivated(info.isFanOn());
//                    mTvDoorStatus.setText(info.isDoorOpen() ? getString(R.string.print_adjust_enclosure_on) : getString(R.string.print_adjust_enclosure_off));
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print_adjustment_enclosure;
    }

    @OnClick(R2.id.btn_led_strip)
    public void onClickLed() {
        playNormalClickSound();
        boolean isLedOn = mViewModel.isEnclosureLedOn();
        // We define level 0 means close the led, 1-100 means open the led (Max value 100).
        mViewModel.setLedLevel(isLedOn ? 0 : 100);
    }

    @OnClick(R2.id.btn_fan)
    public void onClickFan() {
        playNormalClickSound();
        boolean isFanOn = mViewModel.isEnclosureFanOn();

        // We define level 0 means close the fan, 1-100 means open the fan (Max value 100).
        mViewModel.setFanLevel(isFanOn ? 0 : 100);
    }

    @Override
    protected A400EnclosureControlViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(A400EnclosureControlViewModel.class);
    }
}
