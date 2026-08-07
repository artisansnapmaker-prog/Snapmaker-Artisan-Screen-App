package fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingZ.sensor;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;

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

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingZ.A400LevelingZViewModel;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400DirectionControlPanelTemp;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400LevelingZCalibrationSensor2Fragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.fragment_calibration_image)
    ImageView mIvImage;
    @BindView(R2.id.fragment_calibration_z_content)
    TextView mTvContent;
    @BindView(R2.id.cp_a400_leveling_z_calibration_move)
    A400DirectionControlPanelTemp mCpMove;
    @BindView(R2.id.bt_a400_leveling_z_calibration_submit)
    Button mBtnConfirm;
    @BindView(R2.id.tv_calibration_title)
    TextView mTvTitle;

    private IMachine mA400Machine;
    private A400LevelingZViewModel mViewModel;
    private boolean mIsDoubleFdm;
    private DecisionDialog mDecisionDialog;

    public static Fragment newInstance() {
        return new A400LevelingZCalibrationSensor2Fragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        mA400Machine = ServiceContainer.getInstance().getService(IMachine.class);
        mIsDoubleFdm = mA400Machine.getFDMController().getHeadType() == HEAD_3DP_DOUBLE_EXTRUDER;
        initView();
        watchMovingState();
        mViewModel.A400LevelingZSensorCalibration(mViewModel.getExtruderIndex() + 1);
    }

    private void initView() {

        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        setTitle(R.string.calibration_Z_offset_calibration_title);
        mGuideProgressBar.setMax(4);
        mGuideProgressBar.setVisibility(View.VISIBLE);
        Glide.with(requireContext())
                .load(R.drawable.pic_z_offset_calibration_double_extruder_manual_mode_calibration_left_nozzle)
                .apply(options)
                .into(mIvImage);


        mDecisionDialog = DecisionDialog.create(requireContext()).setDialogStatus(DecisionDialog.BTN_ONE,
                false, false, false, true);

        mViewModel.getIsMovePopUpObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMove -> {
                    int extruderIndex = mViewModel.getExtruderIndex();
                    switch (extruderIndex) {
                        case 2:
                            mTvTopBarContent.setText(getString(R.string.a400_calibration_z_leveling_z_offset, getString(R.string.a400_right), 3, 4));
                            mGuideProgressBar.setProgress(3);
                            mBtnConfirm.setText(R.string.all_next);
                            mGuideProgressBar.invalidate();
                            mTvTitle.setText(R.string.a400_calibration_manual_calibration_of_nozzle_r_title);
                            mTvContent.setText(R.string.a400_calibration_manual_calibration_of_nozzle_r_content);
                            Glide.with(requireContext())
                                    .load(mIsDoubleFdm ? R.drawable.pic_z_offset_calibration_double_extruder_manual_mode_calibration_right_nozzle :
                                            R.drawable.pic_z_offset_calibration_single_extruder_manual_mode_calibration_right_nozzle)
                                    .apply(options)
                                    .into(mIvImage);
                            break;
                        case 3:
                            mTvTopBarContent.setText(getString(R.string.a400_calibration_z_leveling_z_offset, getString(R.string.a400_left), 4, 4));
                            mGuideProgressBar.setProgress(4);
                            mGuideProgressBar.invalidate();
                            mBtnConfirm.setText(R.string.all_save);
                            mTvTitle.setText(R.string.a400_calibration_manual_calibration_of_nozzle_l_title);
                            mTvContent.setText(R.string.a400_calibration_manual_calibration_of_nozzle_l_content);
                            Glide.with(requireContext())
                                    .load(mIsDoubleFdm ? R.drawable.pic_z_offset_calibration_double_extruder_manual_mode_calibration_left_nozzle :
                                            R.drawable.pic_z_offset_calibration_single_extruder_manual_mode_calibration_right_nozzle)
                                    .apply(options)
                                    .into(mIvImage);
                            break;

                        default:
                            break;
                    }
                });

        mCpMove.setStepWidths(0.02f, 0.1f, 1f, 5)
                .setOnDirectionClickListener(new A400DirectionControlPanelTemp.OnDirectionClickListener() {
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
                .subscribe(aBoolean -> mCpMove.setEnabled(!aBoolean));

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
        int extruderIndex = mViewModel.getExtruderIndex();
        switch (extruderIndex) {
            case 2:
                DecisionDialog.create(requireContext())
                        .setDialogStatus(DecisionDialog.BTN_TWO, false, false, true, true)
                        .setTitle(R.string.calibration_z_leveling_sensor_submit_nozzle_r_title)
                        .setType(DecisionDialog.TIP_TYPE)
                        .setContent(R.string.calibration_z_leveling_sensor_submit_nozzle_r_content)
                        .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, ((dialog, which) -> {
                            dialog.dismiss();
                        }))
                        .setSecondTv(R.string.all_save, R.color.select_dialog_blue_txt, (dialog, which) -> {
                            dialog.dismiss();
                            mViewModel.A400LevelingZSensorCalibration(mViewModel.getExtruderIndex() + 1);
                        }).show();
                break;
            case 3:
                DecisionDialog.create(requireContext())
                        .setDialogStatus(DecisionDialog.BTN_TWO, false, false, true, true)
                        .setTitle(R.string.calibration_z_leveling_sensor_submit_nozzle_l_title)
                        .setType(DecisionDialog.TIP_TYPE)
                        .setContent(R.string.calibration_z_leveling_sensor_submit_nozzle_l_content)
                        .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, ((dialog, which) -> {
                            dialog.dismiss();
                        }))
                        .setSecondTv(R.string.all_save, R.color.select_dialog_blue_txt, (dialog, which) -> {
                            dialog.dismiss();
                            ((A400LevelingZCalibrationSensorActivity) requireActivity()).gotoLevelingZCalibrationComplete();
                        }).show();

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
