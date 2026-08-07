package fabscreen.features.settings.a400.moduleassistant.replacemodule;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.orhanobut.logger.Logger;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceModuleConfirmationFragment extends ReplaceModuleProgressFragment {

    @BindView(R2.id.ll_removed)
    LinearLayout mLlRemoved;
    @BindView(R2.id.ll_added)
    LinearLayout mLlAdded;
    @BindView(R2.id.iv_close)
    ImageView mIvClose;

    @BindView(R2.id.view_a400_laser_password_fullscreen)
    View mViewLaserPassword;
    @BindView(R2.id.tv_a400_laser_password_tap_to_enter)
    TextView mTvLaserPasswordTap;

    private CustomKeyboardUtil mCustomKeyboardUtil;

    private ReplaceModuleViewModel mViewModel;

    private String mPassWord;

    public static Fragment newInstance() {
        return new ReplaceModuleConfirmationFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceModuleViewModel.class);
        mViewModel.queryException();
        initView();
    }

    private void initView() {
        setMainTitle(getString(R.string.replace_module_title));
        setSubTitle(getString(R.string.replace_module_confirm_title) + getString(R.string.a400_bracket_has_date, "3/3"));
        setProgress(3, 3);
        mIvClose.setVisibility(View.INVISIBLE);
        setIfShowClose(false);

        List<String> removedModules = mViewModel.getRemovedModuleList();
        List<String> addedModules = mViewModel.getAddedModuleList();

        Logger.d("removed: %1$s, added: %2$s", removedModules, addedModules);

        // refresh view
        if (removedModules.isEmpty()) {
            TextView textView = new TextView(requireContext());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            textView.setTextColor(Color.WHITE);
            textView.setText(R.string.a400_settings_replace_module_na);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = 15;
            mLlRemoved.addView(textView, lp);
        } else {
            for (String moduleName : removedModules) {
                TextView textView = new TextView(requireContext());
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                textView.setTextColor(Color.WHITE);
                textView.setText(moduleName);
                textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), R.drawable.shape_a400_module_indicator, null), null, null, null);
                textView.setCompoundDrawablePadding((int) DimensUtils.dp2px(12));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = 15;
                mLlRemoved.addView(textView, lp);
            }
        }

        if (addedModules.isEmpty()) {
            TextView textView = new TextView(requireContext());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            textView.setTextColor(Color.WHITE);
            textView.setText(R.string.a400_settings_replace_module_na);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = 15;
            mLlAdded.addView(textView, lp);
        } else {
            for (String moduleName : addedModules) {
                TextView textView = new TextView(requireContext());
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                textView.setTextColor(Color.WHITE);
                textView.setText(moduleName);
                textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), R.drawable.shape_a400_module_indicator, null), null, null, null);
                textView.setCompoundDrawablePadding((int) DimensUtils.dp2px(12));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = 15;
                mLlAdded.addView(textView, lp);
            }
        }

        mCustomKeyboardUtil = new CustomKeyboardUtil(requireContext());
        bindLaserPasswordKeyboardListener(new TextWatcher() {
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
                                        goToReplaceComplete();
                                    }
                                }, LogHelper::log);
                    }
                }
            }
        });

    }

    public void showLaserPasswordView() {
        mViewLaserPassword.setVisibility(View.VISIBLE);
    }

    public void hideLaserPasswordView() {
        mViewLaserPassword.setVisibility(View.INVISIBLE);
    }

    public void bindLaserPasswordKeyboardListener(TextWatcher textWatcher) {
        mCustomKeyboardUtil.bindKeyboardListener(mTvLaserPasswordTap, textWatcher);
    }

    public void showLaserPasswordKeyboard() {
        mCustomKeyboardUtil.showKeyboard(mTvLaserPasswordTap, CustomKeyboardUtil.INPUT_TYPE_QWERTY_ABC);
        mCustomKeyboardUtil.setMaxLength(4);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_TEXT);
    }

    public void showError() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.all_wifi_dialog_connect_failed_wrong_password)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt,
                        (dialog, which) -> dialog.dismiss())
                .setSecondTv(R.string.all_retry, R.color.select_dialog_yellow_txt,
                        (dialog, which) -> {
                            dialog.dismiss();
                            showLaserPasswordKeyboard();
                        }).show();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_module_confirmation;
    }

    @OnClick({R2.id.btn_fail, R2.id.btn_done})
    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.btn_fail) {
            // do on fail
            DecisionDialog.create(requireContext())
                    .setDialogStatus(1, true, false, true, true)
                    .setType(DecisionDialog.ERROR_TYPE)
                    .setPic(R.drawable.pic_a400_dialog_failed_72x72)
                    .setTitle(R.string.replace_module_recognition_fail)
                    .setContent(R.string.replace_module_fail_dialog_content)
                    .setFirstTv(R.string.all_retry, R.color.select_dialog_white_txt, (dialog, which) -> {
                        dialog.dismiss();
                        retry();
                    })
                    .show();
        } else if (id == R.id.btn_done) {
            if (mViewModel.isSecondHead()) {
                ShowGotoUpdate();
            } else if (mViewModel.needGoToGuide()) {
                ShowGotoGuide();
            } else {
                mPassWord = mViewModel.getProductSerialNumber();
                Logger.i("Requesting sn number to continue... %s", mPassWord);
                // TODO: check tool head preparation
                if (mViewModel.isLaserToolHeadNeedUnlock()) {
                    showLaserPasswordView();
                } else {
                    goToReplaceComplete();
                }
            }
//            if (mViewModel.needGoToGuide()) {
//                ShowGotoGuide();
//            } else {
//                goToReplaceComplete();
//            }
        }
    }

    @OnClick(R2.id.tv_a400_laser_password_tap_to_enter)
    void onClickEnter(View v) {
        playNormalClickSound();
        showLaserPasswordKeyboard();
    }

    private void retry() {
        if (requireActivity() instanceof A400ReplaceModuleActivity) {
            mViewModel.restartMachine()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(this::handleRestartResult);
        }
    }

    private void handleRestartResult(Integer result) {
        switch (result) {
            case 0:
                ((A400ReplaceModuleActivity) requireActivity()).goToReplaceModuleRestart();
                break;
            case 1:
            default:
                new SuperToastHelper.Builder()
                        .setMessage("Fail to restart!")
                        .build()
                        .showToast(requireContext());
        }
    }

    private void goToReplaceComplete() {
        if (requireActivity() instanceof A400ReplaceModuleActivity) {
            ((A400ReplaceModuleActivity) requireActivity()).goToComplete();
        }
    }


    private void ShowGotoGuide() {
        if (requireActivity() instanceof A400ReplaceModuleActivity) {
            ((A400ReplaceModuleActivity) requireActivity()).goToGuide();
        }
    }

    private void showGoProposalGuide() {
        if (requireActivity() instanceof A400ReplaceModuleActivity) {
            ((A400ReplaceModuleActivity) requireActivity()).goToProposalGuide();
        }
    }

    private void ShowGotoUpdate() {
        mRouter.routeToOldUpdate().start(requireContext());
        requireActivity().finish();
    }
}
