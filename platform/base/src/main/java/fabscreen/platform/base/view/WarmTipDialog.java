package fabscreen.platform.base.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import fabscreen.platform.base.R;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.helper.SoundUtil;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.BaseAppService;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;

public class WarmTipDialog {

    public enum WarmTipDialogSize {
        SIZE_S,
        SIZE_M
    }

    private static WarmTipDialog sInstance;
    private AlertDialog mDialog;
    private WarmTipDialogSize mDialogSize = WarmTipDialogSize.SIZE_S;
    private TextView mTipTv;
    private ImageView mTipImg;
    private TextView mTitleTv;
    private CircularProgressIndicator mCpProgress;
    private boolean mIsA400;

    public static final int TIP_TYPE = 101;
    public static final int WARMING_TYPE = 102;
    public static final int ERROR_TYPE = 103;
    private int mType = TIP_TYPE;
    public static int mSoundId;
    public static BaseAppService mApp;

    public static WarmTipDialog create(Context context) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        final Activity activity = (Activity) context;

        // Create new dialog and config
        AlertDialog dialog = builder.create();
        sInstance = new WarmTipDialog();
        sInstance.mIsA400 = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().seriesId == IMachine.MachineSeries.A;
        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(sInstance.mIsA400 ? R.layout.dialog_a400_warm_tip : R.layout.dialog_j1_warm_tip, null);
        dialog.setView(view);
        mApp = (BaseAppService) ServiceContainer.getInstance().getService(IAppService.class);

        sInstance.mDialog = dialog;
        sInstance.mDialog.setCanceledOnTouchOutside(false);
        sInstance.mTipTv = view.findViewById(R.id.tip_tv);
        sInstance.mTipImg = view.findViewById(R.id.tip_icon);
        sInstance.mTitleTv = view.findViewById(R.id.title_tv);

        if (sInstance.mIsA400) {
            sInstance.mCpProgress = view.findViewById(R.id.progressbar_dialog_parsing);
        }

        dialog = builder.create();
        dialog.setView(view);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (sInstance.mIsA400) {
                    SoundUtil.stopSound(mApp.getSoundPool(), mSoundId);
                }
            }
        });

        return sInstance;
    }

    public static WarmTipDialog getsInstance() {
        return sInstance;
    }

    public WarmTipDialog setPic(int imgResources) {
        mTipImg.setImageResource(imgResources);
        mTipImg.setVisibility(View.VISIBLE);
        if (mIsA400) {
            mCpProgress.setVisibility(View.GONE);
        }
        return this;
    }

    public WarmTipDialog setTitle(String title) {
        mTitleTv.setText(title);
        mTitleTv.setVisibility(View.VISIBLE);
        return this;
    }

    public WarmTipDialog setTitle(@StringRes int titleId) {
        mTitleTv.setText(titleId);
        mTitleTv.setVisibility(View.VISIBLE);
        return this;
    }

    public WarmTipDialog setContent(@StringRes int content) {
        mTipTv.setText(content);
        mTipTv.setVisibility(View.VISIBLE);
        return this;
    }

    public WarmTipDialog setContentColor(@ColorRes int color) {
        mTipTv.setTextColor(ContextCompat.getColor(mDialog.getContext(), color));
        return this;
    }

    public WarmTipDialog setContent(String content) {
        mTipTv.setText(content);
        mTipTv.setVisibility(View.VISIBLE);
        return this;
    }

    public WarmTipDialog setProgressVisible(boolean visible) {
        if (mIsA400) {
            if (visible == true) {
                mTipImg.setVisibility(View.GONE);
            }
            mCpProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        return this;
    }

    public WarmTipDialog setOUtSideCanTouch(boolean canTouch) {
        mDialog.setCanceledOnTouchOutside(canTouch);
        return this;
    }

    public WarmTipDialog setDialogWidthSize(WarmTipDialogSize size) {
        mDialogSize = size;
        return this;
    }

    public WarmTipDialog setType(int type) {
        mType = type;
        return this;
    }

    public void show() {
        if (mDialog == null) {
            return;
        }
        mDialog.show();
        Window window = mDialog.getWindow();
        if (window == null) return;
        TypedValue typedValue = new TypedValue();
        int dialogWidth = R.attr.theme_dialog_small_width;
        switch (mDialogSize) {
            case SIZE_S:
                dialogWidth = R.attr.theme_dialog_small_width;
                break;
            case SIZE_M:
                dialogWidth = R.attr.theme_dialog_middle_width;
                break;
        }
        mDialog.getContext().getTheme().resolveAttribute(dialogWidth, typedValue, true);
        window.setLayout(
                DimensUtils.dp2px(typedValue.getFloat(), mDialog.getContext()),
                WindowManager.LayoutParams.WRAP_CONTENT);

        if (!mIsA400) {
            return;
        }

        switch (mType) {
            case TIP_TYPE:
                mSoundId = SoundUtil.playSoundHasId(mApp.getSoundPool(), mApp.getSoundIdByResourceId(R.raw.sound_dialog_tip));
                break;
            case WARMING_TYPE:
                mSoundId = SoundUtil.playSoundHasId(mApp.getSoundPool(), mApp.getSoundIdByResourceId(R.raw.sound_dialog_warming));
                break;
            case ERROR_TYPE:
                mSoundId = SoundUtil.playSoundHasId(mApp.getSoundPool(), mApp.getSoundIdByResourceId(R.raw.sound_dialog_error));
                break;
        }

    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

}
