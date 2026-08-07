package fabscreen.features.machinetools.setup.singledual.loadfilament;

import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.FDMController;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class HeatingNozzleDialogViewModel extends BaseViewModel {
    private final BehaviorSubject<float[]> mLeftTempsSubj = BehaviorSubject.createDefault(new float[]{0, 0});

    public HeatingNozzleDialogViewModel() {
        FDMController fdmController = getServiceContainer().getService(IMachine.class).getFDMController();
        fdmController.getToolheadStatusSubjectHolder().getObservable()
                .as(bindToLifecycle())
                .subscribe(status -> {
                    Extruder extruder = status.getExtruderList().get(0);
                    mLeftTempsSubj.onNext(new float[]{extruder.getTemperature(), extruder.getTargetTemperature()});
                }, LogHelper::log);
    }

    public Observable<float[]> getTempsObservable() {
        return mLeftTempsSubj.hide();
    }
}
