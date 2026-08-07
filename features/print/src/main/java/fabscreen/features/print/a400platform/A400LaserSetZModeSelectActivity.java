package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.print.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.view.BaseActivity;

@Route(path = RoutePath.PRINT_LASER_SET_Z_SELECT)
public class A400LaserSetZModeSelectActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        FrameLayout mFlContainer = findViewById(R.id.fragment_container);
        mFlContainer.setBackgroundResource(R.color.palette_black_transparent_20);
        Fragment fragment = A400LaserSetZModeSelectFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.push_alpha_out);
    }
}
