package fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingBed.auto;


import android.os.Bundle;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.machinetools.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.view.BaseActivity;

@Route(path = RoutePath.TOOLS_CALIBRATION_A400_3DP_LEVELING_BED_AUTO)
public class A400LevelingBedCalibrationAutoActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        gotoBedHeating();
    }

    public void gotoBedHeating() {
        addFragment(R.id.fragment_container, A400LevelingBedCalibrationBedHeatingFragment.newInstance());
    }

    public void gotoLevelingBedCalibrationAuto() {
        replaceFragment(R.id.fragment_container, A400LevelingBedCalibrationAutoFragment.newInstance());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
