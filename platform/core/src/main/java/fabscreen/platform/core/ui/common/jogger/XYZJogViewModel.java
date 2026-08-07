package fabscreen.platform.core.ui.common.jogger;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.IStructure;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.core.ui.data.MoveController;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import static fabscreen.platform.core.ui.data.MoveController.Direction.IDLE;
import static fabscreen.platform.core.ui.data.MoveController.getInstance;

public class XYZJogViewModel extends BaseViewModel {
    private final float[] mLinearStepWidths = {0.1f, 1f, 10f, 100f};
    private final float[] mRotaryStepWidths = {0.2f, 1f, 10f, 90f};
    private int mWidthPos;
    public MachineInfo mMachineInfo;
    private final BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<MoveController.Direction> mMovingStatusSubject = BehaviorSubject.createDefault(IDLE);
    public IMachine mMachine;

    public XYZJogViewModel() {
        mMachine = ServiceContainer.getInstance().getService(IMachine.class);
        mMachineInfo = mMachine.getMachineInfoSubjectHolder().getValue();
        mWidthPos = 1;
    }

    public void changeStepWidth(int pos) {
        mWidthPos = pos;
    }

    public boolean isRotaryAvailable() {
        return mMachineInfo.isRotaryAvailable;
    }

    public Observable<ResponseStructure> moveToPosition(MoveController.Direction direction) {
        if (direction == null) {
            ResponseStructure<IStructure> iStructureResponseStructure = new ResponseStructure<>();
            iStructureResponseStructure.resultProp.setValue(1);
            return Observable.just(iStructureResponseStructure);
        }

        float stepWidth = MoveController.Direction.isRotary(direction) ? mRotaryStepWidths[mWidthPos] : mLinearStepWidths[mWidthPos];
        mIsMovingSubject.onNext(true);
        mMovingStatusSubject.onNext(direction);
        return getInstance()
                .stepToPosition(direction, stepWidth)
                .doOnNext(response -> {
                    mIsMovingSubject.onNext(false);
                    mMovingStatusSubject.onNext(IDLE);
                })
                .doOnError(e -> {
                    mIsMovingSubject.onNext(false);
                    mMovingStatusSubject.onNext(IDLE);
                });
    }

    public Observable<Boolean> getMovingObservable() {
        return mIsMovingSubject.hide();
    }

    public Observable<MoveController.Direction> getMoveStateObservable() {
        return mMovingStatusSubject.hide();
    }

    public boolean getIsRotaryAvailable() {
        return mMachine.getMachineInfoSubjectHolder().getValue().isRotaryAvailable;
    }
}
