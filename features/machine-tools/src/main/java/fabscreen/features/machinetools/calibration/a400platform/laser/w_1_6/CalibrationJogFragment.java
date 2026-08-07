package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.BaseCalibrationProgressFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400XYZBControlPanel;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;

public abstract class CalibrationJogFragment extends BaseCalibrationProgressFragment {
    @BindView(R2.id.iv_calibration_desc)
    ImageView mIvCalibrationDesc;
    @BindView(R2.id.tv_calibration_desc_title)
    TextView mTvCalibrationDescTitle;
    @BindView(R2.id.tv_calibration_desc_content)
    TextView mTvCalibrationDescContent;
    @BindView(R2.id.cp_a400_calibration_move)
    A400XYZBControlPanel mXYZBCalibrationControl;
    @BindView(R2.id.btn_run_boundary)
    TextView mBtnRunBoundary;
    @BindView(R2.id.iv_help)
    ImageView mIvHelp;
    @BindView(R2.id.btn_next)
    protected Button mBtnNext;
    @BindView(R2.id.cv_main_pic)
    CardView mCvMainPic;
    @BindView(R2.id.vp_main_pic)
    VideoPlayerIJK mVpMainPic;
    public FileParsingDialog fabLoading;


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    protected void initView() {
        fabLoading = FileParsingDialog.create(getActivity())
                .setContent(R.string.all_move_show);
        mIvHelp.setVisibility(View.GONE);
        mXYZBCalibrationControl.setRotaryStuffVisibility(mViewModel.isRotaryAvailable());
        mXYZBCalibrationControl.setOnDirectionClickListener(new A400XYZBControlPanel.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                mViewModel.moveByStep(direction);
            }

            @Override
            public void onPositionChange(int position) {
                mViewModel.changeStepWidth(position);
            }

            @Override
            public void changPanel(int position) {

            }
        });

    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_manual_focus_calibration;
    }

    @OnClick(R2.id.btn_next)
    void onNextClicked() {
        playNormalClickSound();
        goNext();
    }

    @Optional
    @OnClick(R2.id.btn_run_boundary)
    void onRunBoundaryClicked() {
        playNormalClickSound();
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.pic_toolhead_run_boundary)
                .setTitle(R.string.toolhead_run_boundary)
                .setContent(R.string.toolhead_run_boundary_message)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setSecondTv(R.string.all_confirm, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.runBoundary();
                }).show();


    }

    protected abstract void goNext();
}
