package fabscreen.features.settings.j1;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.INetwork;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.entity.toolhead.FdmToolhead;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class J1SettingsAttendanceViewModel extends BaseViewModel {
    public Observable<Boolean> J1FactoryReset() {
        return ServiceContainer.getInstance().getService(IMachine.class)
                .getMachineController()
                .requestMachineFactoryReset(0)
                .flatMap(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        // Request success.
                        ServiceContainer.getInstance().getService(INetwork.class).removeOrDisableAllWifi();
                        ServiceContainer.getInstance().getService(IPreferences.class).getHelper().reset();
                        ServiceContainer.getInstance().getService(IPrintWorkspace.class).clearAllWorkSpaceFiles();
                        return Observable.just(true);
                    } else {
                        return Observable.just(false);
                    }
                });
    }

    /**
     * @param extruderIndex 0 left, 1 right
     *                      query M412: 1 is on, 0 is off
     */
    public boolean isRunoutRecoveryEnabled(int extruderIndex) {
        FdmToolhead.FdmToolheadStatus fdmToolheadStatus = ServiceContainer.getInstance().getService(IMachine.class)
                .getFDMController()
                .getToolheadStatusSubjectHolder(extruderIndex)
                .getValue();
        int filamentDetectionStatus = fdmToolheadStatus.getExtruderList().get(0).getFilamentDetectionStatus();
        return filamentDetectionStatus == 1;
    }

    public boolean isLightOn() {
        // TODO: 2022/5/26 return the real value
        return true;
    }

    public void setRunoutRecoveryEnabled(int index, boolean isChecked) {
        ServiceContainer.getInstance().getService(IMachine.class)
                .getFDMController()
                .setFilamentSensorStatus(index, 0, isChecked ? 1 : 0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                }, LogHelper::log);

    }

    public void setLightingEnabled(boolean isChecked) {
    }
}
