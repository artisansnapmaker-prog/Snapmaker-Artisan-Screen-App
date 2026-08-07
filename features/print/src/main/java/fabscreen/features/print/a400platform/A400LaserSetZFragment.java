package fabscreen.features.print.a400platform;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.features.print.a400platform.viewmodel.PrintReadyViewModel;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400LaserSetZFragment extends BaseFragment {
    public static Fragment newInstance() {
        return new A400LaserSetZFragment();
    }

    @BindView(R2.id.rl_calibration_check_mode)
    RelativeLayout mRlCheckMode;
    @BindView(R2.id.tv_calibration_check_mode)
    TextView mTvModeName;
    @BindView(R2.id.tv_touch_desc)
    TextView mTvTouchDesc;
    @BindView(R2.id.btn_start_work)
    Button mBtnStartWork;
    @BindView(R2.id.top_bar_back)
    Button mBack;
    @BindView(R2.id.top_bar_title)
    TextView mTvTopBarTitle;
    @BindView(R2.id.top_bar_content)
    TextView mTvTopBarContent;
    @BindView(R2.id.top_bar_ico)
    ImageView mIvTopBarIco;
    @BindView(R2.id.view_guide_progress_bar)
    LinearProgressIndicator mProgress;

    private WarmTipDialog mMovingDialog;
    Fragment mDescFragment;
    private int mCurrentMode = -1;

    private PrintReadyViewModel mViewModel;
    private boolean mHasShowFocusLeverTips = true;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel initialization.
        mViewModel = getViewModel();

        mViewModel.checkMoveLaserReadyPosition()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    if (aBoolean && mViewModel.getWorkType() == IMachine.WorkType.LASER) {
                        // Laser indicator(using laser output directly) will power off
                        // when homing due to inline mode implement.
                        // We need to recheck the mode and reopen laser indicator if needed.
                        doubleCheckLaserIndicator();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        mRlCheckMode.setEnabled(true);
        mViewModel.updateMode();
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mViewModel.onStop();
    }

    @Override
    protected PrintReadyViewModel getViewModel() {
        return getViewModelProvider().get(PrintReadyViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_laser_last_prepare_step;
    }

    private void initView() {
        final int prepareMode = mViewModel.getPrepareMode();
        if (mCurrentMode == prepareMode) {
            // Refresh view is no need. return instead.
            return;
        }

        // Initialize title and content first.
        mTvTopBarTitle.setText(R.string.a400_print_laser_job_preparation_title);
        mTvTopBarContent.setText(getString(R.string.a400_print_adjust_laser_height_title));
        mIvTopBarIco.setVisibility(View.GONE);
        mProgress.setMax(2);
        mProgress.setProgress(1);

        mCurrentMode = prepareMode;
        mMovingDialog = WarmTipDialog.create(requireContext())
                .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                .setProgressVisible(true)
                .setTitle(R.string.all_move_show)
                .setContent(R.string.all_move_show_content);

        // Build Fragment with selected mode.
        if (mViewModel.getIsRotaryAvailable()) {
            mDescFragment = buildFragmentPageWith4AxisPrepareMode(prepareMode);
        } else {
            mDescFragment = buildFragmentPageWith3AxisPrepareMode(prepareMode);
        }

        mViewModel.getMaterialThicknessObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(materialThickness -> {
                    if (mViewModel.isPrepareModeNeedMaterialHeight()) {
                        // Material thickness must be positive value.
                        mBtnStartWork.setEnabled(materialThickness >= 0);
                    } else {
                        mBtnStartWork.setEnabled(true);
                    }
                });

        // Replace fragment after prepare mode changed.
        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.push_alpha_in, R.anim.push_alpha_out)
                .replace(R.id.fcv_prepare_mode, mDescFragment)
                .commit();

        mViewModel.getMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        mMovingDialog.show();
                    } else {
                        mMovingDialog.dismiss();
                    }

                    mRlCheckMode.setEnabled(!aBoolean);
                    mBack.setEnabled(!aBoolean);

                    if (!mViewModel.isPrepareModeNeedMaterialHeight()) {
                        mBtnStartWork.setEnabled(!aBoolean);
                    }
                });
    }

    private void doubleCheckLaserIndicator() {
        if (mViewModel.getIsRotaryAvailable()) {
            if (mCurrentMode == 2 && mViewModel.isOriginIndicatorActive()) {
                mViewModel.setOriginIndicatorState(true);
            }
        } else {
            if (mCurrentMode == 3 && mViewModel.isOriginIndicatorActive()) {
                // re-active the indicator
                mViewModel.setOriginIndicatorState(true);
            }
        }
    }

    private Fragment buildFragmentPageWith3AxisPrepareMode(int prepareMode) {
        Fragment fragment = A400LaserAutoThickMeasureFragment.newInstance();
        int headType = mViewModel.getHeadType();
        switch (headType) {
            case Module.ModuleType.HEAD_LASER:
                Logger.w("1600mw Laser was not supported, skip silently.");
                break;
            case Module.ModuleType.HEAD_LASER_10W:
                fragment = init3AxisLaser10wSetZPage(prepareMode);

                mViewModel.setFocusAssistLight(prepareMode == 0)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(responseStructure -> {

                        }, LogHelper::log);
                break;
            case Module.ModuleType.HEAD_LASER_20W:
                fragment = init3AxisLaser20wSetZPage(prepareMode);
                break;
            case Module.ModuleType.HEAD_LASER_40W:
                fragment = init3AxisLaser40wSetZPage(prepareMode);
                break;
            case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                fragment = init3AxisLaser2wSetZPage(prepareMode);
                break;
            default:
                Logger.w("Not laser module found, skip silently.");
                break;
        }
        return fragment;
    }

    private Fragment buildFragmentPageWith4AxisPrepareMode(int prepareMode) {
        Fragment fragment = ThicknessCheckFragment.newInstance();
        int headType = mViewModel.getHeadType();
        switch (headType) {
            case Module.ModuleType.HEAD_LASER:
                Logger.w("1600mw Laser was not supported, skip silently.");
                break;
            case Module.ModuleType.HEAD_LASER_10W:
                fragment = init4AxisLaser10wSetZPage(prepareMode);
                break;
            case Module.ModuleType.HEAD_LASER_20W:
                fragment = init4AxisLaser20wSetZPage(prepareMode);
                break;
            case Module.ModuleType.HEAD_LASER_40W:
                fragment = init4AxisLaser40wSetZPage(prepareMode);
                break;
            case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                fragment = init4AxisLaser2wSetZPage(prepareMode);
                break;
            default:
                Logger.w("Not laser module found, skip silently.");
                break;
        }
        return fragment;
    }

    // Not Implemented.
    private Fragment init3AxisLaser1600mwSetZPage(int mode) {
        return null;
    }

    private Fragment init3AxisLaser10wSetZPage(int mode) {
        Fragment fragment = A400LaserAutoThickMeasureFragment.newInstance();
        switch (mode) {
            case 0:
                // Auto thickness measurement
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_automatic_thickness_measurement_title);
                fragment = A400LaserAutoThickMeasureFragment.newInstance();
                break;
            case 1:
                // Input material thickness
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_input_thickness_sub_title);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
//                    mTvTouchDesc.setVisibility(View.VISIBLE);
                break;
            case 2:
                // Touch material
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_trouch_material_surface_title);
                Bundle workBundle = new Bundle();
                workBundle.putInt("pic", R.drawable.pic_a400_laser_10w_touch_material);
                workBundle.putString("title", getString(R.string.a400_print_laser_calibration_plate_assisted_subtitle));
                workBundle.putInt("desc", R.string.a400_print_laser_calibration_plate_assisted_message_desc);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                break;
            case 3:
                // Manual focus
                // open laser
                mViewModel.setOriginIndicatorState(true);
                mTvModeName.setText(R.string.a400_print_laser_manual_focus_title);
                Bundle bundle = new Bundle();
                bundle.putInt("pic", R.drawable.pic_a400_laser_10w_manual_focus);
                bundle.putString("title", getString(R.string.a400_print_laser_manual_focus_subtitle));
                bundle.putInt("desc", R.string.a400_print_laser_manual_focus_content);
                fragment = WorkPrepareWithJogFragment.newInstance(bundle);
                break;
            default:
                break;
        }
        return fragment;
    }

    private Fragment init3AxisLaser20wSetZPage(int mode) {
        Logger.d("init 3Axis Laser 20W Page");
        Fragment fragment = A400LaserAutoThickMeasureFragment.newInstance();
        switch (mode) {
            case 0:
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_input_thickness_sub_title);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
                break;
            case 1:
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_focus_lever_assisted_subtitle);
                Bundle workBundle = new Bundle();
                workBundle.putInt("pic", R.drawable.pic_a400_laser_20w_touch_material);
                workBundle.putString("title", getString(R.string.a400_print_laser_focus_lever_assisted_subtitle));
                workBundle.putInt("desc", R.string.a400_print_laser_focus_lever_assisted_message_desc);
                workBundle.putBoolean("tip", mHasShowFocusLeverTips);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                mHasShowFocusLeverTips = false;
                break;
            default:
                break;
        }
        return fragment;
    }

    private Fragment init3AxisLaser40wSetZPage(int mode) {
        Fragment fragment = A400LaserAutoThickMeasureFragment.newInstance();
        switch (mode) {
            case 0:
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_input_thickness_sub_title);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
                break;
            case 1:
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_focus_lever_assisted_subtitle);
                Bundle workBundle = new Bundle();
                workBundle.putInt("pic", R.drawable.pic_a400_laser_20w_touch_material);
                workBundle.putString("title", getString(R.string.a400_print_laser_focus_lever_assisted_subtitle));
                workBundle.putInt("desc", R.string.a400_print_laser_focus_lever_assisted_message_desc);
                workBundle.putBoolean("tip", mHasShowFocusLeverTips);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                mHasShowFocusLeverTips = false;
                break;
            default:
                break;
        }
        return fragment;
    }

    private Fragment init3AxisLaser2wSetZPage(int mode) {
        Fragment fragment = A400LaserAutoThickMeasureFragment.newInstance();
        switch (mode) {
            case 0:
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_input_thickness_sub_title);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
                break;
            case 1:
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_focus_lever_assisted_subtitle);
                Bundle workBundle = new Bundle();
                workBundle.putInt("pic", R.drawable.pic_a400_laser_2w_touch_material);
                workBundle.putString("title", getString(R.string.a400_print_laser_focus_lever_assisted_subtitle));
                workBundle.putInt("desc", R.string.a400_print_laser_focus_lever_assisted_message_desc);
                workBundle.putBoolean("tip", mHasShowFocusLeverTips);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                mHasShowFocusLeverTips = false;
                break;
            default:
                break;
        }
        return fragment;
    }

    // Not implemented.
    private Fragment init4AxisLaser1600mwSetZPage(int mode) {
        return null;
    }

    private Fragment init4AxisLaser10wSetZPage(int mode) {
        Fragment fragment = ThicknessCheckFragment.newInstance();
        switch (mode) {
            case 0:
                // Input diameter
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_input_diameter_subtitle);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
                break;
            case 1:
                // Touch material
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_touch_material_subtitle);
                Bundle workBundle = new Bundle();
                workBundle.putString("title", getString(R.string.a400_print_laser_touch_material_surface_title));
                workBundle.putInt("desc", R.string.a400_print_laser_touch_material_surface_content);
                workBundle.putInt("pic", R.drawable.pic_a400_laser_10w_four_axis_touch_material);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                break;
            case 2:
                // Manual focus
                // open laser
                mViewModel.setOriginIndicatorState(true);
                mTvModeName.setText(R.string.a400_print_laser_4axis_manual_focus_title);
                Bundle bundle = new Bundle();
                bundle.putInt("pic", R.drawable.pic_a400_laser_10w_four_axis_manual_focus);
                bundle.putString("title", getString(R.string.a400_print_laser_manually_adjust_laser_height_title));
                bundle.putInt("desc", R.string.a400_print_laser_manually_adjust_laser_height_content);
                fragment = WorkPrepareWithJogFragment.newInstance(bundle);
                break;
            default:
                break;
        }
        return fragment;
    }

    private Fragment init4AxisLaser20wSetZPage(int mode) {
        Fragment fragment = ThicknessCheckFragment.newInstance();
        switch (mode) {
            case 0:
                // Input diameter
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_input_diameter_subtitle);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
                break;
            case 1:
                // Touch material
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_touch_material_subtitle);
                Bundle workBundle = new Bundle();
                workBundle.putString("title", getString(R.string.a400_print_laser_touch_material_surface_title));
                workBundle.putInt("desc", R.string.a400_print_laser_40w_touch_material_surface_content);
                workBundle.putInt("pic", R.drawable.pic_a400_laser_20w_four_axis_touch_material);
                workBundle.putBoolean("tip", mHasShowFocusLeverTips);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                mHasShowFocusLeverTips = false;
                break;
            default:
                break;
        }
        return fragment;
    }

    private Fragment init4AxisLaser40wSetZPage(int mode) {
        Fragment fragment = ThicknessCheckFragment.newInstance();
        switch (mode) {
            case 0:
                // Input diameter
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_input_diameter_subtitle);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
                break;
            case 1:
                // Touch material
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_touch_material_subtitle);
                Bundle workBundle = new Bundle();
                workBundle.putString("title", getString(R.string.a400_print_laser_touch_material_surface_title));
                workBundle.putInt("desc", R.string.a400_print_laser_40w_touch_material_surface_content);
                workBundle.putInt("pic", R.drawable.pic_a400_laser_20w_four_axis_touch_material);
                workBundle.putBoolean("tip", mHasShowFocusLeverTips);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                mHasShowFocusLeverTips = false;
                break;
            default:
                break;
        }
        return fragment;
    }

    private Fragment init4AxisLaser2wSetZPage(int mode) {
        Fragment fragment = ThicknessCheckFragment.newInstance();
        switch (mode) {
            case 0:
                // Input diameter
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_input_diameter_subtitle);
                fragment = ThicknessCheckFragment.newInstance();
                mBtnStartWork.setEnabled(mViewModel.getMaterialThicknessValue() >= 0);
                break;
            case 1:
                // Touch material
                mViewModel.setOriginIndicatorState(false);
                mTvModeName.setText(R.string.a400_print_laser_4axis_touch_material_subtitle);
                Bundle workBundle = new Bundle();
                workBundle.putString("title", getString(R.string.a400_print_laser_touch_material_surface_title));
                workBundle.putInt("desc", R.string.a400_print_laser_2w_touch_material_surface_content);
                workBundle.putInt("pic", R.drawable.pic_a400_laser_2w_four_axis_touch_material);
                workBundle.putBoolean("tip", mHasShowFocusLeverTips);
                fragment = WorkPrepareWithJogFragment.newInstance(workBundle);
                mHasShowFocusLeverTips = false;
                break;
            default:
                break;
        }
        return fragment;
    }

    private void moveToZ() {
        mViewModel.moveToZ()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    if (mViewModel.getHeadType() == Module.ModuleType.HEAD_LASER_10W || mViewModel.getHeadType() == Module.ModuleType.HEAD_LASER) {
                        mViewModel.setExposeTime(0);
                    }
                    if (aBoolean) {
                        // Go to set origin page after ensuring work origin Z.
                        ((PrintA400Activity) requireActivity()).goToSetOrigin();
                    }
                }, LogHelper::log);
    }

    @Override
    protected void back() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.WARMING_TYPE)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(getString(R.string.a400_quit_job_preparation))
                .setContent(getString(R.string.a400_print_dialog_action_stop_title, getString(R.string.a400_quit_job_preparation).toLowerCase()))
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setSecondTv(R.string.all_quit, R.color.select_dialog_yellow_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (mCurrentMode == 0 && getViewModel().getHeadType() == Module.ModuleType.HEAD_LASER_10W) {
                            mViewModel.setFocusAssistLight(false)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .as(bindToLifecycle())
                                    .subscribe(responseStructure -> {
                                    }, LogHelper::log);
                            mViewModel.setExposeTime(0);
                        }
                        mViewModel.setOriginIndicatorState(false);
                        if (getActivity() != null) {
                            Logger.d("Route: Back from " + getClass().getSimpleName());
                            getActivity().onBackPressed();
                        }
                    }
                })
                .show();
    }

    @OnClick(R2.id.rl_calibration_check_mode)
    void onClickedMode() {
        mRlCheckMode.setEnabled(false);
        playNormalClickSound();
        ServiceContainer.getInstance().getService(IRouter.class)
                .routeWithClassPath(RoutePath.PRINT_LASER_SET_Z_SELECT)
                .start(getContext());
    }

    @OnClick(R2.id.btn_start_work)
    void onStartWorkClicked() {
        playNormalClickSound();
        DecisionDialog.create(getContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.TIP_TYPE)
                .setPic(R.drawable.ic_laser_turn_on_224x224)
                .setTitle(R.string.a400_print_laser_open_reminder_titie)
                .setContent(R.string.a400_print_laser_open_reminder_desc)
                .setCanceledOnTouchOutSide(true)
                .setFirstTv(getResources().getString(R.string.all_cancel), R.color.select_dialog_left_text_color, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setSecondTv(getResources().getString(R.string.all_confirm), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    // move Z and next step(start work?).
                    moveToZ();
                })
                .show();
    }
}
