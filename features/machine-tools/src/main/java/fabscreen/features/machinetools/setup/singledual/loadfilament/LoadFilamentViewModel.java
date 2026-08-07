package fabscreen.features.machinetools.setup.singledual.loadfilament;

import com.orhanobut.logger.Logger;

import java.util.List;

import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.FDMController;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LoadFilamentViewModel extends BaseViewModel {

    private final FDMController mFdmController;
    private final BehaviorSubject<Integer> mLoadFilamentSubject = BehaviorSubject.create();
    private final BehaviorSubject<Float> mE0TemperatureSubject = BehaviorSubject.create();
    private final BehaviorSubject<Float> mE1TemperatureSubject = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mHeatingResultSubject = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mIsLoadingSubject = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<Boolean> mIsHeatingSubject = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);
    private int mCurrentLoading;
    private boolean mE0NeedTriggered = true;
    private boolean mE1NeedTriggered = true;
    private List<Extruder> mExtruders;
    private boolean mIsSwitchExtruder = false;


    public LoadFilamentViewModel() {
        IMachine machine = getServiceContainer().getService(IMachine.class);
        mFdmController = machine.getFDMController();
        // X375 Y15 Z25
        Vector vector = new Vector();
        vector.setX(375);
        vector.setY(15);
        vector.setZ(25);
        // Home first
        machine.getMachineController().home(0)
                .doOnSubscribe(disposable -> mIsMovingSubject.onNext(true))
//                .flatMap(result -> MoveController.getInstance().stepToPosition(MoveController.Direction.DOWN, 100, 3000))
//                .flatMap(response -> MoveController.getInstance().stepToPosition(MoveController.Direction.RIGHT, 200, 6000))
//                .flatMap(response -> MoveController.getInstance().stepToPosition(MoveController.Direction.FORWARD, 100, 6000))
                .flatMap(response -> machine.getMachineController().gotoAbsolutePosition(vector, 6000))
                .doOnNext(result -> mIsMovingSubject.onNext(false))
                .doOnError(e -> mIsMovingSubject.onNext(false))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> watchExtruderChange(), LogHelper::log);
    }

    public void heatExtruders() {
        setExtruderTemperature(0, 205);
        setExtruderTemperature(1, 220);

        // waiting for extruder0 temperature reaching 205, ignoring extruder1.
        mE0TemperatureSubject
                .doOnSubscribe(disposable -> mIsHeatingSubject.onNext(true))
                .filter(temperature -> temperature >= 200)
                .take(1)
                .doOnNext(temperature -> mIsHeatingSubject.onNext(false))
                .doOnError(throwable -> mIsHeatingSubject.onNext(false))
                .as(bindToLifecycle())
                .subscribe(temperature -> {
//                    mIsHeatingSubject.onNext(false);
                }, LogHelper::log);
    }

    private void watchExtruderChange() {
        subscribeExtruder();
        mFdmController.getToolheadStatusSubjectHolder().getObservable()
                .as(bindToLifecycle())
                .subscribe(status -> {
                    mExtruders = status.getExtruderList();
                    mE0TemperatureSubject.onNext(mExtruders.get(0).getTemperature());
                    mE1TemperatureSubject.onNext(mExtruders.get(1).getTemperature());
                    boolean e0FilamentDetected = mExtruders.get(0).getFilamentStatus();
                    boolean e1FilamentDetected = mExtruders.get(1).getFilamentStatus();
                    onDetectionFilament(e0FilamentDetected, e1FilamentDetected);
                }, LogHelper::log);
    }

    private void onDetectionFilament(boolean e0, boolean e1) {
        if (mIsMovingSubject.getValue() || mIsLoadingSubject.getValue() || mIsSwitchExtruder)
            return;
        Logger.d("e0 is %1$s, e1 is %2$s", e0, e1);
        if (!e0 && mE0NeedTriggered) {
            onFilamentTriggered(0);
        }
        if (!e1 && mE1NeedTriggered) {
            onFilamentTriggered(1);
        }
    }

    private void onFilamentTriggered(int index) {
        float eTemperature = mExtruders.get(index).getTemperature();
        // Expect extruder temperature 205 celsius degrees, but we add more tolerance for temperature fluctuation(Current -5).
        if (eTemperature <= 200) {
            // not hot enough, ignore.
            Logger.d("Extruder %d temperature not enough, current %.0f", index, eTemperature);
            return;
        }

        if (mIsLoadingSubject.getValue()) {
            // is already loading, ignore
            Logger.d("is loading, ignore, %d", index);
            return;
        }

        if (mCurrentLoading == index) {
            startExtrudeFilament(mCurrentLoading);
        }
    }

    public void startExtrudeFilament(int index) {
        mFdmController.requestActivatedExtrusion(0, 160, 240, 16, 1500)
                .doOnSubscribe(disposable -> mIsLoadingSubject.onNext(true))
                .doOnNext(response -> mIsLoadingSubject.onNext(false))
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        mLoadFilamentSubject.onNext(index);
                        if (index == 0) {
                            mE0NeedTriggered = false;
                        } else {
                            mE1NeedTriggered = false;
                        }
                    } else {
                        throw new IllegalStateException("Extruder" + index + "extrude fail");
                    }
                }, LogHelper::log);
    }

    public void confirmLoad(int index) {
        // Cool down current loading Extruder with "stand by temperature".
        setExtruderTemperature(mCurrentLoading, 150);
        // We had at most 2 extruder for the loading procedure,
        // so we will load next extruder if unload extruder available.
        if (index < 2) {
            loadFilament(index);
        }
    }

    public void setExtruderTemperature(int index, int temperature) {
        mFdmController.setExtruderTemperature(0, index, temperature)
                .as(bindToLifecycle())
                .subscribe(response -> {
                }, LogHelper::log);
    }

    public void loadFilament(int extruderIndex) {

        // reset to true for wrong status before load
        if (extruderIndex == 0) {
            mE0NeedTriggered = true;
        } else {
            mE1NeedTriggered = true;
        }
        mCurrentLoading = extruderIndex;
        mIsSwitchExtruder = true;
        mFdmController.switchExtruder(0, extruderIndex)
                .as(bindToLifecycle())
                .subscribe(response -> {
                    mIsSwitchExtruder = false;
                }, LogHelper::log);
    }

    public Observable<Boolean> reset() {
        mE0NeedTriggered = true;
        mE1NeedTriggered = true;
        mCurrentLoading = 0;
        mIsSwitchExtruder = true;
        return mFdmController.switchExtruder(0, 0)
                .doOnNext(responseStructure -> mIsSwitchExtruder = false)
                .map(ResponseStructure::isSuccess);
    }

    public Observable<Integer> getLoadFilamentResultObservable() {
        return mLoadFilamentSubject.hide();
    }

    public void unsubscribeExtruder() {
        mFdmController.unSubscribeExtruderChange();
    }

    public void subscribeExtruder() {
        mFdmController.subscribeExtruderChange();
    }

    public Observable<Boolean> getHeatingObservable() {
        return mIsHeatingSubject.hide();
    }

    public Observable<Boolean> getLoadingObservable() {
        return mIsLoadingSubject.hide();
    }

    public Observable<Boolean> getMovingObservable() {
        return mIsMovingSubject.hide();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        unsubscribeExtruder();
    }
}
