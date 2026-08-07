package fabscreen.features.settings.a400.about;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_IDLE;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import fabscreen.features.settings.R;
import fabscreen.features.settings.a400.A400SettingsActivity;
import fabscreen.features.settings.j1.BaseSettingsAboutFragment;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.structure.MachineFault;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400SettingsAboutMachineFragment extends BaseSettingsAboutFragment {
    TextView tvTopToast;
    View floatView;
    ConstraintLayout clTopToast;
    private CustomKeyboardUtil mCustomKeyboardUtil;

    public static A400SettingsAboutMachineFragment newInstance() {
        return new A400SettingsAboutMachineFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_settings_about;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.all_about_machine);
        mCustomKeyboardUtil = new CustomKeyboardUtil(requireActivity());
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
        clTopToast = floatView.findViewById(R.id.cl_top_toast);
        tvTopToast = floatView.findViewById(R.id.tv_top_toast);
        tvTopToast.setTextColor(getResources().getColor(R.color.palette_white_pure, null));
        tvTopToast.setTextSize(24);
        ivTopToast.setImageResource(R.drawable.pic_a400_warning_68x68);
        return floatView;
    }

    @Override
    protected void onClickEditName(View v) {
        // TODO: keyboard
        mCustomKeyboardUtil.bindKeyboardListener(v, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = String.valueOf(s).substring(0, Math.min(s.length(), 32));
                mTvMachineName.setText(name);
                getServiceContainer().getService(IPreferences.class).getHelper().setMachineName(name);
            }
        });
        mCustomKeyboardUtil.setPreInputText(mViewModel.getUserMachineName());
        mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_QWERTY_ABC);
    }

    @Override
    protected void initExportLogsPopup() {
        View exportView = getLayoutInflater().inflate(R.layout.popup_a400_export_logs, (ViewGroup) requireView(), false);
        TextView tvUsb = exportView.findViewById(R.id.tv_to_usb);
        TextView tvLuban = exportView.findViewById(R.id.tv_to_luban);

        tvUsb.setOnClickListener(v -> {
            Logger.d("Exporting logs to usb disk...");
            playNormalClickSound();
            mExportWindow.dismiss();
            mViewModel.exportLogsToUDisk();
        });

        tvLuban.setOnClickListener(v -> {
            playNormalClickSound();
            Logger.d("Exporting logs to Luban...");
            mExportWindow.dismiss();
            if (mViewModel.isRemoteAvailable()) {
                mViewModel.exportLogsToRemote();
            } else {
                new SuperToastHelper.Builder()
                        .setMessage(getString(R.string.a400_about_please_connect_luban_first))
                        .build()
                        .showToast(requireContext());
            }
        });

        mExportWindow = new PopupWindow(exportView, (int) DimensUtils.dp2px(360), (int) DimensUtils.dp2px(218));
        mExportWindow.setElevation(8);
        mExportWindow.setOnDismissListener(() -> playArrowAnimation(true));
    }

    @Override
    protected void showAsDropDownWithOffset() {
        playArrowAnimation(false);
        mExportWindow.showAsDropDown(mLlExportLogs, (int) DimensUtils.dp2px(522), (int) DimensUtils.dp2px(-7));
    }

    private void playArrowAnimation(boolean isDismiss) {
        ValueAnimator animator = ValueAnimator.ofFloat(isDismiss ? -90f : 90f, isDismiss ? 90f : -90f);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> mIvExport.setRotation((Float) animation.getAnimatedValue()));
        animator.setDuration(100);
        animator.start();
    }

    @Override
    protected void goToCertification() {
        if (requireActivity() instanceof A400SettingsActivity) {
            ((A400SettingsActivity) requireActivity()).goToLongTextDisplay(
                    R.string.a400_settings_about_certification_page_title,
                    R.string.a400_settings_about_certification_page_content
            );
        }
    }
}
