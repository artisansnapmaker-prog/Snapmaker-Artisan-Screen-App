package fabscreen.features.machinetools.calibration.j1Platform.levelingBed;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.J1CalibrationBaseFragment;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.FDMController;
import fabscreen.platform.base.service.machine.entity.module.HeatedBed;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LevelingBedAuxiliaryCalibrationInfoFragment extends J1CalibrationBaseFragment {
    @BindView(R2.id.iv_show_image)
    ImageView mIvShowImage;
    @BindView(R2.id.tv_nozzle_state_left)
    TextView mShowStateLeft;
    @BindView(R2.id.iv_nozzle_state_left)
    ImageView mIvShowStateLeft;
    @BindView(R2.id.tv_nozzle_state_right)
    TextView mShowStateRight;
    @BindView(R2.id.iv_nozzle_state_right)
    ImageView mIvShowStateRight;
    @BindView(R2.id.tv_bed_state)
    TextView mShowStateBed;
    @BindView(R2.id.iv_bed_state)
    ImageView mIvShowStateBed;
    @BindView(R2.id.tv_text_line_heating)
    TextView mTvLineHeating;
    @BindView(R2.id.btn_next)
    Button mBtNext;
    FDMController fdmController;

    private boolean isReady = false;
    private final BehaviorSubject<Boolean> mLeftTemperatureState = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<Boolean> mRightTemperatureState = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<Boolean> mBedTemperatureState = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<Integer> mBedTemperatureSubj = BehaviorSubject.create();

    public static Fragment newInstance() {
        return new LevelingBedAuxiliaryCalibrationInfoFragment();
    }

    private static Boolean getTemperatureState(Boolean leftTemperatureState, Boolean rightTemperatureState, boolean bed) {
        return leftTemperatureState && rightTemperatureState && bed;
    }

    private static Boolean getTemperatureState(Boolean leftTemperatureState, Boolean rightTemperatureState) {
        return leftTemperatureState && rightTemperatureState;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fdmController = ServiceContainer.getInstance().getService(IMachine.class).getFDMController();
        initView();
        goHome();
    }

    @Override
    public void heating() {
        fdmController.setExtruderTemperature(0, 0, 220)
                .flatMap(success -> success.isSuccess() ? fdmController.setExtruderTemperature(1, 0, 220) : Observable.just(success))
                .flatMap(success -> success.isSuccess() ? ServiceContainer.getInstance().getService(IMachine.class).getMachineController().getHeatedBed().setZoneTargetTemperature(0, 60) : Observable.just(success))
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (!responseStructure.isSuccess()) {
                        errorBack("setExtruderTemperature", responseStructure.resultProp.getValue());
                    }
                }, LogHelper::log);

        Observable.zip(mLeftTemperatureState, mRightTemperatureState, LevelingBedAuxiliaryCalibrationInfoFragment::getTemperatureState)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        if (!isReady) {
                            isReady = true;
                            DecisionDialog.create(getContext())
                                    .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                                    .setType(DecisionDialog.TIP_TYPE)
                                    .setContent(getString(R.string.nozzle_heated_moved))
                                    .setFirstTv(getResources().getString(R.string.guide_got_it), R.color.select_dialog_orange_txt, ((dialog, which) -> {
                                        dialog.dismiss();
                                        fabMoving.show();
                                        Vector vector = new Vector();
                                        vector.setX(50);
                                        fdmController.CalibrationDrawBackZ()
                                                .flatMap(responseStructure -> ServiceContainer.getInstance().getService(IMachine.class).getMachineController().MoveRelativeHome(vector, 0))
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .as(bindToLifecycle())
                                                .subscribe(responseStructure -> {
                                                    fabMoving.dismiss();
                                                    if (responseStructure.isGeneralError()) {
                                                        back();
                                                    }
                                                }, LogHelper::log);
                                    }))
                                    .show();
                        }
                    }
                });
    }

    private void goHome() {
        fabHoming.show();
        IMachine service = ServiceContainer.getInstance().getService(IMachine.class);
        service.getMachineController().home(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(homeState -> {
                    fabHoming.dismiss();
                    if (homeState == 0) {
                        showRemovePlateDialog(mBedTemperatureSubj.getValue());
                    }
                }, LogHelper::log);
    }

    private void initView() {
        Glide.with(this)
                .load(R.drawable.gif_leveling_bed_auxiliary_calibration_info)
                .into(mIvShowImage);
        fdmController
                .getToolheadStatusSubjectHolder(0)
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolHeadInfo -> {
                            Extruder extruder = fdmToolHeadInfo.getExtruderList().get(0);
                            int leftTemperature = (int) extruder.getTemperature();
                            int targetTemperature = (int) extruder.getTargetTemperature();
                            mShowStateLeft.setText(leftTemperature + "/" + targetTemperature + "°C");
                            boolean b = targetTemperature != 0 && leftTemperature >= targetTemperature - 3;
                            if (b) {
                                mIvShowStateLeft.setImageResource(R.drawable.icon_nozzle_left_orange_64x64);
                                mLeftTemperatureState.onNext(b);
                            } else {
                                mIvShowStateLeft.setImageResource(R.drawable.icon_nozzle_left_yellow_64x64);
                            }

                        }
                );

        fdmController
                .getToolheadStatusSubjectHolder(1)
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolHeadInfo -> {
                            Extruder extruder = fdmToolHeadInfo.getExtruderList().get(0);
                            int leftTemperature = (int) extruder.getTemperature();
                            int targetTemperature = (int) extruder.getTargetTemperature();
                            mShowStateRight.setText(leftTemperature + "/" + targetTemperature + "°C");
                            boolean b = targetTemperature != 0 && leftTemperature >= targetTemperature - 3;
                            if (b) {
                                mIvShowStateRight.setImageResource(R.drawable.icon_nozzle_right_orange_64x64);
                                mRightTemperatureState.onNext(b);
                            } else {
                                mIvShowStateRight.setImageResource(R.drawable.icon_nozzle_right_yellow_64x64);
                            }
                        }
                );

        ServiceContainer.getInstance().getService(IMachine.class).getMachineController().getHeatedBed()
                .getHeatedBedStatusSubjectHolder()
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(bedStatus -> {
                            HeatedBed.ZoneInfo zoneInfo = bedStatus.getZoneList().get(0);
                            int targetTemperature = zoneInfo.getTargetTemperature();
                            int temperature = (int) zoneInfo.getCurrentTemperature();
                            mBedTemperatureSubj.onNext(temperature);
                            mShowStateBed.setText(temperature + "/" + targetTemperature + "°C");
                            boolean b = targetTemperature != 0 && temperature >= targetTemperature - 10;
                            if (b) {
                                mIvShowStateBed.setImageResource(R.drawable.icon_heated_bed_orange_64x64);
                                mBedTemperatureState.onNext(b);
                            } else {
                                mIvShowStateBed.setImageResource(R.drawable.icon_heated_bed_yellow_64x64);
                            }
                        }
                );

        Observable.zip(mLeftTemperatureState, mRightTemperatureState, mBedTemperatureState, LevelingBedAuxiliaryCalibrationInfoFragment::getTemperatureState)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mTvLineHeating.setText(R.string.heated_success);
                    mBtNext.setEnabled(success);
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_auxiliary_calibration_extruder_heating;
    }

    @OnClick(R2.id.btn_next)
    void onClickNext() {
        playNormalClickSound();
        fabMoving.show();
        fdmController.setCalibrationMode(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success.isSuccess()) {
                        moveContent(1);
                    } else {
                        errorBack("setCalibrationMode", success.resultProp.getValue());
                    }
                }, LogHelper::log);
    }

    private void moveContent(int content) {
        fdmController.calibratePointByIndex(content, true)
                .flatMap(success -> success.isSuccess() ? fdmController.calibratePointByIndex(content + 1, false) : Observable.just(success))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    fabMoving.dismiss();
                    if (success.isSuccess()) {
                        if (getActivity() == null) return;
                        ((LevelingBedAuxiliaryCalibrationActivity) getActivity()).gotoLevelingBedAuxiliaryCalibration();
                    } else {
                        errorBack("calibratePointByIndex", success.resultProp.getValue());
//                        DecisionDialog.create(getContext())
//                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
//                                .setContent("机器发生了异常,请退出: " + ProcessName + "\tCode:" + initTemperature)
//                                .setFirstTv(getString(R.string.all_quit), R.color.selector_switch_thumb, ((dialog, which) -> {
//                                    ServiceContainer.getInstance().getService(IMachine.class).getFDMController()
//                                            .exitCalibration(false)
//                                            .observeOn(AndroidSchedulers.mainThread())
//                                            .as(bindToLifecycle())
//                                            .subscribe(success -> {
//                                                dialog.dismiss();
//                                                if (success.isSuccess()) {
//
//                                                    ServiceContainer.getInstance().getService(IRouter.class).routeToCalibrationPage().startAndClear(getContext());
//                                                    finishActivityWithResultOk();
//                                                }
//                                            });
//                                }))
//                                .show();
                    }

                });
    }

    @Override
    public void onResume() {
        super.onResume();
        ServiceContainer.getInstance().getService(IMachine.class)
                .getFDMController().subscribeExtruderChange();
        ServiceContainer.getInstance().getService(IMachine.class)
                .getMachineController().getHeatedBed().subscribeTemperatureChange();
    }

    @Override
    public void onPause() {
        super.onPause();
        ServiceContainer.getInstance().getService(IMachine.class)
                .getFDMController().unSubscribeExtruderChange();
        ServiceContainer.getInstance().getService(IMachine.class)
                .getMachineController().getHeatedBed().unsubscribeTemperatureChange();
    }
}
