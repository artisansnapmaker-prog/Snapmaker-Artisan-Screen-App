package com.snapmaker.fabscreena400.module.home;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreena400.R;
import com.snapmaker.fabscreena400.R2;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.features.home.HomePrintIdleModuleFragment;
import fabscreen.features.home.a400.HomePrintingModuleFragment;
import fabscreen.features.settings.a400.A400SettingsContainerFragment;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.crash.CrashCollectHandler;
import fabscreen.platform.base.lib.network.AccessPoint;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IFileManagerService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.INetwork;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRemote;
import fabscreen.platform.base.service.machine.controller.ErrorController;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.remote.RemoteClient;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.A400_INDEX)
public class HomeActivity extends BaseActivity {

    @BindView(R2.id.btn_a400_home_top_bar_luban)
    Button mBtLuban;
    @BindView(R2.id.btn_a400_home_top_bar_wifi)
    Button mBtnWifi;
    @BindView(R2.id.vp_home_back)
    VideoPlayerIJK mVpHomeBack;
    @BindView(R2.id.iv_a400_home_navigate_calibration)
    ImageView mIvCalibration;
    @BindView(R2.id.tv_home_navigate_calibration)
    TextView mTvCalibration;
    @BindView(R2.id.tv_a400_home_top_bar_title)
    TextView mTvMachineName;

    IRemote mIRemoteService;

    private boolean mBootStartUp = true;

    DecisionDialog mFabConfirm;

    private HomeViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        initVideo();

        mViewModel = getViewModel(HomeViewModel.class);
        mIRemoteService = ServiceContainer.getInstance().getService(IRemote.class);
        replaceHomePrintFragment(false);

        if (ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getNeedQueryMachineError()) {
            ServiceContainer.getInstance().getService(IMachine.class)
                    .getErrorController()
                    .queryException()
                    .as(bindToLifecycle())
                    .subscribe(responseStructure -> {
                    }, LogHelper::log);

            ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setNeedQueryMachineError(false);
        }

        if (savedInstanceState == null) {
            observeUpdate();
        }

        ServiceContainer.getInstance().getService(IFileManagerService.class).isHaveUsbDevices();
    }

    private void observeUpdate() {
//        if (ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getShowUpdateResultWhenHome()) {
//            mRouter.routeToUpdateSuccess(2, null).start(this);
//        }
        mViewModel.getUpdateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(hasUpdate -> showUpdateDialog(hasUpdate), LogHelper::log);
    }

    private void showUpdateDialog(String version) {
        DecisionDialog.create(this)
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, false, false)
                .setPic(R.drawable.ic_update_224x224)
                .setTitle(R.string.a400_firmawre_updeta_title)
                .setContent(R.string.a400_firmawre_updeta_content)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> {
                    mViewModel.setLastCheckVersion(version);
                    dialog.dismiss();
                })
                .setSecondTv(R.string.a400_home_firmware_action_update_now, R.color.select_dialog_blue_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mRouter.routeToSettingsPage(A400SettingsContainerFragment.SETTINGS_FIRMWARE).start(HomeActivity.this);
                }).show();
    }

    @OnClick(R2.id.btn_a400_home_top_bar_luban)
    void onClickLuban() {
        playNormalClickSound();
        RemoteClient nowClient = mIRemoteService.getNowClient();
        if (mFabConfirm == null) {
            mFabConfirm = DecisionDialog.create(this)
                    .setTitle(R.string.a400_remote_state_title)
                    .setContent(String.format("%s (%s)", nowClient.getDeviceName(), nowClient.getConnectingClients()))
                    .setType(DecisionDialog.WARMING_TYPE)
                    .setDialogStatus(DecisionDialog.BTN_TWO, false, false, true, true)
                    .setFirstTv(R.string.all_close, R.color.select_dialog_white_txt, (dialog, i) -> dialog.dismiss())
                    .setSecondTv(R.string.all_disconnect, R.color.select_dialog_red_txt, ((dialog, which) -> {
                        ServiceContainer.getInstance().getService(IRemote.class).clearConnection();
                        dialog.dismiss();
                    }));
        }
        if (mFabConfirm.isShowing()) {
            return;
        } else {
            mFabConfirm.setContent(String.format("%s (%s)", nowClient.getDeviceName(), nowClient.getConnectingClients()));
            mFabConfirm.show();
        }
    }

    @OnClick(R2.id.rl_a400_home_navigate_control)
    void onClickControl() {
        playNormalClickSound();
        mRouter.routeToControlPage().start(this);
    }

    @OnClick(R2.id.rl_a400_home_navigate_calibration)
    void onClickCalibration() {
        playNormalClickSound();
        mRouter.routeToCalibrationPage().start(this);
    }

    @OnClick({R2.id.rl_a400_home_navigate_settings, R2.id.btn_a400_home_top_bar_wifi})
    void onClickSetting() {
        playNormalClickSound();
        mRouter.routeToSettingsPage().start(this);
    }

    @OnClick(R2.id.btn_a400_home_top_bar_replace_model)
    void onClickReplaceModel() {
        playNormalClickSound();
        mRouter.routeToSettingsPage(A400SettingsContainerFragment.SETTINGS_MODULE_ASSISTANT).start(this);
    }

    @OnClick(R2.id.btn_a400_home_top_bar_about_entry)
    void onClickAboutMachine() {
        playNormalClickSound();
        mRouter.routeToSettingsPage(A400SettingsContainerFragment.SETTINGS_ABOUT).start(this);
    }

    private void initVideo() {
        mVpHomeBack.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/home.webm");
        mVpHomeBack.setLooping(true);
        mVpHomeBack.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVpHomeBack.setLooping(false);
        mVpHomeBack.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ErrorController.EmergencyStopState emergencyStopState = ServiceContainer.getInstance().getService(IAppService.class).getEmergencyStopState();
        switch (emergencyStopState) {
            case EMERGENCY_STOP_STATE_RELEASE:
            case EMERGENCY_STOP_STATE_PRESS:
                mRouter.routeToEmergencyStopPage(emergencyStopState == ErrorController.EmergencyStopState.EMERGENCY_STOP_STATE_RELEASE)
                        .start(this, Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return;
            case EMERGENCY_STOP_STATE_NORMAL:
            default:
                break;
        }

        initVideo();

        IMachine.WorkType workType = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType;
        switch (workType) {
            case CNC:
                mIvCalibration.setImageResource(R.drawable.ic_a400_tools_68x68);
                mTvCalibration.setText(R.string.all_tool);
                break;
            case FDM:
            case LASER:
            case NONE:
            default:
                mIvCalibration.setImageResource(R.drawable.ic_a400_home_calibration_68x68);
                mTvCalibration.setText(R.string.home_calibration);
                break;
        }
//        mVpHomeBack.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/home.webm");
//        mVpHomeBack.setLooping(true);

        if (workType == IMachine.WorkType.FDM && mMachine.getMachineController().getHeatedBed() == null) {
            // todo Toast.makeText(this, "热床没有连接", Toast.LENGTH_SHORT).show();
        } else if (workType == IMachine.WorkType.LASER && mMachine.getLaserController() == null && !mMachine.getLaserController().getLaserCameraController().isConnected()) {
            DecisionDialog.create(this)
                    .setContent("Bluetooth is not ready.")
                    .setType(DecisionDialog.WARMING_TYPE)
                    .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                    .setFirstTv(fabscreen.platform.base.R.string.all_close, fabscreen.platform.base.R.color.select_dialog_white_txt, ((dialog, which) -> {
                        dialog.dismiss();
                    }));
        }

        if (mMachine != null) {
            int printState = mMachine.getNewPrintController().getPrintState();
            boolean isPrinting1 = MachineOperationStatus.isPrinting(printState);
            if (mBootStartUp && isPrinting1) {
                Logger.w("Machine is printing while touchscreen start up.");

            }
            replaceHomePrintFragment(isPrinting1);

            // Listen changes
            ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController()
                    .getPrintStateObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(integer -> {
                        boolean isPrinting = MachineOperationStatus.isPrinting(integer);
                        replaceHomePrintFragment(isPrinting);
                    }, LogHelper::log);
        }

        if (mIRemoteService != null) {
            mIRemoteService.getRemoteConnectedObservable().observeOn(AndroidSchedulers.mainThread()).as(bindToLifecycle()).subscribe(statr -> {
                mBtLuban.setVisibility(statr == 2 ? View.VISIBLE : View.GONE);
            });
        }
        // Network
        ServiceContainer.getInstance().getService(INetwork.class).getActiveNetworkObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(accessPoint -> {
                    if (accessPoint != AccessPoint.NULL_ACCESS_POINT) {
                        Logger.d("Wi-Fi connected.");
                        mBtnWifi.setBackgroundResource(R.drawable.ic_a400_home_wifi_68x68);
                    } else {
                        mBtnWifi.setBackgroundResource(R.drawable.ic_a400_home_wifi_no_link_68x68);
                    }
                });


        mTvMachineName.setText(ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getMachineName());
    }

    private void replaceHomePrintFragment(boolean isPrinting) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fl_a400_home_fragment_container);

        if (isPrinting) {
            if (fragment instanceof HomePrintingModuleFragment) {
                return;
            }
            replaceFragment(R.id.fl_a400_home_fragment_container, HomePrintingModuleFragment.newInstance());
        } else {
            if (fragment instanceof HomePrintIdleModuleFragment) {
                return;
            }
            replaceFragment(R.id.fl_a400_home_fragment_container, HomePrintIdleModuleFragment.newInstance());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
