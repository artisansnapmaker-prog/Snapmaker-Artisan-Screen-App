package fabscreen.features.settings.common;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.FabException;
import fabscreen.platform.base.helper.SemVerHelper;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.VersionChangeLog;
import fabscreen.platform.base.lib.VersionResponse;
import fabscreen.platform.base.lib.api.ApiClient;
import fabscreen.platform.base.lib.api.ApiObserver;
import fabscreen.platform.base.lib.update.UpdateFileParser;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IHttpDownloadManager;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class S30FirmwareUpdateViewModel extends BaseViewModel {

    private final BehaviorSubject<FirmwareDisplayStatus> mFirmStatusSubj = BehaviorSubject.createDefault(FirmwareDisplayStatus.CHECKING);
    // 0~100, progress; -1 fail
    private final BehaviorSubject<Integer> mDownloadProgressSubj = BehaviorSubject.create();
    private final BehaviorSubject<VersionInfo> mVersionInfoSubj = BehaviorSubject.create();

    private final IPreferences.Helper mPrefHelper;
    private final IAppService mAppService;
    private final IHttpDownloadManager mDownloadManager;
    private String mUrl;

    public S30FirmwareUpdateViewModel() {
        mPrefHelper = getServiceContainer().getService(IPreferences.class).getHelper();
        mAppService = getServiceContainer().getService(IAppService.class);
        mDownloadManager = getServiceContainer().getService(IHttpDownloadManager.class);
        checkOnline();
        observeDownload();
    }

    private void observeDownload() {
        mDownloadManager.getProgressObservable()
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    if (progress == -1) {
                        if (mFirmStatusSubj.getValue() == FirmwareDisplayStatus.DOWNLOADING) {
                            mFirmStatusSubj.onNext(FirmwareDisplayStatus.DOWNLOAD_FAIL);
                        }
                    } else if (progress == -2) {
                        // canceled
                        if (mFirmStatusSubj.getValue() == FirmwareDisplayStatus.DOWNLOADING) {
                            mFirmStatusSubj.onNext(FirmwareDisplayStatus.TO_BE_DOWNLOADED);
                        }
                    } else if (progress == 100) {
                        delayNotifyFinish();
                    } else {
                        if (mFirmStatusSubj.getValue() == FirmwareDisplayStatus.TO_BE_DOWNLOADED) {
                            mFirmStatusSubj.onNext(FirmwareDisplayStatus.DOWNLOADING);
                        }
                        if (mVersionInfoSubj.getValue() != null) {
                            mDownloadProgressSubj.onNext(progress);
                        }
                    }
                }, LogHelper::log);
    }

    private void delayNotifyFinish() {
        Observable.timer(2, TimeUnit.SECONDS)
                .as(bindToLifecycle())
                .subscribe(aLong -> {
                    if (mFirmStatusSubj.getValue() == FirmwareDisplayStatus.DOWNLOADING) {
                        mFirmStatusSubj.onNext(FirmwareDisplayStatus.DOWNLOADED);
                    }
                });
    }

    public Observable<VersionInfo> getNewVersionInfoObservable() {
        return mVersionInfoSubj.hide();
    }

    public int getVersionSizeInMegabyte() {
        return mVersionInfoSubj.getValue().fileSize;
    }

    public MachineStatus getMachineStatusValue() {
        return getServiceContainer().getService(IMachine.class).getMachineStatusSubjectHolder().getValue();
    }

    public void stopWork() {
        try {
            getServiceContainer().getService(IMachine.class).getNewPrintController().stop();
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

    public void exitCalibration() {
        try {
            Observable<ResponseStructure> responseStructureObservable = null;
            IMachine.WorkType workType = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType;
            switch (workType) {
                case FDM:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getFDMController().exitCalibration(false);
                    break;
                case LASER:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getLaserController().exitCalibration(false);
                    break;
                case CNC:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getCNCController().exitCalibration(false);
                    break;
            }
            if (responseStructureObservable == null) return;
            responseStructureObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (!success.isSuccess()) {
                            Logger.d("Exit Calibration: " + success);
                        }
                    }, LogHelper::log);
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

    public Observable<MachineStatus> getMachineStatusObservable() {
        return getServiceContainer().getService(IMachine.class).getMachineStatusSubjectHolder().getObservable();
    }

    public enum FirmwareDisplayStatus {
        // Checking for updates via internet.
        CHECKING,
        // Checking didn't get result because of net error.
        CHECK_FAIL,
        // Got checking result, no update available(current already latest).
        LATEST,
        // Got checking result, update available on air, can be downloaded.
        TO_BE_DOWNLOADED,
        // Downloading in progress
        DOWNLOADING,
        // Error during downloading.
        DOWNLOAD_FAIL,
        // Latest update bin available on device.
        DOWNLOADED
    }

    public String getCurrentVersion() {
        String versionFull = mPrefHelper.getLastUpdatePackageVersion();
        String curVersion;
        if (TextUtils.isEmpty(versionFull)) {
            curVersion = mAppService.getApp().getAppVersionName();
        } else {
            curVersion = versionFull;
            String[] versionSplits = versionFull.split("_");
            if (versionSplits.length > 1) {
                curVersion = versionSplits[1];
            }
        }
        return curVersion;
    }

    public FirmwareDisplayStatus getCurrentStatus() {
        return mFirmStatusSubj.getValue();
    }

    public Observable<FirmwareDisplayStatus> getFirmwareStatusObservable() {
        return mFirmStatusSubj.distinctUntilChanged();
    }

    public Observable<Integer> getDownloadProgressObservable() {
        return mDownloadProgressSubj.distinctUntilChanged();
    }

    public void startDownload() {
        mFirmStatusSubj.onNext(FirmwareDisplayStatus.DOWNLOADING);
        mDownloadManager.start(mUrl, UpdateFileParser.getBigBinPath(mAppService.getAppContext()));
    }

    public void cancelDownload() {
        mDownloadManager.cancel();
    }

    private void checkOnline() {
        ApiClient client = new ApiClient(mPrefHelper.getApiHost());
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
                        mUrl = response.data.new_version.url;
                        Logger.d("new version is %s", newVersion);
                        compareVersion(response.data.new_version);
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                        mFirmStatusSubj.onNext(FirmwareDisplayStatus.CHECK_FAIL);
                    }
                });
    }

    private void compareVersion(VersionResponse.NewVersionData newVersion) {
        boolean newFirmwareAvailable = false;
        String[] versionStringSplit;

        String parseNewFirmwareVersion = null;
        versionStringSplit = newVersion.version.split("_");
        if (versionStringSplit.length > 1) {
            parseNewFirmwareVersion = versionStringSplit[1];
            Logger.d("new version is %s", parseNewFirmwareVersion);
        } else {
            // Could not recognize this version name.
            Logger.d("New version name %s", newVersion.version);
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

        if (!newFirmwareAvailable) {
            mFirmStatusSubj.onNext(FirmwareDisplayStatus.LATEST);
        } else {
            processNewVersionInfo(newVersion);
            Logger.d("checked: New firmware available %s", newVersion.version);
            Logger.d("cached version is %s", UpdateFileParser.getBigBinVersion(mAppService.getAppContext()));
            if (UpdateFileParser.isBigBinAvailable(mAppService.getAppContext()) && newVersion.version.equals(UpdateFileParser.getBigBinVersion(mAppService.getAppContext()))) {
                cancelDownload();
                mFirmStatusSubj.onNext(FirmwareDisplayStatus.DOWNLOADED);
            } else {
                mFirmStatusSubj.onNext(FirmwareDisplayStatus.TO_BE_DOWNLOADED);
            }
        }
    }

    private void processNewVersionInfo(VersionResponse.NewVersionData newVersion) {
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.name = newVersion.version.split("_")[1];
        versionInfo.fileSize = (int) (newVersion.package_size / 1024f / 1024f);
        versionInfo.releaseTime = newVersion.version.split("_")[2];
        versionInfo.changelogs = getMergedChangelogs(newVersion.change_log);
        mVersionInfoSubj.onNext(versionInfo);
    }

    private List<ChangelogItem> getMergedChangelogs(VersionChangeLog changeLog) {

        List<String> features = changeLog.getFeatures();
        List<String> improvements = changeLog.getImprovement();
        List<String> bugFixes = changeLog.getBugFixes();

        List<ChangelogItem> changeLogList = new ArrayList<>();
        changeLogList.add(new ChangelogItem("Features", ChangelogItem.ChangelogType.TITLE));
        for (String feature : features) {
            changeLogList.add(new ChangelogItem(feature, ChangelogItem.ChangelogType.DESC));
        }
        changeLogList.add(new ChangelogItem("Improvements", ChangelogItem.ChangelogType.TITLE));
        for (String improvement : improvements) {
            changeLogList.add(new ChangelogItem(improvement, ChangelogItem.ChangelogType.DESC));
        }
        changeLogList.add(new ChangelogItem("Bug Fixes", ChangelogItem.ChangelogType.TITLE));
        for (String bugFix : bugFixes) {
            changeLogList.add(new ChangelogItem(bugFix, ChangelogItem.ChangelogType.DESC));
        }

        return changeLogList;
    }

    public static class ChangelogItem {
        public String words;
        public ChangelogType type;

        public ChangelogItem(String words, ChangelogType type) {
            this.words = words;
            this.type = type;
        }

        public enum ChangelogType {
            TITLE,
            DESC
        }
    }

    public static class VersionInfo {
        public String name;
        // Assume bin file always larger than 1M.
        public int fileSize;
        public String releaseTime;
        public List<ChangelogItem> changelogs;
    }
}
