package com.snapmaker.s30.modules.home;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.snapmaker.s30.BuildConfig;
import com.snapmaker.s30.R;
import com.snapmaker.s30.R2;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.features.home.HomePrintIdleModuleFragment;
import fabscreen.features.home.a400.HomePrintingModuleFragment;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.view.BaseActivity;

@Route(path = RoutePath.S30_INDEX)
public class HomeActivity extends BaseActivity {

    @BindView(R2.id.tv_activity_name)
    TextView mTvName;
    @BindView(R2.id.bt_Calibration)
    TextView mBtCalibration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        // S30 show version
        mTvName.setText("S30 demo " + BuildConfig.VERSION_NAME);

        addFragment(R.id.fragment_container, HomePrintIdleModuleFragment.newInstance());


    }

    @OnClick(R2.id.bt_Control)
    public void onClickControl() {
        playNormalClickSound();
        mRouter.routeToControlPage().start(this);
    }

    @OnClick(R2.id.bt_Calibration)
    public void onClickCalibration() {
        playNormalClickSound();
        mRouter.routeToCalibrationPage().start(this);

    }

    @OnClick(R2.id.bt_Setting)
    public void onClickSetting() {
        playNormalClickSound();
        mRouter.routeToSettingsPage().start(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        IMachine.WorkType workType = mMachine.getMachineInfoSubjectHolder().getValue().workType;
        if (workType == IMachine.WorkType.CNC) {
            mBtCalibration.setText("Tools");
        } else {
            mBtCalibration.setText("Calibration");
        }

        NewPrintController NewPrintController = mMachine.getNewPrintController();
        boolean isPrinting = MachineOperationStatus.isPrinting(NewPrintController.getPrintState());
        if (isPrinting) {
            replaceHomePrintFragment(HomePrintingModuleFragment.newInstance());
        } else {
            replaceHomePrintFragment(HomePrintIdleModuleFragment.newInstance());
        }
    }

    private void replaceHomePrintFragment(Fragment fragment) {
        replaceFragment(R.id.fragment_container, fragment);
    }
}
