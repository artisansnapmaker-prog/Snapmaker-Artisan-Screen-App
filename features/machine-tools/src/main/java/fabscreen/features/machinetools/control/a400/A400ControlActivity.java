package fabscreen.features.machinetools.control.a400;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.machinetools.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.structure.CoordinateSystemInfo;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.TOOLS_CONTROL_A400)
public class A400ControlActivity extends BaseActivity {
    public WarmTipDialog fabLoading;
    IMachine service;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        addFragment(R.id.fragment_container, A400ControlContainerFragment.newInstance());
        service = ServiceContainer.getInstance().getService(IMachine.class);
        fabLoading = WarmTipDialog.create(this)
                .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                .setProgressVisible(true)
                .setTitle(R.string.all_move_show)
                .setType(WarmTipDialog.TIP_TYPE)
                .setContent(R.string.all_move_show_content);

        if (MachineOperationStatus.SYSTEM_STATUS_IDLE.valueEquals(service.getNewPrintController().getPrintState())) {
            checkHome();
        }

    }

    public void goToEnclosureSettings() {
        addFragment(R.id.fragment_container, A400ControlEnclosureSettingsFragment.newInstance());
    }

    public void goToAirPurifierSettings() {
        addFragment(R.id.fragment_container, A400AirPurifierControlSettingsFragment.newInstance());
    }

    public void checkHome() {
        service.getMachineController()
                .pullCoordinate()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystemInfoResponseStructure -> {
                    CoordinateSystemInfo dataProp = coordinateSystemInfoResponseStructure.dataProp;
                    boolean homed = dataProp.getHomed();
                    if (!homed) {
                        goHome();
                    }
                });
    }

    public void goHome() {
        DecisionDialog.create(this)
                .setContent(R.string.a400_dialog_request_go_home_desc)
                .setDialogStatus(DecisionDialog.BTN_ONE, true, false, false, true)
                .setPic(R.drawable.pic_a400_request_go_home)
                .setType(DecisionDialog.TIP_TYPE)
                .setFirstTv(R.string.a400_go_home, R.color.select_dialog_blue_txt, ((dialog, which) -> {
                    dialog.dismiss();
                    fabLoading.show();
                    onHomeNext(service.getMachineController().updateCoordinateSystem(0)
                            .flatMap(machineStatus -> service.getMachineController().home(0)));

                })).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (service.getMachineController().getHomeResultObservable() == null) {
            fabLoading.dismiss();
            return;
        }
        onHomeNext(service.getMachineController().getHomeResultObservable());
    }

    private void onHomeNext(Observable<Integer> observable) {
        observable.flatMap(integer -> Observable.just(integer == 0))
                .flatMap(aBoolean -> {
                    if (aBoolean) {
                        if (service.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM) {
                            return Observable.just(aBoolean);
                        } else {
                            return service.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(machineStatus.coordinateID == 1));
                        }
                    } else {
                        return Observable.just(aBoolean);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    fabLoading.dismiss();
                }, log -> {
                    fabLoading.dismiss();
                    LogHelper.log(log);
                });
    }
}
