package fabscreen.features.welcome.a400;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.features.welcome.R;
import fabscreen.features.welcome.R2;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.core.ui.common.WelcomeWifiPasswordFragment;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;
import fabscreen.platform.lib.LogHelper;

@Route(path = RoutePath.WELCOME_A400)
public class A400WelcomeActivity extends BaseActivity {
    @BindView(R2.id.iv_welcome_hello)
    VideoPlayerIJK mVpVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a400_welcome);
        ButterKnife.bind(this);
        initView();
        startLanguageFragment();
    }

    private void initView() {
        mVpVideo.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/hello.webm");
        mVpVideo.setLooping(true);
        if (ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getNeedQueryMachineError()) {
            ServiceContainer.getInstance().getService(IMachine.class).getErrorController().queryException().as(bindToLifecycle()).subscribe(responseStructure -> {
            }, LogHelper::log);
            ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setNeedQueryMachineError(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVpVideo.setLooping(false);
        mVpVideo.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVpVideo.setLooping(true);
        mVpVideo.start();
    }

    /**
     * Language Page 1, Language
     */
    public void startLanguageFragment() {
        replaceFragment(R.id.fcv_welcome, A400WelcomeLanguageFragment.newInstance());
    }

    /**
     * Welcome page 2, hello
     */
    public void startHelloFragment() {
        replaceFragment(R.id.fcv_welcome, A400HelloFragment.newInstance());
    }


    /**
     * Welcome page 3, name
     */
    public void startNameFragment() {
        replaceFragment(R.id.fcv_welcome, A400WelcomeNameFragment.newInstance());
    }

    /**
     * Welcome page 4, Wi-Fi
     */
    public void startWiFiFragment() {
        addFragment(R.id.fcv_welcome, A400WelcomeWiFiListFragment.newInstance());
    }

    /**
     * Welcome page 5, Wi-Fi password
     */
    public void startPasswordFragment() {
        addFragment(R.id.fcv_welcome, WelcomeWifiPasswordFragment.newInstance());
    }

    /**
     * Welcome page , terms
     * <p>
     * User should agree terms and conditions to use this app.
     */
    public void startTermsFragment() {
        addFragment(R.id.fcv_welcome, A400WelcomeTermsFragment.newInstance());
    }


    public void goToGuide() {
        finish();
        mRouter.routeToGuideMilestone().start(this);
    }
}
