package fabscreen.features.print.j1platform;

import static fabscreen.platform.base.RoutePath.PRINT_PRINT_J1_AJUSTMENT_MENT_CONTAINER;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.lib.print.PrintListener;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.service.machine.entity.parts.Fan;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.CircularProgressView;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class PrintJ1Fragment extends BaseFragment {
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_PRINTING = 1;
    private static final int STATUS_PAUSED = 2;
    private static final int STATUS_COMPLETED = 3;

    @BindView(R2.id.cpv_j1_print_progress)
    CircularProgressView mCpvProgress;
    @BindView(R2.id.tv_j1_print_current_mode)
    TextView mTvPrintMode;
    @BindView(R2.id.tv_j1_print_filename)
    TextView mTvFilename;
    @BindView(R2.id.tv_j1_print_remaining_time_value)
    TextView mTvRemainingTime;
    @BindView(R2.id.tv_j1_print_progress)
    TextView mTvProgress;
    @BindView(R2.id.rl_j1_print_notice_temperature)
    RelativeLayout mViewNotice;
    @BindView(R2.id.btn_j1_print_pause)
    TextView mBtnPause;
    @BindView(R2.id.btn_j1_print_resume)
    TextView mBtnResume;
    @BindView(R2.id.btn_j1_print_stop)
    TextView mBtnStop;
    @BindView(R2.id.btn_j1_print_complete)
    TextView mBtnComplete;
    @BindView(R2.id.btn_j1_print_stop_one_extruder)
    TextView mBtnStopOneExtruder;

    @BindView(R2.id.iv_j1_print_left_extruder_temp)
    ImageView mIvLeftExtruderTemp;
    @BindView(R2.id.iv_j1_print_right_extruder_temp)
    ImageView mIvRightExtruderTemp;
    @BindView(R2.id.iv_j1_print_heat_bed_temp)
    ImageView mIvHeatedBedTemp;
    @BindView(R2.id.tv_j1_print_left_extruder_temp)
    TextView mTvLeftExtruderTemp;
    @BindView(R2.id.tv_j1_print_right_extruder_temp)
    TextView mTvRightExtruderTemp;
    @BindView(R2.id.tv_j1_print_heated_bed_temp)
    TextView mTvHeatedBedTemp;
    @BindView(R2.id.tv_j1_print_fan_speed_l)
    TextView mTvFanSpeedL;
    @BindView(R2.id.tv_j1_print_fan_speed_r)
    TextView mTvFanSpeedR;
    @BindView(R2.id.tv_j1_print_work_speed)
    TextView mTvWorkSpeed;
    @BindView(R2.id.rl_j1_print_details_adjustment)
    RelativeLayout mRlAdjustment;

    boolean mIsFirstLeftExtruderTemp = false;
    boolean mIsFirstRightExtruderTemp = false;
    boolean mIsFirstHeatedBedTemp = false;

    private IRouter mRouter;
    private IPrintWorkspace mWorkspace;
    private NewPrintController mNewPrintController;
    private IMachine mJ1Machine;
    private final int mIsBackUpMode = 0;

    private int mHeadType;
    // Mock data temporary.
    private float mEstimatedTime = 0;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    // indicates the the machine is moving, hence buttons should be disabled temporarily.
    private final BehaviorSubject<Boolean> mWaitingSubject = BehaviorSubject.createDefault(false);
    private int mPrintStatus;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mJ1Machine = ServiceContainer.getInstance().getService(IMachine.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRouter = ServiceContainer.getInstance().getService(IRouter.class);
        mWorkspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        mNewPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
        initPrint();
    }

    @Override
    public void onPause() {
        super.onPause();
        unSubscribeTemperature();
        mNewPrintController.unSubscribeExtruderWorkSpeedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                }, LogHelper::log);
    }

    @Override
    public void onResume() {
        super.onResume();

        subscribeTemperature();
        mNewPrintController.subscribeExtruderWorkSpeedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                }, LogHelper::log);
        bindMachineStatusToView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_print;
    }

    void subscribeTemperature() {
        mJ1Machine.getFDMController().subscribeExtruderChange();
        mJ1Machine.getFDMController().subscribeFanChange();
        mJ1Machine.getMachineController().getHeatedBed().subscribeTemperatureChange();
        mNewPrintController.subscribePrintModeStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                }, LogHelper::log);
    }

    void unSubscribeTemperature() {
        mJ1Machine.getFDMController().unSubscribeExtruderChange();
        mJ1Machine.getFDMController().unSubscribeFanChange();
        mJ1Machine.getMachineController().getHeatedBed().unsubscribeTemperatureChange();
        mNewPrintController.unsubscribePrintModeStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                }, LogHelper::log);
    }

    void bindMachineStatusToView() {
        mJ1Machine.getFDMController().getToolheadStatusSubjectHolder(0)
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolheadStatus -> {
                    Fan leftFan = fdmToolheadStatus.getFanList().get(0);
                    Extruder toolHeadExtruder = fdmToolheadStatus.getExtruderList().get(0);
                    float t0temp = toolHeadExtruder.getTemperature();
                    float t0TargetTemp = toolHeadExtruder.getTargetTemperature();
                    int fanSpeed = (int) (leftFan.getSpeedLevel() / 255f * 100);
                    // Product requirements: For the first time the current temperature is higher than
                    // the target temperature, it needs to turn yellow to remind the user that the temperature is being heated
                    if (t0temp <= t0TargetTemp && t0TargetTemp != 0 && !mIsFirstLeftExtruderTemp) {
                        mIvLeftExtruderTemp.setImageResource(R.drawable.icon_nozzle_left_yellow_64x64);
                    } else {
                        if (t0TargetTemp != 0) {
                            mIsFirstLeftExtruderTemp = true;
                        }
                        mIvLeftExtruderTemp.setImageResource(R.drawable.icon_nozzle_left_normal_64x64);
                    }
                    mTvLeftExtruderTemp.setText(String.format(Locale.ENGLISH, "%.0f/%.0f℃", t0temp, t0TargetTemp));
                    // TODO: Percentage display, different execution headers
                    mTvFanSpeedL.setText(getString(R.string.print_j1_fan_speed_l, fanSpeed));
                }, LogHelper::log);

        mJ1Machine.getFDMController().getToolheadStatusSubjectHolder(1)
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolheadStatus -> {
                    Fan fan = fdmToolheadStatus.getFanList().get(0);
                    int fanSpeed = (int) (fan.getSpeedLevel() / 255f * 100);
                    Extruder toolHeadStatus = fdmToolheadStatus.getExtruderList().get(0);
                    float t1temp = toolHeadStatus.getTemperature();
                    float t1TargetTemp = toolHeadStatus.getTargetTemperature();
                    if (t1temp <= t1TargetTemp && t1TargetTemp != 0 && !mIsFirstRightExtruderTemp) {
                        mIvRightExtruderTemp.setImageResource(R.drawable.icon_nozzle_right_yellow_64x64);
                    } else {
                        if (t1TargetTemp != 0) {
                            mIsFirstRightExtruderTemp = true;
                        }
                        mIvRightExtruderTemp.setImageResource(R.drawable.icon_nozzle_right_normal_64x64);
                    }
                    mTvRightExtruderTemp.setText(String.format(Locale.ENGLISH, "%.0f/%.0f℃", t1temp, t1TargetTemp));
                    mTvFanSpeedR.setText(getString(R.string.print_j1_fan_speed_r, fanSpeed));
                }, LogHelper::log);

        mJ1Machine.getMachineController()
                .getHeatedBed()
                .getHeatedBedStatusSubjectHolder()
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(heatedBedStatus -> {
                    float heatedBedCurrentTemperature = heatedBedStatus.getZoneList().get(0).getCurrentTemperature();
                    int heatedBedTargetTemperature = heatedBedStatus.getZoneList().get(0).getTargetTemperature();
                    if (heatedBedCurrentTemperature <= heatedBedTargetTemperature && heatedBedTargetTemperature != 0 && !mIsFirstHeatedBedTemp) {
                        mIvHeatedBedTemp.setImageResource(R.drawable.icon_heated_bed_yellow_64x64);
                    } else {
                        if (heatedBedTargetTemperature != 0) {
                            mIsFirstHeatedBedTemp = true;
                        }
                        mIvHeatedBedTemp.setImageResource(R.drawable.icon_heated_bed_normal_64x64);
                    }
                    mTvHeatedBedTemp.setText(String.format(Locale.ENGLISH, "%.0f/%d℃", heatedBedCurrentTemperature, heatedBedTargetTemperature));
                }, LogHelper::log);

        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController()
                .getExtruderWorkSpeedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(speed -> {
                    mTvWorkSpeed.setText(speed + "mm/s");
                }, LogHelper::log);

        mNewPrintController
                .getPrintModeStatusObservable()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(integer -> {
                    int printModeStatusValue = (int) integer;
                    if (printModeStatusValue == -1) {
                        printModeStatusValue = ServiceContainer.getInstance().getService(IPrintWorkspace.class).getPrintMode();
                    }
                    mTvPrintMode.setText(getPrintModeStr(printModeStatusValue));
                }, LogHelper::log);
    }

    void initPrint() {
//        mNewPrintController.startWatchPrintIssueRequest();
        mEstimatedTime = mWorkspace.getEstimatedTime();
        boolean isPrinting = MachineOperationStatus.isPrinting(mNewPrintController.getPrintState());

        if (isPrinting) {
            // Initializing from last printing
            resumePrintFromHome();
        } else {
            // If mode is change
            float printModeXOffset = ServiceContainer.getInstance().getService(IPrintWorkspace.class).getPrintModeXOffset();
            if (printModeXOffset != 0) {
                Logger.d("requesting Print Start Offset...");
                mNewPrintController.requestPrintStartOffset(printModeXOffset, 0, 0)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(responseStructure -> {
                            Logger.d("Set Print Start Offset " + responseStructure.isSuccess());
                        }, LogHelper::log);
            }
            // Initialize a new print job.
            AndroidSchedulers.mainThread().scheduleDirect(this::startPrint, 300, TimeUnit.MILLISECONDS);
        }

        ServiceContainer.getInstance().getService(IMachine.class)
                .getMachineStatusSubjectHolder()
                .getObservable()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(machineStatus -> {
                    Logger.d("Machine work status " + machineStatus.status);
                    switch (machineStatus.status) {
                        case IMachine.WorkStatus.WORK_STATUS_IDLE:
                            break;
                        default:
                            break;
                    }
                }, LogHelper::log);

        mNewPrintController
                .getPrintStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    Logger.d("print state " + status);
                    mPrintStatus = status;
//                    mBtnStart.setVisibility(status == STATUS_IDLE ? Button.VISIBLE : Button.GONE);
                    mBtnPause.setVisibility(status == STATUS_PRINTING ? Button.VISIBLE : Button.GONE);
                    mBtnResume.setVisibility(status == STATUS_PAUSED ? Button.VISIBLE : Button.GONE);
                    mBtnStop.setVisibility((status != STATUS_IDLE && status != STATUS_COMPLETED) ? Button.VISIBLE : Button.GONE);
                    mBtnComplete.setVisibility(status == STATUS_COMPLETED ? Button.VISIBLE : Button.GONE);
                    mRlAdjustment.setVisibility(status == STATUS_COMPLETED ? View.GONE : View.VISIBLE);
                    if (status == STATUS_COMPLETED) {
                        updateProgress();
                    }
                }, LogHelper::log);

        // enable or disable buttons
        mWaitingSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(waiting -> {
//                    mBtnStart.setEnabled(!waiting);
                    mBtnPause.setEnabled(!waiting);
                    mBtnResume.setEnabled(!waiting);
                    mBtnStop.setEnabled(!waiting);
                    mBtnStopOneExtruder.setEnabled(!waiting);
                });

        mNewPrintController.getFilamentSubjectObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(triggered -> {
                    if (triggered) {
                        int sumFilamentStatus = 0;
                        sumFilamentStatus += mJ1Machine.getFDMController().getToolheadStatusSubjectHolder(0)
                                .getValue().getExtruderList().get(0).getFilamentStatus() ? 1 : 0;
                        sumFilamentStatus += mJ1Machine.getFDMController().getToolheadStatusSubjectHolder(1)
                                .getValue().getExtruderList().get(0).getFilamentStatus() ? 2 : 0;

                        DecisionDialog decisionDialog = DecisionDialog.create(requireContext())
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, true, true)
                                .setTitle(R.string.control_load_filament_failed)
                                .setFirstTv(R.string.all_ok, R.color.select_dialog_orange_txt, (dialog, which) -> {
                                    mNewPrintController.setFilament(true);
                                    dialog.dismiss();
                                });
                        switch (sumFilamentStatus) {
                            case 1:
                                decisionDialog.setContent(R.string.error_left_extruder_unable_discharge);
                                break;
                            case 2:
                                decisionDialog.setContent(R.string.error_right_extruder_unable_discharge);
                                break;
                            case 3:
                            default:
                                decisionDialog.setContent(R.string.error_double_extruder_unable_discharge);
                                break;
                        }
                        decisionDialog.show();
                    }
                });

        Observable.combineLatest(mNewPrintController.getPrintStateObservable()
                        , mJ1Machine.getMachineController().getHeatedBed().getHeatedBedStatusSubjectHolder().getObservable(),
                        (status, heatedBedStatus) -> (heatedBedStatus.getZoneList().get(0).getCurrentTemperature() > 40 && status == STATUS_COMPLETED))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(showNotice -> {
                    mViewNotice.setVisibility(showNotice ? Button.VISIBLE : Button.GONE);
                });

        updateProgress();
    }

    public void resumePrintFromHome() {

        Disposable sub;
        // Update
        sub = Observable.interval(0, 2, TimeUnit.SECONDS)
                .takeUntil(tick -> ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState() == STATUS_COMPLETED)
                .filter(tick -> ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState() == STATUS_PRINTING)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    updateProgress();
                }, Throwable::printStackTrace);
        mCompositeDisposable.add(sub);
    }

    private void startPrint() {
        mCompositeDisposable.clear();
        NewPrintController NewPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
        IFile file1 = mWorkspace.getPrintFile();
        mTvFilename.setText(file1.getName());
//        NewPrintController.reset();
        NewPrintController.setFile(file1);
        NewPrintController.setTotalLines(mWorkspace.getFileTotalLineCount());
        setNewPrintControllerListener(NewPrintController);

        // Power Panic
        mWaitingSubject.onNext(true);
        boolean powerOutageFlag = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getRecoveryFlag();

        if (powerOutageFlag) {
            Logger.d("Try Power Loss recovering..");
            NewPrintController.recover();
        } else {
            int printMode = mWorkspace.getPrintMode();
            Logger.d("print mode in workspace %d", printMode);
            if (printMode == IPrintWorkspace.PRINT_MODE_CLONE || printMode == IPrintWorkspace.PRINT_MODE_MIRROR) {
                mBtnStopOneExtruder.setVisibility(View.VISIBLE);
            }
            mTvPrintMode.setText(getPrintModeStr(printMode));

            mNewPrintController.requestChangePrintMode(printMode)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(resultStructure -> {
                        NewPrintController.start();
                        Logger.d("requesting print mode... " + resultStructure.isSuccess());
                    }, LogHelper::log);
        }


        // Update
        Disposable sub = Observable.interval(0, 2, TimeUnit.SECONDS)
                .takeUntil(tick -> ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState() == STATUS_COMPLETED)
                .filter(tick -> ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState() == STATUS_PRINTING)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    updateProgress();
                }, Throwable::printStackTrace);
        mCompositeDisposable.add(sub);
    }

    private void setNewPrintControllerListener(NewPrintController NewPrintController) {
        NewPrintController.setListener(new PrintListener() {
            @Override
            public void onStartSuccess() {
                // oh we started
                Logger.i("Print started.");
                mWaitingSubject.onNext(false);
                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().reset();
                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().start();
            }

            @Override
            public void onStartFailed(int retCode) {
                switch (retCode) {
                    case 202: {
                        Logger.w("Filament used out, unable to start printing.");
//                        handleFilamentRunOut(result -> {
//                            if (result == PrintFragment.HandleFilamentRunOutCallback.RESULT_CANCEL) {
//                                Logger.d("Load canceled, exiting.");
//                                back();
//                            }
//                        });
                        break;
                    }
                    case 203: {
                        Logger.d("Unable to start printing, enclosure door open detected.");
//                        handleEnclosureDoorPaused();
                        break;
                    }
                    default: {
                        Logger.w("Unable to start printing, ret code %d", retCode);
                        DecisionDialog.create(requireContext())
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                                .setContent(getString(R.string.print_warning_start_unable) + retCode)
                                .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                                    dialog.dismiss();
                                    ServiceContainer.getInstance().getService(IRouter.class).routeToHome().startAndClear(requireContext());
                                }).show();
                        break;
                    }
                }
            }

            @Override
            public void onPauseSuccess() {
                Logger.i("Print paused.");
                mWaitingSubject.onNext(false);
                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().stop();
            }

            @Override
            public void onPauseFailed(int retCode) {
                Logger.w("Unable to pause printing.");
                mWaitingSubject.onNext(false);
                // Just a confirm
                DecisionDialog.create(requireContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContent(getString(R.string.print_warning_pause_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                        }).show();
            }

            @Override
            public void onResumeSuccess() {
                Logger.i("Print resumed.");
                mWaitingSubject.onNext(false);
                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().start();
            }

            @Override
            public void onResumeFailed(int retCode) {
                mWaitingSubject.onNext(false);
                switch (retCode) {
                    case 202: {
                        Logger.d("Filament used out, unable to resume printing.");
//                        handleFilamentRunOut(null);
                        break;
                    }
                    case 203: {
//                        handleEnclosureDoorPaused();
                        break;
                    }
                    default: {
                        Logger.w("Unable to resume printing.");
                        DecisionDialog.create(requireContext())
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setContent(getString(R.string.print_warning_resume_unable) + retCode)
                                .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                                    dialog.dismiss();
                                }).show();
                        break;
                    }
                }
            }

            @Override
            public void onResumeFromPowerOutageSuccess() {
                mWaitingSubject.onNext(false);
                // we resumed from power outage
//                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().setPowerOutageFlag(false);
                Logger.i("Print recovered.");

//                // clear flag when resume success
//                Disposable sub = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().resetErrorFlag()
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(success -> {
//                            Logger.d("Error flag removed.");
//                            ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().clearPowerOutageFlag();
//                        }, LogHelper::log);
//                mCompositeDisposable.add(sub);

                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().load();
                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().start();
            }

            @Override
            public void onResumeFromPowerOutageFailed(int retCode) {

                mWaitingSubject.onNext(false);
                switch (retCode) {
                    case 202: {
                        Logger.d("Filament used out, failed to recover from power loss.");
//                        handleFilamentRunOut(result -> {
//                            if (result == PrintFragment.HandleFilamentRunOutCallback.RESULT_CANCEL) {
//                                Logger.d("Load canceled, exiting.");
//                                // Clear power outage flag before exiting.
//                                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().setPowerOutageFlag(false);
//                                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().resetErrorFlag()
//                                        .observeOn(AndroidSchedulers.mainThread())
//                                        .as(bindToLifecycle())
//                                        .subscribe(success -> {
//                                            Logger.d("Error flag removed.");
//                                            ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().clearPowerOutageFlag();
//                                            back();
//                                        }, e -> {
//                                            LogHelper.log(e);
//                                            back();
//                                        });
//                            }
//                        });
                        break;
                    }
                    case 203: {
//                        handleEnclosureDoorPaused();
                        break;
                    }
                    default: {
                        Logger.w("Failed to recover from power loss.");
                        DecisionDialog.create(requireContext())
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setContent(getString(R.string.print_warning_resume_unable) + retCode)
                                .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                                    dialog.dismiss();
//                                    ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().setPowerOutageFlag(false);
                                    ServiceContainer.getInstance().getService(IRouter.class).routeToHome().startAndClear(requireContext());
                                }).show();
                        break;
                    }
                }
            }

            @Override
            public void onStopSuccess() {
                mWaitingSubject.onNext(false);
                Logger.i("print stopped.");
                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().stop();
                ServiceContainer.getInstance().getService(IRouter.class).routeToHome().startAndClear(getContext());
                requireActivity().finish();
            }

            @Override
            public void onStopFailed(int retCode) {
                Logger.w("Unable to stop printing, ret code %d", retCode);
                mWaitingSubject.onNext(false);
                DecisionDialog.create(requireContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContent(getString(R.string.print_warning_stop_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                            ServiceContainer.getInstance().getService(IRouter.class).routeToHome().startAndClear(requireContext());
                        }).show();
            }

            @Override
            public void onFinishSuccess() {
                Logger.i("Print Finished.");
                mWaitingSubject.onNext(false);

                ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().stop();


//                Logger.d("Print job costs %s.", BaseApplication.formatTime(ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().getCount()));
                // Finish Print.
//                ((PrintActivity) requireActivity()).gotoPrintCompleteFragment();
            }

            @Override
            public void onFinishFailed(int retCode) {
                Logger.w("Unable to finish printing, ret code %d", retCode);
                mWaitingSubject.onNext(false);
                DecisionDialog.create(requireContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContent(getString(R.string.print_warning_finish_unable) + retCode)
                        .setFirstTv(getString(R.string.all_confirm), R.color.select_dialog_orange_txt, (dialog, which) -> {
                            dialog.dismiss();
                        }).show();
            }
        });
    }

    private void updateProgress() {
        float p = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getProgress();

        // formula: remaining = (1 - p) * p * elapsed / p + (1 - p) * (1 - p) * ETA
        int elapsed = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().getCount();
        int remaining = (int) ((1 - p) * elapsed + (1 - p) * (1 - p) * mEstimatedTime);
        mTvRemainingTime.setText(formatTime(remaining));

        final int percentage = (int) (100 * p);
        mTvProgress.setText(mPrintStatus == STATUS_COMPLETED ? "100" : "" + percentage);
        mCpvProgress.setPercentage(mPrintStatus == STATUS_COMPLETED ? 100 : percentage);
    }

    public static String formatTime(double time) {
        int hour = (int) (time) / 3600;
        int minute = ((int) (time) % 3600) / 60;
        int second = ((int) (time) % 60);

        if (hour < 1) {
            return ServiceContainer.getInstance().getService(IAppService.class).getApp().getString(fabscreen.platform.base.R.string.date_helper_format_time_minute_second, minute, second);
        } else {
            return ServiceContainer.getInstance().getService(IAppService.class).getApp().getString(fabscreen.platform.base.R.string.date_helper_format_time_hour_minute, hour, minute);
        }
    }

    @OnClick(R2.id.rl_j1_print_details_adjustment)
    void onClickAdjustment() {
        playNormalClickSound();
        ServiceContainer.getInstance().getService(IRouter.class)
                .routeWithClassPath(PRINT_PRINT_J1_AJUSTMENT_MENT_CONTAINER)
                .start(getContext());
    }

    @OnClick(R2.id.btn_j1_print_stop)
    void onClickControlStop() {
        playNormalClickSound();
        DecisionDialog.create(getActivity())
                .setDialogStatus(DecisionDialog.BTN_TWO, false, false, false, true)
                .setType(DecisionDialog.WARMING_TYPE)
                .setContent(R.string.j1_print_stop_job_msg)
                .setContentColor(R.color.palette_grey_french)
                .needMoreHeight()
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_grey_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setSecondTv(R.string.all_stop, R.color.select_dialog_orange_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NewPrintController NewPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
                        mWaitingSubject.onNext(true);
                        NewPrintController.stop();
                    }
                }).show();

    }

    @OnClick(R2.id.btn_j1_print_pause)
    void onClickControlPause() {
        playNormalClickSound();
        DecisionDialog.create(getActivity())
                .setDialogStatus(DecisionDialog.BTN_TWO, false, false, false, true)
                .setContent(R.string.j1_print_pause_job_msg)
                .setContentColor(R.color.palette_grey_french)
                .setType(DecisionDialog.WARMING_TYPE)
                .needMoreHeight()
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_grey_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setSecondTv(R.string.all_pause, R.color.select_dialog_orange_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NewPrintController NewPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
                        mWaitingSubject.onNext(true);
                        NewPrintController.pause();
                    }
                }).show();


    }

    @OnClick(R2.id.btn_j1_print_resume)
    void onClickControlResume() {
        playNormalClickSound();
        NewPrintController NewPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
        mWaitingSubject.onNext(true);
        NewPrintController.resume();
    }

    @OnClick(R2.id.btn_j1_print_complete)
    void onClickComplete() {
        playNormalClickSound();
        mRouter.routeToHome().startAndClear(getContext());
    }


    @OnClick(R2.id.btn_j1_print_stop_one_extruder)
    void onClickStopOneExtruder() {
        playNormalClickSound();

        DecisionDialog.create(getActivity())
                .setDialogStatus(DecisionDialog.BTN_THREE, false, false, false, true)
                .setContent(R.string.j1_print_stop_extruder)
                .setContentColor(R.color.palette_grey_french)
                .setType(DecisionDialog.TIP_TYPE)
                .needMoreHeight()
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_grey_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setSecondTv(R.string.print_extruder_left, R.color.select_dialog_orange_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        stopOneExtruder(0);
                    }
                })
                .setThirdTv(R.string.print_extruder_right, R.color.select_dialog_orange_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        stopOneExtruder(1);
                    }
                }).show();

    }

    public void stopOneExtruder(int index) {
        mWaitingSubject.onNext(true);
        NewPrintController NewPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
        NewPrintController.requestStopOneExtruder(index)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(resultStructure -> {
                    mWaitingSubject.onNext(false);
                    if (resultStructure.isSuccess()) {
                        Logger.d("request stop index %d success", index);
                        mBtnStopOneExtruder.setVisibility(View.GONE);
                    } else {
                        Logger.d("request stop index %d failed", index);
                    }
                }, e -> {
                    mWaitingSubject.onNext(false);
                    LogHelper.log(e);
                });
    }

    public String getPrintModeStr(int printMode) {
        String printModeName = "UNKNOWN";
        switch (printMode) {
            case IPrintWorkspace.PRINT_MODE_NORMAL:
                printModeName = getString(R.string.print_print_mode_standard);
                break;
            case IPrintWorkspace.PRINT_MODE_DUAL_EXTRUDER_BACK_UP:
                printModeName = getString(R.string.print_print_mode_back_up);
                break;
            case IPrintWorkspace.PRINT_MODE_CLONE:
                printModeName = getString(R.string.print_print_mode_clone);
                break;
            case IPrintWorkspace.PRINT_MODE_MIRROR:
                printModeName = getString(R.string.print_print_mode_mirror);
                break;
            default:
                printModeName = "UNKNOWN";
                break;
        }
        return printModeName;
    }
}

