package fabscreen.features.machinetools.calibration.a400platform.cnc.toolchange;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.features.machinetools.calibration.a400platform.cnc.manualTool.A400CncManualToolViewModel;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400DirectionControlPanel;
import fabscreen.platform.core.ui.view.A400XYZBControlPanel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400CncToolChange2Fragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.fragment_calibration_image)
    ImageView mIvImage;
    @BindView(R2.id.fragment_calibration_title)
    TextView mTvTitle;
    @BindView(R2.id.fragment_calibration_content)
    TextView mTvContent;
    @BindView(R2.id.cp_a400_calibration_move)
    A400XYZBControlPanel mCpMove;
    @BindView(R2.id.cp_a400_calibration_move_z)
    A400DirectionControlPanel mCpZMove;
    private DecisionDialog mDecisionDialog;

    private A400CncManualToolViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400CncToolChange2Fragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
    }

    private void initView() {
        setTitle(R.string.a400_calibration_cnc_tool_change);
        mTvTopBarContent.setText(R.string.calibration_cnc_tool_change_third_steps);
        mTvTitle.setText(R.string.calibration_cnc_tool_change_first_steps_content_title);
        mTvContent.setText(R.string.calibration_cnc_tool_change_third_steps_content);
        mGuideProgressBar.setMax(3);
        mGuideProgressBar.setProgress(3);
        mGuideProgressBar.setVisibility(View.VISIBLE);
        mCpMove.setVisibility(View.GONE);
        mCpZMove.setVisibility(View.VISIBLE);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(mViewModel.isFourAxis() ? R.drawable.pic_cnc_four_axis_bit_assistant_touch_material : R.drawable.pic_cnc_bit_assistant_touch_material)
                .apply(options)
                .into(mIvImage);
        mCpZMove.setStepWidths(0.1f, 1f, 10f, 100f);
        mCpZMove.setOnDirectionClickListener(new A400DirectionControlPanel.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                playNormalClickSound();
                mViewModel.move(direction, stepWidth).observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(responseStructure -> {
                            if (responseStructure.isGeneralError()) {
                                mDecisionDialog.setContent(getString(R.string.debug_machine_move_restricted))
                                        .setFirstTv(requireContext().getString(R.string.all_confirm),
                                                R.color.select_dialog_blue_txt, (dialog, which) -> {
                                                    dialog.dismiss();
                                                }).show();
                            }
                        }, LogHelper::log);

            }

            @Override
            public void onPositionChange(int position) {
                mViewModel.changeStepWidth(position);
            }

            @Override
            public void changPanel(int position) {

            }
        });
        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mCpMove.setEnabled(!isMoving);
                });

        mViewModel.getIsMachineMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    if (isMoving) {
                        fabLoading.show();
                    } else {
                        fabLoading.dismiss();
                    }
                });
    }

    @OnClick(R2.id.bt_a400_calibration_submit)
    public void onClickSubmit() {
        playNormalClickSound();
        mViewModel.applyZOffset()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success.isSuccess()) {
                        ((A400CncToolChangeAssistantActivity) requireActivity()).gotoComplete();
                    }
                }, LogHelper::log);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_cnc_manual_tool_basic;
    }

    @Override
    protected A400CncManualToolViewModel getViewModel() {
        return getViewModelProvider().get(A400CncManualToolViewModel.class);
    }
}
