package fabscreen.features.settings.j1;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import fabscreen.features.settings.R;
import fabscreen.platform.base.helper.StringToValueUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.lib.file.IPartition;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IFileManagerService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.INetwork;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class S30SettingsAboutViewModel extends BaseViewModel {

    private final IPreferences.Helper mPrefHelper;
    private final IMachine mMachine;
    private final IAppService mAppService;
    private final IFileManagerService mFileManager;
    private final INetwork mNetwork;
    private Disposable mExportDisposable;
    private Context mContext;
    private final PublishSubject<ExportState> mExportStateSubj = PublishSubject.create();

    public S30SettingsAboutViewModel() {
        mAppService = getServiceContainer().getService(IAppService.class);
        mFileManager = getServiceContainer().getService(IFileManagerService.class);
        mPrefHelper = getServiceContainer().getService(IPreferences.class).getHelper();
        mMachine = getServiceContainer().getService(IMachine.class);
        mNetwork = getServiceContainer().getService(INetwork.class);
        mContext = getServiceContainer().getService(IAppService.class).getAppContext();
    }

    public Observable<ExportState> getExportStateObservable() {
        return mExportStateSubj.hide();
    }

    public String getUserMachineName() {
        return mPrefHelper.getMachineName();
    }

    public String getMachineModelName() {
        return mMachine.getMachineInfoSubjectHolder().getValue().getModelName();
    }

    public String getProductSerialNumber() {
        String serialNo = mMachine.getMachineInfoSubjectHolder().getValue().productSerialNumber;
        if (serialNo == null || serialNo.isEmpty() || serialNo.length() < 4) {
            Logger.d("Invalid Serial Number: " + serialNo);
            return "N/A";
        }
        return mMachine.getMachineInfoSubjectHolder().getValue().productSerialNumber.substring(0, serialNo.length() - 4);
    }

    public String getMachineVerifyCode() {
        String serialNo = mMachine.getMachineInfoSubjectHolder().getValue().productSerialNumber;
        if (serialNo == null || serialNo.isEmpty() || serialNo.length() < 4) {
            Logger.d("Invalid Serial Number: " + serialNo);
            return "N/A";
        }
        return serialNo.substring(serialNo.length() - 4);
    }

    public String getWorkArea() {
        MachineInfo info = mMachine.getMachineInfoSubjectHolder().getValue();
        if (info.productId == IMachine.Product.J1) {
            return "345 × 357 × 334" + mContext.getString(R.string.all_unit_mm);
        } else if (info.productId == IMachine.Product.A400) {
            return "400 × 400 × 400" + mContext.getString(R.string.all_unit_mm);
        } else {
            return "Unknown";
        }
    }

    public String getIPAddress() {
        // check ip address
        String addressString = "Not Connected";
        try {
            List<NetworkInterface> interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaceList) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String sAddr = address.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (isIPv4) {
                            addressString = sAddr;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            LogHelper.log(e);
        }
        return addressString;
    }

    public String getMacAddr() {
        return mNetwork.getMacAddress();
    }

    public String getStorageUsage() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return formatStorageUsage(stat.getTotalBytes(), stat.getAvailableBytes());
    }

    private String formatStorageUsage(long totalBytes, long availableBytes) {
        return availableBytes / 1024 / 1024 + mContext.getString(R.string.all_unit_mb) + "/" + totalBytes / 1024 / 1024 + mContext.getString(R.string.all_unit_mb);
    }

    public void clearCache() {
        ServiceContainer.getInstance().getService(IPrintWorkspace.class).clearAllWorkSpaceFiles();
        File cacheDir = getServiceContainer().getService(IAppService.class).getCacheDir();
        deleteDir(cacheDir);
        deleteLog();
    }

    private boolean deleteDir(File file) {
        if (file == null) return true;
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                if (!deleteDir(new File(file, child))) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    public void exportLogsToUDisk() {
        if (mFileManager.getFabUsbDevice() == null) {
            mExportStateSubj.onNext(ExportState.ON_FAIL_NO_U_DISK);
            return;
        }
        mExportStateSubj.onNext(ExportState.ON_START);
        Scheduler.Worker worker = Schedulers.io().createWorker();
        mExportDisposable = worker.schedule(() -> {
            boolean scSucceed = exportToUDisk("SC");
            boolean fwSucceed = exportToUDisk("FW");
            mExportStateSubj.onNext(scSucceed && fwSucceed ? ExportState.ON_SUCCESS : ExportState.ON_FAIL_OTHER);
        });
    }

    /**
     * Export logs from latest to oldest, at most 3 files.
     *
     * @return true if success, false if fail.
     */
    private boolean exportToUDisk(String filePrefix) {
        String diskPath = mAppService.getAppContext().getCacheDir().getAbsolutePath();
        String folderPath = diskPath + File.separatorChar + "log";
        File folder = new File(folderPath);

        // Find max log file suffix number.
        int maxSuffix = 0;
        for (String fileName : Arrays.stream(folder.listFiles()).map(File::getName).collect(Collectors.toList())) {
            Logger.d("File found, name is %s", fileName);
            if (!fileName.contains("_") || !fileName.contains(filePrefix)) continue;
            int tempMax;
            try {
                tempMax = StringToValueUtils.parseInt(fileName.substring(fileName.indexOf("_") + 1, fileName.indexOf(".")));
            } catch (NumberFormatException e) {
                // Not a log file.
                Logger.d("File \"%s\" is not a log file.", fileName);
                continue;
            }
            if (tempMax > maxSuffix) {
                maxSuffix = tempMax;
            }
        }

        // Copy 3 latest log files.
        int fileNo = maxSuffix;
        int copyCount = 0;
        File file;

        while (true) {
            String name = String.format("%s_%s.log", filePrefix, fileNo);
            Logger.d("Start copying file \"%s\"", name);
            file = new File(folderPath, name);

            if (!file.exists() || copyCount >= 20) {
                return true;
            }

            if (file.length() == 0) {
                fileNo--;
                continue;
            }

            Logger.d("Copying file \"%s\"", name);

            IPartition usbDevice = mFileManager.getFabUsbDevice();
            IFile rootFile = usbDevice.getRootFile();
            try {
                IFile logFile = usbDevice.search(rootFile.getAbsolutePath() + name);
                if (logFile != null) {
                    Logger.d("Found existing file, removing...");
                    logFile.removeFile();
                }
                Logger.d("Creating new log file on U disk...");
                logFile = usbDevice.createFile(rootFile, name);
                try (OutputStream outputStream = logFile.getOutputStream();
                     InputStream inputStream = new FileInputStream(file)) {

                    byte[] buf = new byte[102400];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }

                    fileNo--;
                    copyCount++;
                }
            } catch (Throwable e) {
                LogHelper.log(e);
                return false;
            }
        }
    }

    public void deleteLog() {
        String diskPath = mAppService.getAppContext().getCacheDir().getAbsolutePath();
        String folder = diskPath + File.separatorChar + "log";
        deleteDir(new File(folder));
    }

    public MachineStatus getMachineStatusValue() {
        return getServiceContainer().getService(IMachine.class).getMachineStatusSubjectHolder().getValue();
    }

    public void stopWork() {
        try {
            getServiceContainer().getService(IMachine.class).getNewPrintController().stop();
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

    public void exitCalibration() {
        try {
            Observable<ResponseStructure> responseStructureObservable = null;
            IMachine.WorkType workType = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType;
            switch (workType) {
                case FDM:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getFDMController().exitCalibration(false);
                    break;
                case LASER:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getLaserController().exitCalibration(false);
                    break;
                case CNC:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getCNCController().exitCalibration(false);
                    break;
            }
            if (responseStructureObservable == null) return;
            responseStructureObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (!success.isSuccess()) {
                            Logger.d("Exit Calibration: " + success);
                        }
                    }, LogHelper::log);
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

    public Observable<MachineStatus> getMachineStatusObservable() {
        return getServiceContainer().getService(IMachine.class).getMachineStatusSubjectHolder().getObservable();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mExportDisposable != null) {
            mExportDisposable.dispose();
        }
    }

    public void exportLogsToRemote() {
        // TODO: 2022/6/18 export to luban/lava
        mExportStateSubj.onNext(ExportState.ON_START);
        mExportStateSubj.onNext(ExportState.ON_FAIL_NO_REMOTE);
    }

    public boolean isRemoteAvailable() {
        return false;
    }

    public enum ExportState {
        ON_START,
        ON_SUCCESS,
        ON_FAIL_NO_LOGS,
        ON_FAIL_NO_U_DISK,
        ON_FAIL_NO_REMOTE,
        ON_FAIL_OTHER
    }
}
