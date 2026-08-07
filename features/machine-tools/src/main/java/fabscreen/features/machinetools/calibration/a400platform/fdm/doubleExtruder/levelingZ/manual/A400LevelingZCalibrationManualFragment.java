package fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingZ.manual;

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
import fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingZ.A400LevelingZViewModel;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400DirectionControlPanelTemp;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400LevelingZCalibrationManualFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.fragment_calibration_image)
    ImageView mIvImage;
    @BindView(R2.id.fragment_calibration_z_content)
    TextView mTvContent;
    @BindView(R2.id.tv_calibration_title)
    TextView mTvContentTitle;

    @BindView(R2.id.cp_a400_leveling_z_calibration_move)
    A400DirectionControlPanelTemp mCpMove;
    private DecisionDialog mDecisionDialog;

    private A400LevelingZViewModel mViewModel;

    private int extruderIndex = -1;

    public static Fragment newInstance() {
        return new A400LevelingZCalibrationManualFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        watchMovingState();
        mViewModel.checkHome()
                .flatMap(aBoolean -> mViewModel.setCalibrationMode(53))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        mViewModel.A400LevelingZCalibration(0);
                    } else {
                        requireActivity().finish();
                    }
                }, LogHelper::log);
    }

    private void initView() {
        mDecisionDialog = DecisionDialog.create(requireContext()).setDialogStatus(DecisionDialog.BTN_ONE,
                false, false, false, true);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        setTitle(R.string.guide_a400_double_extruder_step_1_2_title);
        mTvTopBarContent.setText(R.string.calibration_a400_double_extruder_z_offset_step_one);
        mGuideProgressBar.setMax(2);
        mTvContentTitle.setText(R.string.guide_a400_double_extruder_step_1_for_1_3_content_title);
        mTvContent.setText(getString(R.string.a400_leveling_z_manual_content, getString(R.string.a400_left).toLowerCase()));
        mGuideProgressBar.setVisibility(View.VISIBLE);
        Glide.with(requireContext())
                .load(R.drawable.pic_z_offset_calibration_double_extruder_manual_mode_calibration_left_nozzle)
                .apply(options)
                .into(mIvImage);
        mViewModel.getIsMovePopUpObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMove -> {
                    if (isMove) {
                        fabLoading.show();
                    } else {
                        fabLoading.dismiss();
                    }

                    if (extruderIndex == mViewModel.getExtruderIndex()) {
                        return;
                    }
                    extruderIndex = mViewModel.getExtruderIndex();
                    switch (extruderIndex) {
                        case 0:
                            mTvTopBarContent.setText(R.string.calibration_a400_double_extruder_z_offset_step_one);
                            Glide.with(requireContext())
                                    .load(R.drawable.pic_z_offset_calibration_double_extruder_manual_mode_calibration_left_nozzle)
                                    .apply(options)
                                    .into(mIvImage);
                            mTvContent.setText(getString(R.string.a400_leveling_z_manual_content, getString(R.string.a400_left).toLowerCase()));
                            mTvContentTitle.setText(R.string.guide_a400_double_extruder_step_1_for_1_3_content_title);
                            mGuideProgressBar.setProgress(1);
                            mGuideProgressBar.invalidate();
                            break;
                        case 1:
                            Glide.with(requireContext())
                                    .load(R.drawable.pic_z_offset_calibration_double_extruder_manual_mode_calibration_right_nozzle)
                                    .apply(options)
                                    .into(mIvImage);
                            mTvContent.setText(getString(R.string.a400_leveling_z_manual_content, getString(R.string.a400_right).toLowerCase()));
                            mTvTopBarContent.setText(R.string.calibration_a400_double_extruder_z_offset_step_two);
                            mTvContentTitle.setText(R.string.guide_a400_double_extruder_step_1_for_2_3_content_title);
                            mGuideProgressBar.setProgress(2);
                            mGuideProgressBar.invalidate();
                            break;
                        default:
                            break;
                    }

                });
        mCpMove.setStepWidths(0.02f, 0.1f, 1f, 5).setOnDirectionClickListener(new A400DirectionControlPanelTemp.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                mViewModel.move(direction, stepWidth)
                        .observeOn(AndroidSchedulers.mainThread())
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
        });

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    mCpMove.setEnabled(!aBoolean);
                });
    }

    private void watchMovingState() {
        mViewModel.getMoveStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshByMovingState, LogHelper::log);
    }

    private void refreshByMovingState(MoveController.Direction direction) {
        mCpMove.refreshMoveState(direction);
    }

    @OnClick(R2.id.bt_a400_leveling_z_calibration_submit)
    public void onClickSubmit() {
        playNormalClickSound();
        extruderIndex = mViewModel.getExtruderIndex();
        switch (extruderIndex) {
            case 0:
                DecisionDialog.create(getContext())
                        .setTitle(R.string.a400_leveling_z_calibration_manual_save_l)
                        .setContent(R.string.a400_leveling_z_calibration_manual_save_l_content)
                        .setType(DecisionDialog.TIP_TYPE)
                        .setDialogStatus(DecisionDialog.BTN_TWO, false, false, true, true)
                        .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, ((dialog, which) -> {
                            dialog.dismiss();
                        }))
                        .setSecondTv(R.string.all_save, R.color.select_dialog_blue_txt, ((dialog, which) -> {
                            dialog.dismiss();
                            mViewModel.A400LevelingZCalibration(mViewModel.getExtruderIndex() + 1);
                        })).show();
                break;
            case 1:
                DecisionDialog.create(getContext())
                        .setTitle(R.string.a400_leveling_z_calibration_manual_save_r)
                        .setContent(R.string.a400_leveling_z_calibration_manual_save_r_content)
                        .setType(DecisionDialog.TIP_TYPE)
                        .setDialogStatus(DecisionDialog.BTN_TWO, false, false, true, true)
                        .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, ((dialog, which) -> {
                            dialog.dismiss();
                        }))
                        .setSecondTv(R.string.all_save, R.color.select_dialog_blue_txt, ((dialog, which) -> {
                            dialog.dismiss();
                            ((A400LevelingZCalibrationManualActivity) requireActivity()).gotoLevelingZCalibrationComplete();
                        })).show();
                break;
            default:
                break;
        }
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_leveling_z_calibration_2;
    }

    @Override
    protected A400LevelingZViewModel getViewModel() {
        return getViewModelProvider().get(A400LevelingZViewModel.class);
    }
}
