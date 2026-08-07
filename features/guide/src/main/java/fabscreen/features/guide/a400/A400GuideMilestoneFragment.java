package fabscreen.features.guide.a400;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC_200W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_20W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_2W_INFRARED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_40W;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.features.guide.R;
import fabscreen.features.guide.R2;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;

public class A400GuideMilestoneFragment extends BaseFragment {
    @BindView(R2.id.tv_title_main)
    TextView mTvTitle;
    @BindView(R2.id.tv_main_desc)
    TextView mTvMainDesc;
    @BindView(R2.id.rv_procedure_list)
    RecyclerView mRvProcedureList;
    @BindView(R2.id.btn_start_or_continue)
    Button mBtnStartOrContinue;

    private int mNextProcedure = 1;
    private long mMachineSN;
    private List<GuideProcedure> mProcedureList = new ArrayList<>();
    private ProcedureListAdapter mAdapter;
    private A400GuideMilestoneViewModel mViewModel;
    protected IPreferences.Helper helper;
    private long mTime;
    private int mTouchCount = 0;


    public static Fragment newInstance() {
        return new A400GuideMilestoneFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        helper = getServiceContainer().getService(IPreferences.class).getHelper();
        initView();
    }

    @Override
    protected A400GuideMilestoneViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(A400GuideMilestoneViewModel.class);
    }

    /**
     * 3dp
     * - single-dual
     * - single-single
     * laser
     * - 10w
     * - 1.6w
     * - rotary
     * cnc
     */
    private void initView() {
        mTvTitle.setText(R.string.guide_a400_setup_completed_title);
        mAdapter = new ProcedureListAdapter(mProcedureList);
        mRvProcedureList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRvProcedureList.setAdapter(mAdapter);
        mProcedureList.clear();

        mMachineSN = helper.getA400MachineSn();
        mNextProcedure = helper.getA400MachineStep(mMachineSN) + 1;

        switch (mViewModel.getHeadType()) {
            case HEAD_3DP_DOUBLE_EXTRUDER:
                mProcedureList.add(new GuideProcedure(getString(R.string.a400_guide_3dp_dual_extruder_step_1_title), false));
                mProcedureList.add(new GuideProcedure(getString(R.string.a400_guide_3dp_dual_extruder_step_2_title), false));
                mProcedureList.add(new GuideProcedure(getString(R.string.a400_guide_3dp_dual_extruder_step_3_title), false));
                break;
            case HEAD_3DP:
                mProcedureList.add(new GuideProcedure(getString(R.string.a400_guide_3dp_heated_bed_leveling_title), false));
                mProcedureList.add(new GuideProcedure(getString(R.string.a400_guide_3dp_load_filament_title), false));
                break;
            case HEAD_LASER_10W:
                if (mViewModel.isRotaryAvailable()) {
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_central_axis_calibration_title), false));
                } else {
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_thickness_m_calibration_title), false));
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_camera_calibration_title), false));
                }
                break;
            case HEAD_LASER:
                if (mViewModel.isRotaryAvailable()) {
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_central_axis_calibration_title), false));
                } else {
                    mProcedureList.add(new GuideProcedure(getString(R.string.a400_guide_laser_manual_focus_calibration_title), false));
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_camera_calibration_title), false));
                }
                break;
            case HEAD_LASER_20W:
            case HEAD_LASER_40W:
                if (mViewModel.isRotaryAvailable()) {
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_central_axis_calibration_title), false));
                } else {
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_laser_40w_platform_height_calibration_title), false));
                }
                break;
            case HEAD_LASER_2W_INFRARED:
                if (mViewModel.isRotaryAvailable()) {
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_central_axis_calibration_title), false));
                } else {
                    mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_laser_2w_platform_height_calibration_title), false));
                }
                break;
            case HEAD_CNC:
            case HEAD_CNC_200W:
                mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_safety_goggles_title), false));
                mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_fix_material_title), false));
                mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_attach_bit_title), false));
                mProcedureList.add(new GuideProcedure(getString(R.string.guide_a400_tools_screen_title), false));
                break;
            default:
                mRouter.backHome().start(requireContext());
                break;
        }

        if (helper.getA400MachineStep(mMachineSN) == 0) {
            //开始的时候标题和内容
            initContent();
        } else {
            //记住步骤后的标题和内容
            refreshUIFromProcedure(helper.getA400MachineStep(mMachineSN) - 1);
        }

        if (mNextProcedure > 1) {
            for (int i = 0; i < mNextProcedure - 1; i++) {
                mProcedureList.get(i).activated = true;
            }
        }

        mAdapter.notifyItemRangeInserted(0, mProcedureList.size());

        playProcedureCompleteSound();
    }

    public void initContent() {
        switch (mViewModel.getHeadType()) {
            case HEAD_3DP_DOUBLE_EXTRUDER:
                mTvMainDesc.setText(R.string.guide_a400_3dp_dual_extruder_msg_desc);
                break;
            case HEAD_3DP:
                mTvMainDesc.setText(R.string.guide_a400_3dp_single_extruder_msg_title);
                break;
            case HEAD_LASER_10W:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setText(R.string.guide_a400_10w_laser_4axis_msg_desc);
                } else {
                    mTvMainDesc.setText(R.string.guide_a400_10w_laser_3axis_msg_desc);
                }
                break;
            case HEAD_LASER:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setText(R.string.guide_a400_laser_4axis_msg_desc);
                } else {
                    mTvMainDesc.setText(R.string.guide_a400_laser_3axis_msg_desc);
                }
                break;
            case HEAD_LASER_20W:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setText(R.string.guide_a400_20w_laser_4axis_msg_desc);
                } else {
                    mTvMainDesc.setText(R.string.guide_a400_20w_laser_3axis_msg_desc);
                }
                break;
            case HEAD_LASER_40W:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setText(R.string.guide_a400_40w_laser_4axis_msg_desc);
                } else {
                    mTvMainDesc.setText(R.string.guide_a400_40w_laser_3axis_msg_desc);
                }
                break;
            case HEAD_LASER_2W_INFRARED:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setText(R.string.guide_a400_2w_laser_4axis_msg_desc);
                } else {
                    mTvMainDesc.setText(R.string.guide_a400_2w_laser_3axis_msg_desc);
                }
                break;
            case HEAD_CNC:
            case HEAD_CNC_200W:
                mTvMainDesc.setText(mViewModel.isRotaryAvailable() ? R.string.guide_a400_cnc_4axis_msg_desc : R.string.guide_a400_cnc_3axis_msg_desc);
                break;
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_guide_milstone;
    }

    @OnClick(R2.id.btn_guide_a400_escape_exit)
    void onClickEscapeExit() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - mTime < 500) {
            mTouchCount += 1;
        } else {
            mTouchCount = 1;
        }
        mTime = currentTime;
        if (mTouchCount >= 5) {
            Logger.i("Escape exit trigger from guide procedure step %d", mNextProcedure);
            mRouter.routeToSettingsPage().start(requireContext());
        }
    }

    @OnClick(R2.id.btn_start_or_continue)
    void onStartClick() {
        playNormalClickSound();
        A400GuideMilestoneActivity activity = (A400GuideMilestoneActivity) requireActivity();
        switch (mViewModel.getHeadType()) {
            case HEAD_3DP_DOUBLE_EXTRUDER:
                singleDualSetup(activity);
                break;
            case HEAD_3DP:
                singleSingleSetup(activity);
                break;
            case HEAD_LASER_10W:
                if (mViewModel.isRotaryAvailable()) {
                    laserWithRotarySetup(activity, HEAD_LASER_10W);
                } else {
                    tenWLaserSetup(activity);
                }
                break;
            case HEAD_LASER_20W:
                if (mViewModel.isRotaryAvailable()) {
                    laserWithRotarySetup(activity, HEAD_LASER_20W);
                } else {
                    twentyWLaserSetup(activity);
                }
                break;
            case HEAD_LASER_40W:
                if (mViewModel.isRotaryAvailable()) {
                    laserWithRotarySetup(activity, HEAD_LASER_40W);
                } else {
                    fortyWLaserSetup(activity);
                }
                break;
            case HEAD_LASER_2W_INFRARED:
                if (mViewModel.isRotaryAvailable()) {
                    laserWithRotarySetup(activity, HEAD_LASER_2W_INFRARED);
                } else {
                    twoWLaserSetup(activity);
                }
                break;
            case HEAD_LASER:
                if (mViewModel.isRotaryAvailable()) {
                    laserWithRotarySetup(activity, HEAD_LASER);
                } else {
                    originalLaserSetup(activity);
                }
                break;
            case HEAD_CNC:
            case HEAD_CNC_200W:
                cncSetup(activity);
                break;
        }
    }

    public void onMilestoneAchieved(int requestCode) {
        Logger.d("milestone %d achieved!", requestCode);
        int procedureIndex = requestCode - 1;
        if (mViewModel.getHeadType() == HEAD_CNC || mViewModel.getHeadType() == HEAD_CNC_200W) {
            // If cnc, activated all.
            for (GuideProcedure procedure : mProcedureList) {
                procedure.activated = true;
            }
            procedureIndex = mProcedureList.size() - 1;
            mAdapter.notifyItemRangeChanged(0, mProcedureList.size());
        } else {
            mProcedureList.get(procedureIndex).activated = true;
            mAdapter.notifyItemChanged(procedureIndex);
        }

        switch (mViewModel.getHeadType()) {
            case HEAD_CNC:
            case HEAD_CNC_200W:
                helper.setA400MachineStep(mMachineSN, 1);
                break;
            case HEAD_LASER:
            case HEAD_LASER_10W:
            case HEAD_LASER_2W_INFRARED:
            case HEAD_LASER_20W:
            case HEAD_LASER_40W:
                if (mViewModel.isRotaryAvailable()) {
                    helper.setA400MachineStep(mMachineSN, 1);
                } else {
                    helper.setA400MachineStep(mMachineSN, requestCode);
                }
                break;
            case HEAD_3DP:
            case HEAD_3DP_DOUBLE_EXTRUDER:
                helper.setA400MachineStep(mMachineSN, requestCode);
                break;
        }

        mNextProcedure++;
        refreshUIFromProcedure(procedureIndex);
        playProcedureCompleteSound();
    }

    public void onMilestoneRewound(int requestCode) {
        Logger.d("Milestone %d rewound!", requestCode);
        int procedureIndex = requestCode - 1;
        refreshUIFromProcedure(procedureIndex);
        mProcedureList.get(procedureIndex).activated = false;
        mAdapter.notifyItemChanged(procedureIndex);

        if (procedureIndex == 0) {
            mTvTitle.setText(R.string.guide_a400_setup_completed_title);
            initContent();
        } else {
            refreshUIFromProcedure(procedureIndex - 1);
        }

        switch (mViewModel.getHeadType()) {
            case HEAD_CNC:
            case HEAD_CNC_200W:
                helper.setA400MachineStep(mMachineSN, 0);
                break;

            case HEAD_LASER:
            case HEAD_LASER_10W:
                if (mViewModel.isRotaryAvailable()) {
                    helper.setA400MachineStep(mMachineSN, 0);
                } else {
                    helper.setA400MachineStep(mMachineSN, procedureIndex);
                }
                break;

            case HEAD_3DP:
            case HEAD_3DP_DOUBLE_EXTRUDER:
                helper.setA400MachineStep(mMachineSN, procedureIndex);
                break;
        }
        mNextProcedure--;
        playProcedureCompleteSound();

    }

    public void refreshUIFromProcedure(int procedureIndex) {
        switch (mViewModel.getHeadType()) {
            case HEAD_CNC:
            case HEAD_CNC_200W:
                if (procedureIndex == mProcedureList.size() - 1) {
                    mTvMainDesc.setVisibility(View.GONE);
                    mTvTitle.setText(R.string.guide_a400_setup_guidance_completed_title);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                }
                break;

            case HEAD_LASER:
                // No UI refresh is needed when using Laser with rotary.
                if (mViewModel.isRotaryAvailable()) break;

                mTvMainDesc.setVisibility(View.VISIBLE);
                if (procedureIndex == mProcedureList.size() - 1) {
                    mTvTitle.setText(R.string.guide_a400_laser_10w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_10w_4axis_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                } else {
                    mTvTitle.setText(R.string.guide_a400_laser_10w_step_1_successfully_title);
                    mTvMainDesc.setText(R.string.a400_guide_laser_camera_capture_calibration_intro_desc_2);
                    mBtnStartOrContinue.setText(R.string.all_continue);
                }
                break;

            case HEAD_LASER_10W:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_10w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_10w_4axis_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                } else {
                    if (procedureIndex == 0) {
                        mTvMainDesc.setVisibility(View.VISIBLE);
                        mTvTitle.setText(R.string.guide_a400_laser_10w_step_1_successfully_title);
                        mTvMainDesc.setText(R.string.guide_a400_laser_10w_step_1_successfully_msg_desc);
                        mBtnStartOrContinue.setText(R.string.all_continue);
                    } else if (procedureIndex == mProcedureList.size() - 1) {
                        mTvMainDesc.setVisibility(View.VISIBLE);
                        mTvTitle.setText(R.string.guide_a400_laser_10w_success_title);
                        mTvMainDesc.setText(R.string.guide_a400_laser_10w_all_successfully_msg_desc);
                        mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                    }
                }
                break;
            case HEAD_LASER_20W:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_20w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_10w_4axis_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                } else {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_20w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_20w_all_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                }
                break;
            case HEAD_LASER_40W:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_40w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_10w_4axis_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                } else {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_40w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_40w_all_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                }
                break;
            case HEAD_LASER_2W_INFRARED:
                if (mViewModel.isRotaryAvailable()) {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_2w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_10w_4axis_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                } else {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_2w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_laser_2w_all_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                }
                break;
            case HEAD_3DP:
                break;

            case HEAD_3DP_DOUBLE_EXTRUDER:
                if (procedureIndex == 0) {
                    //step one success
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_dual_extruder_step_1_successfully_title);
                    mTvMainDesc.setText(R.string.guide_a400_dual_extruder_step_1_successfully_msg);
                    mBtnStartOrContinue.setText(R.string.all_continue);
                } else if (procedureIndex == 1) {
                    //step two success
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_dual_extruder_step_2_successfully_title);
                    mTvMainDesc.setText(R.string.guide_a400_dual_extruder_step_2_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.all_continue);
                } else if (procedureIndex == mProcedureList.size() - 1) {
                    mTvMainDesc.setVisibility(View.VISIBLE);
                    mTvTitle.setText(R.string.guide_a400_laser_10w_success_title);
                    mTvMainDesc.setText(R.string.guide_a400_dual_extruder_all_successfully_msg_desc);
                    mBtnStartOrContinue.setText(R.string.guide_a400_start_creating);
                }
                break;
        }
    }

    private void cncSetup(A400GuideMilestoneActivity activity) {
        switch (mNextProcedure) {
            case 1:
                activity.goToCNCSetupForResult(mNextProcedure);
                break;

            default:
                activity.goHomePage();
                break;
        }
    }

    private void laserWithRotarySetup(A400GuideMilestoneActivity activity, int headType) {
        switch (mNextProcedure) {
            case 1:
                // TODO
                Bundle pageData = new Bundle();
                pageData.putString("title", getString(R.string.a400_guide_laser_10w_central_axis_calibration_title));
                switch (headType) {
                    case HEAD_LASER:
                        pageData.putInt("image", R.drawable.pic_initialize_1_6w_laser_module_central_axis_calibration_578x434);
                        break;
                    case HEAD_LASER_2W_INFRARED:
                        pageData.putInt("image", R.drawable.pic_laser_2w_central_axis_calibration_cylinder_material_ouch_material_479x359);
                    case HEAD_LASER_20W:
                    case HEAD_LASER_40W:
                        pageData.putInt("image", R.drawable.pic_laser_20w_central_axis_calibration_cylinder_material_ouch_material_479x359);
                        break;
                    case HEAD_LASER_10W:
                    default:
                        pageData.putInt("image", R.drawable.pic_initialize_10w_laser_module_central_axis_calibration_578x434);
                        break;
                }
                pageData.putInt("desc", R.string.a400_guide_laser_10w_central_axis_calibration_step_1_msg_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_CENTRAL_AXIS);
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;

            default:
                activity.goHomePage();
                break;
        }

    }


    private void originalLaserSetup(A400GuideMilestoneActivity activity) {
        Bundle pageData = new Bundle();
        switch (mNextProcedure) {
            case 1:
                pageData.putString("title", getString(R.string.a400_guide_laser_manual_focus_calibration_intro_title));
                pageData.putInt("image", R.drawable.pic_initialize_1_6w_laser_module_manual_focus_calibration);
                pageData.putInt("desc", R.string.a400_guide_laser_manual_focus_calibration_intro_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_FOCUS_CALIBRATION);
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;
            case 2:
                pageData.putString("title", getString(R.string.a400_guide_laser_camera_capture_calibration_intro_title));
                pageData.putInt("image", R.drawable.pic_initialize_10w_laser_module_camera_calibration);
                pageData.putInt("desc", R.string.a400_guide_laser_camera_capture_calibration_intro_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_10W_CAMERA_CALIBRATION);
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;
            default:
                activity.goHomePage();
        }
    }

    private void singleDualSetup(A400GuideMilestoneActivity activity) {
        switch (mNextProcedure) {
            case 1:
                activity.goToFilamentSetupForResult(mNextProcedure);
                break;
            case 2:
                activity.goToZCalibrationSetupForResult(mNextProcedure);
                break;
            case 3:
                activity.goToXYCalibrationSetupForResult(mNextProcedure);
                break;
            default:
                activity.goHomePage();
                break;
        }
    }

    private void singleSingleSetup(A400GuideMilestoneActivity activity) {
        switch (mNextProcedure) {
            case 1:
                activity.goToHeatedBedLevelingSetupForResult(mNextProcedure);
                break;

            case 2:
                activity.goToSingleSingleFilamentSetupForResult(mNextProcedure);
                break;
            default:
                activity.goHomePage();
        }
    }

    private void tenWLaserSetup(A400GuideMilestoneActivity activity) {
        Bundle pageData = new Bundle();
        switch (mNextProcedure) {
            case 1:
                pageData.putString("title", getString(R.string.guide_a400_thickness_m_calibration_title));
                pageData.putInt("image", 0);
                pageData.putInt("desc", R.string.guide_a400_thickness_measurement_calibration_msg_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_THICKNESS_MEASURE_CALIBRATION);
                pageData.putString("videoPath", "/Laser_3x_10w_Auto_Measurement.webm");
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;
            case 2:
                pageData.putString("title", getString(R.string.guide_a400_camera_calibration_title));
                pageData.putInt("image", R.drawable.pic_initialize_10w_laser_module_camera_calibration);
                pageData.putInt("desc", R.string.guide_a400_camera_calibration_msg_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_10W_CAMERA_CALIBRATION);
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;
            default:
                Logger.d("10W Laser Guide completed, return to home.");
                activity.goHomePage();
                break;
        }
    }

    private void twentyWLaserSetup(A400GuideMilestoneActivity activity) {
        Bundle pageData = new Bundle();
        switch (mNextProcedure) {
            case 1:
                pageData.putString("title", getString(R.string.guide_a400_laser_40w_platform_height_calibration_title));
                pageData.putInt("image", R.drawable.pic_initialize_40w_laser_module_paltform_height_calibration);
                pageData.putInt("desc", R.string.guide_a400_platform_height_calibration_msg_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_40W_PLATFORM_HEIGHT_INFO);
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;
            default:
                activity.goHomePage();
                break;
        }
    }

    private void fortyWLaserSetup(A400GuideMilestoneActivity activity) {
        Bundle pageData = new Bundle();
        switch (mNextProcedure) {
            case 1:
                pageData.putString("title", getString(R.string.guide_a400_laser_40w_platform_height_calibration_title));
                pageData.putInt("image", R.drawable.pic_initialize_40w_laser_module_paltform_height_calibration);
                pageData.putInt("desc", R.string.guide_a400_platform_height_calibration_msg_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_40W_PLATFORM_HEIGHT_INFO);
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;
            default:
                activity.goHomePage();
                break;
        }
    }

    private void twoWLaserSetup(A400GuideMilestoneActivity activity) {
        Bundle pageData = new Bundle();
        switch (mNextProcedure) {
            case 1:
                pageData.putString("title", getString(R.string.guide_a400_laser_2w_platform_height_calibration_title));
                pageData.putInt("image", R.drawable.pic_initialize_2w_laser_module_paltform_height_calibration);
                pageData.putInt("desc", R.string.guide_a400_platform_height_calibration_msg_desc);
                pageData.putString("router_destination", RoutePath.TOOLS_CALIBRATION_A400_LASER_2W_PLATFORM_HEIGHT_INFO);
                activity.goToSetupIntroForResult(mNextProcedure, pageData);
                break;
            default:
                activity.goHomePage();
                break;
        }
    }

    static class GuideProcedure {
        public String name;
        public boolean activated;

        public GuideProcedure(String title, boolean activated) {
            this.name = title;
            this.activated = activated;
        }
    }


    static class ProcedureListAdapter extends RecyclerView.Adapter<ProcedureListAdapter.ViewHolder> {
        private final List<GuideProcedure> mProcedureList;

        public ProcedureListAdapter(List<GuideProcedure> procedureList) {
            mProcedureList = procedureList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guide_procedure, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GuideProcedure procedure = mProcedureList.get(position);
            holder.mTvProcedureName.setText(procedure.name);
            holder.itemView.setActivated(procedure.activated);
        }

        @Override
        public int getItemCount() {
            return mProcedureList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R2.id.tv_procedure_name)
            TextView mTvProcedureName;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
