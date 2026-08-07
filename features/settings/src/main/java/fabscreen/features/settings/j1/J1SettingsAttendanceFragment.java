package fabscreen.features.settings.j1;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class J1SettingsAttendanceFragment extends BaseFragment {
    @BindView(R2.id.sw_filament_l)
    SwitchCompat mSwFilamentL;
    @BindView(R2.id.sw_filament_r)
    SwitchCompat mSwFilamentR;
    @BindView(R2.id.sw_lighting)
    SwitchCompat mSwLighting;
    private J1SettingsAttendanceViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(J1SettingsAttendanceViewModel.class);
        initView();
    }

    private void initView() {
        mSwFilamentL.setChecked(mViewModel.isRunoutRecoveryEnabled(0));
        mSwFilamentR.setChecked(mViewModel.isRunoutRecoveryEnabled(1));
        mSwLighting.setChecked(mViewModel.isLightOn());

        mSwFilamentL.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mViewModel.setRunoutRecoveryEnabled(0, isChecked);
        });

        mSwFilamentR.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mViewModel.setRunoutRecoveryEnabled(1, isChecked);
        });

        mSwLighting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mViewModel.setLightingEnabled(isChecked);
        });
    }

    public static Fragment newInstance() {
        return new J1SettingsAttendanceFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_settings_attendance;
    }

    @OnClick({R2.id.ll_wizard, R2.id.ll_filament_runout_l, R2.id.ll_filament_runout_r, R2.id.ll_lighting, R2.id.tv_factory_reset})
    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.ll_wizard) {
            mRouter.routeToWelcome().start(requireContext());
        } else if (id == R.id.ll_filament_runout_l) {
            mSwFilamentL.toggle();
        } else if (id == R.id.ll_filament_runout_r) {
            mSwFilamentR.toggle();
        } else if (id == R.id.ll_lighting) {
            mSwLighting.toggle();
        } else if (id == R.id.tv_factory_reset) {
            DecisionDialog.create(getContext())
                    .setDialogStatus(DecisionDialog.BTN_TWO, false, false, false, true)
                    .setType(DecisionDialog.WARMING_TYPE)
                    .setContent(R.string.j1_setting_general_factory_factory_reset_content)
                    .setContentColor(R.color.palette_grey_french)
                    .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_left_text_color, ((dialog, which) -> {
                        dialog.dismiss();
                    }))
                    .setSecondTv(getString(R.string.j1_setting_general_factory_reset), R.color.palette_red_sunset, ((dialog, which) -> {
                        DecisionDialog.getsInstance().mCancelBtn.setEnabled(false);
                        DecisionDialog.getsInstance().mSecondBtn.setEnabled(false);
                        mViewModel.J1FactoryReset()
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(result -> {
                                    dialog.dismiss();
                                    mRouter.routeToWelcome().start(requireContext());
                                });
                    }))
                    .show();

        }
    }
}
