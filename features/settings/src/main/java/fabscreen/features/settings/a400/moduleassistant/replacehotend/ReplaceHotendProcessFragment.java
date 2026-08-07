package fabscreen.features.settings.a400.moduleassistant.replacehotend;

import static fabscreen.features.settings.a400.moduleassistant.replacehotend.ReplaceHotendViewModel.ReplaceProcess.ON_RESTART_BEGIN;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.core.ui.base.BaseProgressFragment;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceHotendProcessFragment extends BaseProgressFragment {

    @BindView(R2.id.iv_help)
    ImageView mIvHelp;

    private ReplaceHotendViewModel mViewModel;

    public static Fragment newInstance() {
        return new ReplaceHotendProcessFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_hotend_process;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceHotendViewModel.class);
        initView();
    }

    private void initView() {
        mIvHelp.setVisibility(View.GONE);
        showSetHeatingTemp();
        mViewModel.getReplaceProcessObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshReplaceView, LogHelper::log);

        WarmTipDialog movingDialog = WarmTipDialog.create(requireContext())
                .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                .setProgressVisible(true)
                .setTitle(R.string.all_move_show)
                .setContent(R.string.all_move_show_content);

        mViewModel.moveToolheadToTopCenter()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> handleMovingDialog(movingDialog, isMoving), LogHelper::log);
    }

    private void refreshReplaceView(ReplaceHotendViewModel.ReplaceProcess process) {
        setIfShowClose(process != ON_RESTART_BEGIN);
        switch (process) {
            case ON_HEATING_START:
                showHeating();
                break;
            case ON_HEATED:
                showUnloading();
                break;
            case ON_FILAMENT_CLEARED:
                showCoolingDown();
                break;
            case ON_READY_FOR_REPLACE:
                showDoReplace();
                break;
            case ON_RESTART_BEGIN:
                showRestarting();
                break;
            case ON_SUCCESS:
                goComplete();
                break;
            case ON_ERROR:
                showError();
                break;
        }
    }

    private void showError() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(1, true, false, false, false)
                .setType(DecisionDialog.ERROR_TYPE)
                .setPic(R.drawable.ic_pic_a400_error_68x68)
                .setContent(R.string.replace_module_cannot_continue)
                .setFirstTv(R.string.all_ok, R.color.select_dialog_red_txt, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @OnClick(R2.id.iv_close)
    void onCloseClicked() {
        playNormalClickSound();
        DecisionDialog.create(requireContext())
                .setDialogStatus(2, true, false, true, false)
                .setType(DecisionDialog.WARMING_TYPE)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.replace_hotend_stop_replace)
                .setContent(R.string.replace_hotend_stop_replace_desc)
                .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_left_text_color, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setSecondTv(getString(R.string.all_stop), R.color.select_dialog_yellow_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        stopReplacement();
                    }
                })
                .show();
    }

    private void stopReplacement() {
        FileParsingDialog loadingDialog = FileParsingDialog.create(requireContext())
                .setContent(R.string.all_loading_please_wait)
                .setCanceledOnTouchOutSide(false);
        loadingDialog.show();
        mViewModel.stopReplacement()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(stopped -> {
                    loadingDialog.dismiss();
                    if (stopped) {
                        requireActivity().finish();
                    }
                }, e -> {
                    LogHelper.log(e);
                    loadingDialog.dismiss();
                });
    }

    private void goComplete() {
        if (requireActivity() instanceof ReplaceHotendActivity) {
            ((ReplaceHotendActivity) requireActivity()).goToComplete();
        }
    }

    private void showSetHeatingTemp() {
        setMainTitle(getString(R.string.replace_hotend_title));
        setSubTitle(getString(R.string.replace_hotend_set_heating_temp));
        setProgress(1, 6);
        Bundle bundle = new Bundle();
        bundle.putInt(ReplaceHotendTempDashboardFragment.KEY_OPERATION, ReplaceHotendTempDashboardFragment.SET_TEMP);
        replaceFragment(ReplaceHotendTempDashboardFragment.class, bundle);
    }

    private void showHeating() {
        setSubTitle(getString(R.string.replace_hotend_heat_nozzle_2_6));
        setProgress(2, 6);
        replaceFragment(ReplaceHotendHeatingFragment.class, null);
    }

    private void showUnloading() {
        setSubTitle(getString(R.string.replace_hotend_manual_unloading));
        setProgress(3, 6);
        replaceFragment(ReplaceHotendManualUnloadingFragment.class, null);
    }

    private void showCoolingDown() {
        setSubTitle(getString(R.string.replace_hotend_nozzle_cooldown));
        setProgress(4, 6);
        Bundle bundle = new Bundle();
        bundle.putInt(ReplaceHotendTempDashboardFragment.KEY_OPERATION, ReplaceHotendTempDashboardFragment.COOLING_DOWN);
        replaceFragment(ReplaceHotendTempDashboardFragment.class, bundle);
    }

    private void showDoReplace() {
        setSubTitle(getString(R.string.replace_hotend_title) + getString(R.string.a400_bracket_has_date, "5/6"));
        setProgress(5, 6);
        Bundle bundle = new Bundle();
        bundle.putInt(CommonIntroFragment.KEY_OPERATION, CommonIntroFragment.REPLACE);
        replaceFragment(CommonIntroFragment.class, bundle);
    }

    private void showRestarting() {
        setSubTitle(getString(R.string.all_initialize_title) + getString(R.string.a400_bracket_has_date, "6/6"));
        setProgress(6, 6);
        replaceFragment(ReplaceHotendRestartingFragment.class, null);
    }

    private void replaceFragment(Class<? extends BaseFragment> fragmentClass, Bundle args) {
        getChildFragmentManager().beginTransaction().replace(R.id.fcv_replace_process, fragmentClass, args).commit();
    }

    private void handleMovingDialog(WarmTipDialog dialog, Boolean moving) {
        if (moving) {
            dialog.show();
        } else {
            dialog.dismiss();
        }
    }
}
