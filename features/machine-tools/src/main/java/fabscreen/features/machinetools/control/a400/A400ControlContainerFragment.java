package fabscreen.features.machinetools.control.a400;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_IDLE;

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

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.control.common.ControlContainerViewModel;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.R2;
import fabscreen.platform.core.ui.common.leftsection.A400LeftSectionsAdapter;
import fabscreen.platform.core.ui.common.leftsection.LeftSectionsAdapter;
import fabscreen.platform.core.ui.common.leftsection.SectionAndDetailContainerFragment;
import fabscreen.platform.core.ui.common.leftsection.SectionItem;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400ControlContainerFragment extends SectionAndDetailContainerFragment {
    @BindView(R2.id.view_transparent_mask)
    public View mViewTransparentMask;
    TextView tvTopToast;
    ConstraintLayout clTopToast;

    public static Fragment newInstance() {
        return new A400ControlContainerFragment();
    }

    private ControlContainerViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        View floatView = createFloatView();
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

    @NonNull
    private View createFloatView() {
        ViewGroup rootView = (ViewGroup) requireActivity().findViewById(android.R.id.content).getRootView();
        View floatView = LayoutInflater.from(requireContext()).inflate(fabscreen.platform.base.R.layout.view_a400_top_icon_toast, rootView, false);
        rootView.addView(floatView);
        ImageView ivTopToast = floatView.findViewById(R.id.iv_top_toast);
        clTopToast = floatView.findViewById(R.id.cl_top_toast);
        tvTopToast = floatView.findViewById(R.id.tv_top_toast);
        tvTopToast.setTextColor(getResources().getColor(R.color.palette_white_pure, null));
        tvTopToast.setTextSize(24);
        ivTopToast.setImageResource(R.drawable.pic_a400_warning_68x68);
        return floatView;
    }

    @Override
    protected List<SectionItem> getLeftSections() {
        return mViewModel.getLeftSections();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_section_and_detail_container;
    }

    @Override
    protected LeftSectionsAdapter getSectionsAdapter(List<SectionItem> sectionItems) {
        return new A400LeftSectionsAdapter(sectionItems);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.all_control);
    }

    @Override
    public ControlContainerViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(ControlContainerViewModel.class);
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
}
