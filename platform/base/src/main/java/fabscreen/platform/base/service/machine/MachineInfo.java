package fabscreen.platform.base.service.machine;

import java.util.List;

import fabscreen.platform.base.Constants;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.entity.Module;
import io.reactivex.Observable;

//all machineInfo here, may properties
public class MachineInfo {
    public static final MachineInfo initialValue = new MachineInfo(new Vector(), null);
    public Vector size;
    public String controllerFWVersion;
    // series MachineType  //A150&A250 is a series
    public int seriesId;
    // model under the series 150,250 etc
    public int modelId;
    public int productId;
    // the master control burning serial number, which is an increasing number, and is written when the burner burns
    public String burnSerialNumber;
    // matching with SN of the whole machine package
    public String productSerialNumber;
    //mOutdatedVersionModuleListSubject
    public List<Module> moduleList;

    //virtual attributes, analysed from moduleList
    public boolean isRotaryAvailable;
    public boolean isEnclosureAvailable;
    public boolean isAirPurifierAvailable;
    public boolean isEmergencyStopAvailable;
    public boolean isHeatedBedAvailable;
    public boolean isDryBoxAvailable;

    // enum: fdm laser cnc
    public IMachine.WorkType workType = IMachine.WorkType.NONE;
    public int headType = Module.ModuleType.HEAD_UNPLUGGED;
    public long headSNid = -1;

    private MachineInfo mLastInfo;

    public static MachineInfo create() {
        return new MachineInfo(new Vector(), null);
    }

    private MachineInfo(Vector s, MachineInfo last) {
        size = s;
        if (last != null) {
            last.mLastInfo = null;
        }
        mLastInfo = last;
    }


    public List<Module> getModuleList() {
        return moduleList;
    }

    public void setMachineType(int position) {
    }

    public void setRotaryAvailable(int position) {
    }

    public String getMachineModelSeries() {
        switch (modelId) {
            case Constants.MACHINE_MODEL_SNAPMAKER_A150:
                return "Snapmaker 2.0 A150";
            case Constants.MACHINE_MODEL_SNAPMAKER_A250:
                return "Snapmaker 2.0 A250";
            case Constants.MACHINE_MODEL_SNAPMAKER_A350:
                return "Snapmaker 2.0 A350";
            default:
                return "Unknown";
        }
    }

    public Observable<Boolean> setAirPurifierEnabled(boolean enabled) {
        return null;
    }

    @Override
    public String toString() {
        return "MachineInfo{" +
                "size=" + size +
                ", controllerVersion='" + controllerFWVersion + '\'' +
                ", seriesId=" + seriesId +
                ", modelId=" + modelId +
                ", productId=" + productId +
                ", serialNo='" + burnSerialNumber + '\'' +
                ", productSn='" + productSerialNumber + '\'' +
                ", moduleList=" + moduleList +
                ", isRotaryAvailable=" + isRotaryAvailable +
                ", isEnclosureAvailable=" + isEnclosureAvailable +
                ", isAirPurifierAvailable=" + isAirPurifierAvailable +
                ", isEmergencyStopAvailable=" + isEmergencyStopAvailable +
                ", workType=" + workType +
                ", mLastInfo=" + mLastInfo +
                ", headSNid=" + headSNid +
                '}';
    }

    public void reset() {
        size = new Vector();
        controllerFWVersion = "";
        seriesId = 0;
        modelId = 0;
        productId = 0;
        burnSerialNumber = "";
        productSerialNumber = "";
        moduleList = null;
        isRotaryAvailable = false;
        isEnclosureAvailable = false;
        isAirPurifierAvailable = false;
        isEmergencyStopAvailable = false;
        isHeatedBedAvailable = false;
        isDryBoxAvailable = false;
        workType = IMachine.WorkType.NONE;
        headType = Module.ModuleType.HEAD_UNPLUGGED;
        headSNid = -1;
        mLastInfo = null;
    }

    public String getModelName() {
        switch (productId) {
            case IMachine.Product.A150:
                return "A150";
            case IMachine.Product.A250:
                return "A250";
            case IMachine.Product.A350:
                return "A350";
            case IMachine.Product.A400:
                return "Artisan";
            case IMachine.Product.J1:
                return "J1";
            default:
                return "Unknown";
        }
    }
}
