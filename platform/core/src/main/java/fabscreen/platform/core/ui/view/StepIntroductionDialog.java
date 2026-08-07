package fabscreen.platform.core.ui.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;

public class StepIntroductionDialog {
    @BindView(R2.id.tv_step_introduction_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_step_introduction_content)
    TextView mTvContent;
    @BindView(R2.id.iv_step_introduction)
    ImageView mIvContent;
    @BindView(R2.id.cv_step_introduction)
    CardView mCvContent;
    @BindView(R2.id.iv_step_introduction_back)
    ImageView mIvBack;
    @BindView(R2.id.vp_focus_lever)
    VideoPlayerIJK mVpFocusLever;
    private AlertDialog mDialog;

    private boolean isShowVideo = false;

    public StepIntroductionDialog(AlertDialog dialog, View view) {
        mDialog = dialog;
        ButterKnife.bind(this, view);
    }


    public static StepIntroductionDialog create(Context context) {
        // create dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        final Activity activity = (Activity) context;

        // Create new dialog and config
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_step_introduction, null);
        dialog.setView(view);

        return new StepIntroductionDialog(dialog, view);
    }

    public StepIntroductionDialog setTitle(@StringRes int resId) {
        mTvTitle.setVisibility(View.VISIBLE);
        mTvTitle.setText(resId);
        return this;
    }

    public StepIntroductionDialog setTitle(String resStr) {
        mTvTitle.setVisibility(View.VISIBLE);
        mTvTitle.setText(resStr);
        return this;
    }

    public StepIntroductionDialog setContent(@StringRes int resId) {
        mTvContent.setVisibility(View.VISIBLE);
        mTvContent.setText(resId);
        return this;
    }

    public StepIntroductionDialog setContent(String resStr) {
        mTvContent.setVisibility(View.VISIBLE);
        mTvContent.setText(resStr);
        return this;
    }

    public StepIntroductionDialog setImage(@DrawableRes int resId) {
        mCvContent.setVisibility(View.VISIBLE);
        Glide.with(mDialog.getContext()).load(resId).into(mIvContent);
        return this;
    }

    public StepIntroductionDialog setVideo(String videoPath) {
        if (videoPath == null) return this;

        mVpFocusLever.setVisibility(View.VISIBLE);
        mVpFocusLever.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + videoPath);
        mVpFocusLever.setLooping(true);

        isShowVideo = true;

        return this;
    }

    public StepIntroductionDialog setOnClickBack(View.OnClickListener listener) {
        mIvBack.setVisibility(View.VISIBLE);
        mIvBack.setOnClickListener(listener);
        return this;
    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            if (isShowVideo) {
                mVpFocusLever.stop();
            }
            mDialog.dismiss();

        }
    }

    public void setCanceledOnTouchOutSide(boolean b) {
        mDialog.setCanceledOnTouchOutside(b);
    }

    public void show() {
        if (mDialog != null && !mDialog.isShowing()) {
            mDialog.show();
            Window window = mDialog.getWindow();
            if (window == null) return;
            window.setLayout(
                    DimensUtils.dp2px(1184, mDialog.getContext()),
                    WindowManager.LayoutParams.WRAP_CONTENT);

            if (isShowVideo) {
                mVpFocusLever.start();
            }
        }
    }
}
