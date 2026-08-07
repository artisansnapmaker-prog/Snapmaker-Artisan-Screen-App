package fabscreen.features.machinetools.calibration;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6.A400LaserCalibrationViewModel;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.base.BaseProgressFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public abstract class BaseCalibrationProgressFragment extends BaseProgressFragment {
    protected A400LaserCalibrationViewModel mViewModel;
    private DecisionDialog mDecisionDialog;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        mDecisionDialog = DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(R.string.a400_laser_stop_calibration_title)
                .setContent(R.string.a400_laser_stop_calibration_content)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setSecondTv(R.string.all_stop, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    quitCalibration();
                });
    }

    @Override
    protected abstract int getLayoutResID();

    @OnClick(R2.id.iv_close)
    public void onCloseClicked() {
        playNormalClickSound();
        mDecisionDialog.show();

    }

    private void quitCalibration() {
        mViewModel.quitCalibration(false)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mDecisionDialog.dismiss();
                    back();
                }, LogHelper::log);
    }

    @Override
    protected A400LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(A400LaserCalibrationViewModel.class);
    }
}
