package fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY;

import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_ABS;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_CUSTOM;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_PETG;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_PLA;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationPrintViewModel.XYPrintState.CHECK_HOME_FAIL;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationPrintViewModel.XYPrintState.HEATING_SUCCESS;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationPrintViewModel.XYPrintState.INIT_PRINT_FILE_FAIL;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationPrintViewModel.XYPrintState.SET_TEMPERATURE_FAIL;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationPrintViewModel.XYPrintState.WEATING_HEATING;
import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_COMPLETED;
import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_XY_CALIBRATING;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.parts.Extruder.EXTRUDER_DIAMETER_0_2;
import static fabscreen.platform.base.service.machine.entity.parts.Extruder.EXTRUDER_DIAMETER_0_4;
import static fabscreen.platform.base.service.machine.entity.parts.Extruder.EXTRUDER_DIAMETER_0_6;
import static fabscreen.platform.base.service.machine.entity.parts.Extruder.EXTRUDER_DIAMETER_0_8;
import static fabscreen.platform.base.service.machine.entity.parts.Extruder.EXTRUDER_LEFT;
import static fabscreen.platform.base.service.machine.entity.parts.Extruder.EXTRUDER_RIGHT;

import android.content.Context;
import android.graphics.Bitmap;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fabscreen.features.machinetools.R;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.file.FabLocalFile;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.controller.MachineController;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.controller.PrintEvent;
import fabscreen.platform.base.service.machine.entity.module.HeatedBed;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.service.machine.entity.toolhead.FdmToolhead;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.core.ui.data.FilamentState;
import fabscreen.platform.core.ui.data.PrintProgress;
import fabscreen.platform.lib.LogHelper;
import fabscreen.platform.lib.SubjectHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class A400LevelingXYCalibrationPrintViewModel extends BaseViewModel {
    private final BehaviorSubject<Boolean> mWaitingSubject = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<PrintProgress> mPrintProgressSubject = BehaviorSubject.createDefault(new PrintProgress());
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    IPreferences mPreference;
    int mLeftPrintingTemperature;
    int mRightPrintingTemperature;
    int mBedPrintingTemperature;
    IFile mPrintFile;
    Observable<FdmToolhead.FdmToolheadStatus> mFdmToolHeadObservable;
    Observable<HeatedBed.HeatedBedStatus> mBedObservable;
    private IMachine mA400Machine;
    private NewPrintController mNewPrintController;
    private IPrintWorkspace mWorkspace;
    private IGcodeParser mParser;
    private Context mContext;
    private float mEstimatedTime;
    private boolean mIsSetCalibrationMode;
    private BehaviorSubject<FilamentState> mFilamentStateSubject = BehaviorSubject.createDefault(new FilamentState());
    private PublishSubject<Boolean> mFileParserSubject = PublishSubject.create();

    public A400LevelingXYCalibrationPrintViewModel() {
        super();
        mA400Machine = ServiceContainer.getInstance().getService(IMachine.class);
        mWorkspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        mNewPrintController = mA400Machine.getNewPrintController();
        mParser = ServiceContainer.getInstance().getService(IGcodeParser.class);
        mPreference = ServiceContainer.getInstance().getService(IPreferences.class);
        mContext = getServiceContainer().getService(IAppService.class).getAppContext();
        mFdmToolHeadObservable = mA400Machine
                .getFDMController()
                .getToolheadStatusSubjectHolder(0)
                .getObservable();
        mBedObservable = mA400Machine.getMachineController()
                .getHeatedBed()
                .getHeatedBedStatusSubjectHolder()
                .getObservable();
    }

    public Observable<XYPrintState> init() {
        Observable<XYPrintState> heatingObservable = Observable.zip(mFdmToolHeadObservable, mBedObservable, (fdmToolHeadInfo, bedInfo) -> {
            Extruder leftExtruder = fdmToolHeadInfo.getExtruderList().get(0);
            int leftTemperature = (int) leftExtruder.getTemperature();
            int leftTargetTemperature = (int) leftExtruder.getTargetTemperature();
            Extruder rightExtruder = fdmToolHeadInfo.getExtruderList().get(1);
            int rightTemperature = (int) rightExtruder.getTemperature();
            int rightTargetTemperature = (int) rightExtruder.getTargetTemperature();
            HeatedBed.ZoneInfo zoneInfo = bedInfo.getZoneList().get(0);
            float zoneCurrentTemperature = zoneInfo.getCurrentTemperature();
            int zoneTargetTemperature = zoneInfo.getTargetTemperature();
            if (leftTargetTemperature <= 0 || rightTargetTemperature <= 0) {
                return WEATING_HEATING;
            } else {
                return (rightTemperature >= rightTargetTemperature - 3) && (leftTemperature >= leftTargetTemperature - 3) && (zoneCurrentTemperature >= zoneTargetTemperature - 3) ? HEATING_SUCCESS : WEATING_HEATING;
            }
        });
        return Observable.zip(
                        initPrintFile(),
                        initTemperature(),
                        checkHome(),
                        (printFileResponse,
                         temperatureResponse,
                         checkHomeResponse) -> {
                            if (!temperatureResponse.isSuccess()) {
                                return SET_TEMPERATURE_FAIL;
                            } else if (!printFileResponse) {
                                return INIT_PRINT_FILE_FAIL;
                            } else if (!checkHomeResponse) {
                                return CHECK_HOME_FAIL;
                            }
                            return WEATING_HEATING;
                        })
                .flatMap(xyPrintState -> {
                    if (xyPrintState == WEATING_HEATING) {
                        return heatingObservable;
                    } else {
                        return Observable.just(xyPrintState);
                    }
                });
    }

    private Observable<Boolean> checkHome() {
        if (!mA400Machine.getMachineStatusSubjectHolder().getValue().isHomed) {
            return mA400Machine.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> mA400Machine.getMachineController().home(0))
                    .flatMap(integer -> Observable.just(integer == 0));
        } else {
            return Observable.just(true);
        }
    }

    private Observable<ResponseStructure> initTemperature() {
        mLeftPrintingTemperature = mPreference.getHelper().getA400LevelingXYCalibrationLeftPrintingTemperature();
        mRightPrintingTemperature = mPreference.getHelper().getA400LevelingXYCalibrationRightPrintingTemperature();
        mBedPrintingTemperature = mPreference.getHelper().getA400LevelingXYCalibrationBedPrintingTemperature();
        switch (mPreference.getHelper().getA400BevelingXYMaterialSelection()) {
            case A400_LEVELING_XY_CALIBRATION_PLA:
                mLeftPrintingTemperature = 210;
                mRightPrintingTemperature = 210;
                mBedPrintingTemperature = 60;
                break;
            case A400_LEVELING_XY_CALIBRATION_PETG:
                mLeftPrintingTemperature = 230;
                mRightPrintingTemperature = 230;
                mBedPrintingTemperature = 80;
                break;
            case A400_LEVELING_XY_CALIBRATION_ABS:
                mLeftPrintingTemperature = 235;
                mRightPrintingTemperature = 235;
                mBedPrintingTemperature = 80;
                break;
            case A400_LEVELING_XY_CALIBRATION_CUSTOM:
            default:
                break;
        }
        return mA400Machine.getFDMController().setExtruderTemperature(0, 0, mLeftPrintingTemperature)
                .flatMap(b -> mA400Machine.getFDMController().setExtruderTemperature(0, 1, mRightPrintingTemperature))
                .flatMap(b -> mA400Machine.getMachineController().getHeatedBed().setAllTargetTemperature(mBedPrintingTemperature));
    }

    private Observable<Boolean> initPrintFile() {
        List<Extruder> extruderList = mA400Machine.getFDMController().getToolheadStatusSubjectHolder(0).getValue().getExtruderList();
        float leftDiameter = extruderList.get(EXTRUDER_LEFT).getDiameter();
        float rightDiameter = extruderList.get(EXTRUDER_RIGHT).getDiameter();
        int printFileId = -1;
        String fileName = "calibrationXY";
        // FIXME: Consider matching as a string concatenation?
        if (leftDiameter == EXTRUDER_DIAMETER_0_2) {
            fileName += "_02";
            if (rightDiameter == EXTRUDER_DIAMETER_0_2) {
                fileName += "_02";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_02_02;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_4) {
                fileName += "_04";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_02_04;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_6) {
                fileName += "_06";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_02_06;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_8) {
                fileName += "_08";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_02_08;
            }
        } else if (leftDiameter == EXTRUDER_DIAMETER_0_4) {
            fileName += "_04";
            if (rightDiameter == EXTRUDER_DIAMETER_0_2) {
                fileName += "_02";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_04_02;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_4) {
                fileName += "_04";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_04_04;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_6) {
                fileName += "_06";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_04_06;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_8) {
                fileName += "_08";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_04_08;
            }
        } else if (leftDiameter == EXTRUDER_DIAMETER_0_6) {
            fileName += "_06";
            if (rightDiameter == EXTRUDER_DIAMETER_0_2) {
                fileName += "_02";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_06_02;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_4) {
                fileName += "_04";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_06_04;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_6) {
                fileName += "_06";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_06_06;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_8) {
                fileName += "_08";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_06_08;
            }
        } else if (leftDiameter == EXTRUDER_DIAMETER_0_8) {
            fileName += "_08";
            if (rightDiameter == EXTRUDER_DIAMETER_0_2) {
                fileName += "_02";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_08_02;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_4) {
                fileName += "_04";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_08_04;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_6) {
                fileName += "_06";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_08_06;
            } else if (rightDiameter == EXTRUDER_DIAMETER_0_8) {
                fileName += "_08";
                printFileId = R.raw.a400_eveling_xy_calibration_on_print_08_08;
            }
        }
        if (printFileId == -1) {
            return Observable.just(false);
        }
        fileName += ".gcode";
        File printFile = copyPrintFile(printFileId, fileName);
        if (printFile == null) {
            return Observable.just(false);
        }
        mPrintFile = new FabLocalFile(printFile);
        mParser.destroy();
        mParser.startParse(mPrintFile, IMachine.WorkType.FDM);
        return mParser.getParseProgressObservable()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .takeUntil(progress -> progress == 100)
                .filter(progress -> progress == -1 || progress == 100)
                .flatMap(progress -> {
                    if (progress == 100) {
                        mNewPrintController.reset();
                        mEstimatedTime = mParser.getEstimatedTime();
                        mWorkspace.setPrintFile(mPrintFile);
                        mNewPrintController.setFile(mPrintFile);
                        mNewPrintController.setTotalLines(mParser.getTotalLinesCount());
                        mFileParserSubject.onNext(true);
                        return Observable.just(true);
                    } else {
                        return Observable.just(false);
                    }

                });
    }

    private File copyPrintFile(int printFileId, String fileName) {
        InputStream is = mContext.getResources().openRawResource(printFileId);
        File file = null;
        try {
            file = new File(mContext.getCacheDir().getAbsoluteFile() + "/" + fileName);
            if (file.exists()) {
                file.delete();
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                int read;
                byte[] bytes = new byte[20480];
                while ((read = is.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }
        } catch (Exception e) {
            file = null;
            LogHelper.log(e);
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {

            }
        }
        return file;
    }

    public void requestMachineResume() {
        mWaitingSubject.onNext(true);
        mNewPrintController.resume();
    }

    public void requestMachineStop() {
        mWaitingSubject.onNext(true);
        mNewPrintController.stop();
    }

    public void requestMachinePause() {
        mWaitingSubject.onNext(true);
        mNewPrintController.pause();
    }

    public String getFileName() {
        return mPrintFile.getName();
    }

    public Bitmap getGcodeThumbnail() {
        return mParser.getGcodeThumbnail();
    }

    public Observable<ResponseStructure> setCalibrationMode(int mode) {
        return mA400Machine.getFDMController().setCalibrationMode(mode)
                .doOnNext(responseStructure -> mIsSetCalibrationMode = responseStructure.isSuccess());
    }

    public Observable<ResponseStructure> setExtruderTemperature(int toolheadIndex, int extruderIndex, int temperature) {
        return mA400Machine.getFDMController().setExtruderTemperature(toolheadIndex, extruderIndex, temperature);
    }

    public FilamentState getFilamentStateValue() {
        return mFilamentStateSubject.getValue();
    }

    public void setFilamentState(FilamentState filamentState) {
        mFilamentStateSubject.onNext(filamentState);
    }

    public Observable<FilamentState> getFilamentStateObservable() {
        return mFilamentStateSubject.hide();
    }

    public Observable<FilamentState> checkoutExtruderTemperature() {
        FilamentState value = mFilamentStateSubject.getValue();
        return setExtruderTemperature(0, 0, (int) (((int) value.getLeftTarget()) == 0 ? 210 : value.getLeftTarget()))
                .flatMap(structure -> value.getExtruderNum() == 2 ?
                        setExtruderTemperature(0, 1, (int) (((int) value.getRightTarget()) == 0 ? 210 : value.getRightTarget()))
                        : Observable.just(structure))
                .flatMap(structure -> getFilamentStateObservable());
    }

    public SubjectHolder<FdmToolhead.FdmToolheadStatus> getToolheadStatusSubjectHolder(int toolheadIndex) {
        return mA400Machine.getFDMController().getToolheadStatusSubjectHolder(toolheadIndex);
    }

    public Observable<ResponseStructure> setAllTargetTemperature(int temperature) {
        return mA400Machine.getMachineController()
                .getHeatedBed().setAllTargetTemperature(temperature);
    }

    public SubjectHolder<HeatedBed.HeatedBedStatus> getHeatedBedStatusSubjectHolder() {
        return mA400Machine.getMachineController()
                .getHeatedBed()
                .getHeatedBedStatusSubjectHolder();
    }

    public Observable<Boolean> getFilamentSubjectObservable() {
        return mNewPrintController.getFilamentSubjectObservable();
    }

    public Observable<Boolean> getFileParserObservable() {
        return mFileParserSubject.hide();
    }

    public void setFilament(boolean state) {
        mNewPrintController.setFilament(state);
    }

    public Observable<ResponseStructure> requestActivatedExtrusion(int type, float lengthIn, float speedIn, float lengthOut, float speedOut) {
        return mA400Machine.getFDMController().requestActivatedExtrusion(type, lengthIn, speedIn, lengthOut, speedOut);
    }

    public Observable<Boolean> getWaitingObservable() {
        return mWaitingSubject.hide();
    }

    public Observable<Boolean> getEnclosureSubjectObservable() {
        return mNewPrintController.getEnclosureSubjectObservable();
    }

    public void setEnclosure(boolean state) {
        mNewPrintController.setEnclosure(state);
    }

    public void setPowerOutageFlag(boolean flag) {
        mNewPrintController.setPowerOutageFlag(flag);
    }

    public Observable<PrintProgress> getUpdateProgressObservable() {
        return mPrintProgressSubject.hide();
    }

    public Observable<PrintEvent> getPrintEventObservable() {
        Observable<PrintEvent> printEventObservable = mNewPrintController.getPrintEventObservable();
        printEventObservable
                .as(bindToLifecycle())
                .subscribe(printEvent -> mWaitingSubject.onNext(false), LogHelper::log);
        return printEventObservable;
    }

    public void initPrint() {
        boolean isPrinting = MachineOperationStatus.isPrinting(mNewPrintController.getPrintState());
        if (isPrinting) {
            // Initializing from last printing
            setTimeToUpdateProgress();
        } else {
            startPrint();
        }
        updateProgress();
    }

    public void startPrint() {
        mWaitingSubject.onNext(true);
        mCompositeDisposable.clear();
        // Power Panic
        boolean powerOutageFlag = mNewPrintController.getRecoveryFlag();
        if (powerOutageFlag) {
            Logger.d("Try Power Loss recovering..");
            mNewPrintController.recover();
        } else {
            mNewPrintController.start();
        }
        // Update
        setTimeToUpdateProgress();
    }

    private void setTimeToUpdateProgress() {
        Disposable subscribe = Observable.interval(0, 2, TimeUnit.SECONDS)
                .takeUntil(tick -> mNewPrintController.getPrintState() == SYSTEM_STATUS_COMPLETED.value())
                .filter(tick -> MachineOperationStatus.isPrinting(mNewPrintController.getPrintState()))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    updateProgress();
                }, Throwable::printStackTrace);
        mCompositeDisposable.add(subscribe);
    }

    private void updateProgress() {
        float p = mNewPrintController.getProgress();
        // formula: remaining = (1 - p) * p * elapsed / p + (1 - p) * (1 - p) * ETA
        int elapsed = mNewPrintController.getTickCounter().getCount();
        int remaining = (int) ((1 - p) * elapsed + (1 - p) * (1 - p) * mEstimatedTime);
        final int percentage = (int) (100 * p);
        mPrintProgressSubject.onNext(new PrintProgress(remaining, percentage));
    }

    public Observable<Integer> getPrintStateObservable() {
        Observable<Integer> printStateObservable = mNewPrintController.getPrintStateObservable();
        printStateObservable
                .as(bindToLifecycle())
                .subscribe(integer -> mWaitingSubject.onNext(MachineOperationStatus.isPrintChange(integer)), LogHelper::log);
        return printStateObservable;
    }

    public void onResume() {
        mNewPrintController.subscribeTookHeadSpeed();
        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
            mA400Machine.getMachineController().getEnclosure().subscribeEnclosureInfo();
        }
        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isAirPurifierAvailable) {
            mA400Machine.getMachineController().getAirPurifier().subscribeAirPurifierStatusChange();
        }
        mA400Machine.getFDMController().subscribeExtruderChange();
        mA400Machine.getMachineController().getHeatedBed().subscribeTemperatureChange();
    }

    public void onPause() {
        mNewPrintController.unSubscribeTookHeadSpeed();
        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
            mA400Machine.getMachineController().getEnclosure().unsubscribeEnclosureInfo();
        }
        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isAirPurifierAvailable) {
            mA400Machine.getMachineController().getAirPurifier().unsubscribeAirPurifierStatusChange();
        }
        mA400Machine.getFDMController().unSubscribeExtruderChange();
        mA400Machine.getMachineController().getHeatedBed().unsubscribeTemperatureChange();
    }

    public boolean isPrinting() {
        return MachineOperationStatus.isPrinting(mNewPrintController.getPrintState());
    }

    public boolean isCalibrationMode() {
        return mIsSetCalibrationMode || SYSTEM_STATUS_XY_CALIBRATING.valueEquals(mNewPrintController.getPrintState());
    }

    public Observable<ResponseStructure> exitCalibrationMode() {
        return mA400Machine.getFDMController().exitCalibration(false)
                .flatMap(responseStructure -> coolDownToolHead())
                .flatMap(coolDownBedIfHave -> coolDownBedIfHave());

    }

    private Observable<ResponseStructure> coolDownToolHead() {
        return mA400Machine.getFDMController().setAllExtruderTemperature(0);
    }

    private Observable<ResponseStructure> coolDownBedIfHave() {
        MachineController machineController = mA400Machine.getMachineController();
        return (machineController.getHeatedBed() != null) ? machineController.getHeatedBed().setZoneTargetTemperature(0, 0) : Observable.just(new ResponseStructure());
    }

//    public void cool() {
//        coolDownBedIfHave();
//        coolDownToolHead();
//    }

    public boolean isDoubleExtruder() {
        return mA400Machine.getFDMController().getHeadType() == HEAD_3DP_DOUBLE_EXTRUDER;
    }

    public Observable<ResponseStructure> cool() {
        return coolDownToolHead().flatMap(coolDownBedIfHave -> coolDownBedIfHave());
    }

    public enum XYPrintState {
        SET_TEMPERATURE_FAIL,
        INIT_PRINT_FILE_FAIL,
        CHECK_HOME_FAIL,
        WEATING_HEATING,
        HEATING_SUCCESS;
    }
}
