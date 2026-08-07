package com.snapmaker.fabscreena400.module.home;

import com.orhanobut.logger.Logger;

import fabscreen.platform.base.FabException;
import fabscreen.platform.base.helper.SemVerHelper;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.VersionResponse;
import fabscreen.platform.base.lib.api.ApiClient;
import fabscreen.platform.base.lib.api.ApiObserver;
import fabscreen.platform.base.lib.update.UpdateFileParser;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class HomeViewModel extends BaseViewModel {
    private final IPreferences.Helper mPrefHelper;
    private final BehaviorSubject<String> mUpdateSubj = BehaviorSubject.create();

    public HomeViewModel() {
        mPrefHelper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();
        getLatestVersion();
    }

    private void getLatestVersion() {
        ApiClient client = new ApiClient(mPrefHelper.getApiHost());
        // FIXME: 2022/5/17 wifi may not connected?
        client.getLatestVersion()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(new ApiObserver<VersionResponse>() {
                    @Override
                    protected void onSuccess(VersionResponse response) {
                        if (response.code != 0) {
                            Logger.w("API server response not ok: %s", response.code);
                            return;
                        }
                        // Artisan_VX.Y.Z_YYYYMMDD
                        UpdateFileParser.cacheVersionInfoToDisk(response.data.new_version);

                        boolean newFirmwareAvailable = false;
                        String[] versionStringSplit;

                        String parseNewFirmwareVersion = null;
                        versionStringSplit = response.data.new_version.version.split("_");
                        if (versionStringSplit.length > 1) {
                            parseNewFirmwareVersion = versionStringSplit[1];
                            Logger.d("new version is %s", parseNewFirmwareVersion);
                        } else {
                            // Could not recognize this version name.
                            Logger.d("New version name %s", response.data.new_version);
                            // End silently.
                        }

                        // Get last version from preference.
                        String parseLastFirmwareVersion = null;
                        String lastPackageVersion = mPrefHelper.getLastUpdatePackageVersion();
                        if (lastPackageVersion.isEmpty()) {
                            // Assume that default value means firmware could be updated.
                            newFirmwareAvailable = true;
                        } else {
                            // Try parse the version and compare if new firmware is available(using SemVerHelper).
                            versionStringSplit = lastPackageVersion.split("_");
                            if (versionStringSplit.length > 1) {
                                parseLastFirmwareVersion = versionStringSplit[1];

                                // Try comparing version.
                                try {
                                    newFirmwareAvailable = parseNewFirmwareVersion != null && SemVerHelper.lt(parseLastFirmwareVersion, parseNewFirmwareVersion);
                                } catch (FabException e) {
                                    LogHelper.log(e);
                                }
                            } else {
                                // Could not recognize this version name.
                                // End silently.
                            }
                        }
                        if (newFirmwareAvailable) {
                            mUpdateSubj.onNext(response.data.new_version.version);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                    }
                });
    }

    public Observable<String> getUpdateObservable() {
        return mUpdateSubj.hide();
    }

    public void setLastCheckVersion(String version) {
        mPrefHelper.setLastCheckVersion(version);
    }
}
