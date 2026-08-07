package fabscreen.features.settings.common;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_AIR_PURIFIER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_ENCLOSURE;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_ENCLOSURE_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_HEATED_BED_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC_200W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_20W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_2W_INFRARED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_40W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.LINEAR_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ROTARY_MODULE;

import android.content.Context;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import fabscreen.platform.base.helper.ModuleIdNameMapper;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.model.ModuleCompact;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400ModuleAssistantViewModel extends BaseViewModel {

    private final IMachine mMachine;

    public A400ModuleAssistantViewModel() {
        mMachine = getServiceContainer().getService(IMachine.class);
    }

    public String getMachineName() {
        return ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getMachineName();
    }

    public List<String> getModuleNameList() {
        Context appContext = getServiceContainer().getService(IAppService.class).getAppContext();
        MachineInfo machineInfo = mMachine.getMachineInfoSubjectHolder().getValue();

        List<ModuleCompact> sortedFullIdList = new ArrayList<>();
        // FIXME: Careful here, this list was built with hardcoded indexes!
        sortedFullIdList.add(new ModuleCompact(HEAD_3DP, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_3DP_DOUBLE_EXTRUDER, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_LASER, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_LASER_2W_INFRARED, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_LASER_10W, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_LASER_20W, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_LASER_40W, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_CNC, 0));
        sortedFullIdList.add(new ModuleCompact(HEAD_CNC_200W, 0));
        sortedFullIdList.add(new ModuleCompact(ADDON_HEATED_BED_A400, 0));
        sortedFullIdList.add(new ModuleCompact(ADDON_ENCLOSURE_A400, 0));
        sortedFullIdList.add(new ModuleCompact(ADDON_ENCLOSURE, 0));
        sortedFullIdList.add(new ModuleCompact(ADDON_AIR_PURIFIER, 0));
        sortedFullIdList.add(new ModuleCompact(ROTARY_MODULE, 0));
        sortedFullIdList.add(new ModuleCompact(LINEAR_A400, 0));
        sortedFullIdList.add(new ModuleCompact(LINEAR_A400, 1));
        sortedFullIdList.add(new ModuleCompact(LINEAR_A400, 4));
        sortedFullIdList.add(new ModuleCompact(LINEAR_A400, 2));
        sortedFullIdList.add(new ModuleCompact(LINEAR_A400, 5));

        List<ModuleCompact> sortedIdList = new ArrayList<>(sortedFullIdList);
        List<ModuleCompact> pluggedInIds = new ArrayList<>();

        if (machineInfo != null && machineInfo.moduleList != null) {
            for (Module module : machineInfo.moduleList) {
                Module.ModuleInfo info = module.getModuleInfo();
                pluggedInIds.add(new ModuleCompact(info.getModuleId(), info.getModuleIndex()));
            }
        }

        for (ModuleCompact module : sortedFullIdList) {
            if (!pluggedInIds.contains(module)) {
                sortedIdList.remove(module);
            } else {
                pluggedInIds.remove(module);
            }
        }
        return ModuleIdNameMapper.convertModuleCompactToNames(appContext, sortedIdList);
    }

    public boolean canReplaceHotend() {
        return mMachine.getMachineInfoSubjectHolder().getValue().headType == Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
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

    public boolean isFDMType() {
        return getServiceContainer().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM;
    }
}
