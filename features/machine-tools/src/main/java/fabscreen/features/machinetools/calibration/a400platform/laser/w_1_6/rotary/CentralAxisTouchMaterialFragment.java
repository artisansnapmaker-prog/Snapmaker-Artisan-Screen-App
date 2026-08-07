package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6.rotary;

import android.app.Activity;
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
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.legacy.connection.MockConst;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400XYZBControlPanel;
import fabscreen.platform.core.ui.view.StepIntroductionDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CentralAxisTouchMaterialFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.iv_calibration_desc)
    ImageView mIvPic;
    @BindView(R2.id.cp_a400_leveling_z_calibration_move)
    A400XYZBControlPanel mXYZBCalibrationControl;
    @BindView(R2.id.tv_calibration_desc_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_calibration_desc_content)
    TextView mTvContent;
    @BindView(R2.id.btn_next)
    Button mBtnNext;
    CalibrationCentralAxisViewModel mViewModel;
    private DecisionDialog mDecisionDialog;
    private IPreferences.Helper helper;
    private long mMachineSN;

    private StepIntroductionDialog mFocusLeverHelperDialog;
    private boolean mNeedShowTip = false;

    public static Fragment newInstance() {
        return new CentralAxisTouchMaterialFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(CalibrationCentralAxisViewModel.class);
        helper = getServiceContainer().getService(IPreferences.class).getHelper();
        initView();

        watchMovingState();

        mViewModel.checkHome()
                .flatMap(aBoolean -> aBoolean ? mViewModel.goToRotaryTouchInitPosition() : Observable.just(aBoolean))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(order -> {
                }, LogHelper::log);
    }

    private void initView() {
        setTitle(R.string.a400_central_axis_calibration);
        mTvTopBarContent.setText(R.string.a400_touch_material_2_2);
        mBtnNext.setText(R.string.all_save);
        mGuideProgressBar.setMax(2);
        mGuideProgressBar.setProgress(2);
        mMachineSN = helper.getA400MachineSn();

        mDecisionDialog = DecisionDialog.create(requireContext()).setDialogStatus(DecisionDialog.BTN_ONE,
                false, false, false, true);

        // Load gif resource and contents
        int laserPicResId = -1;
        switch (mViewModel.getMachineToolHead()) {
            case Module.ModuleType.HEAD_LASER:
                laserPicResId = R.drawable.pic_laser_1_6w_central_axis_calibration_cylinder_material_ouch_material_479x359;
                break;
            case Module.ModuleType.HEAD_LASER_10W:
                laserPicResId = R.drawable.pic_laser_10w_central_axis_calibration_cylinder_material_ouch_material_479x359;
                mTvTitle.setText(R.string.a400_calibration_axis_touch_material_title);
                if (helper.getA400MachineStep(mMachineSN) == 0) {
                    mTvContent.setText(R.string.a400_four_axis_calibration_axis_touch_material_content);
                } else {
                    mTvContent.setText(R.string.a400_calibration_axis_touch_material_content);
                }
                break;
            case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                mNeedShowTip = true;
                laserPicResId = R.drawable.pic_laser_2w_central_axis_calibration_cylinder_material_ouch_material_479x359;
                mTvTitle.setText(R.string.a400_laser_2w_calibration_axis_touch_material_title);
                mTvContent.setText(R.string.a400_laser_2w_calibration_axis_touch_material_content);
                break;
            case Module.ModuleType.HEAD_LASER_20W:
            case Module.ModuleType.HEAD_LASER_40W:
                mNeedShowTip = true;
                laserPicResId = R.drawable.pic_laser_20w_central_axis_calibration_cylinder_material_ouch_material_479x359;
                mTvTitle.setText(R.string.a400_laser_40w_calibration_axis_touch_material_title);
                mTvContent.setText(R.string.a400_laser_40w_calibration_axis_touch_material_content);
                break;
            default:
                break;
        }
        if (laserPicResId != -1) {
            RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
            Glide.with(requireContext())
                    .load(laserPicResId)
                    .apply(options)
                    .into(mIvPic);
        }

        mXYZBCalibrationControl.setRotaryStuffVisibility(true);
        mXYZBCalibrationControl.setOnDirectionClickListener(new A400XYZBControlPanel.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                playNormalClickSound();
                mViewModel.moveToPosition(direction)
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

            @Override
            public void onPositionChange(int position) {
                mViewModel.changeStepWidth(position);
            }

            @Override
            public void changPanel(int position) {

            }
        });

        mViewModel.getMovingSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    mXYZBCalibrationControl.setEnabled(!aBoolean);
                }, LogHelper::log);

        mViewModel.getIsHomeMovingSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshWhenHoming, LogHelper::log);

        String videoResPath = "/laser_20w_how_to_use_focus_lever.webm";
        int leverDescResId = R.string.a400_calibration_laser_40w_how_to_use_focus_lever_helper_desc;

        if (ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder()
                .getValue().workType == IMachine.WorkType.LASER) {
            switch (mViewModel.getMachineToolHead()) {
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    videoResPath = "/laser_20w_how_to_use_focus_lever.webm";
                    leverDescResId = R.string.a400_calibration_laser_40w_how_to_use_focus_lever_helper_desc;
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    videoResPath = "/laser_2w_how_to_use_focus_lever.webm";
                    leverDescResId = R.string.a400_calibration_laser_2w_how_to_use_focus_lever_helper_desc;
                    break;
                default:
                    break;
            }
        }

        mFocusLeverHelperDialog = StepIntroductionDialog.create(requireContext())
                .setTitle(R.string.a400_calibration_laser_40w_how_to_use_focus_lever_helper_title)
                .setContent(leverDescResId)
                .setVideo(videoResPath)
                .setOnClickBack(v -> mFocusLeverHelperDialog.dismiss());
        mFocusLeverHelperDialog.setCanceledOnTouchOutSide(false);
        if (mNeedShowTip) {
            mFocusLeverHelperDialog.show();
            mNeedShowTip = false;
        }
    }

    private void refreshWhenHoming(Boolean isHoming) {
        if (isHoming) {
//            if (!fabLoading.isShowing()) {
                fabLoading.show();
//            }
        } else {
            fabLoading.dismiss();
        }
    }

    private void watchMovingState() {
        mViewModel.getMoveStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshByMovingState, LogHelper::log);
    }

    private void refreshByMovingState(MoveController.Direction direction) {
        mXYZBCalibrationControl.refreshMoveState(direction);
    }

    @OnClick(R2.id.btn_next)
    void goNext() {
        playNormalClickSound();
        // save axis position
        mViewModel.saveRotaryAxisZ()
                .map(success -> {
                    // 10W laser needs to set the default laser focal length
                    if (ServiceContainer.getInstance().getService(IMachine.class).getLaserController().getHeadType() == Module.ModuleType.HEAD_LASER_10W) {
                        return ServiceContainer.getInstance().getService(IMachine.class).getLaserController().setFocalLength(MockConst.LASER_MATERIAL_MEASURE_CALIBRATION_OBJECT_HEIGHT).map(ResponseStructure::isSuccess);
                    } else {
                        return success;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> finishActivityWithResultOk(), LogHelper::log);
    }

    @Override
    protected void back() {
        fabBackConfirm = DecisionDialog.create(getContext())
                .setTitle(getTitle())
                .setContent(getString(R.string.a400_calibration_assistant_back_notice, getTitle()))
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, true)
                .setPic(R.drawable.pic_a400_warning_112x112)
                .setFirstTv(getContext().getResources().getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setSecondTv(getContext().getResources().getString(R.string.all_stop), R.color.select_dialog_yellow_txt, ((dialog, which) -> {
                    fabBackConfirm.mCancelBtn.setEnabled(false);
                    fabBackConfirm.mSecondBtn.setEnabled(false);
                    ServiceContainer.getInstance().getService(IMachine.class).getLaserController().exitCalibration(false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(success -> {
                                mViewModel.Lift100Z();
                                if (!success.isSuccess()) {
                                    Logger.d("Exit Calibration: " + success);
                                }
                                dialog.dismiss();
                                requireActivity().setResult(Activity.RESULT_CANCELED);
                                requireActivity().finish();
                            }, LogHelper::log);
                }));
        fabBackConfirm.show();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_calibration_axis_touch_material;
    }
}
