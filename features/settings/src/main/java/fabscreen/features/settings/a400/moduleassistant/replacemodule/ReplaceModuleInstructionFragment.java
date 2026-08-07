package fabscreen.features.settings.a400.moduleassistant.replacemodule;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.core.ui.base.BaseProgressFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceModuleInstructionFragment extends BaseProgressFragment {

    @BindView(R2.id.iv_help)
    ImageView mIvHelp;
    //    @BindView(R2.id.sv_instruction)
//    ScrollView mSvInstruction;
//    @BindView(R2.id.tv_tap_help_tip)
//    TextView mTvTapHelp;
//    @BindView(R2.id.group_4pin_on)
//    Group mGroup4pinOn;
    @BindView(R2.id.iv_replace_module_pic)
    ImageView mIvPic;
    @BindView(R2.id.iv_replace_module_operable_tip)
    TextView mTvOperableTip;
    @BindView(R2.id.iv_replace_module_inoperable_tip)
    TextView mTvInoperableTip;
    @BindView(R2.id.iv_replace_module_step_tip)
    TextView mTvStepTip;

    @BindView(R2.id.rl_replace_module_inoperable)
    View mViewInoperableTip;

    private ReplaceModuleViewModel mViewModel;

    private DecisionDialog restartRequiredDialog;

    public static Fragment newInstance(boolean checked) {
        Fragment fragment = new ReplaceModuleInstructionFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("keep4pinOn", checked);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceModuleViewModel.class);
        initView();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void initView() {
        initTitle();

        setProgress(1, 3);

        refreshInstructionContent();

        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_a400_replace_module_power_off)
                .apply(options)
                .into(mIvPic);
        restartRequiredDialog = DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.a400_settings_replace_module_restart_required_title)
                .setContent(R.string.a400_settings_replace_module_restart_required_msg)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setSecondTv(R.string.all_restart, R.color.select_dialog_yellow_txt, ((dialog, which) -> {
                    restartRequiredDialog.mCancelBtn.setEnabled(false);
                    restartRequiredDialog.mSecondBtn.setEnabled(false);
                    mViewModel.restartMachine()
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(this::handleResult);
                }));
    }

    private void initTitle() {
        setMainTitle(getString(R.string.replace_module_title));
        setSubTitle(getString(R.string.replace_module_title) + getString(R.string.a400_bracket_has_date, "1/3"));
        mIvHelp.setVisibility(View.VISIBLE);
        setIfShowClose(false);
    }

    private void refreshInstructionContent() {
        boolean keep4pinOn = requireArguments().getBoolean("keep4pinOn");
        if (keep4pinOn) {
            mTvOperableTip.setText(R.string.a400_settings_replace_module_mode_keep4pinOn_operable_tip);
            mTvInoperableTip.setText(R.string.a400_settings_replace_module_mode_keep_4pin_on_inoperable_tip);
            mTvStepTip.setText(R.string.a400_settings_replace_module_mode_keep_4pin_on_step_tip);
        } else {
            mTvOperableTip.setText(R.string.a400_settings_replace_module_mode_keep_4pin_off_operable_tip);
//            mTvInoperableTip.setText(R.string.a400_replace_module_mode_keep4pinOff_inoperable_tip);
            mViewInoperableTip.setVisibility(View.GONE);
            mTvStepTip.setText(R.string.a400_settings_replace_module_mode_keep_4pin_off_step_tip);
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_manual_replace_instruction;
    }

    @OnClick(R2.id.btn_next)
    @Override
    public void onClick(View view) {
        super.onClick(view);
        restartRequiredDialog.show();
    }

    private void handleResult(int result) {
        restartRequiredDialog.mCancelBtn.setEnabled(true);
        restartRequiredDialog.mSecondBtn.setEnabled(true);
        switch (result) {
            case 0:
                goToRestarting();
                restartRequiredDialog.dismiss();
                break;
            case 1:
            default:
                restartRequiredDialog.dismiss();
                new SuperToastHelper.Builder()
                        .setMessage(getString(R.string.all_restart_fail))
                        .build()
                        .showToast(requireContext());
                break;
        }
    }

    private void goToRestarting() {
        if (requireActivity() instanceof A400ReplaceModuleActivity) {
            ((A400ReplaceModuleActivity) requireActivity()).goToReplaceModuleRestart();
        }
    }
}
