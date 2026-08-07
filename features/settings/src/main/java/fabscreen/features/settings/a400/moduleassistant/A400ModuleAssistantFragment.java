package fabscreen.features.settings.a400.moduleassistant;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_IDLE;

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
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.features.settings.common.A400ModuleAssistantViewModel;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400ModuleAssistantFragment extends BaseFragment {

    public static Fragment newInstance() {
        return new A400ModuleAssistantFragment();
    }

    @BindView(R2.id.ll_module_list)
    LinearLayout mLlModuleList;
    @BindView(R2.id.group_replace_hotend)
    Group mGroupReplaceHotend;
    @BindView(R2.id.view_transparent_mask)
    public View mViewTransparentMask;
    TextView tvTopToast;
    View floatView;
    ConstraintLayout clTopToast;


    private A400ModuleAssistantViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(A400ModuleAssistantViewModel.class);
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
                    clTopToast.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                    floatView.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
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

    @OnClick(fabscreen.platform.core.R2.id.view_transparent_mask)
    public void onClickMask() {
        playNormalClickSound();
        MachineStatus status = ServiceContainer.getInstance().getService(IMachine.class).getMachineStatusSubjectHolder().getValue();
        boolean isIdle = SYSTEM_STATUS_IDLE.valueEquals(status.status);
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
                    if (isPrint) {
                        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().stop();
                    }
                    exitCalibration();
                    dialog.dismiss();
                })).show();
    }

    public void exitCalibration() {
        try {
            Observable<ResponseStructure> responseStructureObservable = null;
            IMachine.WorkType workType = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType;
            switch (workType) {
                case FDM:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getFDMController().exitCalibration(false);
                    break;
                case LASER:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getLaserController().exitCalibration(false);
                    break;
                case CNC:
                    responseStructureObservable = ServiceContainer.getInstance().getService(IMachine.class).getCNCController().exitCalibration(false);
                    break;
            }
            if (responseStructureObservable == null) return;
            responseStructureObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (!success.isSuccess()) {
                            Logger.d("Exit Calibration: " + success);
                        }
                    }, LogHelper::log);
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mViewModel.canReplaceHotend()) {
            mGroupReplaceHotend.setVisibility(View.VISIBLE);
        }

        if (mLlModuleList.getChildCount() > 1) {
            mLlModuleList.removeViews(1, mLlModuleList.getChildCount() - 1);
        }
        List<String> moduleNameList = mViewModel.getModuleNameList();
        for (String name : moduleNameList) {
            TextView tvName = new TextView(requireContext());
            tvName.setText(name);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            tvName.setTextColor(0xFFFFFFFF);
            tvName.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), R.drawable.shape_a400_module_indicator, null), null, null, null);
            tvName.setCompoundDrawablePadding((int) DimensUtils.dp2px(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = 15;
            mLlModuleList.addView(tvName, lp);
        }
    }

    private void initView() {
        setTitle(R.string.a400_settings_module_assist_title);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_settings_module_assist;
    }

    @OnClick(R2.id.btn_replace_module)
    void onReplaceModuleClicked() {
        playNormalClickSound();
        if (mViewModel.isFDMType()) {
            DecisionDialog.create(getContext())
                    .setType(DecisionDialog.WARMING_TYPE)
                    .setTitle(R.string.a400_settings_module_assistant_unloading_check_title)
                    .setContent(R.string.a400_settings_module_assistant_unloading_check_message)
                    .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                    .setPic(R.drawable.ic_filament_112x112)
                    .setFirstTv(getContext().getResources().getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> {
                        dialog.dismiss();
                    }))
                    .setSecondTv(getContext().getResources().getString(R.string.a400_settings_module_assistant_unloading_check_unloaded), R.color.select_dialog_yellow_txt, ((dialog, which) -> {
                        dialog.dismiss();
                        mRouter.routeToReplaceModules().startForResult(this, 2);
                    })).show();
        } else {
            mRouter.routeToReplaceModules().startForResult(this, 2);
        }
    }

    @OnClick(R2.id.btn_replace_hotend)
    void onReplaceNozzleClicked() {
        playNormalClickSound();
        mRouter.routeToReplaceHotend().start(requireContext());
    }

    @OnClick(R2.id.sv_module_list)
    void guide() {
        playSwitchSound();
        mRouter.routeToGuideMilestone().start(requireContext());
    }
}
