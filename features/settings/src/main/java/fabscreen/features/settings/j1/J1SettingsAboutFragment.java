package fabscreen.features.settings.j1;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import fabscreen.features.settings.R;
import fabscreen.features.settings.common.ExperienceProgramDialogFragment;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.view.SuperToastHelper;

import static fabscreen.features.settings.j1.J1SettingsInputNameFragment.MACHINE_NAME_KEY;
import static fabscreen.features.settings.j1.J1SettingsInputNameFragment.REQUEST_NAME_KEY;

public class J1SettingsAboutFragment extends BaseSettingsAboutFragment {
    public static J1SettingsAboutFragment newInstance() {
        return new J1SettingsAboutFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_settings_about;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the SettingsActivity's FragmentManager
        requireParentFragment().getParentFragmentManager().setFragmentResultListener(REQUEST_NAME_KEY, this, (requestKey, result) -> {
            String machineName = result.getString(MACHINE_NAME_KEY);
            mTvMachineName.setText(machineName);
        });
    }

    @Override
    protected void onClickEditName(View v) {
        // We will get name in the resultListener under onCreate().
        if (requireActivity() instanceof J1SettingsActivity) {
            ((J1SettingsActivity) requireActivity()).goToNameInput();
        }
    }

    @Override
    protected void initExportLogsPopup() {
        View exportView = getLayoutInflater().inflate(R.layout.popup_j1_export_logs, (ViewGroup) requireView(), false);
        View usbDrive = exportView.findViewById(R.id.v_top_area);
        View lava = exportView.findViewById(R.id.v_bottom_area);
        TextView tvLava = exportView.findViewById(R.id.tv_lava);
        ImageView ivHelp = exportView.findViewById(R.id.iv_help);
        tvLava.setTextColor(mViewModel.isRemoteAvailable() ? 0xFFF7F8FA : 0xFF595A66);
        ivHelp.setVisibility(mViewModel.isRemoteAvailable() ? View.INVISIBLE : View.VISIBLE);

        usbDrive.setOnClickListener(v -> {
            Logger.d("Exporting logs to usb disk...");
            mExportWindow.dismiss();
            mViewModel.exportLogsToUDisk();
        });

        lava.setOnClickListener(v -> {
            Logger.d("Exporting logs to LAVA...");
            mExportWindow.dismiss();
            if (mViewModel.isRemoteAvailable()) {
                mViewModel.exportLogsToRemote();
            } else {
                new SuperToastHelper.Builder()
                        .setMessage("Please connect Lava Studio to the machine first.")
                        .build()
                        .showToast(requireContext());
            }
        });

        ivHelp.setOnClickListener(v -> {
            new SuperToastHelper.Builder()
                    .setMessage("Please connect Lava Studio to the machine first.")
                    .build()
                    .showToast(requireContext());
        });

        mExportWindow = new PopupWindow(exportView, (int) DimensUtils.dp2px(180), (int) DimensUtils.dp2px(124));
        mExportWindow.setElevation(8);
    }

    @Override
    protected void showAsDropDownWithOffset() {
        mExportWindow.showAsDropDown(mIvExport);
    }

    @Override
    protected void goToCertification() {
        ExperienceProgramDialogFragment.newInstance(R.string.j1_setting_about_compliance_certification,
                R.string.j1_setting_about_compliance_certification_content, false)
                .show(getChildFragmentManager(), "Compliance");
    }
}
