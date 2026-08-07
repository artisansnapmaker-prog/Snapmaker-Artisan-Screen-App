package fabscreen.features.settings.a400.maintenance.index;

import com.orhanobut.logger.Logger;

import java.io.File;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.INetwork;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SettingsMaintenanceViewModel extends BaseViewModel {
    private static final String WIFI_INFO_FILE_NAME = "wifi_info";

    private final IMachine mMachine;
    private final INetwork mNetworkService;

    public SettingsMaintenanceViewModel() {
        mMachine = getServiceContainer().getService(IMachine.class);
        mNetworkService = getServiceContainer().getService(INetwork.class);
    }

    public Observable<Boolean> doFactoryReset() {
        clearCache();
        ServiceContainer.getInstance().getService(IPreferences.class).getHelper().reset();
        Observable<Boolean> resetMachineObservable = mMachine.getMachineController().requestMachineFactoryReset(0).map(ResponseStructure::isSuccess);
        Observable<Boolean> resetWiFiObservable = Observable.fromCallable(() -> {
            mNetworkService.removeOrDisableAllWifi();
            return true;
        });

        return Observable.zip(resetMachineObservable, resetWiFiObservable, (aBoolean, aBoolean2) -> aBoolean && aBoolean2);
    }

    public void clearCache() {
        ServiceContainer.getInstance().getService(INetwork.class).removeOrDisableAllWifi();
        ServiceContainer.getInstance().getService(IPreferences.class).getHelper().reset();
        ServiceContainer.getInstance().getService(IPrintWorkspace.class).clearAllWorkSpaceFiles();
        File cacheDir = getServiceContainer().getService(IAppService.class).getCacheDir();
        deleteDir(cacheDir);
        File wifiPasswdFile = new File(ServiceContainer.getInstance().getService(IAppService.class).getDataDir(), WIFI_INFO_FILE_NAME);
        if (wifiPasswdFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            wifiPasswdFile.delete();
        }
    }

    private boolean deleteDir(File file) {
        if (file == null) return true;
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                if (!deleteDir(new File(file, child))) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    public IMachine.WorkType getWorkType() {
        return mMachine.getMachineInfoSubjectHolder().getValue().workType;
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
}
