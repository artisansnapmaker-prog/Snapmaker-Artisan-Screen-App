package fabscreen.features.machinetools.setup.laser.tenw;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import java.util.Objects;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.structure.prop.BoolProp;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.LaserPasswordDialog;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SetupIntroFragment extends BaseFragment {
    @BindView(R2.id.iv_setup_intro)
    ImageView mIvSetupIntro;
    @BindView(R2.id.tv_setup_intro)
    TextView mTvSetupIntro;
    @BindView(R2.id.btn_start)
    Button mBtnStart;
    @BindView(R2.id.vp_main_pic)
    VideoPlayerIJK mVpMainPic;
    boolean isShowVideo = false;

    private SetupIntroViewModel mViewModel;
    private IMachine mMachine;
    private LaserPasswordDialog mLaserPasswordDialog;
    private String mPassWord;

    public static Fragment newInstance(Bundle pageData) {
        Fragment fragment = new SetupIntroFragment();
        fragment.setArguments(pageData);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(SetupIntroViewModel.class);
        mMachine = ServiceContainer.getInstance().getService(IMachine.class);
        initView();
    }

    private void initView() {
        mBtnStart.setText(R.string.all_start);

        mTvSetupIntro.setText(requireArguments().getInt("desc"));
        String vpPath = requireArguments().getString("videoPath", "");
        if (!vpPath.isEmpty()) {
            mVpMainPic.setVisibility(View.VISIBLE);
            mVpMainPic.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + vpPath);
            mVpMainPic.setLooping(true);
            isShowVideo = true;
        }
        int picIndex = requireArguments().getInt("image");
        if (picIndex != 0) {
            mIvSetupIntro.setImageResource(picIndex);
        }

        ((SetupIntroActivity) requireActivity()).bindLaserPasswordKeyboardListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    String lowCaseValue = s.toString().toLowerCase();
                    if (!lowCaseValue.equals(mPassWord.substring(mPassWord.length() - 4).toLowerCase())) {
                        showError();
                    } else {
                        // 0:unlock 1:lock
                        mViewModel.setLaserLockStatus(0)
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(success -> {
                                    if (success.isSuccess()) {
                                        goToActivity();
                                    }
                                }, LogHelper::log);
                    }
                }
            }
        });

        mPassWord = mViewModel.getProductSerialNumber();
        Logger.i("Requesting sn number to continue... %s", mPassWord);
    }

    public void showError() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.all_wifi_dialog_connect_failed_wrong_password)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setSecondTv(R.string.all_retry, R.color.select_dialog_yellow_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ((SetupIntroActivity)requireActivity()).showLaserPasswordKeyboard();
                    }
                }).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isShowVideo) {
            mVpMainPic.setLooping(false);
            mVpMainPic.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isShowVideo) {
            mVpMainPic.setLooping(true);
            mVpMainPic.start();
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_setup_intro;
    }


    @OnClick(R2.id.btn_start)
    void onStartClicked() {
        playNormalClickSound();
        mViewModel.getLaserLockStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        boolean isLock = ((BoolProp) responseStructure.dataProp).getValue();
                        if (isLock) {
                            ((SetupIntroActivity)requireActivity()).showLaserPasswordView();
                        } else {
                            // set mode and go
                            ((SetupIntroActivity)requireActivity()).hideLaserPasswordView();
                            goToActivity();
                        }
                    }
                }, LogHelper::log);


    }

    public void goToActivity() {
        mViewModel.setMode(Objects.requireNonNull(requireArguments().getString("router_destination")))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    if (result == 0) {
                        ((SetupIntroActivity) requireActivity()).goToDestinationForResult();
                    }
                }, LogHelper::log);
    }
}
