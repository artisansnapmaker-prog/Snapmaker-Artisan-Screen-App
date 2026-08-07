package fabscreen.features.home.a400;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_PAUSED;
import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_PRINTING;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.home.R;
import fabscreen.features.home.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.controller.PrintEvent;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.FilamentState;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.core.ui.view.StepIntroductionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class HomePrintingModuleFragment extends BaseFragment {
    @BindView(R2.id.iv_print_model)
    ImageView mIvPrintModel;
    @BindView(R2.id.tv_file_name)
    TextView mTvFileName;
    @BindView(R2.id.tv_progress)
    TextView mTvProgress;
    @BindView(R2.id.cpi_progress)
    CircularProgressIndicator mCpiProgress;
    @BindView(R2.id.tv_remain_time)
    TextView mTvRemainingTime;
    @BindView(R2.id.ib_pause)
    ImageButton mBtnPause;
    @BindView(R2.id.ib_resume)
    ImageButton mBtnResume;
    @BindView(R2.id.ib_stop)
    ImageButton mBtnStop;
    DecisionDialog mDecisionDialog;
    public FileParsingDialog fabWorkingChangeDialog;
    private boolean isStateFail = false;

    private HomePrintingModuleViewModel mViewModel;
    // printing/pause
    private int mButtonState;
    private IMachine mA400Machine;
    Disposable mDisCheckFilamentProcess;
    Disposable mDisCheckoutExtruderTemperature;
    private Disposable mPrintEventSubscribe;
    private Disposable mFilamentSubscribe;
    private StepIntroductionDialog checkFilamentProcessTipDialog;
    private DecisionDialog mIsShowFilamentDialog;
    private boolean isStop;

    public static HomePrintingModuleFragment newInstance() {
        return new HomePrintingModuleFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(HomePrintingModuleViewModel.class);
        mA400Machine = ServiceContainer.getInstance().getService(IMachine.class);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_home_printing;
    }

    private void initView() {
        fabWorkingChangeDialog = FileParsingDialog.create(getActivity());
        HomePrintingModuleViewModel.PrintModelInfo info = mViewModel.getPrintModelInfo();
        mIvPrintModel.setImageBitmap(info.thumbnail);
        mTvFileName.setText(info.fileName);
        mTvProgress.setText("0%");

        mViewModel.getPrintProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::updatePrintProgress, LogHelper::log);

        correctButtonState(mViewModel.getPrintStateValue());
        mViewModel.getPrintStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::correctButtonState, LogHelper::log);

        mViewModel.getProcessingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(processing -> {
                    mBtnPause.setEnabled(!processing);
                    mBtnResume.setEnabled(!processing);
                    mBtnStop.setEnabled(!processing);
                }, LogHelper::log);
    }

    private void updatePrintProgress(HomePrintingModuleViewModel.PrintProgress progress) {
        mTvProgress.setText(progress.percentage + "%");
        if (TextUtils.isEmpty(progress.remainDesc)) {
            mTvRemainingTime.setText(R.string.a400_print_detail_remaining_time_preparing);
        } else {
            mTvRemainingTime.setText(getString(R.string.print_remaining_time) + progress.remainDesc);
            mCpiProgress.setProgress(progress.percentage);
        }
    }

    private void onPrintControllerCallback(PrintEvent printEvent) {
        DecisionDialog decisionDialog = DecisionDialog.create(getContext());
        int retCode = printEvent.getErrorCode();
        switch (printEvent.getPrintEventState()) {
            case STATE_SUCCESS:
                // oh we started
                Logger.i("Print started.");
                isStateFail = false;
                break;
            case START_FAIL:
                Logger.w("Unable to start printing, ret code %d", retCode);
                isStateFail = true;
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setContent(retCode == 227 ? getString(R.string.a400_print_enclosure_operation_trigger) : (getString(R.string.print_warning_start_unable) + "\nretCode:" + retCode))
                        .setContentColor(R.color.palette_white_pure)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                decisionDialog.show();
                break;
            case PAUSE_SUCCESS:
                Logger.i("Print paused.");
                if (fabWorkingChangeDialog != null && fabWorkingChangeDialog.isShowing()) {
                    fabWorkingChangeDialog.dismiss();
                }
                break;
            case PAUSE_FAIL:
                Logger.w("Unable to pause printing, ret code %d", retCode);
                if (fabWorkingChangeDialog != null && fabWorkingChangeDialog.isShowing()) {
                    fabWorkingChangeDialog.dismiss();
                }
                if (retCode == 17) return;
                // Just a confirm
                decisionDialog = DecisionDialog.create(getContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                switch (retCode) {
                    case 222: {
                        mViewModel.setFilament(false);
                        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                            mDecisionDialog.dismiss();
                        }
                        return;
                    }
                    case 227:
                        decisionDialog.setContent(getString(R.string.a400_print_enclosure_operation_trigger));
                        break;
                    default:
                        decisionDialog.setContent(getString(R.string.print_warning_pause_unable) + "\nretCode:" + retCode);
                        break;
                }
                decisionDialog.show();
                break;
            case RESUME_SUCCESS:
                Logger.i("Print resumed.");
                break;
            case RESUME_FAIL:
                Logger.w("Unable to resume printing, ret code %d", retCode);
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                switch (retCode) {
                    case 222: {
                        mViewModel.setFilament(false);
                        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                            mDecisionDialog.dismiss();
                        }
                        return;
                    }
                    case 227: {
                        decisionDialog.setContent(getString(R.string.a400_print_enclosure_operation_trigger));
                        break;
                    }
                    default: {
                        decisionDialog.setContent(getString(R.string.print_warning_resume_unable) + "\nretCode:" + retCode);
                        break;
                    }
                }
                decisionDialog.show();
                break;
            case POWER_LOSS_RESUME_SUCCESS:
                Logger.i("Print recovered.");
                // we resumed from power outage
                mViewModel.setPowerOutageFlag(false);
                break;
            case POWER_LOSS_RESUME_FAIL:
                Logger.w("Failed to recover from power loss, ret code %d", retCode);
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                switch (retCode) {
                    case 222: {
                        mViewModel.setFilament(false);
                        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                            mDecisionDialog.dismiss();
                        }
                        return;
                    }
                    case 227: {
                        //ResumeFromPowerOutage
                        decisionDialog.setContent(getString(R.string.a400_dialog_print_enclosure_open_desc, getString(R.string.a400_print_resume_from_power_outage)));
                        break;
                    }
                    default: {
                        decisionDialog.setContent(R.string.print_warning_resume_unable);
                        break;
                    }
                }
                decisionDialog.show();
                break;
            case STOP_SUCCESS:
                Logger.i("print stopped.");
                if (isStop) return;
                isStop = true;
                if (fabWorkingChangeDialog != null && fabWorkingChangeDialog.isShowing()) {
                    fabWorkingChangeDialog.dismiss();
                }
                mRouter.routeToHome().startAndClear(getContext());
                break;
            case STOP_FAIL:
                Logger.w("Unable to stop printing, ret code %d", retCode);
                if (fabWorkingChangeDialog != null && fabWorkingChangeDialog.isShowing()) {
                    fabWorkingChangeDialog.dismiss();
                }
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setContent(R.string.print_warning_stop_unable)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                decisionDialog.show();
                break;
            case FINISH_SUCCESS:
                if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                    mDecisionDialog.dismiss();
                }
                Logger.i("Print Finished.");
                mRouter.routeToHome().startAndClear(getContext());
                break;
            case FINISH_FAIL:
                Logger.w("Unable to finish printing, ret code %d", retCode);
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setContent(R.string.print_warning_finish_unable)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                decisionDialog.show();
                break;
//            case OPEN_DOOR_PAUSE:
//                decisionDialog = DecisionDialog.create(getContext())
//                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
//                        .setType(DecisionDialog.WARMING_TYPE)
//                        .setContentColor(R.color.palette_white_pure)
//                        .setContent(getString(R.string.a400_print_enclosure_open, getString(mViewModel.isIsFdm() ? R.string.printing_toast_3dp_print : R.string.printing_toast_other)))
//                        .setFirstTv(R.string.a400_print_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
//                decisionDialog.show();
//                break;
            default:
                break;
        }
        if (decisionDialog != null && decisionDialog.isShowing()) {
            if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                mDecisionDialog.dismiss();
            }
            mDecisionDialog = decisionDialog;
        }
    }

    private void correctButtonState(int state) {
        if (mButtonState != state) {
            mButtonState = state;
            updateButtonState(false);
        }
    }

    private void updateButtonState(boolean withAnimation) {
        if (SYSTEM_STATUS_PRINTING.valueEquals(mButtonState)) {
            // Animate to printing
            if (mBtnResume.getVisibility() == View.VISIBLE) {
                animateToPrinting(withAnimation ? 200 : 0);
            } else {
                mBtnPause.setVisibility(View.VISIBLE);
            }
        } else if (SYSTEM_STATUS_PAUSED.valueEquals(mButtonState)) {
            // Animate to paused
            if (mBtnPause.getVisibility() == View.VISIBLE) {
                animateToPaused(withAnimation ? 200 : 0);
            } else {
                mBtnPause.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
            mA400Machine.getMachineController().getEnclosure().subscribeEnclosureInfo();
        }

        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isAirPurifierAvailable) {
            mA400Machine.getMachineController().getAirPurifier().subscribeAirPurifierStatusChange();
        }
        if (mPrintEventSubscribe != null && !mPrintEventSubscribe.isDisposed()) {
            mPrintEventSubscribe.dispose();
        }
        mPrintEventSubscribe = mViewModel.getPrintEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::onPrintControllerCallback, LogHelper::log);

        if (mFilamentSubscribe != null && !mFilamentSubscribe.isDisposed()) {
            mFilamentSubscribe.dispose();
        }
        if (mViewModel.isIsFdm()) {
            mViewModel.fdmResume();
            mFilamentSubscribe = mViewModel.getFilamentSubjectObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(triggered -> {
                        if (triggered) {
                            if (mIsShowFilamentDialog != null && mIsShowFilamentDialog.isShowing())
                                return;
                            mIsShowFilamentDialog = DecisionDialog.create(getContext())
                                    .setTitle(R.string.a400_print_dialog_warning_filament_run_out_title)
                                    .setType(DecisionDialog.WARMING_TYPE)
                                    .setContent(getString(R.string.a400_print_dialog_warning_filament_run_out_triggered_content,
                                            mViewModel.getFilamentStateValue().getFailureFilamentIndex() == 0 ?
                                                    getString(R.string.all_left_extruder_abbr) :
                                                    getString(R.string.all_right_extruder_abbr)))
                                    .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, true)
                                    .setPic(R.drawable.pic_a400_warning_112x112)
                                    .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> {
                                        mViewModel.setFilament(true);
                                        dialog.dismiss();
                                    }))
                                    .setSecondTv(getString(R.string.a400_print_dialog_warning_filament_run_out_action_load_filament), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                                        dialog.dismiss();
                                        enterFilamentRefillProcedure();
                                    });
                            mIsShowFilamentDialog.show();
                        }
                    });
        }
    }

    // filament refill procedure
    // 1. Checkout which extruder should refill filament;
    // 2. Checkout extruder temperature, heated up if target is 0;
    // 3. Checkout filament sensor is on or off, start filling filament process;
    // 4. extrude a certain length filament and ask user if ready. If not, continue extruding;
    // 5. user click continue printing, leave refill procedure and resume print.
    void enterFilamentRefillProcedure() {
        if (mDisCheckoutExtruderTemperature != null && !mDisCheckoutExtruderTemperature.isDisposed()) {
            mDisCheckoutExtruderTemperature.dispose();
        }
        FileParsingDialog loadingDialog = FileParsingDialog.create(requireContext()).setContent(R.string.a400_print_dialog_warning_filament_run_out_action_heating_nozzle);
        loadingDialog.show();
        mDisCheckoutExtruderTemperature = mViewModel.checkoutExtruderTemperature()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(filamentState -> {
                    if (filamentState.isTemperatureReached()) {
                        loadingDialog.dismiss();
                        CheckFilamentProcess();
                    }
                }, LogHelper::log);
    }

    private void CheckFilamentProcess() {
        if (mDisCheckoutExtruderTemperature != null && !mDisCheckoutExtruderTemperature.isDisposed()) {
            mDisCheckoutExtruderTemperature.dispose();
        }
        if (mDisCheckFilamentProcess != null && !mDisCheckFilamentProcess.isDisposed()) {
            mDisCheckFilamentProcess.dispose();
        }

        FilamentState mFilamentState = mViewModel.getFilamentStateValue();
        if (checkFilamentProcessTipDialog != null) {
            checkFilamentProcessTipDialog.dismiss();
        }
        checkFilamentProcessTipDialog = StepIntroductionDialog.create(requireContext());
        checkFilamentProcessTipDialog.setCanceledOnTouchOutSide(false);
        checkFilamentProcessTipDialog.setOnClickBack(v -> {
            checkFilamentProcessTipDialog.dismiss();
            mViewModel.setFilament(true);
            mDisCheckoutExtruderTemperature.dispose();
        });
        if (!mFilamentState.getNowFilamentState()) {
            checkFilamentProcessTipDialog.setImage(mViewModel.isDoubleExtruder() ?
                    R.drawable.pic_a400_3dp_double_extruder_pluck_ilament_644x362 :
                    R.drawable.pic_a400_3dp_pluck_ilament_644x362);
            checkFilamentProcessTipDialog.setTitle(R.string.a400_print_dialog_warning_filament_run_out_pull_out_filament_title);
            checkFilamentProcessTipDialog.setContent(R.string.a400_print_dialog_warning_filament_run_out_pull_out_filament_content);
            checkFilamentProcessTipDialog.show();
            mDisCheckFilamentProcess = mViewModel.getFilamentStateObservable()
                    .as(bindToLifecycle())
                    .subscribe(filamentState -> {
                        if (mFilamentState.getNowFilamentState()) {
                            checkFilamentProcessTipDialog.dismiss();
                            CheckFilamentProcess();
                        }
                    }, LogHelper::log);
        } else {
            checkFilamentProcessTipDialog.setImage(mViewModel.isDoubleExtruder() ?
                    R.drawable.pic_a400_3dp_double_extruder_insert_ilament_1065x388 :
                    R.drawable.pic_a400_3dp_insert_ilament_644x362);
            checkFilamentProcessTipDialog.setTitle(R.string.a400_print_dialog_warning_filament_run_out_insert_filament_title);
            checkFilamentProcessTipDialog.setContent(R.string.a400_print_dialog_warning_filament_run_out_insert_filament_content);
            checkFilamentProcessTipDialog.show();
            mDisCheckFilamentProcess = mViewModel.getFilamentStateObservable()
                    .as(bindToLifecycle())
                    .subscribe(filamentState -> {
                        if (!mFilamentState.getNowFilamentState()) {
                            checkFilamentProcessTipDialog.dismiss();
                            fillingFilamentProcess();
                        }
                    }, LogHelper::log);
        }
    }

    private void fillingFilamentProcess() {
        if (mDisCheckFilamentProcess != null && !mDisCheckFilamentProcess.isDisposed()) {
            mDisCheckFilamentProcess.dispose();
        }
        FileParsingDialog loadingDialog = FileParsingDialog.create(requireContext()).setContent(R.string.a400_print_dialog_warning_filament_run_out_action_start_loading);
        loadingDialog.show();
        mViewModel.requestActivatedExtrusion(0, 100, 240, 0, 0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(structure -> {
                    loadingDialog.dismiss();
                    if (structure.isSuccess()) {
                        Logger.d("Filament loaded.");
                        checkFilamentComplete();
                    } else {
                        Logger.d("Filament load fail.");
                        showFilamentExtruderFailedDialog(structure.resultProp.getValue());
                    }
                }, e -> {
                    loadingDialog.dismiss();
                    mViewModel.setFilament(true);
                    LogHelper.log(e);
                    showFilamentExtruderFailedDialog(10086);
                });
    }

    void checkFilamentComplete() {
        DecisionDialog.create(getContext())
                .setTitle(R.string.control_load_filament_success)
                .setContent(R.string.control_load_filament_success_content)
                .setType(DecisionDialog.NOTIFICATION_TYPE)
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.pic_a400_success_112x112)
                .setFirstTv(R.string.control_load_filament, R.color.select_dialog_white_txt, ((dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.setFilament(true);
                    fillingFilamentProcess();
                }))
                .setSecondTv(R.string.all_continue_printing, R.color.select_a400_dialog_success_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.setFilament(true);
                    if (isStateFail) {
                        mViewModel.startPrint();
                    } else {
                        mViewModel.resumePrint();
                    }
                }).show();
    }

    void showFilamentExtruderFailedDialog(int value) {
        DecisionDialog.create(getContext())
                .setContent(String.format("An unknown exception occurred, error code %d", value))
                .setType(DecisionDialog.WARMING_TYPE)
                .setDialogStatus(DecisionDialog.BTN_ONE, true, false, false, true)
                .setPic(R.drawable.pic_a400_warning_112x112)
                .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> {
                    mViewModel.setFilament(true);
                    dialog.dismiss();
                })).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
            mA400Machine.getMachineController().getEnclosure().unsubscribeEnclosureInfo();
        }

        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isAirPurifierAvailable) {
            mA400Machine.getMachineController().getAirPurifier().unsubscribeAirPurifierStatusChange();
        }
        if (mPrintEventSubscribe != null && !mPrintEventSubscribe.isDisposed()) {
            mPrintEventSubscribe.dispose();
        }
        if (mFilamentSubscribe != null && !mFilamentSubscribe.isDisposed()) {
            mFilamentSubscribe.dispose();
        }
        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
            mDecisionDialog.dismiss();
        }
        if (mViewModel.isIsFdm()) {
            mViewModel.fdmPause();
        }
    }

    @OnClick(R2.id.v_bottom_container)
    void onClickStart() {
        playNormalClickSound();
        ServiceContainer.getInstance().getService(IRouter.class).routeToPrintPage().start(getContext());
    }

    @OnClick(R2.id.ib_stop)
    void onClickControlStop() {
        playNormalClickSound();
        if (isStateFail) {
            mRouter.routeToHome().start(requireActivity());
        } else {
            fabWorkingChangeDialog.setContent(R.string.a400_print_stoping).show();
            mViewModel.stopPrint();
        }
    }

    @OnClick(R2.id.ib_pause)
    void onClickControlPause() {
        playNormalClickSound();
        if (isStateFail) {
            fabWorkingChangeDialog.setContent(R.string.a400_job_starting).show();
            mViewModel.startPrint();
            mButtonState = SYSTEM_STATUS_PAUSED.value();
            updateButtonState(true);
        } else {
            fabWorkingChangeDialog.setContent(R.string.a400_print_pausing).show();
            mViewModel.pausePrint();
            mButtonState = SYSTEM_STATUS_PAUSED.value();
            updateButtonState(true);
        }
    }

    @OnClick(R2.id.ib_resume)
    void onClickControlResume() {
        playNormalClickSound();
        mViewModel.resumePrint();
        mButtonState = SYSTEM_STATUS_PRINTING.value();
        updateButtonState(true);
    }

    private void animateToPaused(int duration) {
        TranslateAnimation animation = new TranslateAnimation(0, -DimensUtils.dp2px(120), 0, 0);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setDuration(duration);
        mBtnPause.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mBtnStop.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBtnPause.setVisibility(View.INVISIBLE);
                mBtnResume.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void animateToPrinting(int duration) {
        TranslateAnimation animation = new TranslateAnimation(0, DimensUtils.dp2px(120), 0, 0);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setDuration(duration);
        mBtnResume.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBtnResume.setVisibility(View.INVISIBLE);
                mBtnPause.setVisibility(View.VISIBLE);
                mBtnStop.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
