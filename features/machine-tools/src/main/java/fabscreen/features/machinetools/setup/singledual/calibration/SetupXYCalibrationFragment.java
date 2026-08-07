package fabscreen.features.machinetools.setup.singledual.calibration;

import static android.app.Activity.RESULT_CANCELED;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;

public class SetupXYCalibrationFragment extends BaseFragment {

    private SetupXYCaliViewModel mViewModel;

    public static Fragment newInstance() {
        return new SetupXYCalibrationFragment();
    }

    @BindView(R2.id.tv_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_sub_title)
    TextView mTvSubTitle;
    @BindView(R2.id.progress)
    LinearProgressIndicator mProgress;
    @BindView(R2.id.btn_close)
    Button mBtnClose;
    @BindView(R2.id.iv_demonstrate)
    ImageView mIvSetupDemonstrate;
    @BindView(R2.id.tv_demonstrate_desc)
    TextView mTvSetupDesc;
    @BindView(R2.id.btn_start_or_next)
    Button mBtnStartOrNext;

    @BindView(R2.id.view_cali_points)
    View mVCaliPoints;
    @BindView(R2.id.tv_cali_desc)
    TextView mTvCaliDesc;
    @BindView(R2.id.tv_time_estimation)
    TextView mTvTimeEstimation;
    @BindView(R2.id.iv_guide_problem)
    ImageView mIvGuideProblem;

    @BindView(R2.id.btn_setup_return_previous_procedure)
    Button mBtnRewoundProcedure;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(SetupXYCaliViewModel.class);
        initView();
    }

    private void initView() {
        mTvTitle.setText(R.string.guide_a400_double_extruder_step_3_title);
        mTvSetupDesc.setText(R.string.guide_a400_double_extruder_step_3_msg);
        mProgress.setVisibility(View.INVISIBLE);
        mBtnClose.setVisibility(View.INVISIBLE);
        mIvGuideProblem.setVisibility(View.GONE);
        // pic_initialize_dual_extrusion_module_xy_offset_calibration_578x434
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        mIvSetupDemonstrate.setBackground(null);
        Glide.with(requireContext())
                .load(R.drawable.pic_initialize_dual_extrusion_module_xy_offset_calibration_578x434)
                .apply(options)
                .into(mIvSetupDemonstrate);
        mBtnRewoundProcedure.setVisibility(Button.VISIBLE);
        mBtnRewoundProcedure.setText(R.string.guide_a400_fdm_setup_previous_step);
    }

    @OnClick(R2.id.btn_start_or_next)
    void onStartOrNextClicked() {
        playNormalClickSound();
        DecisionDialog.create(getContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.TIP_TYPE)
                .setPic(R.drawable.ic_a400_clean_up_112x112)
                .setTitle(getString(R.string.calibration_a400_procedure_start_confirm_dialog_title,
                        getString(R.string.guide_a400_double_extruder_step_3_title)))
                .setContent(R.string.calibration_a400_procedure_start_confirm_dialog_content_3dp)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setSecondTv(R.string.all_next, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    ((SetupXYCalibrationActivity) requireActivity()).goToXYCalibration();
                })
                .show();

    }

    @OnClick(R2.id.btn_setup_return_previous_procedure)
    void onClickReturnPreviousProcedure() {
        playNormalClickSound();
        onActivityResult(0, RESULT_CANCELED, null);
        requireActivity().finish();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_guide_setup;
    }
}
