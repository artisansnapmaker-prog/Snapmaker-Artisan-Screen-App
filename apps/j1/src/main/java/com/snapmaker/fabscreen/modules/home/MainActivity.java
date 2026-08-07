package com.snapmaker.fabscreen.modules.home;

import android.content.pm.ActivityInfo;

import com.snapmaker.fabscreen.R;

import fabscreen.platform.base.BaseMainActivity;
import fabscreen.platform.base.BaseMainViewModel;
import fabscreen.platform.base.view.DecisionDialog;

public class MainActivity extends BaseMainActivity {

    @Override
    protected BaseMainViewModel getViewModelByChild() {
        return getViewModel(MainViewModel.class);
    }

    @Override
    protected void modifyView() {
        super.modifyView();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onUpdateFinished() {

    }

    @Override
    protected void onInitFinished() {
        // welcome, guide, or home.
        mRouter.routeToHome().start(this);
    }

    @Override
    protected void onInitTimeout() {
        DecisionDialog.create(this)
                .setContent(R.string.all_j1_can_not_connect)
                .setType(DecisionDialog.WARMING_TYPE)
                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                .setFirstTv(R.string.all_ok, fabscreen.platform.base.R.color.select_dialog_orange_txt, ((dialog, which) -> {
                    dialog.dismiss();
                    mRouter.routeToHome().start(this);
                })).show();
    }
}

