package fabscreen.platform.base.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import fabscreen.platform.base.R;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.helper.SoundUtil;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.BaseAppService;
import fabscreen.platform.base.service.IAppService;

public class FileLoadingDialog {

    private static FileLoadingDialog sInstance;

    private AlertDialog mDialog;

    public CircularProgressIndicator mProgressbar;
    //    public CircularProgressView mCirProgressbar;
    public TextView mTvProgressContent;
    public TextView mTvContent;
    public ImageView mIvClose;
    private static boolean mIsJ1;
    public static BaseAppService mApp;
    public static int mSoundId;

    public static FileLoadingDialog create(Context context, boolean isJ1) {
        mIsJ1 = isJ1;
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
        final View view = inflater.inflate(mIsJ1 ? R.layout.dialog_j1_file_loading : R.layout.dialog_a400_file_loading, null);
        dialog.setView(view);
        sInstance = new FileLoadingDialog();
        sInstance.mDialog = dialog;
        if (mIsJ1) {
            sInstance.mProgressbar = view.findViewById(R.id.progressbar);
            sInstance.mTvContent = view.findViewById(R.id.tv_dialog_loading_tv);
        } else {
            sInstance.mProgressbar = view.findViewById(R.id.progressbar);
            sInstance.mTvProgressContent = view.findViewById(R.id.tv_progress_content);
            sInstance.mTvContent = view.findViewById(R.id.tv_dialog_loading_tv);
            sInstance.mIvClose = view.findViewById(R.id.iv_close);

            sInstance.mIvClose.setOnClickListener(v -> {
                sInstance.dismiss();
            });
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!mIsJ1) {
                    SoundUtil.stopSound(mApp.getSoundPool(), mSoundId);
                }
            }
        });

        return sInstance;
    }

    public FileLoadingDialog setCanceledOnTouchOutSide(boolean cancel) {
        mDialog.setCanceledOnTouchOutside(cancel);
        return this;
    }

    public FileLoadingDialog setContent(String progress) {
        mTvContent.setText(progress);
        return this;
    }

    public FileLoadingDialog setProgress(int progress) {
        mTvProgressContent.setText(progress + "%");
        mProgressbar.setProgress(progress);
        return this;
    }

    public FileLoadingDialog setClosable(boolean enabled) {
        mIvClose.setVisibility(ImageView.VISIBLE);
        return this;
    }

    public void show() {
        mDialog.show();
        Window window = mDialog.getWindow();
        mProgressbar.show();
        if (window == null) {
            return;
        }
        window.setLayout(
                mIsJ1 ? WindowManager.LayoutParams.WRAP_CONTENT : DimensUtils.dp2px(580, mDialog.getContext()),
                WindowManager.LayoutParams.WRAP_CONTENT);

        if (!mIsJ1) {
            mSoundId = SoundUtil.playSoundHasId(mApp.getSoundPool(), mApp.getSoundIdByResourceId(fabscreen.platform.base.R.raw.sound_dialog_tip));
        }
    }

    public void dismiss() {
        if (mDialog.isShowing()) {
            if (mProgressbar != null) {
                mProgressbar.hide();
            }
            mDialog.dismiss();
        }
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }
}
