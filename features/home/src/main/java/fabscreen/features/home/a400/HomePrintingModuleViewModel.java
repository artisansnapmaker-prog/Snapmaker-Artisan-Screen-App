package fabscreen.features.home.a400;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_COMPLETED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.controller.PrintEvent;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.core.ui.data.FilamentState;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class HomePrintingModuleViewModel extends BaseViewModel {
    private final BehaviorSubject<PrintProgress> mPrintProgressSubj = BehaviorSubject.createDefault(new PrintProgress(0, ""));
    private final BehaviorSubject<Boolean> mProcessingSubj = BehaviorSubject.createDefault(false);
    private final PublishSubject<PrintEvent> mPrintEventSubj = PublishSubject.create();
    private final IMachine mA400Machine;

    private final NewPrintController mNewPrintController;
    private final IPrintWorkspace mWorkspace;
    private final IGcodeParser mGcodeParser;
    private BehaviorSubject<FilamentState> mFilamentStateSubject = BehaviorSubject.createDefault(new FilamentState());
    private Disposable mToolheadStatusSubscribe;
    private IMachine.WorkType mWorkType;

    public HomePrintingModuleViewModel() {
        mA400Machine = getServiceContainer().getService(IMachine.class);
        mNewPrintController = mA400Machine.getNewPrintController();
        mWorkspace = getServiceContainer().getService(IPrintWorkspace.class);
        mGcodeParser = getServiceContainer().getService(IGcodeParser.class);
        mWorkType = mA400Machine.getMachineInfoSubjectHolder().getValue().workType;
        observePrintProgress();
    }

    public PrintModelInfo getPrintModelInfo() {
        return new PrintModelInfo(mWorkspace.getFileName(), mGcodeParser.getGcodeThumbnail());
    }

    public Observable<PrintProgress> getPrintProgressObservable() {
        return mPrintProgressSubj.hide();
    }

    public Observable<Integer> getPrintStateObservable() {
        Observable<Integer> printStateObservable = mNewPrintController.getPrintStateObservable();
        printStateObservable
                .as(bindToLifecycle())
                .subscribe(integer -> mProcessingSubj.onNext(MachineOperationStatus.isPrintChange(integer)), LogHelper::log);
        return printStateObservable;
    }

    public Observable<Boolean> getProcessingObservable() {
        return mProcessingSubj.hide();
    }

    public Observable<PrintEvent> getPrintEventObservable() {
        Observable<PrintEvent> printEventObservable = mNewPrintController.getPrintEventObservable();
        printEventObservable
                .as(bindToLifecycle())
                .subscribe(printEvent -> mProcessingSubj.onNext(false), LogHelper::log);
        return printEventObservable;
    }

    public void resumePrint() {
        mProcessingSubj.onNext(true);
        mNewPrintController.resume();
    }

    public void pausePrint() {
        mProcessingSubj.onNext(true);
        mNewPrintController.pause();
    }

    public void startPrint() {
        mProcessingSubj.onNext(true);
        mNewPrintController.start();
    }

    public void stopPrint() {
        mProcessingSubj.onNext(true);
        mNewPrintController.stop();
    }

    private void observePrintProgress() {
        Observable.interval(0, 2, TimeUnit.SECONDS)
                .takeUntil(tick -> mNewPrintController.getPrintState() == SYSTEM_STATUS_COMPLETED.value())
                .filter(tick -> MachineOperationStatus.isPrinting(mNewPrintController.getPrintState()))
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    // formula: remaining = (1 - p) * p * elapsed / p + (1 - p) * (1 - p) * ETA
                    updatePrintProgress();
                });
    }

    private void updatePrintProgress() {
        float p = mNewPrintController.getProgress();
        int elapsed = mNewPrintController.getTickCounter().getCount();
        int remain = (int) ((1 - p) * elapsed + (1 - p) * (1 - p) * mWorkspace.getEstimatedTime());
        mPrintProgressSubj.onNext(new PrintProgress((int) (p * 100), formatRemainTime(remain)));
    }

    private String formatRemainTime(int time) {
        //Remaining Time: 20d 13h 20min
        return formatTime(time);
    }

    @NonNull
    private String formatTime(int time) {
        int hour = time / 3600;
        int minute = (time % 3600) / 60;
        int second = (time % 60);

        String remainTime;

        if (hour < 1) {
            remainTime = ServiceContainer.getInstance().getService(IAppService.class).getApp().getString(fabscreen.platform.base.R.string.date_helper_format_time_minute_second, minute, second);
        } else {
            remainTime = ServiceContainer.getInstance().getService(IAppService.class).getApp().getString(fabscreen.platform.base.R.string.date_helper_format_time_hour_minute, hour, minute);
        }
        return remainTime;
    }

    public int getPrintStateValue() {
        mProcessingSubj.onNext(MachineOperationStatus.isPrintChange(mNewPrintController.getPrintState()));
        return mNewPrintController.getPrintState();
    }

    public void setFilament(boolean state) {
        mNewPrintController.setFilament(state);
    }

    public void setPowerOutageFlag(boolean flag) {
        mNewPrintController.setPowerOutageFlag(flag);
    }

    public boolean isIsFdm() {
        return mWorkType == IMachine.WorkType.FDM;
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

    public Observable<ResponseStructure> requestActivatedExtrusion(int type, float lengthIn, float speedIn, float lengthOut, float speedOut) {
        return mA400Machine.getFDMController().requestActivatedExtrusion(type, lengthIn, speedIn, lengthOut, speedOut);
    }

    public Observable<ResponseStructure> setExtruderTemperature(int toolheadIndex, int extruderIndex, int temperature) {
        return mA400Machine.getFDMController().setExtruderTemperature(toolheadIndex, extruderIndex, temperature);
    }

    public boolean isDoubleExtruder() {
        return mA400Machine.getFDMController().getHeadType() == HEAD_3DP_DOUBLE_EXTRUDER;
    }

    public Observable<Boolean> getFilamentSubjectObservable() {
        return mNewPrintController.getFilamentSubjectObservable();
    }

    public void fdmResume() {
        mA400Machine.getFDMController().subscribeExtruderChange();
        mA400Machine.getMachineController().getHeatedBed().subscribeTemperatureChange();
        if (mToolheadStatusSubscribe != null && !mToolheadStatusSubscribe.isDisposed())
            mToolheadStatusSubscribe.dispose();
        // Subscribe FDM extruder data, including extruder(s) temperature info, filament sensor state.
        mToolheadStatusSubscribe = mA400Machine.getFDMController().getToolheadStatusSubjectHolder(0)
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolheadStatus -> {
                    Extruder leftExtruder = fdmToolheadStatus.getExtruderList().get(0);
                    float leftExtruderTemp = leftExtruder.getTemperature();
                    float leftExtruderTargetTemp = leftExtruder.getTargetTemperature();
                    boolean leftExtruderFilamentStatus = leftExtruder.getFilamentStatus();
                    boolean hasMultipleExtruders = fdmToolheadStatus.getExtruderList().size() > 1;
                    setFilamentState(getFilamentStateValue().setFilamentState(0,
                            leftExtruderFilamentStatus,
                            leftExtruderTargetTemp,
                            leftExtruderTargetTemp != 0 && leftExtruderTargetTemp - 5 <= leftExtruderTemp,
                            leftExtruder.getState() == 1));
                    // If FDM ToolHead has two or more extruders
                    if (hasMultipleExtruders) {
                        Extruder rightExtruder = fdmToolheadStatus.getExtruderList().get(1);
                        float rightExtruderTemp = rightExtruder.getTemperature();
                        float rightExtruderTargetTemp = rightExtruder.getTargetTemperature();
                        boolean rightExtruderFilamentStatus = rightExtruder.getFilamentStatus();
                        setFilamentState(getFilamentStateValue().setFilamentState(1,
                                rightExtruderFilamentStatus,
                                rightExtruderTargetTemp,
                                rightExtruderTargetTemp != 0 && rightExtruderTargetTemp - 5 <= rightExtruderTemp,
                                rightExtruder.getState() == 1
                        ));
                    }
                }, LogHelper::log);
    }

    public void fdmPause() {
        mA400Machine.getFDMController().unSubscribeExtruderChange();
        mA400Machine.getMachineController().getHeatedBed().unsubscribeTemperatureChange();
        if (mToolheadStatusSubscribe != null && !mToolheadStatusSubscribe.isDisposed())
            mToolheadStatusSubscribe.dispose();
    }

    public static class PrintProgress {
        public int percentage;
        public String remainDesc;

        public PrintProgress(int percentage, @NonNull String remainDesc) {
            this.percentage = percentage;
            this.remainDesc = remainDesc;
        }
    }

    public static class PrintModelInfo {
        public String fileName;
        public Bitmap thumbnail;

        public PrintModelInfo(String name, Bitmap thumbnail) {
            this.fileName = name;
            this.thumbnail = thumbnail;
        }
    }

}
