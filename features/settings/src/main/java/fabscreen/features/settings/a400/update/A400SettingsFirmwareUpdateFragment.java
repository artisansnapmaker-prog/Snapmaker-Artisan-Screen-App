package fabscreen.features.settings.a400.update;

import static android.app.Activity.RESULT_OK;
import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_IDLE;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.features.settings.common.S30FirmwareUpdateViewModel;
import fabscreen.features.settings.common.S30FirmwareUpdateViewModel.ChangelogItem;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.lib.update.UpdateFileParser;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.A400ProgressButton;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400SettingsFirmwareUpdateFragment extends BaseFragment {

    @BindView(R2.id.tv_new_version)
    TextView mTvNewVersion;
    @BindView(R2.id.ll_change_log)
    LinearLayout mLlChangeLog;
    @BindView(R2.id.btn_update)
    A400ProgressButton mBtnUpdate;
    @BindView(R2.id.tv_version_size)
    TextView mTvVersionSize;
    @BindView(R2.id.tv_setting_bar_right)
    TextView mTvLocalUpgrade;
    @BindView(R2.id.tv_new_version_tip)
    TextView mTvNewVersionTip;
    @BindView(R2.id.group_new_version_content)
    Group mGroupNewVersionContent;
    @BindView(R2.id.group_checking)
    Group mGroupChecking;
    @BindView(R2.id.group_check_fail)
    Group mGroupCheckFail;
    @BindView(R2.id.group_latest)
    Group mGroupLatest;
    @BindView(R2.id.view_transparent_mask)
    public View mViewTransparentMask;
    TextView tvTopToast;
    View floatView;
    ConstraintLayout clTopToast;

    private S30FirmwareUpdateViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400SettingsFirmwareUpdateFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(S30FirmwareUpdateViewModel.class);
        initView();
        showPrintState();
    }

    private void showPrintState() {
        floatView = createFloatView();
        floatView.setVisibility(View.INVISIBLE);
        mViewModel.getMachineStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    boolean isIdle = SYSTEM_STATUS_IDLE.valueEquals(status.status);
                    boolean isPrint = status.status <= 10;
                    boolean is3DP = getServiceContainer().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM;
                    tvTopToast.setText(getString(R.string.a400_toast_operation_block_by_machine_desc, getString(isPrint && is3DP ? R.string.a400_toast_operation_block_by_machine_printing : R.string.a400_toast_operation_block_by_machine_working)));
                    floatView.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                    clTopToast.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                    mViewTransparentMask.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                }, LogHelper::log);
    }

    @Override
    public void onPause() {
        super.onPause();
        floatView.setVisibility(View.INVISIBLE);
    }

    @NonNull
    private View createFloatView() {
        ViewGroup rootView = (ViewGroup) requireActivity().findViewById(android.R.id.content).getRootView();
        View floatView = LayoutInflater.from(requireContext()).inflate(R.layout.view_a400_top_icon_toast, rootView, false);
        rootView.addView(floatView);
        ImageView ivTopToast = floatView.findViewById(R.id.iv_top_toast);
        tvTopToast = floatView.findViewById(R.id.tv_top_toast);
        clTopToast = floatView.findViewById(R.id.cl_top_toast);
        tvTopToast.setTextColor(getResources().getColor(R.color.palette_white_pure, null));
        tvTopToast.setTextSize(24);
        ivTopToast.setImageResource(R.drawable.pic_a400_warning_68x68);
        return floatView;
    }

    @OnClick(R2.id.view_transparent_mask)
    public void onClickMask() {
        playSwitchSound();
        MachineStatus status = mViewModel.getMachineStatusValue();
        boolean isPrint = status.status <= 10;
        boolean is3DP = getServiceContainer().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM;
        String workName = "";
        if (isPrint) {
            workName = getServiceContainer().getService(IPrintWorkspace.class).getFileName();
        } else {
            workName = getString(R.string.all_calibration);
        }
        DecisionDialog.create(getContext())
                .setType(DecisionDialog.WARMING_TYPE)
                .setTitle(getString(R.string.all_stop) + " " + getString(isPrint && is3DP ? R.string.a400_toast_operation_block_by_machine_printing : R.string.a400_toast_operation_block_by_machine_working))
                .setContent(getString(R.string.a400_calibration_assistant_back_notice, workName))
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, true)
                .setPic(R.drawable.ic_pic_a400_error_112x112)
                .setFirstTv(getContext().getResources().getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setSecondTv(getContext().getResources().getString(R.string.all_stop), R.color.select_dialog_red_txt, ((dialog, which) -> {
                    dialog.dismiss();
                    if (isPrint) {
                        mViewModel.stopWork();
                    }
                    mViewModel.exitCalibration();
                })).show();
    }


    private void initView() {
        mGroupLatest.setVisibility(View.INVISIBLE);
        mGroupCheckFail.setVisibility(View.INVISIBLE);
        mGroupLatest.setVisibility(View.INVISIBLE);
        mGroupNewVersionContent.setVisibility(View.INVISIBLE);
        mTvLocalUpgrade.setVisibility(View.VISIBLE);
        mTvLocalUpgrade.setText(R.string.a400_firmware_update_local_update_title);
        LinearGradient linearGradient = new LinearGradient(0, 0, mTvLocalUpgrade.getPaint().getTextSize() * mTvLocalUpgrade.getText().length(), 0, 0xFF1A41F5, 0xFF1A8CF5, Shader.TileMode.CLAMP);
        mTvLocalUpgrade.getPaint().setShader(linearGradient);
        mTvLocalUpgrade.invalidate();
        setTitle(String.format(getString(R.string.a400_firmware_update_firm_version_title), mViewModel.getCurrentVersion()));

        mViewModel.getNewVersionInfoObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::onGetNewVersionInfo, LogHelper::log);

        mViewModel.getFirmwareStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshStatus, LogHelper::log);

        mViewModel.getDownloadProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    int totalSizeInMegabyte = mViewModel.getVersionSizeInMegabyte();
                    int downloadedSizeInMegabyte = (int) (totalSizeInMegabyte * progress / 100f);
                    mBtnUpdate.setDownloadedSize(downloadedSizeInMegabyte);
                }, LogHelper::log);
    }

    private void onGetNewVersionInfo(S30FirmwareUpdateViewModel.VersionInfo version) {
        mTvNewVersion.setText(getString(R.string.a400_firmware_update_firmware) + version.name);
        displayChangelogs(version.changelogs);
        mTvVersionSize.setText(version.fileSize + getString(R.string.all_unit_mb) + " | " + version.releaseTime);
        mBtnUpdate.setMaxSize(version.fileSize);
    }

    private void refreshStatus(S30FirmwareUpdateViewModel.FirmwareDisplayStatus status) {
        switch (status) {
            case CHECKING:
                mGroupChecking.setVisibility(View.VISIBLE);
                break;
            case CHECK_FAIL:
                mGroupChecking.setVisibility(View.INVISIBLE);
                mGroupCheckFail.setVisibility(View.VISIBLE);
                break;
            case LATEST:
                mGroupChecking.setVisibility(View.INVISIBLE);
                mGroupLatest.setVisibility(View.VISIBLE);
                break;
            case TO_BE_DOWNLOADED:
                mGroupChecking.setVisibility(View.INVISIBLE);
                mGroupNewVersionContent.setVisibility(View.VISIBLE);
                mTvNewVersionTip.setText(R.string.a400_firmware_update_new_update_available);
                mTvNewVersionTip.setTextColor(0xffffab00);
                mBtnUpdate.setState(A400ProgressButton.State.IDLE);
                break;
            case DOWNLOADING:
                mGroupChecking.setVisibility(View.INVISIBLE);
                mGroupNewVersionContent.setVisibility(View.VISIBLE);
                mTvNewVersionTip.setText(R.string.a400_firmware_update_new_update_available);
                mTvNewVersionTip.setTextColor(0xffffab00);
                mBtnUpdate.setState(A400ProgressButton.State.DOWNLOADING);
                break;
            case DOWNLOADED:
                mGroupChecking.setVisibility(View.INVISIBLE);
                mGroupNewVersionContent.setVisibility(View.VISIBLE);
                mTvNewVersionTip.setText(R.string.a400_firmware_update_new_version_downloaded);
                mTvNewVersionTip.setTextColor(0xff62c864);
                mBtnUpdate.setState(A400ProgressButton.State.DOWNLOADED);
                break;
            case DOWNLOAD_FAIL:
                mGroupChecking.setVisibility(View.INVISIBLE);
                mTvNewVersionTip.setText(R.string.a400_firmware_update_download_fail);
                mTvNewVersionTip.setTextColor(0xffd0021b);
                mBtnUpdate.setState(A400ProgressButton.State.IDLE);
                break;
        }
    }

    private void displayChangelogs(List<ChangelogItem> items) {
        mLlChangeLog.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            ChangelogItem item = items.get(i);
            TextView textView = new TextView(requireContext());
            textView.setTextColor(Color.WHITE);
            textView.setText(item.words);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            if (item.type == ChangelogItem.ChangelogType.TITLE) {
                textView.setTextColor(0xffc9c9c9);
                textView.setTypeface(Typeface.DEFAULT_BOLD);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                layoutParams.leftMargin = (int) DimensUtils.dp2px(0f);
                layoutParams.bottomMargin = (int) DimensUtils.dp2px(12f);
                if (i > 0) {
                    layoutParams.topMargin = (int) DimensUtils.dp2px(48f);
                }
            } else {
                textView.setTextColor(0xff848484);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                layoutParams.leftMargin = (int) DimensUtils.dp2px(12f);
                layoutParams.bottomMargin = (int) DimensUtils.dp2px(4f);
            }
            mLlChangeLog.addView(textView, layoutParams);
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_s30_firmware_update;
    }


    @OnClick(R2.id.tv_setting_bar_right)
    void onUpdateLocalClicked() {
        playNormalClickSound();
        mRouter.routeToFilesPage(4).startForResult(this, 1);
    }

    @OnClick(R2.id.btn_update)
    void onUpdateClicked() {
        playNormalClickSound();
        switch (mViewModel.getCurrentStatus()) {
            case TO_BE_DOWNLOADED:
            case DOWNLOAD_FAIL:
                mViewModel.startDownload();
                break;
            case DOWNLOADING:
                mViewModel.cancelDownload();
                break;
            case DOWNLOADED:
                goToUpdate(UpdateFileParser.getBigBinPath(requireContext().getApplicationContext()), true);
                break;
        }
    }

    private void goToUpdate(String filePath, boolean isLocal) {
        mRouter.routeToUpdateInProgress(filePath, isLocal).start(requireContext());
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
}
