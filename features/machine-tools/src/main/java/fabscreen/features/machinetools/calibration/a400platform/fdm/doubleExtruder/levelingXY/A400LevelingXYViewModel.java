package fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY;

import android.content.Context;

import java.util.ArrayList;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.structure.DeviationStructure;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.prop.ArrayProp;
import fabscreen.platform.base.view.BaseViewModel;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

public class A400LevelingXYViewModel extends BaseViewModel {
    public static final int ADJUST_VOLUME = 12;
    public static final float CHANGE_AMOUNT = 0.08f;
    private float AdjustX;
    private float AdjustY;

    NewPrintController NewPrintController;
    Context mContext;
    private IPrintWorkspace mWorkspace;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    // indicates the the machine is moving, hence buttons should be disabled temporarily.
    private BehaviorSubject<Boolean> mWaitingSubject = BehaviorSubject.createDefault(false);

    public A400LevelingXYViewModel() {
        super();
        NewPrintController = getServiceContainer().getService(IMachine.class).getNewPrintController();
        mContext = getServiceContainer().getService(IAppService.class).getAppContext();
        mWorkspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
    }

    public void setAdjustX(int progress) {
        AdjustX = progress * CHANGE_AMOUNT;
    }

    public void setAdjustY(int progress) {
        AdjustY = progress * CHANGE_AMOUNT;
    }

    public Observable<ResponseStructure> setAdjust() {
        return getServiceContainer().getService(IMachine.class).getFDMController().getExtruderOffset(0)
                .flatMap(responseStructure -> {
                    ArrayProp<DeviationStructure> dataProp = (ArrayProp) responseStructure.dataProp;
                    ArrayList<DeviationStructure> value = (ArrayList<DeviationStructure>) dataProp.getValue();
                    for (DeviationStructure d : value) {
                        if (d.getAxis() == 0) d.setValue(d.getValue() + AdjustX);
                        if (d.getAxis() == 1) d.setValue(d.getValue() + AdjustY);
                    }
                    return getServiceContainer().getService(IMachine.class).getFDMController().setExtruderOffset(0, value);
                });
    }
}
