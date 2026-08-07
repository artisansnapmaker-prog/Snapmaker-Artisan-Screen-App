package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.BaseCalibrationProgressFragment;
import fabscreen.platform.core.ui.view.RulerView;
import fabscreen.platform.core.ui.view.StepIntroductionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CalibrationChooseLineFragment extends BaseCalibrationProgressFragment {
    public static Fragment newInstance() {
        return new CalibrationChooseLineFragment();
    }

    @BindView(R2.id.rv_line_chooser)
    RulerView mRvLineChooser;
    @BindView(R2.id.iv_help)
    ImageView mIvHelp;
    StepIntroductionDialog mStepIntroductionDialog;

    private A400LaserCalibrationViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
    }

    private void initView() {
        setMainTitle(getString(R.string.calibration_manual_focus_calibration_title));
        setSubTitle(getString(R.string.calibration_manual_focus_calibration_four_subtitle));
        setProgress(4, 4);
        mIvHelp.setVisibility(View.VISIBLE);
        mStepIntroductionDialog = StepIntroductionDialog.create(requireContext())
                .setTitle(getViewModel().isRotaryAvailable() ? R.string.a400_laser_four_axis_calibrate_four_axis_title :
                        R.string.a400_laser_four_axis_calibrate_title)
                .setContent(R.string.a400_laser_four_axis_calibrate_content)
                .setImage(R.drawable.pic_laser_manual_focus_calibration_0_5mm_pitch_best_engraved_line)
                .setOnClickBack(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mStepIntroductionDialog.dismiss();
                    }
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_manual_focus_calibration_choose_line;
    }

    @Override
    protected A400LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(A400LaserCalibrationViewModel.class);
    }

    @OnClick(R2.id.iv_help)
    void onHelpClicked() {
        playNormalClickSound();
        mStepIntroductionDialog.show();
    }

    @OnClick(R2.id.btn_save)
    void onSaveClicked() {
        playNormalClickSound();
        mViewModel.saveFocalLenAndQuit(mRvLineChooser.getCurrentValue())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    finishActivityWithResultOk();
                }, LogHelper::log);
    }

    @OnClick(R2.id.btn_quit)
    void onQuitClicked() {
        playNormalClickSound();
        onCloseClicked();
    }
}
