package fabscreen.features.settings.a400.moduleassistant.replacemodule;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC_200W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;

import android.text.TextUtils;
import android.util.ArraySet;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import fabscreen.platform.base.Constants;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.fabserver.RetryWithDelay;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.entity.Toolhead;
import fabscreen.platform.base.service.machine.entity.toolhead.CNCToolhead;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ReplaceModuleViewModel extends BaseViewModel {

    private final IMachine mMachine;

    private final PublishSubject<Boolean> mIsRestartSuccessSubj = PublishSubject.create();
    private PublishSubject<MachineInfo> mMachineInfoSubj;
    private final List<Module> mOldModuleList;
    private List<Module> mRemovedModuleList;
    private List<Module> mAddedModuleList;
    private List<String> mNeedCalibrateModuleNames;
    public IPreferences.Helper mPrefHelper;
    public static final int THREE_AXIS = 0;
    public static final int FOUR_AXIS = 1;
    protected MachineInfo mMachineInfo;
    protected IMachine.WorkType mWorkType;

    public ReplaceModuleViewModel() {
        mMachine = getServiceContainer().getService(IMachine.class);
        mOldModuleList = mMachine.getMachineInfoSubjectHolder().getValue().moduleList;
        mPrefHelper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();
        mMachineInfo = mMachine.getMachineInfoSubjectHolder().getValue();
        mWorkType = mMachineInfo.workType;
        observeModuleInfo();
    }

    private void observeModuleInfo() {
        mMachine.getMachineInfoSubjectHolder().getObservable()
                .as(bindToLifecycle())
                .subscribe(machineInfo -> {
                    if (mMachineInfoSubj != null) {
                        mMachineInfoSubj.onNext(machineInfo);
                    }
                }, e -> {
                    LogHelper.log(e);
                    if (mMachineInfoSubj != null) {
                        mMachineInfoSubj.onError(e);
                    }
                });
    }

    public Observable<Integer> startReplaceModuleMode(boolean switchOn) {
        return mMachine.getMachineController()
                .startReplacePartsMode(switchOn)
                .map(structure -> structure.resultProp.getValue());
    }

    public Observable<Integer> restartMachine() {
        return mMachine.getMachineController()
                .restartMachine()
                .map(structure -> structure.resultProp.getValue())
                .doOnNext(result -> {
                    if (result == 0) {
                        // delay and request heartbeat
//                        requestHeartbeatWithDelay();
                        restartFab();
                    }
                });
    }

    private void restartFab() {
        mMachine.onRestart()
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        // Machine rebooted and will start push heartbeat msg, which will trigger
                        // MachineController to init modules.
                        watchModulesInit();
                    } else {
                        Logger.d("subscribe fail");
                        mIsRestartSuccessSubj.onNext(false);
                    }
                }, e -> {
                    mIsRestartSuccessSubj.onNext(false);
                    LogHelper.log(e);
                });
    }

    private void requestHeartbeatWithDelay() {
        Logger.d("replacement request heartbeat with delay");
        // TODO: 2022/5/14 use delay() instead
        Schedulers.io().scheduleDirect(() ->
                mMachine.getConnectionController().requestHeartbeat()
                        .retryWhen(new RetryWithDelay(4, 2000))
                        .as(bindToLifecycle())
                        .subscribe(response -> {
                            if (response.isSuccess()) {
                    /*
                     Machine rebooted and will start push heartbeat msg, which will trigger
                     MachineController to init modules.
                     */
                                watchModulesInit();
                            } else {
                                Logger.d("subscribe fail");
                            }
                        }, e -> {
                            mIsRestartSuccessSubj.onNext(false);
                            LogHelper.log(e);
                        }), 10, TimeUnit.SECONDS);
    }

    private void watchModulesInit() {
        mMachineInfoSubj = PublishSubject.create();
        mMachineInfoSubj
                .filter(machineInfo -> machineInfo.moduleList != null)
                .timeout(30, TimeUnit.SECONDS)
                .as(bindToLifecycle())
                .subscribe(machineInfo -> {
                    mRemovedModuleList = findAbsent(mOldModuleList, machineInfo.moduleList);
                    mAddedModuleList = findAbsent(machineInfo.moduleList, mOldModuleList);
                    mIsRestartSuccessSubj.onNext(true);
                    mMachineInfoSubj.onComplete();
                }, e -> {
                    LogHelper.log(e);
                    mIsRestartSuccessSubj.onNext(false);
                });
    }

    public Observable<Boolean> getMachineRestartObservable() {
        return mIsRestartSuccessSubj.hide();
    }

    public List<String> getRemovedModuleList() {
        List<String> moduleNames = new ArrayList<>();
        if (mRemovedModuleList != null) {
            mRemovedModuleList.forEach(module -> moduleNames.add(module.getDisplayName()));
        }
        return moduleNames;
    }

    public List<String> getAddedModuleList() {
        List<String> moduleNames = new ArrayList<>();
        if (mAddedModuleList != null) {
            mAddedModuleList.forEach(module -> moduleNames.add(module.getDisplayName()));
        }
        return moduleNames;
    }

    public List<String> getNeedCalibrateModuleList() {
        if (mNeedCalibrateModuleNames == null) {
            mNeedCalibrateModuleNames = new ArrayList<>();
            if (mAddedModuleList != null) {
                for (Module module : mAddedModuleList) {
                    if (module instanceof Toolhead && !(module instanceof CNCToolhead)) {
                        mNeedCalibrateModuleNames.add(module.getDisplayName());
                    }
                }
            }
        }
        return mNeedCalibrateModuleNames;
    }

    public boolean needCalibrate() {
        return getNeedCalibrateModuleList().size() > 0;
    }

    private List<Module> findAbsent(List<Module> assumeBigger, List<Module> assumeSmaller) {

        List<Module> absent = new ArrayList<>(assumeBigger);
        List<Module> smaller = new ArrayList<>(assumeSmaller);

        for (Module moduleInBigger : assumeBigger) {
            for (Module moduleInSmaller : smaller) {
                if (moduleInSmaller.getModuleInfo().getSn() == moduleInBigger.getModuleInfo().getSn() && moduleInSmaller.getModuleInfo().getModuleId() == moduleInBigger.getModuleInfo().getModuleId() && moduleInSmaller.getDisplayName().equals(moduleInBigger.getDisplayName())) {
                    absent.remove(moduleInBigger);
                    smaller.remove(moduleInSmaller);
                    break;
                }
            }
        }

        Logger.d("Absent found, list=%s", absent.stream().map(module -> module.getDisplayName() + "***" + module.getModuleInfo().getSn()).collect(Collectors.toList()));
        return absent;
    }

    public boolean needGoToGuide() {
        long currentToolheadSN = mMachine.getMachineInfoSubjectHolder().getValue().headSNid;
        if (currentToolheadSN == -1) {
            // No toolhead detected.
            return false;
        }


        int guideStep;
        long lastToolheadSN = mPrefHelper.getA400MachineSn();
        Set<String> pluggedSnList = mPrefHelper.getA400PluggedSnList();
        if (pluggedSnList == null) {
            pluggedSnList = new ArraySet<>();
            pluggedSnList.add(String.valueOf(currentToolheadSN));
            mPrefHelper.setA400PluggedSnList(pluggedSnList);
        }
        int rotaryType;

        Logger.d("last toolHead sn list" + pluggedSnList.toString());
        Logger.d("current toolHead sn " + currentToolheadSN);

        boolean isToolHeadHadPlugged = isToolHeadHadPlugged(currentToolheadSN, pluggedSnList);
        Logger.d("Last work type " + mWorkType);

        mWorkType = mMachine.getMachineInfoSubjectHolder().getValue().workType;

        Logger.d("Update work type " + mWorkType);

        if (mWorkType == IMachine.WorkType.FDM) {
            mPrefHelper.setA400MachineRotary(-1);
            if (lastToolheadSN == 0 || !isToolHeadHadPlugged) {
                Logger.d("New tool head plugged, start setting up guide.");
                pluggedSnList.add(String.valueOf(currentToolheadSN));
                mPrefHelper.setA400PluggedSnList(pluggedSnList);
                mPrefHelper.setA400MachineSn(currentToolheadSN);
                mPrefHelper.setA400MachineStep(currentToolheadSN, 0);
                guideStep = 0;
            } else {
                guideStep = mPrefHelper.getA400MachineStep(currentToolheadSN);
            }
        } else {
            rotaryType = isRotaryAvailable() ? FOUR_AXIS : THREE_AXIS;
            if (lastToolheadSN == 0 || !isToolHeadHadPlugged
                    || mPrefHelper.getA400MachineRotary() == -1 || mPrefHelper.getA400MachineRotary() != rotaryType) {
                Logger.d("New tool head plugged or may changed, start setting up guide.");
                pluggedSnList.add(String.valueOf(currentToolheadSN));
                mPrefHelper.setA400PluggedSnList(pluggedSnList);
                mPrefHelper.setA400MachineSn(currentToolheadSN);
                mPrefHelper.setA400MachineStep(currentToolheadSN, 0);
                mPrefHelper.setA400MachineRotary(rotaryType);
                guideStep = 0;
            } else {
                guideStep = mPrefHelper.getA400MachineStep(currentToolheadSN);
            }
        }

        // Magic numbers are step count of each guide.
        // Need to be refactored, MainActivity don't need to know this count.
        switch (getHeadType()) {
            case HEAD_3DP_DOUBLE_EXTRUDER:
                return guideStep < 3;

            case HEAD_3DP:
                return guideStep < 2;

            case HEAD_LASER_10W:
            case HEAD_LASER:
                return guideStep < (mMachine.getMachineInfoSubjectHolder().getValue().isRotaryAvailable ? 1 : 2);

            case HEAD_CNC:
            case HEAD_CNC_200W:
                return guideStep < 1;
        }
        return false;
    }

    private boolean isToolHeadHadPlugged(long currentToolHeadSn, Set<String> pluggedToolHeadSnList) {
        for (String pluggedSn : pluggedToolHeadSnList) {
            if (String.valueOf(currentToolHeadSn).equals(pluggedSn)) {
                return true;
            }
        }
        return false;
    }

    public int getHeadType() {
        return mMachine.getMachineInfoSubjectHolder().getValue().headType;
    }

    public boolean isRotaryAvailable() {
        return mMachine.getMachineInfoSubjectHolder().getValue().isRotaryAvailable;
    }

    public boolean isLaserToolHeadNeedUnlock() {
        IMachine.WorkType workType = mMachine.getMachineInfoSubjectHolder().getValue().workType;

        if (workType != IMachine.WorkType.LASER) {
            return false;
        }

        if (TextUtils.isEmpty(getProductSerialNumber()) || mMachine.getMachineInfoSubjectHolder().getValue().headSNid == -1) {
            Logger.e("machine SN is null, skipping...");
            return false;
        }

        return true;
    }

    public void queryException() {
        ServiceContainer.getInstance().getService(IMachine.class).getErrorController().queryException()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                }, LogHelper::log);
    }

    public boolean isSecondHead() {
        int headType = getHeadType();
        return
                headType == Module.ModuleType.HEAD_3DP ||
                        headType == Module.ModuleType.HEAD_CNC ||
                        headType == Module.ModuleType.HEAD_LASER;
    }

    public Observable<ResponseStructure> setLaserLockStatus(int lockStatus) {
        return mMachine.getLaserController().setLaserLockStatus(lockStatus);
    }

    public Observable<Boolean> moveToProperPosition() {
        float initX = 200f;
        float initY = 400f;
        float initZ = 250f;
        Vector vector = new Vector();
        vector.setX(initX);
        vector.setY(initY);
        vector.setZ(initZ);
        IMachine.WorkType workType = mMachine.getMachineInfoSubjectHolder().getValue().workType;
        return  checkHome().flatMap(homed -> homed ? moveToPosition(vector, workType == IMachine.WorkType.FDM) : Observable.just(false));
    }

    private Observable<Boolean> moveToPosition(Vector vector, boolean isUsingG53) {
        return mMachine.getMachineController().updateCoordinateSystem(0)
                .flatMap(result -> result.coordinateID == 0 ? ServiceContainer.getInstance().getService(IMachine.class).getMachineController().gotoAbsolutePosition(vector) : Observable.just((new ResponseStructure())))
                .flatMap(resultStructure -> {
                    if (resultStructure.isSuccess()) {
                        return isUsingG53 ? Observable.just(true) : mMachine.getMachineController().updateCoordinateSystem(1).flatMap(result -> Observable.just(true));
                    } else {
                        return Observable.just(false);
                    }
                });
    }

    public Observable<Boolean> checkHome() {
        IMachine service = ServiceContainer.getInstance().getService(IMachine.class);
        if (!service.getMachineStatusSubjectHolder().getValue().isHomed) {
            return service.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> service.getMachineController().home(0))
                    .flatMap(integer -> Observable.just(integer == 0));
        } else {
            return Observable.just(true);
        }
    }

    public String getProductSerialNumber() {
        return mMachine.getMachineInfoSubjectHolder().getValue().productSerialNumber;
    }

}
