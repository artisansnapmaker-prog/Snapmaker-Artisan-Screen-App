package fabscreen.features.settings.a400.moduleassistant.replacemodule;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceModuleIntroFragment extends BaseFragment {

    private ReplaceModuleViewModel mViewModel;

    @BindView(R2.id.sw_work_mode)
    SwitchCompat mSwWorkMode;
    @BindView(R2.id.iv_replace_module_pic)
    ImageView mIvMainPic;

    private DecisionDialog powerCutDialog;

    public static Fragment newInstance() {
        return new ReplaceModuleIntroFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_module;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceModuleViewModel.class);
        initView();
    }

    private void initView() {
        setTitle(getString(R.string.a400_replace_module_title));

        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext()).load(R.drawable.pic_a400_replace_modules_intro_578x434).apply(options).into(mIvMainPic);

        mSwWorkMode.setOnCheckedChangeListener((buttonView, isChecked) -> playSwitchSound());

        powerCutDialog = DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.a400_settings_replace_module_power_off_title)
                .setContent(R.string.a400_settings_replace_module_power_off_msg)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setSecondTv(R.string.all_next, R.color.select_dialog_yellow_txt, ((dialog, which) -> {
                    powerCutDialog.mCancelBtn.setEnabled(false);
                    powerCutDialog.mSecondBtn.setEnabled(false);
                    // TODO: move tool head and axis
                    mViewModel.startReplaceModuleMode(mSwWorkMode.isChecked())
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(this::handleResult);
                }));
    }

    @OnClick({R2.id.btn_start, R2.id.view_move_to_proper_position})
    @Override
    public void onClick(View view) {
        super.onClick(view);
        playNormalClickSound();
        if (view.getId() == R.id.btn_start) {
            powerCutDialog.show();
        } else if (view.getId() == R.id.view_move_to_proper_position) {
            DecisionDialog.create(requireContext())
                    .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                    .setPic(R.drawable.pic_toolhead_run_boundary)
                    .setType(DecisionDialog.TIP_TYPE)
                    .setTitle(requireContext().getString(R.string.a400_settings_replace_module_machine_move_title))
                    .setContent(R.string.a400_settings_replace_module_machine_move_desc)
                    .setSecondTv(requireContext().getString(R.string.all_confirm),
                            R.color.select_dialog_yellow_txt, (dialog, which) -> {
                                dialog.dismiss();
                                // show loading dialog
                                WarmTipDialog movingDialog = WarmTipDialog.create(requireContext())
                                        .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                                        .setPic(R.drawable.ic_block_setup)
                                        .setTitle(R.string.all_move_show)
                                        .setContent(R.string.all_move_show_content);
                                movingDialog.show();
                                mViewModel.moveToProperPosition()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .as(bindToLifecycle())
                                        .subscribe(result -> {
                                            movingDialog.dismiss();
                                        }, e -> {
                                            movingDialog.dismiss();
                                            LogHelper.log(e);
                                        });
                            }).
                    setFirstTv(requireContext().getString(R.string.all_cancel),
                            R.color.select_dialog_white_txt, (dialog, which) -> {
                                dialog.dismiss();
                            })
                    .show();
        }
    }

    private void handleResult(int result) {
        powerCutDialog.mCancelBtn.setEnabled(true);
        powerCutDialog.mSecondBtn.setEnabled(true);
        switch (result) {
            case 0:
                ((A400ReplaceModuleActivity) requireActivity()).goToReplaceModuleInstruction(mSwWorkMode.isChecked());
                break;
            case 1:
            case 2:
            default:
        }
        powerCutDialog.dismiss();
    }
}
