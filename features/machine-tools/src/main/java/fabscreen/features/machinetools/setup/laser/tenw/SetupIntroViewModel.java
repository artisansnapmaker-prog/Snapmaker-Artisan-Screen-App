package fabscreen.features.machinetools.setup.laser.tenw;

import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.LaserController;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import io.reactivex.Observable;

import static fabscreen.platform.base.RoutePath.TOOLS_CALIBRATION_A400_LASER_10W_CAMERA_CALIBRATION;
import static fabscreen.platform.base.RoutePath.TOOLS_CALIBRATION_A400_LASER_40W_PLATFORM_HEIGHT_INFO;
import static fabscreen.platform.base.RoutePath.TOOLS_CALIBRATION_A400_LASER_THICKNESS_MEASURE_CALIBRATION;

public class SetupIntroViewModel extends BaseViewModel {

    private LaserController mLaserController;
    private IMachine mMachine;

    public SetupIntroViewModel() {
        IMachine machine = getServiceContainer().getService(IMachine.class);
        mLaserController = machine.getLaserController();
        mMachine = getServiceContainer().getService(IMachine.class);
    }

    public Observable<Integer> setMode(String destination) {
        switch (destination) {
            case TOOLS_CALIBRATION_A400_LASER_10W_CAMERA_CALIBRATION:
                return mLaserController.setCalibrationMode(2).map(response -> response.resultProp.getValue());
            case TOOLS_CALIBRATION_A400_LASER_THICKNESS_MEASURE_CALIBRATION:
                return mLaserController.setCalibrationMode(0).map(response -> response.resultProp.getValue());
            case TOOLS_CALIBRATION_A400_LASER_40W_PLATFORM_HEIGHT_INFO:
                return mLaserController.setCalibrationMode(1).map(response -> response.resultProp.getValue());
            default:
                return Observable.just(0);
        }
    }

    public Observable<ResponseStructure> setLaserLockStatus(int lockStatus) {
        return mLaserController.setLaserLockStatus(lockStatus);
    }

    public Observable<ResponseStructure> getLaserLockStatus() {
        return mLaserController.getLaserLockStates();
    }

    public String getProductSerialNumber() {
        return mMachine.getMachineInfoSubjectHolder().getValue().productSerialNumber;
    }

}
