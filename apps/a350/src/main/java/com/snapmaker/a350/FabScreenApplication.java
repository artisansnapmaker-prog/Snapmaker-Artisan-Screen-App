package com.snapmaker.a350;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.orhanobut.logger.Logger;

import fabscreen.platform.base.BaseApplication;
import fabscreen.platform.base.helper.DPCHelper;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.MultiLanguageManager;
import fabscreen.platform.lib.BuildConfig;
import fabscreen.platform.lib.LogHelper;

public class FabScreenApplication extends BaseApplication {
    @Override
    public String getAppVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getBuildTime() {
        return BuildConfig.BUILD_RELEASE_DATE;
    }

    @Override
    public String getSerialPortPath() {
        return "/dev/ttyHSL1";
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            // In simulators, we need an active device owner to enter Kiosk mode
            if (!DPCHelper.isDeviceOwner(this)) {
                DPCHelper.becomeDeviceOwner(this);
            }
        }

        Logger.i("Application started!");

        // Touchscreen of Snapmaker 2.0 comes with SDK 25 (N_MR1)
        Logger.d("Build SDK version: %d", Build.VERSION.SDK_INT);

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = packageInfo.versionName;
            Logger.d("FabScreen version: %s", version);
        } catch (PackageManager.NameNotFoundException e) {
            LogHelper.log(e);
        }

        // Reload resources if we change locales/languages.
        int language = ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getUserSelectedLanguage();
        MultiLanguageManager.applyApplicationLanguage(getBaseContext(), MultiLanguageManager.language2Locale(language));

//        mFactory = new ViewModelProviderFactory(getModel());

//        startServices();

    }

}
