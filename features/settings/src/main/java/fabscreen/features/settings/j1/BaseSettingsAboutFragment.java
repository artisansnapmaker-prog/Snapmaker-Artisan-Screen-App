package fabscreen.features.settings.j1;

import static fabscreen.features.settings.j1.S30SettingsAboutViewModel.ExportState.ON_SUCCESS;
import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_IDLE;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.FileLoadingDialog;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class BaseSettingsAboutFragment extends BaseFragment {

    @BindView(R2.id.tv_machine_name)
    public TextView mTvMachineName;
    @BindView(R2.id.tv_machine_model)
    TextView mTvMachineModel;
    @BindView(R2.id.tv_work_area)
    TextView mTvWorkArea;
    @BindView(R2.id.tv_ip_address)
    TextView mTvIpAddress;
    @BindView(R2.id.tv_mac_address)
    TextView mTvMacAddress;
    @BindView(R2.id.tv_serial_number_address)
    TextView mTvSnAddress;
    @BindView(R2.id.tv_storage)
    TextView mTvStorage;
    @BindView(R2.id.iv_export)
    public ImageView mIvExport;
    @BindView(R2.id.ll_export_logs)
    public LinearLayout mLlExportLogs;

    @BindView(R2.id.tv_machine_verify_code)
    TextView mTvVerifyCode;


    private long mTime = 0;
    private int mTouchCount = 0;
    private boolean isDeveloper;

    protected S30SettingsAboutViewModel mViewModel;
    protected PopupWindow mExportWindow;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(S30SettingsAboutViewModel.class);
        initView();
    }

    private void initView() {
        mTvMachineName.setText(mViewModel.getUserMachineName());
        mTvMachineModel.setText(mViewModel.getMachineModelName());
        mTvSnAddress.setText(mViewModel.getProductSerialNumber());
        mTvVerifyCode.setText(mViewModel.getMachineVerifyCode());
        mTvWorkArea.setText(mViewModel.getWorkArea());
        mTvIpAddress.setText(mViewModel.getIPAddress());
        mTvStorage.setText(mViewModel.getStorageUsage());
        if (!TextUtils.isEmpty(mViewModel.getMacAddr())) {
            mTvMacAddress.setText(mViewModel.getMacAddr());
        }
        initExportLogsPopup();
        FileLoadingDialog loading = FileLoadingDialog.create(requireContext(), true).setContent("Exporting...");
        mViewModel.getExportStateObservable()
                .flatMap(exportState -> ON_SUCCESS.equals(exportState) ? Observable.timer(10, TimeUnit.SECONDS).flatMap(t -> Observable.just(exportState)) : Observable.just(exportState))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(state -> handleExportResult(loading, state), LogHelper::log);
    }

    @OnClick({R2.id.ll_edit_name, R2.id.ll_export_logs, R2.id.ll_certification, R2.id.tv_clear_cache})
    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.ll_edit_name) {
            onClickEditName(view);
        } else if (id == R.id.ll_export_logs) {
            showPopup();
        } else if (id == R.id.ll_certification) {
            goToCertification();
        } else if (id == R.id.tv_clear_cache) {
            MachineStatus status = mViewModel.getMachineStatusValue();
            boolean isIdle = SYSTEM_STATUS_IDLE.valueEquals(status.status);
            if (isIdle) {
                showClearCache();
            } else {
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
                            showClearCache();

                        })).show();
            }
        }
    }

    private void showClearCache() {
        DecisionDialog.create(getContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.WARMING_TYPE)
                .setContent(R.string.a400_settings_clean_cache_msg)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.a400_settings_clean_cache_title)
                .setContentColor(R.color.palette_grey_french)
                .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_left_text_color, ((decisionDialog, which1) -> {
                    decisionDialog.dismiss();
                }))
                .setSecondTv(getString(R.string.all_confirm), R.color.select_dialog_yellow_txt, ((decisionDialog, which1) -> {
                    decisionDialog.dismiss();
                    mViewModel.clearCache();
                }))
                .show();
    }

    private void showPopup() {
        if (mExportWindow.isShowing()) {
            mExportWindow.dismiss();
        } else {
            showAsDropDownWithOffset();
            mExportWindow.setFocusable(true);
            mExportWindow.setTouchable(true);
            mExportWindow.setOutsideTouchable(true);
        }
    }

    protected abstract void showAsDropDownWithOffset();

    protected abstract void onClickEditName(View v);

    protected abstract void initExportLogsPopup();

    protected abstract void goToCertification();

    @OnClick(R2.id.ll_settings_about_model_name)
    public void onclickMachineName() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - mTime < 500) {
            mTouchCount += 1;
        } else {
            mTouchCount = 1;
        }
        mTime = currentTime;
        if (mTouchCount >= 5) {
            isDeveloper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getMachineDeveloper();

            DecisionDialog.create(requireContext())
                    .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                    .setType(DecisionDialog.TIP_TYPE)
                    .setContent(isDeveloper ? "关闭开发者模式?" : "开启开发者模式")
                    .needMoreHeight()
                    .setCanceledOnTouchOutSide(true)
                    .setFirstTv(isDeveloper ? "关闭" : "开启", R.color.select_dialog_orange_txt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setMachineDeveloper(!isDeveloper);
                            if (requireActivity() instanceof J1SettingsActivity) {
                                ((J1SettingsActivity) requireActivity()).setDeveloperState(!isDeveloper);
                            }
                        }
                    }).show();
        }
    }

    private void handleExportResult(FileLoadingDialog loading, S30SettingsAboutViewModel.ExportState state) {
        switch (state) {
            case ON_START:
                loading.show();
                break;
            case ON_SUCCESS:
                loading.dismiss();
                new SuperToastHelper.Builder()
                        .setDrawable(R.drawable.ic_toast_success)
                        .setMessage("Exported")
                        .build()
                        .showToast(requireContext());
                break;
            case ON_FAIL_NO_REMOTE:
                loading.dismiss();
                new SuperToastHelper.Builder()
                        .setMessage("Please connect Lava Studio to the machine first.")
                        .build()
                        .showToast(requireContext());
                break;
            case ON_FAIL_NO_U_DISK:
                loading.dismiss();
                new SuperToastHelper.Builder()
                        .setMessage("Please insert a USB flash drive first.")
                        .build()
                        .showToast(requireContext());
                break;
            case ON_FAIL_OTHER:
                loading.dismiss();
                new SuperToastHelper.Builder()
                        .setMessage("Fail")
                        .build()
                        .showToast(requireContext());
                break;
        }
    }
}
