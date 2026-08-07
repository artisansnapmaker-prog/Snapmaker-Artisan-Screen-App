package fabscreen.features.settings.a400.update;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;

@Route(path = RoutePath.A400_SETTINGS_OLD_UPDATE_MODULES)
public class A400OldUpdateTipsActivity extends BaseActivity {
    @BindView(R2.id.iv_old_update)
    VideoPlayerIJK mVpVideo;

    private long mTime = 0;
    private int mTouchCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_update);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initVideo();
    }

    private void initVideo() {
        mVpVideo.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/hello.webm");
        mVpVideo.setLooping(true);
        mVpVideo.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVpVideo.setLooping(false);
        mVpVideo.stop();
    }

    @OnClick(R2.id.btn_old_update_start)
    public void onClickUpdateStart() {
        mRouter.routeToFilesPage(4).startForResult(this, 1);
    }

    @OnClick(R2.id.btn_old_update_escape_exit)
    void onClickEscapeExit() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - mTime < 1000) {
            mTouchCount += 1;
        } else {
            mTouchCount = 1;
        }
        mTime = currentTime;
        if (mTouchCount >= 5) {
            Logger.i("Escape exit triggered from update tip page.");
            DecisionDialog.create(this)
                    .setDialogStatus(2, true, false, false, false)
                    .setCanceledOnTouchOutSide(false)
                    .setPic(R.drawable.pic_a400_warning_112x112)
                    .setContent(R.string.a400_settings_two_point_o_old_update_emergency_escape_exit_content)
                    .setFirstTv(R.string.all_cancel, R.color.select_dialog_grey_txt, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setSecondTv(R.string.all_confirm, R.color.select_dialog_red_txt, ((dialog, which) -> {
                        mRouter.routeToHome().startAndClear(this);
                        dialog.dismiss();
                    }))
                    .show();

        }
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
                goToUpdate(filePath, isLocal);
            }
        }
    }

    private void goToUpdate(String filePath, boolean isLocal) {
        mRouter.routeToUpdateInProgress(filePath, isLocal).start(this);
    }
}
