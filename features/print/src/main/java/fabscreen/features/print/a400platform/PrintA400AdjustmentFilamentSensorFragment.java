package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.entity.toolhead.FdmToolhead;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintA400AdjustmentFilamentSensorFragment extends BaseFragment {
    @BindView(R2.id.tv_print_setting_name)
    TextView mTvSettingName;

    @BindView(R2.id.switch_filament_sensor)
    SwitchCompat mSwFilamentRunoutDetection;

    public static Fragment newInstance() {
        return new PrintA400AdjustmentFilamentSensorFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        mTvSettingName.setText(R.string.a400_print_print_setting_filament_sensor_title);
        // As controller end-point advised, filament sensor detection will be enabled/disabled when one of the extruder was set.
        // So request only one of the extruder was recommended.
        mSwFilamentRunoutDetection.setChecked(isRunoutRecoveryEnabled(0));

        mSwFilamentRunoutDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            playSwitchSound();
            setRunoutRecoveryEnabled(0, isChecked);
        });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print_adjustment_filament_sensor;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    public boolean isRunoutRecoveryEnabled(int extruderIndex) {
        FdmToolhead.FdmToolheadStatus fdmToolheadStatus = ServiceContainer.getInstance().getService(IMachine.class)
                .getFDMController()
                .getToolheadStatusSubjectHolder()
                .getValue();
        int filamentDetectionStatus = fdmToolheadStatus.getExtruderList().get(extruderIndex).getFilamentDetectionStatus();
        return filamentDetectionStatus == 0;
    }

    public void setRunoutRecoveryEnabled(int index, boolean isChecked) {
        ServiceContainer.getInstance().getService(IMachine.class)
                .getFDMController()
                .setFilamentSensorStatus(0, index, isChecked ? 0 : 1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                }, LogHelper::log);
    }
}
