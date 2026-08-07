package com.snapmaker.s30;

import fabscreen.platform.base.BaseApplication;
import fabscreen.platform.base.BuildConfig;

public class S30Application extends BaseApplication {
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
        return null;
    }

    // we can init special Service in different apps


    @Override
    public void initServiceContainer() {
        super.initServiceContainer();
    }

    @Override
    public void initAppService() {
        super.initAppService();
    }

    @Override
    public void initBaseServices() {
        super.initBaseServices();
    }
}
