package com.snapmaker.a350.modules.home;

import android.content.Intent;
import android.os.Bundle;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.ButterKnife;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.crash.CrashCollectHandler;
import fabscreen.platform.base.lib.update.FabUpdatePackage;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.lib.LogHelper;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // FIXME：
//        if (ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().isEmergencyStopAvailable) {
//            ServiceContainer.getInstance().getService(IMachine.class).getMachineController().onEmergencyStop();
//            AndroidSchedulers.mainThread().scheduleDirect(this::gotoEmergencyPage, 200, Constants.TIME_UNIT);
//            return;
//        }

        // Check if com.snapmaker.update is installed
        Intent updateIntent = new Intent("com.snapmaker.updateApkBroadcast");
        updateIntent.putExtra("OPERATION", "update");
        sendBroadcast(updateIntent);

        if (ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getMachineUpdatedFlag()) {
            finishUpdateFromStartUp();
        }

//        MachineStatusManager.getConnectedStatus().getObservable()
//                .skip(1)
//                .observeOn(AndroidSchedulers.mainThread())
//                .filter(connected -> connected)
//                .flatMap(connected -> MachineStatusManager.getMachineInfoHolder().getObservable())
//                .filter(machineStatus -> !machineStatus.isDefault)
//                .flatMap(machineStatus -> ServiceContainer.getInstance().getService(IAppService.class).getSlaveComputer().useBatchGcodeMode(0).onExceptionResumeNext(Observable.just(-1)))
//                .take(1)
//                .as(bindToLifecycle())
//                .subscribe(machineStatus -> {
//                    Logger.d("machineStatus " + machineStatus);
//                    // TODO: Needs to refactor setNewPrintController business.
////                    ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().disposeAll();
//                    // Reset NewPrintController
//                    IPrintController NewPrintController = (machineStatus != -1) ? new DeprecatedBatchNewPrintController(ServiceContainer.getInstance().getService(IAppService.class).getNewPrintController()) : new DeprecatedPrintController(ServiceContainer.getInstance().getService(IAppService.class).getNewPrintController());
////                    ServiceContainer.getInstance().getService(IAppService.class).setNewPrintController(NewPrintController);
////                    ServiceContainer.getInstance().getService(IAppService.class).getRemoteController().setNewPrintController(NewPrintController);
////                    ServiceContainer.getInstance().getService(IAppService.class).getSlaveComputer().getMachineController().setNewPrintController(NewPrintController);
//                    // restore Activity
//                    restoreActivity();
//                });
//        ServiceContainer.getInstance().getService(IAppService.class).getSlaveComputer().connect();
        // set Check Update when boot up
        ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setCheckUpdateFlag(true);
    }

    private void restoreActivity() {
        Intent intent = getIntent();
        ArrayList<Intent> intents = intent.getParcelableArrayListExtra(CrashCollectHandler.FABSCREEN_CRASH);
        // Get the Activities opened before crash and open them again through the Router
        if (intents != null && intents.size() > 2) {
            // Skip opening com.snapmaker.fabscreen.modules.home.MainActivity and open fabscreen.features.home.MainActivity
            ServiceContainer.getInstance().getService(IRouter.class).start(this, intents.get(1));
            for (int i = 2; i < intents.size() - 1; i++) {
                // crashing Activity
                ServiceContainer.getInstance().getService(IRouter.class).start(mApp.getCurrentActivity(), intents.get(i));
            }
        } else {
            startActivity(new Intent(this, HomeActivity.class));
        }
    }

    @Override
    protected void onResume() {
//        if (ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().isEmergencyStopAvailable) {
////            ServiceContainer.getInstance().getService(IMachine.class).getMachineController().onEmergencyStop();
//            Logger.d("MainActivity emergency stop");
//            AndroidSchedulers.mainThread().scheduleDirect(this::gotoEmergencyPage, 200, Constants.TIME_UNIT);
//        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Only stopped when activity is destroyed for good. And not stopped during an orientation
        // change.
        if (isFinishing()) {
            unbindBackgroundServices();
        }
    }

    private void finishUpdateFromStartUp() {
        Logger.d("Finishing up last update...");
        String dataFolderPath = ServiceContainer.getInstance().getService(IAppService.class).getDataDir().getAbsolutePath() + File.separatorChar + "update";
        String cacheFolderPath = ServiceContainer.getInstance().getService(IAppService.class).getCacheDir().getAbsolutePath() + File.separatorChar + "update";

        File saveFolder = new File(dataFolderPath);
        if (!saveFolder.exists()) {
            saveFolder.mkdir();
        }

        // Clear update files in folder
        File[] files = saveFolder.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                f.delete();
            }
        }

        // save update file from cache.
        File cacheUpdateFile = new File(cacheFolderPath, "update.bin");
        if (cacheUpdateFile.exists()) {
            // parse package header
            String cacheUpdateFileVersion = parseUpdateFileHeader(cacheUpdateFile);
            if (cacheUpdateFileVersion != null && !cacheUpdateFileVersion.equals(ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getLastUpdatePackageVersion())) {
                // Cache file version is not equal to update version.
                Logger.d("Update: removing outdated cache update file...");
                cacheUpdateFile.delete();
                return;
            }

            File saveFile = new File(saveFolder, "update.bin");
            BufferedSource bufferedSource = null;
            BufferedSink bufferedSink = null;
            try {
                bufferedSource = Okio.buffer(Okio.source(new FileInputStream(cacheUpdateFile)));
                bufferedSink = Okio.buffer(Okio.sink(new FileOutputStream(saveFile)));
                // copy file from source with buffer
                int len;
                byte[] buffer = new byte[1024 * 16];
                while ((len = bufferedSource.read(buffer)) != -1) {
                    bufferedSink.write(buffer, 0, len);
                }
                bufferedSink.close();
                bufferedSource.close();
                Logger.d("Firmware update finish.");
            } catch (IOException e) {
                LogHelper.log(e);
            } finally {
                try {
                    if (bufferedSink != null) {
                        bufferedSink.close();
                    }
                    if (bufferedSource != null) {
                        bufferedSource.close();
                    }
                } catch (IOException e) {
                    LogHelper.log(e);
                }
            }
        } else {
            Logger.w("Update file not exist.");
        }
    }

    private String parseUpdateFileHeader(File file) {
        try {
            BufferedSource source = Okio.buffer(Okio.source(new FileInputStream(file)));
            byte[] p = source.readByteArray(file.length());
            FabUpdatePackage.UpdatePackageHeader cacheHeader = FabUpdatePackage.UpdatePackageHeader.parse(p);
            if (cacheHeader != null && cacheHeader.getVersion() != null) {
                return cacheHeader.getVersion();
            } else {
                return null;
            }
        } catch (IOException e) {
            LogHelper.log(e);
            return null;
        }
    }

    // services belongs to application, don't stop services here
    @Deprecated
    private void unbindBackgroundServices() {
        // Stop all services
//        BaseApplication.getInstance().unregister();
//        BaseApplication.getInstance().stopServices();
//        BaseApplication.getInstance().stopDiscoverServer();
//        BaseApplication.getInstance().stopHttpServer();
    }

    private void gotoEmergencyPage() {
        ServiceContainer.getInstance().getService(IRouter.class).routeToEmergencyStopPage(false).start(this);
    }
}
