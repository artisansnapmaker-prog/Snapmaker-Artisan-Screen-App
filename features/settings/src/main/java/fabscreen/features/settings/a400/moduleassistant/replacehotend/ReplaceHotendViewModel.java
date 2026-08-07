package fabscreen.features.settings.a400.moduleassistant.replacehotend;

import androidx.annotation.NonNull;

import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.MachineController;
import fabscreen.platform.base.service.machine.entity.toolhead.FdmToolhead;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

public class ReplaceHotendViewModel extends BaseViewModel {
    private static final String TAG = "ReplaceHotendViewModel";

    public static final int IDLE = -1;
    public static final int LEFT_BUSY = 0;
    public static final int RIGHT_BUSY = 1;

    private final IMachine mMachine;
    private final BehaviorSubject<ReplaceProcess> mReplaceProcessSubj = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mMovingSubj = BehaviorSubject.create();
    // -1 idle; 0 left busy; 1 right busy.
    private final BehaviorSubject<Integer> mExtrudeSubj = BehaviorSubject.createDefault(IDLE);
    private final BehaviorSubject<int[]> mUserSelectTempSubj = BehaviorSubject.createDefault(new int[]{200, 200});
    private final BehaviorSubject<NozzleTemp> mNozzleTempSubj = BehaviorSubject.create();


    public ReplaceHotendViewModel() {
        mMachine = getServiceContainer().getService(IMachine.class);
        observeExtruderChange();
    }

    public Observable<ReplaceProcess> getReplaceProcessObservable() {
        return mReplaceProcessSubj.hide();
    }

    public Observable<int[]> getUserSelectTempObservable() {
        return mUserSelectTempSubj.hide();
    }

    public Observable<NozzleTemp> getNozzleTempObservable() {
        return mNozzleTempSubj.hide();
    }

    public Observable<Integer> getExtrudeObservable() {
        return mExtrudeSubj.hide();
    }

    @SuppressWarnings("rawtypes")
    public void heatUpNozzle() {
        Observable<ResponseStructure> heatLeftObservable = mMachine.getFDMController().setExtruderTemperature(0, 0, getUserSelectTempL());
        Observable<ResponseStructure> heatRightObservable = mMachine.getFDMController().setExtruderTemperature(0, 1, getUserSelectTempR());

        Observable.zip(heatLeftObservable, heatRightObservable, (responseStructure, responseStructure2) -> responseStructure.isSuccess() && responseStructure2.isSuccess())
                .doOnSubscribe(disposable -> mReplaceProcessSubj.onNext(ReplaceProcess.ON_HEATING_START))
                .flatMap(started -> getNozzleTempObservable())
                .as(bindToLifecycle())
                .subscribe(temps -> {
                    // Logger.t(TAG).d("Nozzle is heating up...temp: %s", temps.toString());
                    // 5 for tolerance.
                    if (temps.l >= getUserSelectTempL() - 5 && temps.r >= getUserSelectTempR() - 5 && mReplaceProcessSubj.getValue() == ReplaceProcess.ON_HEATING_START) {
                        mReplaceProcessSubj.onNext(ReplaceProcess.ON_HEATED);
                    }
                });

        mMachine.getFDMController().subscribeExtruderChange();
    }

    public void restartMainboard() {
        mMachine.getMachineController().restartMachine()
                .doOnSubscribe(disposable -> mReplaceProcessSubj.onNext(ReplaceProcess.ON_RESTART_BEGIN))
                .filter(ResponseStructure::isSuccess)
                .flatMap(response -> mMachine.onRestart())
                .filter(success -> success)
                .flatMap(success -> checkModuleListUntilGot())
                .as(bindToLifecycle())
                .subscribe(success -> mReplaceProcessSubj.onNext(ReplaceProcess.ON_SUCCESS), LogHelper::log);
    }

    public String[] getReplaceHotendName() {
        FdmToolhead.FdmToolheadStatus toolheadStatus = mMachine.getFDMController().getToolheadStatusSubjectHolder().getValue();
        float diameterL = toolheadStatus.getExtruderList().get(0).getDiameter();
        float diameterR = toolheadStatus.getExtruderList().get(1).getDiameter();
        return new String[]{"L " + diameterL + "mm", "R " + diameterR + "mm"};
    }

    public void setUserSelectedTempL(float temp) {
        int tempL = (int) temp;
        int tempR = mUserSelectTempSubj.getValue()[1];
        mUserSelectTempSubj.onNext(new int[]{tempL, tempR});
    }

    public void setUserSelectedTempR(float temp) {
        int tempL = mUserSelectTempSubj.getValue()[0];
        int tempR = (int) temp;
        mUserSelectTempSubj.onNext(new int[]{tempL, tempR});
    }

    public int getUserSelectTempL() {
        return mUserSelectTempSubj.getValue()[0];
    }

    public int getUserSelectTempR() {
        return mUserSelectTempSubj.getValue()[1];
    }

    public Observable<Boolean> moveToolheadToTopCenter() {
        MachineController controller = mMachine.getMachineController();
        MachineInfo machineInfo = mMachine.getMachineInfoSubjectHolder().getValue();

        Vector machineSize = machineInfo.size;
        Vector dest = new Vector();
        dest.setX(machineSize.getX() / 2);
        dest.setY(machineSize.getY() / 2);
        Vector dest1 = new Vector();
        dest1.setZ(machineSize.getZ() / 2);

        controller.homeIfNotYet(0)
                .doOnSubscribe(disposable -> mMovingSubj.onNext(true))
                .filter(result -> result == 0)
                /*.doOnNext(result ->Logger.t(TAG).d("When will this be called?"))*/
                .flatMap(result -> controller.updateCoordinateSystemIfNot(0))
                .doOnSubscribe(disposable -> Logger.t(TAG).d("Homed. Updating coordinate..."))
                .flatMap(machineStatus -> controller.gotoAbsolutePosition(dest, 6000))
                .flatMap(machineStatus -> controller.gotoAbsolutePosition(dest1, 2400))
                .doOnSubscribe(disposable -> Logger.t(TAG).d("Moving to top dest: %s", dest.toString()))
                .doOnNext(response -> mMovingSubj.onNext(false))
                .as(bindToLifecycle())
                .subscribe(response -> {
                    // Move finish.
                }, LogHelper::log);

        return mMovingSubj.hide();
    }

    public void unloadFilament(int extruderIndex) {

        switchExtruder(extruderIndex)
                .doOnSubscribe(disposable -> mExtrudeSubj.onNext(extruderIndex))
                .concatMap((Function<Boolean, ObservableSource<Boolean>>) aBoolean -> aBoolean ? mMachine.getFDMController().requestActivatedExtrusion(0, 50, 200, 100, 150).map(ResponseStructure::isSuccess) : Observable.just(false))
                .doOnNext(unloaded -> mExtrudeSubj.onNext(IDLE))
                .doOnError(e -> mExtrudeSubj.onNext(IDLE))
                .as(bindToLifecycle())
                .subscribe();
    }

    private Observable<Boolean> switchExtruder(int targetIndex) {
        // Extruder state is refreshing by subscription, which guarantees the state is latest.
        return mMachine.getFDMController().getToolheadStatusSubjectHolder().getValue().getExtruderList().get(targetIndex).getState() == 1 ?
                Observable.just(true)
                : mMachine.getFDMController().switchExtruder(0, targetIndex).map(ResponseStructure::isSuccess);
    }

    private void observeExtruderChange() {
        mMachine.getFDMController().getToolheadStatusSubjectHolder(0).getObservable()
                .as(bindToLifecycle())
                .subscribe(toolheadStatus -> {
                    if (toolheadStatus == null) return;

                    int tempL = (int) toolheadStatus.getExtruderList().get(0).getTemperature();
                    int tempR = (int) toolheadStatus.getExtruderList().get(1).getTemperature();
                    int rcvTargetL = (int) toolheadStatus.getExtruderList().get(0).getTargetTemperature();
                    int rcvTargetR = (int) toolheadStatus.getExtruderList().get(1).getTargetTemperature();

//                    Logger.t(TAG).d("Extruder status %s", toolheadStatus);

                    emitTemps(tempL, tempR, rcvTargetL, rcvTargetR);
//                    emitFilamentStatus(filamentStatusL, filamentStatusR);
                }, LogHelper::log);
    }

    /**
     * User has confirmed that filament in extruders are cleared.
     * User can confirm this only when current process is ON_HEATED.
     */
    public void filamentClearConfirmed() {
        if (mReplaceProcessSubj.getValue() != ReplaceProcess.ON_HEATED) return;
        mReplaceProcessSubj.onNext(ReplaceProcess.ON_FILAMENT_CLEARED);
        coolDownHotend();
    }

    private void emitTemps(int tempL, int tempR, int targetL, int targetR) {
        NozzleTemp nozzleTemp = new NozzleTemp();
        nozzleTemp.l = tempL;
        nozzleTemp.r = tempR;
        nozzleTemp.targetL = targetL;
        nozzleTemp.targetR = targetR;
        mNozzleTempSubj.onNext(nozzleTemp);
    }

    private void coolDownHotend() {
        // turn on fan
        setFanState(true).as(bindToLifecycle()).subscribe();

        mMachine.getFDMController().stopExtruderHeat().as(bindToLifecycle()).subscribe();
        getNozzleTempObservable().as(bindToLifecycle()).subscribe(nozzleTemp -> {
//            Logger.t(TAG).d("Nozzle is cooling down... temp: %s", nozzleTemp.toString());
            if (nozzleTemp.l <= 45 && nozzleTemp.r <= 45 && mReplaceProcessSubj.getValue() == ReplaceProcess.ON_FILAMENT_CLEARED) {
                mMachine.getFDMController().unSubscribeExtruderChange();
                powerOffToolhead();
            }
        });
    }

    private void powerOffToolhead() {
        // Turn off fan(necessary?), then power off the toolhead.
        setFanState(false)
                .concatMap(success -> mMachine.getMachineController().startReplacePartsMode(true))
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        mReplaceProcessSubj.onNext(ReplaceProcess.ON_READY_FOR_REPLACE);
                    } else {
                        mReplaceProcessSubj.onNext(ReplaceProcess.ON_ERROR);
                    }
                });
    }

    @SuppressWarnings("rawtypes")
    private Observable<Boolean> setFanState(boolean on) {
        int fanSpeed = on ? 255 : 0;
        Observable<ResponseStructure> leftFanObservable = mMachine.getFDMController().setFanSpeed(0, 0, fanSpeed);
        Observable<ResponseStructure> rightFanObservable = mMachine.getFDMController().setFanSpeed(0, 1, fanSpeed);
        return Observable.zip(leftFanObservable, rightFanObservable, (responseStructure, responseStructure2) -> responseStructure.isSuccess() && responseStructure2.isSuccess());
    }

    private Observable<Boolean> checkModuleListUntilGot() {
        return Observable.interval(5, TimeUnit.SECONDS)
                .flatMap(tick -> Observable.just(mMachine.getMachineInfoSubjectHolder().getValue().moduleList != null))
                .takeUntil(listNotNull -> listNotNull)
                .filter(listNotNull -> listNotNull);
    }

    /**
     * Cool down both the hotend before stop.
     */
    public Observable<Boolean> stopReplacement() {
        return mMachine.getFDMController().setExtruderTemperature(0, 0, 0)
                .zipWith(
                        mMachine.getFDMController().setExtruderTemperature(0, 1, 0),
                        (responseStructure, responseStructure2) -> responseStructure.isSuccess() && responseStructure2.isSuccess()
                );
    }

    @Override
    protected void onCleared() {
        // unsubscribe
        mMachine.getFDMController().unSubscribeExtruderChange();
        super.onCleared();
    }

    enum ReplaceProcess {
        ON_HEATING_START,
        ON_HEATED,
        ON_FILAMENT_CLEARED,
        ON_READY_FOR_REPLACE,
        ON_RESTART_BEGIN,
        ON_SUCCESS,
        ON_ERROR
    }

    public static class NozzleTemp {
        public int l;
        public int r;
        public int targetL;
        public int targetR;

        @NonNull
        @Override
        public String toString() {
            return "NozzleTemp{" +
                    "l=" + l +
                    ", r=" + r +
                    ", targetL=" + targetL +
                    ", targetR=" + targetR +
                    '}';
        }
    }
}
