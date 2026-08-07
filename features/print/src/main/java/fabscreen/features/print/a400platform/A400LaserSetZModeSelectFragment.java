package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.common.VerticalSpaceItemDecoration;
import fabscreen.platform.core.ui.common.selection.CalibrationModeSelectionAdapter;
import fabscreen.platform.core.ui.common.selection.CalibrationModeSelectionItem;

public class A400LaserSetZModeSelectFragment extends BaseFragment {
    protected final List<CalibrationModeSelectionItem> mSectionItems = new ArrayList<>();
    protected List<CalibrationModeSelectionItem> items = new ArrayList<>();
    protected IPreferences.Helper mPreferenceHelper;
    @BindView(R2.id.rv_sections)
    RecyclerView mRvSections;
    @BindView(R2.id.li_calibration_right_select)
    LinearLayout mlySections;
    @BindView(R2.id.ly_calibration_right_select)
    ConstraintLayout mCLRightSelect;

    private MachineInfo mMachineInfo;

    public static Fragment newInstance() {
        return new A400LaserSetZModeSelectFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreferenceHelper = getServiceContainer().getService(IPreferences.class).getHelper();
        initView();
    }

    private void initView() {
        Animation animRightIn = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_in_right);
        mCLRightSelect.startAnimation(animRightIn);

        IMachine machine = ServiceContainer.getInstance().getService(IMachine.class);
        mMachineInfo = machine.getMachineInfoSubjectHolder().getValue();
        mMachineInfo = machine.getMachineInfoSubjectHolder().getValue();
        if (mSectionItems.size() > 0) {
            mSectionItems.clear();
            items.clear();
        }
        mlySections.setVisibility(View.GONE);
        mSectionItems.addAll(getSections());
        mRvSections.setLayoutManager(new LinearLayoutManager(requireContext()));
        VerticalSpaceItemDecoration decoration = new VerticalSpaceItemDecoration(DimensUtils.dp2px(12));
        mRvSections.addItemDecoration(decoration);

        CalibrationModeSelectionAdapter adapter = new CalibrationModeSelectionAdapter(mSectionItems);
        mRvSections.setAdapter(adapter);
        adapter.setOnSectionSelectedListener(position -> {
            playNormalClickSound();
            onSectionSelected(position);
            onExit();
        });

        int mode = 0;
        if (mMachineInfo.isRotaryAvailable) {
            switch (mMachineInfo.headType) {
                case Module.ModuleType.HEAD_LASER:
                    mode = mPreferenceHelper.getFourAxisLaserPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_10W:
                    mode = mPreferenceHelper.getFourAxis10WLaserPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    mode = mPreferenceHelper.getFourAxisLaser20wPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    mode = mPreferenceHelper.getFourAxisLaser2wPrintZOriginMode();
                    break;
                default:
                    break;
            }
        } else {
            switch (mMachineInfo.headType) {
                case Module.ModuleType.HEAD_LASER:
                case Module.ModuleType.HEAD_LASER_10W:
                    mode = mPreferenceHelper.getLaserPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    mode = mPreferenceHelper.getLaser20wPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    mode = mPreferenceHelper.getLaser2wPrintZOriginMode();
                    break;
                default:
                    break;
            }
        }
        adapter.setSelection(mode);
        onSectionSelected(mode);

    }


    private void onSectionSelected(int position) {
        // Save position
        if (mMachineInfo.isRotaryAvailable) {
            switch (mMachineInfo.headType) {
                case Module.ModuleType.HEAD_LASER:
                    mPreferenceHelper.setFourAxisLaserPrintZOriginMode(position);
                    break;
                case Module.ModuleType.HEAD_LASER_10W:
                    mPreferenceHelper.setFourAxis10WLaserPrintZOriginMode(position);
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    mPreferenceHelper.setFourAxisLaser20wPrintZOriginMode(position);
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    mPreferenceHelper.setFourAxisLaser2wPrintZOriginMode(position);
                    break;
                default:
                    break;
            }
        } else {
            switch (mMachineInfo.headType) {
                case Module.ModuleType.HEAD_LASER:
                case Module.ModuleType.HEAD_LASER_10W:
                    mPreferenceHelper.setLaserPrintZOriginMode(position);
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    mPreferenceHelper.setLaser20wPrintZOriginMode(position);
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    mPreferenceHelper.setLaser2wPrintZOriginMode(position);
                    break;
                default:
                    break;
            }
        }
    }

    private List<CalibrationModeSelectionItem> getSections() {
        if (mMachineInfo.isRotaryAvailable) {
            switch (mMachineInfo.headType) {
                case Module.ModuleType.HEAD_LASER:
                case Module.ModuleType.HEAD_LASER_10W:
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_4axis_input_diameter_title, R.string.a400_print_laser_4axis_input_diameter_content));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_4axis_touch_material_title, R.string.a400_print_laser_4axis_touch_material_content));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_4axis_manual_focus_title, R.string.a400_print_laser_4axis_manual_focus_content));
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_4axis_input_diameter_title, R.string.a400_print_laser_4axis_input_diameter_content));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_4axis_touch_material_title, R.string.a400_print_laser_4axis_touch_material_content));
                    break;
                default:
                    break;
            }
        } else {
            switch (mMachineInfo.headType) {
                case Module.ModuleType.HEAD_LASER:
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_input_thickness_sub_title, R.string.a400_print_laser_input_thickness_content_desc));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_manual_focus_title, R.string.a400_print_laser_manual_focus_content_desc));
                    break;
                case Module.ModuleType.HEAD_LASER_10W:
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_automatic_thickness_measurement_title, R.string.a400_print_laser_automatic_thickness_measurement_content_desc));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_input_thickness_sub_title, R.string.a400_print_laser_input_thickness_content_desc));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_trouch_material_surface_title, R.string.a400_print_laser_touch_material_surface_content_desc));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_manual_focus_title, R.string.a400_print_laser_manual_focus_content_desc));
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_input_material_thickness_sub_title, R.string.a400_print_laser_input_material_thickness_content_desc));
                    items.add(new CalibrationModeSelectionItem(R.string.a400_print_laser_focus_lever_sub_title, R.string.a400_print_laser_focus_lever_content_desc));
                    break;
                default:
                    break;
            }
        }
        return items;
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_laser_setz_selection;
    }

    @OnClick(R2.id.tv_air_purifier_error_power_off_calibration_right_select_exit)
    public void onClickExit() {
        playNormalClickSound();
        onExit();
    }

    @OnClick(R2.id.tv_empty)
    public void onClickBg() {
        onExit();
    }

    private void onExit() {
        if (getActivity() != null) {
            Animation animRightOut = AnimationUtils.loadAnimation(requireContext(),
                    R.anim.push_out_right);
            mCLRightSelect.startAnimation(animRightOut);
            getActivity().finish();
        }
    }
}
