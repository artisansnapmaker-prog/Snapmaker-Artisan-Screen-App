package fabscreen.features.machinetools.calibration.a400platform.laser.w_10.cameraCalibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.controller.PrintEvent;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class A400CameraCalibration10w2Fragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.top_bar_back)
    Button mBtnBack;
    @BindView(R2.id.iv_a400_calibration_camera_doing)
    ImageView mIvImage;
    Disposable mPrintControllerCallbackSub;
    private DecisionDialog mDecisionDialog;
    private boolean isStop;

    public static Fragment newInstance() {
        return new A400CameraCalibration10w2Fragment();
    }

    private Disposable subscribe;
    private DecisionDialog mQuitDialog;
    private A400CameraCalibration10wViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRouter = ServiceContainer.getInstance().getService(IRouter.class);
        mViewModel = getFragmentScopeViewModel(A400CameraCalibration10wViewModel.class);
        initView();
        subscribe = mViewModel.init()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(cameraCalibrationState -> {
                    switch (cameraCalibrationState) {
                        case READY_SUCCESS:
                            getPrintControllerCallback();
                            initPrint();
                            subscribe.dispose();
                            break;
                        default:
                            Logger.d("An error occurred: " + cameraCalibrationState);
                            restoreAndExit(false);
                            break;
                    }
                }, e -> {
                    LogHelper.log(e);
                    restoreAndExit(false);
                });

        mQuitDialog = DecisionDialog.create(getActivity())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, false, true)
                .setType(DecisionDialog.WARMING_TYPE)
                .setPic(R.drawable.pic_a400_warning_112x112)
                .setTitle(R.string.a400_calibration_stop_calibration)
                .setContent(getString(R.string.a400_calibration_assistant_back_notice, getString(R.string.a400_calibration_camera_calibration_10w_2_title)))
                .setFirstTv(requireContext().getResources().getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> dialog.dismiss()))
                .setSecondTv(requireContext().getResources().getString(R.string.all_stop), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    mQuitDialog.mCancelBtn.setEnabled(false);
                    mQuitDialog.mSecondBtn.setEnabled(false);
                    if (mViewModel.isPrinting()) {
                        mViewModel.requestMachineStop();
                    } else if (mViewModel.isCalibrationMode()) {
                        mQuitDialog.dismiss();
                        restoreAndExit(false);
                    } else {
                        mQuitDialog.dismiss();
                        restoreAndExit(false);
                    }
                });
    }

    void initPrint() {
        mViewModel.startPrint();
        mViewModel
                .getPrintStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(integer -> {
                }, LogHelper::log);

        mViewModel.getWaitingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(waiting -> {
                    // enable or disable buttons
                });
    }

    private void initView() {
        mBtnBack.setVisibility(View.INVISIBLE);
        setTitle(R.string.a400_calibration_camera_calibration_10w_2_title);
        mTvTopBarContent.setText(R.string.a400_calibration_camera_calibration_10w_2_content);
        mGuideProgressBar.setMax(2);
        mGuideProgressBar.setProgress(2);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_laser_camera_calibration_calibrating_new)
                .apply(options)
                .into(mIvImage);
    }

    private void getPrintControllerCallback() {
        Disposable subscribe = mViewModel.getPrintEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::onPrintEventCallback, LogHelper::log);
        if (!subscribe.isDisposed()) {
            if (mPrintControllerCallbackSub != null && !mPrintControllerCallbackSub.isDisposed()) {
                mPrintControllerCallbackSub.dispose();
            }
            mPrintControllerCallbackSub = subscribe;
        }
    }

    private void onPrintEventCallback(PrintEvent printEvent) {
        DecisionDialog decisionDialog = DecisionDialog.create(getContext());
        int retCode = printEvent.getErrorCode();
        Logger.d("PrintEvent callback " + printEvent);
        switch (printEvent.getPrintEventState()) {
            case STATE_SUCCESS:
                break;
            case POWER_LOSS_RESUME_SUCCESS:
                mViewModel.setPowerOutageFlag(false);
                break;
            case STOP_SUCCESS:
                if (isStop) return;
                isStop = true;
                if (mQuitDialog != null && mQuitDialog.isShowing()) {
                    mQuitDialog.dismiss();
                    mQuitDialog.mCancelBtn.setEnabled(true);
                    mQuitDialog.mSecondBtn.setEnabled(true);
                }
                restoreAndExit(false);
                break;
            case STOP_FAIL:
                if (mQuitDialog != null && mQuitDialog.isShowing()) {
                    mQuitDialog.dismiss();
                    mQuitDialog.mCancelBtn.setEnabled(true);
                    mQuitDialog.mSecondBtn.setEnabled(true);
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
                Logger.i("Print Finished.");
                toDoTakePhoto();
                break;
            case OPEN_DOOR_PAUSE:
                decisionDialog = DecisionDialog.create(getContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setContent(getString(R.string.a400_dialog_print_enclosure_open_desc, getString(R.string.a400_calibration_camera_calibration_10w_2_title)))
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> {
                            if (mViewModel.isPrinting()) {
                                mViewModel.requestMachineStop();
                            } else {
                                restoreAndExit(false);
                            }
                            dialog.dismiss();
                        }));
                decisionDialog.show();
                break;
            case START_FAIL:
                if (retCode == 227) {
                    decisionDialog = DecisionDialog.create(getContext())
                            .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                            .setType(DecisionDialog.WARMING_TYPE)
                            .setContentColor(R.color.palette_white_pure)
                            .setContent(getString(R.string.a400_dialog_print_enclosure_open_desc, getString(R.string.a400_calibration_camera_calibration_10w_2_title)))
                            .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> {
                                if (mViewModel.isPrinting()) {
                                    mViewModel.requestMachineStop();
                                } else {
                                    restoreAndExit(false);
                                }
                                dialog.dismiss();
                            }));
                    decisionDialog.show();
                } else {
                    restoreAndExit(false);
                }
                break;
            default:
                restoreAndExit(false);
                break;
        }
        if (decisionDialog != null && decisionDialog.isShowing()) {
            if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                mDecisionDialog.dismiss();
            }
            mDecisionDialog = decisionDialog;
        }
    }

    private void toDoTakePhoto() {
        mViewModel.toDoTakePhoto()
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    Logger.d("Camera Calibration process " + aBoolean);
                    restoreAndExit(true);
                }, e -> {
                    LogHelper.log(e);
                    restoreAndExit(false);
                });
    }

    private void restoreAndExit(boolean isSave) {
        Logger.d("Camera Calibration exiting with result %b.", isSave);
        Observable.timer(500, TimeUnit.MILLISECONDS)
                .flatMap(time -> (mViewModel.isCalibrationMode() ? mViewModel.exitCalibration(isSave).flatMap(response -> mViewModel.turnLightAndReset()) : mViewModel.turnLightAndReset()))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    if (isSave) {
                        finishActivityWithResultOk();
                    } else {
                        requireActivity().finish();
                    }
                }, e -> {
                    LogHelper.log(e);
                    if (isSave) {
                        finishActivityWithResultOk();
                    } else {
                        requireActivity().finish();
                    }
                });
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_calibration_camrea_doing;
    }

    @Override
    protected void back() {
        if (mQuitDialog.isShowing()) {
            return;
        }
        mQuitDialog.show();
    }

}
