package fabscreen.platform.base;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.io.File;

import butterknife.ButterKnife;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class BaseMainActivity extends BaseActivity {
    private static final String TAG = "BaseMainActivity";

    protected BaseMainViewModel mViewModel;
    protected boolean mIsFirstTimeIn = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        ButterKnife.bind(this);
        mViewModel = getViewModelByChild();
        modifyView();
        observeEvent();
    }

    protected abstract BaseMainViewModel getViewModelByChild();

    protected void modifyView() {
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsFirstTimeIn) {
            // Check self install process once at a boot up.
            Intent intent = getIntent();
            boolean newApkInstalled = intent.getBooleanExtra("newApkInstalled", false);
            Logger.d("MainActivity check new apk installed: %s", newApkInstalled);
            boolean emBinUpdated = ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getEmBinUpdatedFlag();
            mViewModel.checkFinishUpdating(newApkInstalled || emBinUpdated);
            intent.putExtra("newApkInstalled", false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsFirstTimeIn = false;
    }

    private void observeEvent() {
        mViewModel.getInitStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::handleInitResult, LogHelper::log);
    }

    private void handleInitResult(BaseMainViewModel.InitStatus status) {
        Logger.d("Handling initial result... status is %s", status);
        hideProgressbar();
        switch (status) {
            case FINISH:
                onInitFinished();
                break;
            case MAINBOARD_TIMEOUT:
                onInitTimeout();
                break;
            case MAINBOARD_BOOT_MODE:
                onBootDetected();
                break;
            case MODULE_FW_OUTDATED:
                onModuleFWOutdated();
                break;
            case UPDATE_FINISHED:
                // Show update finished, wait user confirm.
                onUpdateFinished();
                break;
        }
    }

    protected abstract int getLayoutResId();

    protected abstract void onUpdateFinished();

    protected abstract void onInitFinished();

    protected void onModuleFWOutdated() {
        Logger.d("Module firmware outdated!");
    }

    protected void hideProgressbar() {
    }

    /**
     * "boot detected" is a result of mc updating suddenly stopped without success.
     * Screen wouldn't update if mc update fail, so when we reach here, cached files are still
     * available(they haven't been moved to the persist folder yet).
     */
    private void onBootDetected() {
        Logger.d("Mainboard \"boot\" detected. Try to update using cached update.bin");
        File cachedBigBinFile = mViewModel.getCachedBigBinFile();
        if (cachedBigBinFile.exists()) {
            mRouter.routeToUpdateInProgress(cachedBigBinFile.getAbsolutePath(), true).start(this);
        } else {
            Logger.d("No update.bin found under cache, enter manual recovery...");
            mRouter.routeToRecoveryMode().start(this);
        }
    }

    protected void onInitTimeout() {
        DecisionDialog.create(this)
                .setDialogStatus(1, false, false, true, true)
                .setTitle("No heartbeat")
                .setContent("No heartbeat detected after 1 minute, enter home though?")
                .setFirstTv("Yes", R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mRouter.routeToHome().start(this);
                })
                .show();
    }
}
