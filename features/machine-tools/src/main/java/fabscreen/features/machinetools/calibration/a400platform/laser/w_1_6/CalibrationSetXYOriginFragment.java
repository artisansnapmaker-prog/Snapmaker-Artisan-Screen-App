package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6;

import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import fabscreen.features.machinetools.R;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.ViewUtils;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CalibrationSetXYOriginFragment extends CalibrationJogFragment {

    private DecisionDialog mDecisionDialog;

    public static Fragment newInstance() {
        return new CalibrationSetXYOriginFragment();
    }

    @Override
    protected void initView() {
        super.initView();
        setMainTitle(getString(R.string.calibration_manual_focus_calibration_title));
        setSubTitle(getString(R.string.calibration_manual_focus_calibration_two_subtitle));
        setProgress(2, 4);

        mTvCalibrationDescTitle.setText(R.string.calibration_manual_focus_calibration_two_content_title);
        mTvCalibrationDescContent.setText(R.string.calibration_manual_focus_calibration_two_content);
        mBtnRunBoundary.setVisibility(View.VISIBLE);

        mCvMainPic.setVisibility(View.VISIBLE);
        if (getViewModel().isRotaryAvailable()) {
            mVpMainPic.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/Laser_4x_1.6w_Set_XY_Origin.webm");
        } else {
            mVpMainPic.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/Laser_3x_1.6w_Set_XY_Origin.webm");
        }
        mVpMainPic.setLooping(true);
        mBtnRunBoundary.setEnabled(false);
        mViewModel.getLoadBoundaryObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> mBtnRunBoundary.setEnabled(true), LogHelper::log);

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> ViewUtils.enableButtons((ViewGroup) requireView(), !isMoving), LogHelper::log);

        mDecisionDialog = DecisionDialog.create(getContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.TIP_TYPE)
                .setPic(R.drawable.ic_laser_turn_on_224x224)
                .setTitle(R.string.open_laser_title)
                .setContent(R.string.a400_calibration_laser_on_reminder)
                .setCanceledOnTouchOutSide(true)
                .setFirstTv(getResources().getString(R.string.all_cancel), R.color.select_dialog_left_text_color, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setSecondTv(getResources().getString(R.string.all_confirm), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    setOriginAndGo();
                });
        mXYZBCalibrationControl.hasZ(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mVpMainPic.setLooping(false);
        mVpMainPic.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVpMainPic.setLooping(true);
        mVpMainPic.start();
    }

    @Override
    protected void goNext() {
        mDecisionDialog.show();
    }

    private void setOriginAndGo() {
        mViewModel.setXYOrigin()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success ->
                {
                    mDecisionDialog.dismiss();
                    ((A400LaserManualFocusCalibrationActivity) requireActivity()).goToEngravingWork();
                }, LogHelper::log);
    }
}
