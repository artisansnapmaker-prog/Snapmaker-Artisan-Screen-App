package com.snapmaker.fabscreena400.module.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreena400.R;
import com.snapmaker.fabscreena400.R2;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.platform.base.BaseMainActivity;
import fabscreen.platform.base.BaseMainViewModel;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.structure.prop.BoolProp;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.A400_MAIN)
public class MainActivity extends BaseMainActivity {

    @BindView(R2.id.cpi_progress)
    CircularProgressIndicator mCpiProgress;
    @BindView(R2.id.view_a400_laser_password_fullscreen)
    View mViewLaserPassword;
    @BindView(R2.id.tv_a400_laser_password_tap_to_enter)
    TextView mTvLaserPasswordTap;

    private String mPassWord;
    private MachineInfo mMachineInfo;
    private IMachine.WorkType mWorkType;

    CustomKeyboardUtil mCustomKeyboardUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setNeedQueryMachineError(true);

        // Set up laser password view and keyboard.
        // Password view was gone as default, show up if laser was locked.
        mViewLaserPassword.setVisibility(View.GONE);
        mCustomKeyboardUtil = new CustomKeyboardUtil(this);
        mCustomKeyboardUtil.bindKeyboardListener(mTvLaserPasswordTap, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    String lowCaseValue = s.toString().toLowerCase();
                    if (!lowCaseValue.equals(mPassWord.substring(mPassWord.length() - 4).toLowerCase())) {
                        showError();
                    } else {
                        // 0:unlock 1:lock
                        getViewModel(MainViewModel.class).setLaserLockStatus(0)
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(success -> {
                                    if (success.isSuccess()) {
                                        // Unlock success, route to home directly.
                                        goToHome();
                                    } else {
                                        // TODO: What if unlock failed?
                                    }
                                }, LogHelper::log);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsFirstTimeIn && mViewModel.getInitStatus() == BaseMainViewModel.InitStatus.FINISH) {
            // Initialization done, Start dispatching routes and ready to next page.
            dispatchRoutes();
        }
    }

    @Override
    protected MainViewModel getViewModelByChild() {
        return getViewModel(MainViewModel.class);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void hideProgressbar() {
        mCpiProgress.hide();
    }

    @Override
    protected void onInitFinished() {
        dispatchRoutes();
    }

    protected void onModuleFWOutdated() {
        super.onModuleFWOutdated();
        if (mViewModel.isSecondHead()) {
            mRouter.routeToOldUpdate().start(this);
        } else {
            // User will leave this page and come back after em update finish.
            DecisionDialog.create(this)
                    .setDialogStatus(1, true, false, true, false)
                    .setPic(R.drawable.ic_update_224x224)
                    .setTitle(R.string.a400_dialog_boot_up_outdated_module_update_notiication_title)
                    .setContent(R.string.a400_dialog_boot_up_outdated_module_update_notiication_content)
                    .setFirstTv(R.string.all_update, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                        dialog.dismiss();
                        mRouter.routeToUpdateModules(mViewModel.getEMBinFile().getAbsolutePath()).start(MainActivity.this);
                    })
                    .show();
        }
    }

    private void dispatchRoutes() {
        // If it is the second generation, will be prompted to upgrade
        if (mViewModel.isSecondHead()) {
            mRouter.routeToOldUpdate().start(this);
            return;
        }
        // Setup Machine
        if (mViewModel.needGoWelcome()) {
            mRouter.routeToWelcome().start(this);
            return;
        }

        // Guide
        if (mViewModel.needGoToGuide()) {
            mRouter.routeToGuideMilestone().start(this);
            return;
        }

        mMachine = ServiceContainer.getInstance().getService(IMachine.class);
        mMachineInfo = mMachine.getMachineInfoSubjectHolder().getValue();
        mWorkType = mMachineInfo.workType;

        // Check if laser is unlock, ask for unlock if not.
        if (mWorkType == IMachine.WorkType.LASER) {
            if (TextUtils.isEmpty(mViewModel.getProductSerialNumber()) || mMachine.getMachineInfoSubjectHolder().getValue().headSNid == -1) {
                Logger.e("machine SN is null, skipping...");
                mViewLaserPassword.setVisibility(View.VISIBLE);
                goToHome();
                return;
            }
            mPassWord = mViewModel.getProductSerialNumber();
            getViewModel(MainViewModel.class).getLaserLockStatus()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(responseStructure -> {
                        if (responseStructure.isSuccess()) {
                            boolean isLock = ((BoolProp) responseStructure.dataProp).getValue();
                            if (isLock) {
                                Logger.i("laser is lock");
                                mViewLaserPassword.setVisibility(View.VISIBLE);
                            } else {
                                // set mode and go
                                Logger.i("laser is unLock");
                                goToHome();
                            }
                        }
                    });
        } else {
            // FDM and CNC(including tool head not plugged)，route to home directly.
            goToHome();
        }
    }

    public void showError() {
        DecisionDialog.create(this)
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(fabscreen.features.machinetools.R.drawable.ic_yellow_warn)
                .setTitle(fabscreen.features.machinetools.R.string.all_wifi_dialog_connect_failed_wrong_password)
                .setFirstTv(fabscreen.features.machinetools.R.string.all_cancel, fabscreen.features.machinetools.R.color.select_dialog_white_txt, (dialog, which) -> dialog.dismiss())
                .setSecondTv(fabscreen.features.machinetools.R.string.all_retry, fabscreen.features.machinetools.R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    showKeyboard(mTvLaserPasswordTap);
                }).show();
    }

    private void showKeyboard(View v) {
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_QWERTY_ABC);
        mCustomKeyboardUtil.setMaxLength(4);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_TEXT);
    }

    @OnClick(R2.id.tv_a400_laser_password_tap_to_enter)
    void onClickEnter(View v) {
        playNormalClickSound();
        showKeyboard(v);
    }

    public void goToHome() {
        mRouter.routeToHome().start(this);
    }

    @Override
    protected void onUpdateFinished() {
        mRouter.routeToUpdateSuccess(2, null).startForResult(this, 1);
    }

    /**
     * User confirmed the successfully updating, back to this page.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            mViewModel.confirmUpdate();
        }
    }
}

