package fabscreen.features.settings.a400.ota;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.settings.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.view.BaseActivity;

@Route(path = RoutePath.OTA_TEST)
public class A400OTATestActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        startOTATestFragment();
    }

    void startOTATestFragment() {
        Fragment fragment = A400SettingsOTATestFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }
}
