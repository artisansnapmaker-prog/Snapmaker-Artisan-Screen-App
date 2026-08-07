package fabscreen.features.guide.j1;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.guide.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.view.BaseActivity;

@Route(path = RoutePath.GUIDE_j1)
public class J1GuideActivity extends BaseActivity {
    private IPreferences.Helper helper;
    private boolean isFirstWizard = false;
    private boolean isSafetyGlass = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        helper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();
        isFirstWizard = true;
        isSafetyGlass = true;
    }


    public void initInfoFragment() {
        addFragment(R.id.fragment_container, GuideJ1InfoFragment.newInstance());
    }

    public void initSafetyGlassFragment() {
        addFragment(R.id.fragment_container, GuideJ1SafetyGlassFragment.newInstance());
    }

    public void initGuideJ1TemperatureSelfCheckFragment() {
        addFragment(R.id.fragment_container, GuideJ1TemperatureSelfCheckFragment.newInstance());
    }

    public void initGuideJ1Success() {
        addFragment(R.id.fragment_container, GuideJ1SuccessfullyFragment.newInstance());
    }

    public void checkNext() {
        if (isFirstWizard) {
            isFirstWizard = false;
            initInfoFragment();
        } else if (isSafetyGlass) {
            isSafetyGlass = false;
            initSafetyGlassFragment();
        }
//        else if (!helper.getGuideTemperatureSelfCheck()) {
//            initGuideJ1TemperatureSelfCheckFragment();
//        }
        else if (!helper.getGuideCalibration()) {
            ServiceContainer.getInstance().getService(IRouter.class).routeToCalibrationPage(true).start(this);
        } else {
            helper.setGuideTemperatureSelfCheck(false);
            helper.setGuideCalibration(false);
            helper.setMachineSetup3DP(true);
            initGuideJ1Success();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNext();
    }
}
