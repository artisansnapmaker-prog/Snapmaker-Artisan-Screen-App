package fabscreen.features.machinetools.setup.laser.tenw;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentContainerView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.TOOLS_SETUP_COMMON_INTRO)
public class SetupIntroActivity extends BaseActivity {
    @BindView(R2.id.tv_title)
    TextView mTvTitle;
    @BindView(R2.id.progress)
    LinearProgressIndicator mProgress;
    @BindView(R2.id.btn_close)
    Button mBtnClose;


    @BindView(R2.id.view_a400_laser_password_fullscreen)
    View mViewLaserPassword;
    @BindView(R2.id.tv_a400_laser_password_tap_to_enter)
    TextView mTvLaserPasswordTap;

    @BindView(R2.id.fcv_setup_content)
    FragmentContainerView mFcvIntro;


    private Bundle mPageData;

    private CustomKeyboardUtil mCustomKeyboardUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        mProgress.setVisibility(View.GONE);
        mBtnClose.setVisibility(View.GONE);
        mPageData = getIntent().getBundleExtra("page_data");
        mTvTitle.setText(mPageData.getString("title"));

        mCustomKeyboardUtil = new CustomKeyboardUtil(this);

        addFragment(R.id.fcv_setup_content, SetupIntroFragment.newInstance(mPageData));
    }

    public void showLaserPasswordView() {
        mViewLaserPassword.setVisibility(View.VISIBLE);
        mFcvIntro.setVisibility(FragmentContainerView.INVISIBLE);
    }

    public void hideLaserPasswordView() {
        mViewLaserPassword.setVisibility(View.INVISIBLE);
        mFcvIntro.setVisibility(FragmentContainerView.VISIBLE);
    }

    public void bindLaserPasswordKeyboardListener(TextWatcher textWatcher) {
        mCustomKeyboardUtil.bindKeyboardListener(mTvLaserPasswordTap, textWatcher);
    }

    public void showLaserPasswordKeyboard() {
        mCustomKeyboardUtil.showKeyboard(mTvLaserPasswordTap, CustomKeyboardUtil.INPUT_TYPE_QWERTY_ABC);
        mCustomKeyboardUtil.setMaxLength(4);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_TEXT);
    }

    public void goToDestinationForResult() {
        mRouter.routeWithClassPath(mPageData.getString("router_destination")).startForResult(this, 1);
    }

    @OnClick(R2.id.tv_a400_laser_password_tap_to_enter)
    void onClickEnter(View v) {
        playNormalClickSound();
        showLaserPasswordKeyboard();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            setResult(resultCode);
            finish();
        }
    }
}
