package fabscreen.features.machinetools.control.a400.viewmodel;

import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.FDMController;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.entity.toolhead.FdmToolhead;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

public class A400FilamentControlViewModel extends BaseViewModel {
    public final int A400_DEFAULT_HEATING = 200;
    public final int A400_DEFAULT_TEMPERATURE_FLUCTUATION = 3;
    private FDMController mFdmController;
    private int mNowActivateNozzle = -1;
    PublishSubject<FilamentTager> mTargetSubject = PublishSubject.create();

    public A400FilamentControlViewModel() {
        mFdmController = ServiceContainer.getInstance().getService(IMachine.class).getFDMController();
        mTargetSubject.sample(300, TimeUnit.MILLISECONDS)
                .flatMap(filamentTager -> mFdmController.setExtruderTemperature(0, filamentTager.index, filamentTager.progress))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(structure -> {
                    if (structure.isSuccess()) {
                        Logger.d("Temperature set.");
                    } else {
                        Logger.d("Temperature set fail.");
                    }
                }, LogHelper::log);

    }


    public boolean isDoubleExtruder() {
        return mFdmController.getHeadType() == Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
    }

    public Observable<FdmToolhead.FdmToolheadStatus> getToolheadStatusObservable() {
        return mFdmController.getToolheadStatusSubjectHolder(0).getObservable();
    }

    public FdmToolhead.FdmToolheadStatus getToolheadStatusValue() {
        return mFdmController.getToolheadStatusSubjectHolder(0).getValue();
    }

    public void setTargetTemp(int index, int progress) {
        mTargetSubject.onNext(new FilamentTager(index, progress));
    }

    public Observable<Boolean> switchExtruder(int toolheadIndex, int extruderIndex) {
        return mFdmController.switchExtruder(toolheadIndex, extruderIndex).flatMap(responseStructure -> Observable.just(responseStructure.isSuccess()));
    }

    public Observable<ResponseStructure> FilamentMove(boolean isLoad) {
        if (isLoad) {
            return mFdmController.requestActivatedExtrusion(0, 90, 240, 0, 0);
        } else {
            return mFdmController.requestActivatedExtrusion(0, 10, 300, 100, 300);
        }
    }

    public Observable<Integer> getActivateNozzle() {
        return mFdmController.getToolheadStatusSubjectHolder().getObservable().flatMap(fdmToolheadStatus -> {
            if (fdmToolheadStatus.getExtruderList().size() > 1) {
                for (int i = 0; i < fdmToolheadStatus.getExtruderList().size(); i++) {
                    if (fdmToolheadStatus.getExtruderList().get(i).getState() == 1) {
                        mNowActivateNozzle = i;
                        return Observable.just(mNowActivateNozzle);
                    }
                }
                mNowActivateNozzle = -1;
                return Observable.just(mNowActivateNozzle);
            } else {
                mNowActivateNozzle = fdmToolheadStatus.getExtruderList().get(0).getState() == 1 ? 0 : -1;
                return Observable.just(mNowActivateNozzle);
            }
        });
    }

    public void subscribeDataChange() {
        mFdmController.subscribeExtruderChange();
    }

    public void unSubscribeDataChange() {
        mFdmController.unSubscribeExtruderChange();
    }

    public int getNowActivateNozzle() {
        return mNowActivateNozzle;
    }

    public Observable<ResponseStructure> getRequestActivatedExtrusion() {
        return mFdmController.getRequestActivatedExtrusionObservable();
    }

    public Observable<Boolean> moveToProperPosition() {
        float initX = 200f;
        float initY = 400f;
        float initZ = 250f;
        Vector vector = new Vector();
        vector.setX(initX);
        vector.setY(initY);
        vector.setZ(initZ);
        IMachine.WorkType workType = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType;
        return  checkHome().flatMap(homed -> homed ? moveToPosition(vector, workType == IMachine.WorkType.FDM) : Observable.just(false));
    }

    private Observable<Boolean> moveToPosition(Vector vector, boolean isUsingG53) {
        IMachine a400Machine = ServiceContainer.getInstance().getService(IMachine.class);
        return a400Machine.getMachineController().updateCoordinateSystem(0)
                .flatMap(result -> result.coordinateID == 0 ? a400Machine.getMachineController().gotoAbsolutePosition(vector) : Observable.just((new ResponseStructure())))
                .flatMap(resultStructure -> {
                    if (resultStructure.isSuccess()) {
                        return isUsingG53 ? Observable.just(true) : a400Machine.getMachineController().updateCoordinateSystem(1).flatMap(result -> Observable.just(true));
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

    class FilamentTager {
        int index;
        int progress;

        public FilamentTager(int index, int progress) {
            this.index = index;
            this.progress = progress;
        }
    }
}
