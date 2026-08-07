package com.snapmaker.fabscreena400.module.home;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC_200W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_20W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_2W_INFRARED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_40W;

import android.util.ArraySet;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.List;
import java.util.Set;

import fabscreen.platform.base.BaseMainViewModel;
import fabscreen.platform.base.FabException;
import fabscreen.platform.base.helper.FileHelper;
import fabscreen.platform.base.helper.SemVerHelper;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.update.UpdateFileParser;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.controller.LaserController;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.lib.LogHelper;
import fabscreen.platform.lib.SubjectHolder;
import io.reactivex.Observable;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainViewModel extends BaseMainViewModel {
    private static final String TAG = "MainViewModel";
    public IPreferences.Helper mPrefHelper;
    private LaserController mLaserController;
    public static final int THREE_AXIS = 0;
    public static final int FOUR_AXIS = 1;

    public MainViewModel() {
        super();
        mPrefHelper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();
        IMachine machine = getServiceContainer().getService(IMachine.class);
        mLaserController = machine.getLaserController();
        loadIjkSO();
    }

    @Override
    protected Observable<Boolean> checkModuleVersions() {
        Logger.t(TAG).d("Checking module versions...");
        SubjectHolder<MachineInfo> infoHolder = mMachine.getMachineInfoSubjectHolder();

        return infoHolder.getObservable()
                .filter(info -> info.moduleList != null)
                .flatMap(info -> checkEMVersionLatest(info.moduleList));
    }

    /**
     * Check em.bin and moduleList to compare versions, update if not latest.
     * This won't do anything after an updating, assuming user won't change modules during update.
     */
    private Observable<Boolean> checkEMVersionLatest(List<Module> moduleList) {
        return Observable.fromCallable(() -> !checkIfEMNeedsUpdate(moduleList, getEMBinFile()));
    }

    public File getEMBinFile() {
        return new File(FileHelper.getPersistUpdateFilesDir(mAppService.getAppContext()), "em.bin");
    }

    private boolean checkIfEMNeedsUpdate(List<Module> moduleList, File emFile) {
        if (!emFile.exists()) return false;
        List<Integer> indexes = UpdateFileParser.parseEMBinIndexes(emFile);
        for (Module module : moduleList) {
            int pluggedModuleId = module.getModuleInfo().getModuleId();
            String pluggedModuleVersion = module.getModuleInfo().getFirmwareVersion();
            Logger.d("Module %1$s plugged: %2$s, bin: %3$s",
                    module.getModuleInfo().getKey(),
                    pluggedModuleVersion,
                    UpdateFileParser.parseEMVersion(emFile));

            boolean isPluggedVersionOutdated = !pluggedModuleVersion.equalsIgnoreCase(UpdateFileParser.parseEMVersion(emFile));
            // Use SemVerHelper to judge if plugged module version was outdated.
            // outdated -> means version is lower than latest version, not judging string equalization
            try {
                isPluggedVersionOutdated = SemVerHelper.lt(pluggedModuleVersion, UpdateFileParser.parseEMVersion(emFile));
            } catch (FabException e) {
                LogHelper.log(e);
            }

            // ignore case for bad bin
            if (isSecondHead(module) || (indexes.contains(pluggedModuleId) && isPluggedVersionOutdated) && module.getModuleInfo().getModuleState() == 2) {
                return true;
            }
        }
        return false;
    }

    private boolean isSecondHead(Module module) {
        switch (module.getModuleInfo().getModuleId()) {
            case HEAD_3DP:
            case HEAD_CNC:
            case HEAD_LASER:
                return true;
            default:
                return false;
        }
    }

    private void loadIjkSO() {
        // Load so file
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        } catch (Exception e) {
            LogHelper.log(e);
        }
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
        // FDM tool head.
        if (mWorkType == IMachine.WorkType.FDM) {
            // What was this for?
            mPrefHelper.setA400MachineRotary(-1);

            // First time to load machine tool head serial number into sp.
            if (lastToolheadSN == 0 || !isToolHeadHadPlugged) {
                Logger.d("New tool head plugged, start setting up guide.");
                pluggedSnList.add(String.valueOf(currentToolheadSN));
                mPrefHelper.setA400PluggedSnList(pluggedSnList);
                mPrefHelper.setA400MachineStep(currentToolheadSN, 0);
                mPrefHelper.setA400MachineSn(currentToolheadSN);
                guideStep = 0;
            } else {
                guideStep = mPrefHelper.getA400MachineStep(currentToolheadSN);
            }
        } else {
            rotaryType = isRotaryAvailable() ? FOUR_AXIS : THREE_AXIS;
            // First time using || New tool head detected || First time using rotary || Rotary type was not match last
            if (lastToolheadSN == 0 || !isToolHeadHadPlugged
                    || mPrefHelper.getA400MachineRotary() == -1 || mPrefHelper.getA400MachineRotary() != rotaryType) {
                Logger.d("New tool head plugged or may changed, start setting up guide.");
                pluggedSnList.add(String.valueOf(currentToolheadSN));
                mPrefHelper.setA400PluggedSnList(pluggedSnList);
                mPrefHelper.setA400MachineStep(currentToolheadSN, 0);
                mPrefHelper.setA400MachineSn(currentToolheadSN);
                mPrefHelper.setA400MachineRotary(rotaryType);
                guideStep = 0;
            } else {
                guideStep = mPrefHelper.getA400MachineStep(currentToolheadSN);
            }
        }

        Logger.t(TAG).d("worktype %1$s, last sn %2$s, cur sn %3$s, guide step %4$s", mWorkType.name(), String.valueOf(lastToolheadSN), String.valueOf(currentToolheadSN), String.valueOf(guideStep));

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
            case HEAD_LASER_2W_INFRARED:
            case HEAD_LASER_20W:
            case HEAD_LASER_40W:
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

    @Override
    public boolean needGoWelcome() {
        return !mPrefHelper.getMachineSetupFlag();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        IjkMediaPlayer.native_profileEnd();
    }

    public Observable<ResponseStructure> setLaserLockStatus(int lockStatus) {
        return mLaserController.setLaserLockStatus(lockStatus);
    }

    public Observable<ResponseStructure> getLaserLockStatus() {
        return mLaserController.getLaserLockStates();
    }

    @Override
    public boolean isRotaryAvailable() {
        return mMachine.getMachineInfoSubjectHolder().getValue().isRotaryAvailable;
    }

    @Override
    public String getProductSerialNumber() {
        return mMachine.getMachineInfoSubjectHolder().getValue().productSerialNumber;
    }

    @Override
    protected long getOccupiedSpaceInMegaByte() {
        return 500;
    }
}
