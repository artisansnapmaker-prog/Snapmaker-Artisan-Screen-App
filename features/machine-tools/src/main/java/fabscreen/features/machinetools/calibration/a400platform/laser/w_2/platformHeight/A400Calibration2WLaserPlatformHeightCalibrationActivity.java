package fabscreen.features.machinetools.calibration.a400platform.laser.w_2.platformHeight;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.calibration.a400platform.laser.w_40.platformHeight.A400Calibration40WLaserPlatformHeightCompleteFragment;
import fabscreen.features.machinetools.calibration.a400platform.laser.w_40.platformHeight.A400Calibration40WLaserPlatformHeightFragment;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.view.BaseActivity;

@Route(path = RoutePath.TOOLS_CALIBRATION_A400_LASER_2W_PLATFORM_HEIGHT_INFO)
public class A400Calibration2WLaserPlatformHeightCalibrationActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);
        }

        initRootFragment();
    }

    public void initRootFragment() {
        addFragment(R.id.fragment_container, A400Calibration2WLaserPlatformHeightFragment.newInstance());
    }

    public void gotoComplete() {
        addFragment(R.id.fragment_container, A400Calibration2WLaserPlatformHeightCompleteFragment.newInstance());
    }

}
