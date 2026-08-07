package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.core.ui.common.leftsection.A400RightSectionAndDetailContainerFragment;
import fabscreen.platform.core.ui.common.leftsection.A400RightSectionsAdapter;
import fabscreen.platform.core.ui.common.leftsection.SectionItem;

public class PrintA400AdjustmentContainerFragment extends A400RightSectionAndDetailContainerFragment {

    //    @BindView(R2.id.tv_print_setting_name)
//    TextView mTvName;
    @BindView(R2.id.tv_title)
    TextView mTvTitle;
    @BindView(R2.id.cl_calibration_right_select)
    ConstraintLayout mClRightSelect;
    @BindView(R2.id.fcv_detail)
    FragmentContainerView mFcvDetail;

    public static Fragment newInstance() {
        return new PrintA400AdjustmentContainerFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Animation animLeftIn = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_in_left);
        mFcvDetail.setAnimation(animLeftIn);

        Animation animRightIn = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_in_right);
        mClRightSelect.startAnimation(animRightIn);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_right_section_and_detail_container;
    }

    @Override
    protected List<SectionItem> getLeftSections() {
        List<SectionItem> items = new ArrayList<>();
        MachineInfo value = getServiceContainer().getService(IMachine.class).getMachineInfoSubjectHolder().getValue();
        switch (value.workType) {
            case FDM:
                mTvTitle.setText(R.string.a400_print_adjustment_section_print_settings_title);
//                items.add(new SectionItem("Extruder", R.drawable.select_a400_print_setting_right_z_move, PrintJ1AdjustmentExtruderFragment.newInstance()));
//                items.add(new SectionItem("Heated Bed", R.drawable.select_a400_print_setting_right_z_move, PrintA400AdjustmentHeatedBedFragment.newInstance()));
                items.add(new SectionItem(getString(R.string.a400_print_print_setting_left_z_offset_title), R.drawable.select_a400_print_setting_left_z_move, PrintA400AdjustmentLeftZOffsetFragment.newInstance()));
                items.add(new SectionItem(getString(R.string.a400_print_print_setting_right_z_offset_title), R.drawable.select_a400_print_setting_right_z_move, PrintA400AdjustmentRightZOffsetFragment.newInstance()));
                items.add(new SectionItem(getString(R.string.a400_print_print_setting_part_cooling_fan_title), R.drawable.select_a400_print_setting_fan, PrintA400AdjustmentFanSpeedFragment.newInstance()));
                items.add(new SectionItem(getString(R.string.a400_print_print_setting_left_flow_rate_title), R.drawable.select_a400_print_setting_left_extruder, PrintA400AdjustmentFlowRateFragment.newInstance(PrintA400AdjustmentFlowRateFragment.LEFT_NOZZLE_TYPE)));
                items.add(new SectionItem(getString(R.string.a400_print_print_setting_right_flow_rate_title), R.drawable.select_a400_print_setting_right_extruder, PrintA400AdjustmentFlowRateFragment.newInstance(PrintA400AdjustmentFlowRateFragment.RIGHT_NOZZLE_TYPE)));
                items.add(new SectionItem(getString(R.string.a400_print_print_setting_filament_sensor_title), R.drawable.select_a400_print_setting_filament_sensor, PrintA400AdjustmentFilamentSensorFragment.newInstance()));

                break;
            case LASER:
            case CNC:
            case NONE:
                mTvTitle.setText(R.string.a400_print_adjustment_section_job_settings_title);
                break;
        }
        items.add(new SectionItem(getString(R.string.a400_print_print_setting_part_work_speed_title), R.drawable.selelct_a400_print_setting_work_pacing, PrintA400AdjustmentWorkSpeedFragment.newInstance()));

        if (value.isEnclosureAvailable) {
            items.add(new SectionItem(getString(R.string.all_enclosure), R.drawable.select_a400_print_setting_enclosure, PrintA400AdjustmentEnclosureControlFragment.newInstance()));
        }
        if (value.isAirPurifierAvailable) {
            items.add(new SectionItem(getString(R.string.all_air_purifier), R.drawable.select_a400_print_setting_air_purifier, PrintA400AdjustmentAirPurifierFragment.newInstance()));
        }
        return items;
    }

    @Override
    protected A400RightSectionsAdapter getSectionsAdapter(List<SectionItem> sectionItems) {
        return new A400RightSectionsAdapter(sectionItems);
    }

    @Override
    protected void onSectionSelected(int position, boolean isUserClick) {
        if (isUserClick) {
            playNormalClickSound();
        }
        Fragment fragment = mSectionItems.get(position).fragment;
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction().setCustomAnimations(fabscreen.platform.core.R.anim.push_in_left_fast, 0)
                .replace(fabscreen.platform.core.R.id.fcv_detail, fragment).commit();
    }

    @Override
    protected String getTitle() {
        return null;
    }

    @OnClick(R2.id.btn_close)
    public void onClickBack() {
        Animation animLeftOut = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_out_left);
        mFcvDetail.setAnimation(animLeftOut);

        Animation animRightOut = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_out_right);
        mClRightSelect.startAnimation(animRightOut);
        animLeftOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                back();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
