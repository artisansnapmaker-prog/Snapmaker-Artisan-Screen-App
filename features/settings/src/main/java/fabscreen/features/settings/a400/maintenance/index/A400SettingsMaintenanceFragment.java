package fabscreen.features.settings.a400.maintenance.index;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_IDLE;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.io.DataOutputStream;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.features.settings.a400.A400SettingsActivity;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400SettingsMaintenanceFragment extends BaseFragment {

    @BindView(R2.id.cl_config_params)
    ConstraintLayout mClConfigParams;
    @BindView(R2.id.cl_module_info)
    ConstraintLayout mClModuleInfo;
    @BindView(R2.id.cl_factory_reset)
    ConstraintLayout mClFactoryReset;
    TextView tvTopToast;
    View floatView;
    ConstraintLayout clTopToast;
    private SettingsMaintenanceViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400SettingsMaintenanceFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(SettingsMaintenanceViewModel.class);
        initView();
        showPrintState();
    }

    private void showPrintState() {
        floatView = createFloatView();
        mViewModel.getMachineStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    boolean isIdle = SYSTEM_STATUS_IDLE.valueEquals(status.status);
                    boolean isPrint = status.status <= 10;
                    boolean is3DP = getServiceContainer().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM;
                    tvTopToast.setText(getString(R.string.a400_toast_operation_block_by_machine_desc, getString(isPrint && is3DP ? R.string.a400_toast_operation_block_by_machine_printing : R.string.a400_toast_operation_block_by_machine_working)));
                    clTopToast.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                    floatView.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                }, LogHelper::log);
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

    private void initView() {

        switch (mViewModel.getWorkType()) {
            case CNC:
            case NONE:
                mClConfigParams.setVisibility(View.GONE);
                break;
            case FDM:
                ((TextView) mClConfigParams.findViewById(R.id.tv_title)).setText(R.string.a400_maintenance_config_params_fdm_title);
                mClConfigParams.setVisibility(View.VISIBLE);
                break;
            case LASER:
                ((TextView) mClConfigParams.findViewById(R.id.tv_title)).setText(R.string.a400_maintenance_config_params_laser_title);
                mClConfigParams.setVisibility(View.VISIBLE);
                break;
        }

        ((TextView) mClModuleInfo.findViewById(R.id.tv_title)).setText(R.string.a400_settings_maintenance_machine_information);
        ((TextView) mClFactoryReset.findViewById(R.id.tv_title)).setText(R.string.a400_settings_restore_to_factory_settings);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_settings_maintenance;
    }

    @OnClick({R2.id.cl_config_params, R2.id.cl_module_info, R2.id.cl_factory_reset})
    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.cl_config_params) {
            goToConfigParams();
        } else if (id == R.id.cl_module_info) {
            goToModuleInfo();
        } else if (id == R.id.cl_factory_reset) {
            MachineStatus status = mViewModel.getMachineStatusValue();
            boolean isIdle = SYSTEM_STATUS_IDLE.valueEquals(status.status);
            if (isIdle) {
                warnFactoryReset();
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
                            warnFactoryReset();
                        })).show();
            }
        }
    }

    private void warnFactoryReset() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.WARMING_TYPE)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.a400_settings_factory_reset_title)
                .setContent(R.string.a400_settings_factory_reset_msg)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> dialog.dismiss())
                .setSecondTv(R.string.all_confirm, R.color.select_dialog_yellow_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        doFactoryReset();
                    }
                })
                .show();
    }

    private void doFactoryReset() {
        FileParsingDialog loadingDialog = FileParsingDialog.create(requireContext())
                .setCanceledOnTouchOutSide(false)
                .setContent(R.string.settings_firmware_factory_reset);

        loadingDialog.show();

        // Tricky here. We delay 1000ms to showing dialog for a better UI transition.
        mViewModel.doFactoryReset()
                .delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    loadingDialog.dismiss();
//                    PowerManager manager = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
//                    manager.reboot(null);
                    // Use system command instead of service call to avoid system UI showing.
                    Process process = Runtime.getRuntime().exec("reboot");
                    DataOutputStream os = new DataOutputStream(process.getOutputStream());
                    os.writeBytes("reboot" + "\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    os.close();
                    process.waitFor();
                }, e -> {
                    loadingDialog.dismiss();
                    LogHelper.log(e);
                });
    }


    private void goToConfigParams() {
        if (requireActivity() instanceof A400SettingsActivity) {
            ((A400SettingsActivity) requireActivity()).goToMaintainConfigParams();
        }
    }

    private void goToModuleInfo() {
        if (requireActivity() instanceof A400SettingsActivity) {
            ((A400SettingsActivity) requireActivity()).goToMachineInfo();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        floatView.setVisibility(View.INVISIBLE);
    }
}
