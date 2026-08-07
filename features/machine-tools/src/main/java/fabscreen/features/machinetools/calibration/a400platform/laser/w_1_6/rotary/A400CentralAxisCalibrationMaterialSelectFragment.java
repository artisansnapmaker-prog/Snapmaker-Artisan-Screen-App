package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6.rotary;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.common.VerticalSpaceItemDecoration;
import fabscreen.platform.core.ui.common.selection.CalibrationModeSelectionAdapter;
import fabscreen.platform.core.ui.common.selection.CalibrationModeSelectionItem;

public class A400CentralAxisCalibrationMaterialSelectFragment extends BaseFragment {
    protected final List<CalibrationModeSelectionItem> mSectionItems = new ArrayList<>();
    protected List<CalibrationModeSelectionItem> items = new ArrayList<>();
    protected IPreferences.Helper helper;
    @BindView(R2.id.rv_sections)
    RecyclerView mRvSections;
    @BindView(R2.id.li_calibration_right_select)
    LinearLayout mlySections;
    @BindView(R2.id.ly_calibration_right_select)
    ConstraintLayout mCLRightSelect;
    @BindView(R2.id.textView)
    TextView mTvTitle;

    public static Fragment newInstance() {
        return new A400CentralAxisCalibrationMaterialSelectFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        helper = getServiceContainer().getService(IPreferences.class).getHelper();
        initView();
    }

    private void initView() {
        Animation animRightIn = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_in_right);
        mTvTitle.setText(R.string.a400_choose_a_material);
        mCLRightSelect.startAnimation(animRightIn);
        if (mSectionItems.size() > 0) {
            mSectionItems.clear();
            items.clear();
        }
        mlySections.setVisibility(View.GONE);
        mSectionItems.addAll(getSections());
        mRvSections.setLayoutManager(new LinearLayoutManager(requireContext()));
        CalibrationModeSelectionAdapter adapter = new CalibrationModeSelectionAdapter(mSectionItems);
        mRvSections.setAdapter(adapter);
        VerticalSpaceItemDecoration decoration = new VerticalSpaceItemDecoration(DimensUtils.dp2px(12));
        mRvSections.addItemDecoration(decoration);
        adapter.setSelection(helper.getA400CentralAxisCalibrationMaterialType());
        adapter.setOnSectionSelectedListener(this::onSectionSelected);
    }


    private void onSectionSelected(int position) {
        //save Perent
        playNormalClickSound();
        helper.setA400CentralAxisCalibrationMaterialType(position);
        onExit();
    }

    private List<CalibrationModeSelectionItem> getSections() {
        items.add(new CalibrationModeSelectionItem(R.string.a400_laser_central_axis_calibration_material_cylinder_material, 0));
        return items;
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_calibration_leveling_z_selection;
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