package com.snapmaker.fabscreen.modules.home;

import com.orhanobut.logger.Logger;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.VersionResponse;
import fabscreen.platform.base.lib.api.ApiClient;
import fabscreen.platform.base.lib.api.ApiObserver;
import fabscreen.platform.base.lib.update.UpdateFileParser;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseViewModel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class HomeViewModel extends BaseViewModel {
    private final IPreferences.Helper mPrefHelper;
    private final BehaviorSubject<Boolean> mUpdateSubj = BehaviorSubject.create();

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
                        UpdateFileParser.cacheVersionInfoToDisk(response.data.new_version);
                        String newVersion = response.data.new_version.version;
                        Logger.d("new version is %s", newVersion);
                        if (!newVersion.equals(mPrefHelper.getLastUpdatePackageVersion())) {
                            Logger.d("New firmware available " + response.data.new_version.version);
                            mUpdateSubj.onNext(true);
//                            router.routeToUpdateDialog().start(getAppContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                    }
                });
    }

    public Observable<Boolean> getUpdateObservable() {
        return mUpdateSubj.hide();
    }
}
