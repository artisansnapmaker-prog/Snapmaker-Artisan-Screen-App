package fabscreen.features.guide.a400;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.orhanobut.logger.Logger;

import fabscreen.features.guide.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.lib.LogHelper;

@Route(path = RoutePath.GUIDE_A400_MILESTONE)
public class A400GuideMilestoneActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        addFragment(R.id.fragment_container, A400GuideMilestoneFragment.newInstance());

        if (ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getNeedQueryMachineError()) {
            ServiceContainer.getInstance().getService(IMachine.class).getErrorController().queryException().as(bindToLifecycle()).subscribe(responseStructure -> {
            }, LogHelper::log);
            ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setNeedQueryMachineError(false);
        }
    }

    public void goHomePage() {
        mRouter.backHome().start(this);
    }

    public void goToFilamentSetupForResult(int requestCode) {
        mRouter.routeToFilamentSetup().startForResult(this, requestCode);
    }

    public void goToZCalibrationSetupForResult(int requestCode) {
        mRouter.routeToZCalibrationSetup().startForResult(this, requestCode);
    }

    public void goToXYCalibrationSetupForResult(int requestCode) {
        mRouter.routeToXYCalibrationSetup().startForResult(this, requestCode);
    }

    public void goToHeatedBedLevelingSetupForResult(int requestCode) {
        mRouter.routeToHeatedBedLevelingSetup().startForResult(this, requestCode);
    }

    public void goToSingleSingleFilamentSetupForResult(int requestCode) {
        mRouter.routeToSingleSingleFilamentSetup().startForResult(this, requestCode);
    }

    public void goToSetupIntroForResult(int requestCode, Bundle bundle) {
        mRouter.routeToSetupIntro(bundle).startForResult(this, requestCode);
    }

    public void goToCNCSetupForResult(int requestCode) {
        mRouter.routeToGuideCNC().startForResult(this, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        A400GuideMilestoneFragment fragment = (A400GuideMilestoneFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) return;
        switch (resultCode) {
            case RESULT_OK:
                fragment.onMilestoneAchieved(requestCode);
                break;
            case RESULT_CANCELED:
                Logger.d("on result canceled");
                fragment.onMilestoneRewound(requestCode - 1);
                break;
        }

    }

}
