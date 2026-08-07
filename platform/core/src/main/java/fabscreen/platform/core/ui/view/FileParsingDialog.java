package fabscreen.platform.core.ui.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.helper.SoundUtil;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.BaseAppService;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.core.R;

public class FileParsingDialog {

    private static FileParsingDialog sInstance;

    public TextView mTvContent;
    public CircularProgressIndicator mProgressbar;
    private AlertDialog mDialog;
    public static BaseAppService mApp;
    private static boolean mIsA400;
    public static int mSoundId;

    public static FileParsingDialog create(Context context) {
        if (sInstance != null) {
            sInstance.mDialog.cancel();
            sInstance.mDialog.dismiss();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        final Activity activity = (Activity) context;
        mApp = (BaseAppService) ServiceContainer.getInstance().getService(IAppService.class);

        // Create new dialog and config
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_file_parsing, null);
        dialog.setView(view);
        sInstance = new FileParsingDialog();
        sInstance.mDialog = dialog;
        mIsA400 = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().seriesId == IMachine.MachineSeries.A;

        sInstance.mTvContent = view.findViewById(R.id.tv_dialog_parsing_tv);
        sInstance.mProgressbar = view.findViewById(R.id.progressbar_dialog_parsing);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mIsA400) {
                    SoundUtil.stopSound(mApp.getSoundPool(), mSoundId);
                }
            }
        });

        return sInstance;
    }

    public FileParsingDialog setContent(String content) {
        mTvContent.setText(content);
        return this;
    }

    public FileParsingDialog setContent(int stringID) {
        mTvContent.setText(stringID);
        return this;
    }

    public FileParsingDialog setCanceledOnTouchOutSide(boolean cancel) {
        mDialog.setCanceledOnTouchOutside(cancel);
        return this;
    }

    public void show() {
        mDialog.show();
        Window window = mDialog.getWindow();
        mProgressbar.show();
        if (window == null) {
            return;
        }
        window.setLayout(DimensUtils.dp2px(580, mDialog.getContext()),
                WindowManager.LayoutParams.WRAP_CONTENT);

        if (mIsA400) {
            mSoundId = SoundUtil.playSoundHasId(mApp.getSoundPool(), mApp.getSoundIdByResourceId(fabscreen.platform.base.R.raw.sound_dialog_tip));
        }
    }


    public void dismiss() {
        if (mDialog.isShowing()) {
            mProgressbar.hide();
            mDialog.dismiss();
        }
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

}
