package fabscreen.features.settings.a400.ota;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class A400SettingsOTATestFragment extends BaseFragment {
    private static File RECOVERY_DIR = new File("/cache/recovery");
    private static File COMMAND_FILE = new File(RECOVERY_DIR, "command");
    private static String mZipDstFile = "/data/update.zip";
    private String mZipSrcFile = "/sdcard/update.zip";

    @BindView(R2.id.tv_settings_ota_progress)
    TextView mTvProgress;

    private Context mContext;
    private Handler mHandler = new Handler();
    private BehaviorSubject<Integer> mProgressSubject = BehaviorSubject.createDefault(-1);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();

        mProgressSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    if (progress > 0) {
                        mTvProgress.setText("Progress: " + progress);
                    } else {
                        mTvProgress.setVisibility(TextView.GONE);
                    }
                }, LogHelper::log);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static Fragment newInstance() {
        return new A400SettingsOTATestFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_settings_ota_test;
    }

    @OnClick(R2.id.btn_settings_ota_test_start)
    void onClickOTAStart() {
        mRouter.routeToFilesPage(5).startForResult(this, 1);
    }

    private static void recoveryMode(Context context) throws IOException {
        String arg = "--update_package=/data/update.zip";
        RECOVERY_DIR.mkdirs();
        FileWriter command = new FileWriter(COMMAND_FILE);
        try {
            command.write(arg); // 往/cache/recovery/command中写入recoveryELF的执行参数。
            command.write("\n");
        } finally {
            command.close();
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot("recovery,quiescent");
        throw new IOException("Reboot failed (no permissions?)");
    }

    public void systemReboot(boolean confirm) {
        Intent intent = new Intent("android.ido.intent.action.set.reboot");
        intent.putExtra("confirm", confirm);
        requireActivity().sendBroadcast(intent);
    }

    private Runnable otaZipRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (fileIsExists(mZipDstFile)) {
                    if (copyFile(new File(mZipSrcFile), new File(mZipDstFile))) {
                        recoveryMode(mContext);
                        systemReboot(true);
                    } else {
                        Logger.e("copyFile error!");
                        DecisionDialog.create(requireContext())
                                .setDialogStatus(1, true, false, false, false)
                                .setPic(R.drawable.ic_pic_a400_error_112x112)
                                .setContent("Copy failed!")
                                .setFirstTv(R.string.all_ok, R.color.select_dialog_red_txt, ((dialog, which) -> {
                                    dialog.dismiss();
                                }))
                                .show();
                    }
                } else {
                    if (copyFile(new File(mZipSrcFile), new File(mZipDstFile))) {
                        recoveryMode(mContext);
                        systemReboot(true);
                    } else {
                        Logger.e("copyFile error!");
                        DecisionDialog.create(requireContext())
                                .setDialogStatus(1, true, false, false, false)
                                .setPic(R.drawable.ic_pic_a400_error_112x112)
                                .setContent("Copy failed!")
                                .setFirstTv(R.string.all_ok, R.color.select_dialog_red_txt, ((dialog, which) -> {
                                    dialog.dismiss();
                                }))
                                .show();
                    }
                }
            } catch (Exception e) {
                LogHelper.log(e);
                Logger.e("recoverMode error!");
            }
        }
    };

    public boolean copyFile(File src, File dst) {
        long inSize = src.length();
        long outSize = 0;
        int progress = 0;
        //listener.onCopyProgress(progress);
        try {
            if (dst.exists()) {
                dst.delete();
            }
            dst.createNewFile();
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            int length = -1;
            byte[] buf = new byte[1024];
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
                outSize += length;
                int temp = (int) (((float) outSize) / inSize * 100);
                if (temp != progress) {
                    progress = temp;
                    //listener.onCopyProgress(progress);
                    Logger.d("current progress " + progress);
                    mProgressSubject.onNext(progress);
                }
            }
            out.flush();
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean fileIsExists(String strFile) {
        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                // update file got, copy and update
                if (data == null) return;
                String filePath = data.getStringExtra("file_path");
                boolean isLocal = data.getBooleanExtra("is_local", false);
                mZipSrcFile = filePath;
                mHandler.post(otaZipRunnable);
            }
        }
    }
}
