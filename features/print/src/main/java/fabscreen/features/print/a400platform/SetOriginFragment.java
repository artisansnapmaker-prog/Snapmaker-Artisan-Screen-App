package fabscreen.features.print.a400platform;

import static fabscreen.platform.base.service.IMachine.WorkType.CNC;
import static fabscreen.platform.base.service.IMachine.WorkType.LASER;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.features.print.a400platform.viewmodel.PrintReadyViewModel;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.core.ui.view.ViewUtils;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SetOriginFragment extends BaseFragment {

    public static Fragment newInstance() {
        return new SetOriginFragment();
    }

    @BindView(R2.id.btn_next)
    Button mBtnNext;
    @BindView(R2.id.fcv_jog_mode)
    FragmentContainerView mFcvJogMode;
    @BindView(R2.id.tv_run_boundary)
    TextView mTvRunBoundary;
    @BindView(R2.id.view_guide_progress_bar)
    LinearProgressIndicator mProgressBar;
    @BindView(R2.id.top_bar_title)
    TextView mTvTopBarTitle;
    @BindView(R2.id.top_bar_content)
    TextView mTvTopBarContent;
    @BindView(R2.id.top_bar_ico)
    ImageView mIvTapBarIcon;
    @BindView(R2.id.tv_model_type)
    TextView tvModelType;
    @BindView(R2.id.tv_tip)
    TextView mTvTip;

    private int mCurrentMode = -1;
    private int mHeadType;

    private IPreferences.Helper mHelper;
    private IMachine.WorkType mWorkType;

    private PrintReadyViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        if (mViewModel.getWorkType() == LASER) {
            mViewModel.setOriginIndicatorState(true);
        }
        initView();
        mViewModel.checkHome()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    if (aBoolean && mViewModel.getWorkType() == LASER) {
                        mViewModel.setOriginIndicatorState(true);
                    }
                }, LogHelper::log);
    }

    @Override
    public void onResume() {
        super.onResume();

        // FIXME: Needs to be clean up within this code, too much nesting in here makes code more difficult to read and understand
        if (requireActivity() instanceof PrintA400Activity && mWorkType == CNC && mViewModel.getIsRotaryAvailable()) {
            tvModelType.setVisibility(View.GONE);
            refreshView(1);
        } else if (requireActivity() instanceof PrintA400Activity && mWorkType == CNC) {
            tvModelType.setVisibility(View.VISIBLE);
            String origin = ServiceContainer.getInstance().getService(IGcodeParser.class).getOrigin();
            int mode = mHelper.getA400ManualToolCalibrationMode();
            if (origin == null) {
                refreshView(mode);
                return;
            }
            if (origin.equals(getString(R.string.all_gcode_parser_work_origin_bottom_position_a)) || origin.equals(getString(R.string.all_gcode_parser_work_origin_bottom_position_b))) {
                refreshView(1);
            } else {
                refreshView(mode);
            }
        } else if (requireActivity() instanceof PrintA400Activity && mWorkType == LASER && mViewModel.getIsRotaryAvailable()) {
            tvModelType.setVisibility(View.GONE);
            refreshView(0);
        } else {
            tvModelType.setVisibility(View.VISIBLE);
            int calibrationMode = mHelper.getA400ManualToolCalibrationMode();
            refreshView(calibrationMode);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mViewModel.getWorkType() == IMachine.WorkType.LASER) {
            mViewModel.setOriginIndicatorState(false);
        }
    }

    @Override
    protected PrintReadyViewModel getViewModel() {
        return getViewModelProvider().get(PrintReadyViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_set_origin;
    }

    private void initView() {
        mBtnNext.setText(R.string.all_next);
        mBtnNext.setVisibility(View.VISIBLE);
        mIvTapBarIcon.setVisibility(View.GONE);
        mWorkType = mViewModel.getMachine().getMachineInfoSubjectHolder().getValue().workType;
        switch (mWorkType) {
            case CNC:
                mHeadType = mViewModel.getMachine().getCNCController().getHeadType();
                mTvTopBarTitle.setText(R.string.a400_print_cnc_job_preparation);
                mTvTopBarContent.setText(getString(R.string.a400_print_set_work_origin_format, getString(R.string.a400_bracket_has_date, "1/1")));
                mProgressBar.setMax(2);
                mProgressBar.setProgress(2);
                mBtnNext.setText(R.string.all_start_job);
                break;
            case LASER:
                mHeadType = mViewModel.getMachine().getLaserController().getHeadType();
                mTvTopBarTitle.setText(R.string.a400_print_laser_job_preparation_title);
                mTvTopBarContent.setText(getString(R.string.a400_print_set_work_origin_format, getString(R.string.a400_bracket_has_date, "2/2")));
                mProgressBar.setMax(2);
                mProgressBar.setProgress(2);
                mBtnNext.setText(R.string.all_start_job);
                break;
        }

        //Text underline
        mTvRunBoundary.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        mTvRunBoundary.getPaint().setAntiAlias(true);

        mHelper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();

        WarmTipDialog movingDialog = WarmTipDialog.create(requireContext())
                .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                .setPic(R.drawable.ic_block_setup)
                .setTitle(R.string.all_move_show)
                .setContent(R.string.all_move_show_content);

        mViewModel.getMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    Logger.d("Machine moving status " + isMoving);
                    if (isMoving) {
                        movingDialog.show();
                    } else {
                        movingDialog.dismiss();
                    }
                });
    }


    private void refreshView(int position) {
        if (position == mCurrentMode) return;
        tvModelType.setText(position == 0 ? R.string.a400_print_set_origin_mode_basic_mode_sub_title : R.string.a400_print_set_origin_mode_advanced_mode_sub_title);
        mTvRunBoundary.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        switch (position) {
            case 0:
                String title;
                int desc;
                if (mViewModel.getIsRotaryAvailable()) {
                    title = getString(R.string.a400_print_job_preparation_cnc_set_origin_xyb_title);
                    desc = R.string.a400_print_job_preparation_cnc_set_origin_xyb_content;
                } else {
                    title = getString(R.string.a400_print_set_format_origin, mViewModel.getWorkType() == CNC ? "XYZ " : "XY");
                    desc = mViewModel.getWorkType() == CNC ? R.string.a400_print_job_preparation_cnc_set_origin_xyz_desc :
                            R.string.a400_print_job_preparation_cnc_set_origin_xy_desc;
                }

                // basic mode
                Bundle bundle = new Bundle();
//                bundle.putInt("pic", 0);// TODO: 2022/4/28 the main pic
                bundle.putString("title", title);
                switch (mHeadType) {
                    case Module.ModuleType.HEAD_CNC:
                        desc = mViewModel.getIsRotaryAvailable() ? R.string.a400_print_job_preparation_cnc_set_origin_xyb_content
                                : R.string.a400_print_job_preparation_cnc_set_origin_xyz_desc;
                        bundle.putInt("pic", 0);
                        bundle.putString("videoPath", mViewModel.getIsRotaryAvailable() ? "/CNC_4x_Normal_Set_XY_Origin,webm" : "/CNC_3x_Normal_Set_XY_Origin.webm");
                        break;
                    case Module.ModuleType.HEAD_CNC_200W:
                        desc = mViewModel.getIsRotaryAvailable() ? R.string.a400_print_job_preparation_cnc_set_origin_xyb_content : R.string.a400_print_job_preparation_cnc_set_origin_xyz_desc;
                        bundle.putInt("pic", 0);
                        bundle.putString("videoPath", mViewModel.getIsRotaryAvailable() ? "/CNC_4x_200W_Set_XY_Origin.webm" : "/CNC_3x_200W_Set_XY_Origin.webm");
                        break;
                    case Module.ModuleType.HEAD_LASER:
                        desc = mViewModel.getIsRotaryAvailable() ? R.string.a400_print_job_preparation_cnc_set_origin_xyb_content
                                : R.string.a400_print_job_preparation_cnc_set_origin_xy_desc;
                        bundle.putInt("pic", 0);
                        bundle.putString("videoPath", mViewModel.getIsRotaryAvailable() ? "/Laser_4x_1.6w_Set_XY_Origin.webm" : "/Laser_3x_1.6w_Set_XY_Origin.webm");
                        bundle.putBoolean("hasZ", false);
                        break;
                    case Module.ModuleType.HEAD_LASER_10W:
                        desc = mViewModel.getIsRotaryAvailable() ? R.string.a400_print_job_preparation_cnc_set_origin_xyb_content
                                : R.string.a400_print_job_preparation_cnc_set_origin_xy_desc;
                        bundle.putInt("pic", 0);
                        bundle.putString("videoPath", mViewModel.getIsRotaryAvailable() ? "/Laser_4x_10w_Set_XY_Origin.webm" : "/Laser_3x_10w_Set_XY_Origin.webm");
                        bundle.putBoolean("hasZ", false);
                        break;
                    case Module.ModuleType.HEAD_LASER_20W:
                    case Module.ModuleType.HEAD_LASER_40W:
                        desc = mViewModel.getIsRotaryAvailable() ? R.string.a400_print_job_preparation_cnc_set_origin_xyb_content
                                : R.string.a400_print_job_preparation_laser_40w_set_origin_xy_desc;
                        bundle.putInt("pic", R.drawable.pic_a400_laser_40w_3axis_set_origin);
                        bundle.putString("videoPath", mViewModel.getIsRotaryAvailable() ? "/Laser_4x_20w_Set_XY_Origin.webm" : "/Laser_3x_20w_Set_XY_Origin.webm");
                        bundle.putBoolean("hasZ", false);
                        break;
                    case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                        desc = mViewModel.getIsRotaryAvailable() ? R.string.a400_print_job_preparation_cnc_set_origin_xyb_content
                                : R.string.a400_print_job_preparation_laser_2w_set_origin_xy_desc;
                        bundle.putInt("pic", R.drawable.pic_a400_laser_40w_3axis_set_origin);
                        bundle.putString("videoPath", mViewModel.getIsRotaryAvailable() ? "/Laser_4x_2w_Set_XY_Origin.webm" : "/Laser_3x_2w_Set_XY_Origin.webm");
                        bundle.putBoolean("hasZ", false);
                        break;
                }
                bundle.putInt("desc", desc);
                Fragment fragment = WorkPrepareWithJogFragment.newInstance(bundle);
                watchForButtonStates(fragment);
                getChildFragmentManager().beginTransaction().setCustomAnimations(R.anim.push_alpha_in, R.anim.push_alpha_out).replace(R.id.fcv_jog_mode, fragment, "prepare").commit();

                if (mWorkType == CNC && mViewModel.getIsRotaryAvailable()) {
                    mTvTip.setText("");
                }
                break;
            case 1:
                // advance mode
//                Observable.timer(10, TimeUnit.MILLISECONDS)
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .as(bindToLifecycle())
//                        .subscribe(time -> {
                getChildFragmentManager().beginTransaction().setCustomAnimations(R.anim.push_alpha_in, R.anim.push_alpha_out).replace(R.id.fcv_jog_mode, A400PrepareJogControlFragment.newInstance()).commit();
//                        }, LogHelper::log);
                if (mWorkType == CNC && mViewModel.getIsRotaryAvailable()) {
                    mTvTip.setText(R.string.a400_print_cnc_set_origin_4axis_desc);
                }
                break;
        }
        mCurrentMode = position;
    }

    private void watchForButtonStates(Fragment fragment) {
        Logger.d("is work prepare fragment: %1$s,%2$s", fragment instanceof WorkPrepareWithJogFragment, fragment);
        if (fragment instanceof WorkPrepareWithJogFragment) {
            ((WorkPrepareWithJogFragment) fragment).getButtonsEnableObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(shouldEnable -> ViewUtils.enableButtons((ViewGroup) requireView(), shouldEnable), LogHelper::log);
        }
    }

    public void runBoundary() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.pic_toolhead_run_boundary)
                .setType(DecisionDialog.TIP_TYPE)
                .setTitle(requireContext().getString(R.string.toolhead_run_boundary))
                .setContent(R.string.toolhead_run_boundary_message)
                .setSecondTv(requireContext().getString(R.string.all_confirm), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    // If cur mode is basic, we need to set current position as origin first.
                    mViewModel.runBoundary(mCurrentMode == 0)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(result -> Logger.d("Run boundary result is %s", result), LogHelper::log);
                })
                .setFirstTv(requireContext().getString(R.string.all_cancel), R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void showCNCPrintDialog() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.pic_a400_control_cnc_open)
                .setType(DecisionDialog.TIP_TYPE)
                .setTitle(R.string.cnc_safety_goggles_open)
                .setContent(mViewModel.getIsRotaryAvailable() ? R.string.cnc_rotary_safety_goggles_open_message : R.string.cnc_safety_goggles_open_message)
                .setSecondTv(requireContext().getString(R.string.all_confirm), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    ((PrintA400Activity) requireActivity()).goToPrint();
                })
                .setFirstTv(requireContext().getString(R.string.all_cancel), R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void showErrorDialog(String s) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Error!")
                .setMessage(s)
                .create()
                .show();
    }

    @OnClick(R2.id.tv_run_boundary)
    void onRunBoundaryClicked() {
        playNormalClickSound();
        runBoundary();
    }


    @OnClick(R2.id.btn_next)
    void onNextClicked() {
        playNormalClickSound();
        if (mViewModel.getWorkType() == CNC && mCurrentMode == 1) {
            showCNCPrintDialog();
        } else {
            Observable<Boolean> observable;
            if (mViewModel.getWorkType() == CNC) {
                observable = mViewModel.setXYZOrigin();
            } else if (mViewModel.getWorkType() == LASER && mCurrentMode == 0) {
                observable = mViewModel.setXYOrigin();
            } else {
                observable = Observable.just(true);
            }
            Vector vector = new Vector();
            vector.setX(0);
            vector.setY(0);
            vector.setZ(0);
            observable
                    .flatMap(success -> success ? mViewModel.gotoAbsolutePosition(vector).flatMap(responseStructure -> Observable.just(responseStructure.isSuccess())) : Observable.just(success))
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (success) {
                            switch (mViewModel.getWorkType()) {
                                case FDM:
                                    ((PrintA400Activity) requireActivity()).goToPrint();
                                    break;
                                case LASER:
                                    int titleResId = R.string.laser_safety_goggles_open;
                                    int contentResId = R.string.laser_safety_goggles_open_message;
                                    switch (mViewModel.getHeadType()) {
                                        case Module.ModuleType.HEAD_LASER:
                                        case Module.ModuleType.HEAD_LASER_10W:
                                            titleResId = R.string.laser_safety_goggles_open;
                                            contentResId = mViewModel.getIsRotaryAvailable() ? R.string.laser_rotary_safety_goggles_open_message : R.string.laser_safety_goggles_open_message;
                                            break;
                                        case Module.ModuleType.HEAD_LASER_20W:
                                        case Module.ModuleType.HEAD_LASER_40W:
                                            titleResId = R.string.laser_safety_40w_goggles_open_and_keep_attended;
                                            contentResId = mViewModel.getIsRotaryAvailable() ? R.string.laser_40w_rotary_safety_goggles_open_message : R.string.laser_40w_safety_goggles_open_message;
                                            break;
                                        case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                                            titleResId = R.string.laser_safety_goggles_open;
                                            contentResId = mViewModel.getIsRotaryAvailable() ? R.string.laser_2w_rotary_safety_goggles_open_message : R.string.laser_2w_safety_goggles_open_message;
                                            break;
                                    }
                                    DecisionDialog.create(requireContext())
                                            .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                                            .setType(DecisionDialog.TIP_TYPE)
                                            .setPic(R.drawable.ic_laser_turn_on_224x224)
                                            .setTitle(titleResId)
                                            .setContent(contentResId)
                                            .setSecondTv(requireContext().getString(R.string.all_confirm), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                                                dialog.dismiss();
                                                ((PrintA400Activity) requireActivity()).goToPrint();
                                            })
                                            .setFirstTv(requireContext().getString(R.string.all_cancel), R.color.select_dialog_white_txt, (dialog, which) -> {
                                                dialog.dismiss();
                                            })
                                            .show();
                                    break;
                                case CNC:
                                    showCNCPrintDialog();
                                    break;
                                default:
                                    break;
                            }
                        }
                    }, e -> {
                        showErrorDialog(e.getMessage());
                        LogHelper.log(e);
                    });
        }
    }


    @OnClick(R2.id.tv_model_type)
    public void OnclickModel() {
//        mSelectModelDialog.show();
        playNormalClickSound();
        ServiceContainer.getInstance().getService(IRouter.class)
                .routeWithClassPath(RoutePath.PRINT_MANUAL_TOOL_CHECK_MODE)
                .start(getContext());
    }

    @Override
    protected void back() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.WARMING_TYPE)
                .setPic(R.drawable.ic_yellow_warn)
                .setTitle(getString(R.string.a400_quit_job_preparation))
                .setContent(getString(R.string.a400_print_dialog_action_stop_title,
                        getString(R.string.a400_quit_job_preparation).toLowerCase()))
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> dialog.dismiss())
                .setSecondTv(R.string.all_quit, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    requireActivity().finish();
                }).show();
    }

}
