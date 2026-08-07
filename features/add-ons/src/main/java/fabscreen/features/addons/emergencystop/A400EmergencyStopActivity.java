package fabscreen.features.addons.emergencystop;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.features.addons.R;
import fabscreen.features.addons.R2;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.helper.SoundUtil;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.A400_ADDONS_EMERGENCY_STOP)
public class A400EmergencyStopActivity extends BaseActivity {
    @BindView(R2.id.tip_tv)
    TextView mTipTv;
    @BindView(R2.id.tip_icon)
    ImageView mTipImg;
    @BindView(R2.id.title_tv)
    TextView mTitleTv;
    @BindView(R2.id.v_drop_shadow_line)
    View mTvShadow;
    @BindView(R2.id.v_background)
    View mViewBackground;
    @BindView(R2.id.tv_continue)
    TextView mTvContinue;
    private boolean mIsTriggerOnPowerUp;
    private int mStreamId = -1;
    protected FileParsingDialog fabLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a400_emergency_stop);
        ButterKnife.bind(this);
        onNewIntent(getIntent());
        fabLoading = FileParsingDialog.create(this).setContent(getString(R.string.a400_emergency_stop_restart));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mIsTriggerOnPowerUp = bundle.getBoolean("is_triggered_on_power_up");
        }

        initView();
    }

    private void initView() {
        if (mIsTriggerOnPowerUp) {
            mTipImg.setImageResource(R.drawable.ic_pic_a400_error_112x112);
            mTitleTv.setText(R.string.a400_emergency_stop_title);
            mTipTv.setText(R.string.a400_emergency_stop_release_content);
            mTvShadow.setVisibility(View.VISIBLE);
            mTvContinue.setVisibility(View.VISIBLE);
            mViewBackground.setVisibility(View.GONE);
            if (mStreamId != -1) {
                SoundUtil.stopSound(mApp.getSoundPool(), mStreamId);
            }
        } else {
            mTipImg.setImageResource(R.drawable.ic_pic_a400_error_112x112);
            mTitleTv.setText(R.string.a400_emergency_stop_title);
            mTipTv.setText(R.string.a400_emergency_stop_content);
            mTvShadow.setVisibility(View.GONE);
            mTvContinue.setVisibility(View.GONE);
            mViewBackground.setVisibility(View.VISIBLE);
            if (mStreamId == -1) {
                mStreamId = SoundUtil.playSoundLoop(mApp.getSoundPool(), mApp.getSoundIdByResourceId(R.raw.sound_emergency_stop));
            }
        }
    }

    @OnClick(R2.id.tv_continue)
    public void onClickContinue() {
        mTvContinue.setEnabled(false);
        fabLoading.show();
        mMachine.getMachineController().restartMachine()
                .throttleLast(20, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe((responseStructure -> {
                    fabLoading.dismiss();
                    if (responseStructure.isSuccess()) {
                        mApp.restart();
                        mTvContinue.setEnabled(true);
                    }
                }), LogHelper::log);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mStreamId != -1) {
            SoundUtil.stopSound(mApp.getSoundPool(), mStreamId);
        }
    }
}
