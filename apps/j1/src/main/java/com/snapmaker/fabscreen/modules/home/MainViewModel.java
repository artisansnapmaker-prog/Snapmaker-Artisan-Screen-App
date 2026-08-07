package com.snapmaker.fabscreen.modules.home;

import java.io.File;

import fabscreen.platform.base.BaseMainViewModel;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;

public class MainViewModel extends BaseMainViewModel {
    @Override
    public File getEMBinFile() {
        return null;
    }

    @Override
    protected Observable<Boolean> checkModuleVersions() {
        // J1 don't care module version
        return Observable.just(true);
    }

    @Override
    protected long getOccupiedSpaceInMegaByte() {
        return 0;
    }

    @Override
    public boolean needGoToGuide() {
        // TODO: 2022/7/29 not implemented
        return false;
    }

    @Override
    public boolean needGoWelcome() {
        // TODO: 2022/7/29 not implemented
        return false;
    }

    @Override
    public boolean isRotaryAvailable() {
        return false;
    }

    @Override
    public String getProductSerialNumber() {
        return null;
    }

    @Override
    public CompletableSource requestScope() {
        return super.requestScope();
    }
}
