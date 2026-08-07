package fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY;

import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_ABS;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_CUSTOM;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_PETG;
import static fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingXY.A400LevelingXYCalibrationInfoFragment.A400_LEVELING_XY_CALIBRATION_PLA;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import fabscreen.platform.base.helper.StringToValueUtils;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.common.VerticalSpaceItemDecoration;
import fabscreen.platform.core.ui.common.selection.CalibrationModeSelectionAdapter;
import fabscreen.platform.core.ui.common.selection.CalibrationModeSelectionItem;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;

public class A400CalibrationLevelingXYSelectionFragment extends BaseFragment {
    protected final List<CalibrationModeSelectionItem> mSectionItems = new ArrayList<>();
    protected List<CalibrationModeSelectionItem> items = new ArrayList<>();
    protected IPreferences.Helper helper;
    @BindView(R2.id.rv_sections)
    RecyclerView mRvSections;
    @BindView(R2.id.li_calibration_right_select)
    LinearLayout mlySections;

    @BindView(R2.id.tv_a400_calibration_leveling_election_title)
    TextView mTvXYCalibrationSelectionTitle;

    @BindView(R2.id.rl_calibration_leveling_xy_selection_left_printing_temperature)
    RelativeLayout mRlLeftPrintingTemperature;
    @BindView(R2.id.et_calibration_leveling_xy_selection_left_printing_temperature_title)
    TextView mEdLeftPrintingTemperature;
    int mLeftPrintingTemperature;
    @BindView(R2.id.rl_calibration_leveling_xy_selection_right_printing_temperature)
    RelativeLayout mRlRightPrintingTemperature;
    @BindView(R2.id.et_calibration_leveling_xy_selection_right_printing_temperature)
    TextView mEdRightPrintingTemperature;
    int mRightPrintingTemperature;
    @BindView(R2.id.rl_calibration_leveling_xy_selection_left_standby_temperature)
    RelativeLayout mRlLeftStandbyTemperature;
    @BindView(R2.id.et_calibration_leveling_xy_selection_left_standby_temperature)
    EditText mEdLeftStandbyTemperature;
    int mLeftStandbyTemperature;
    @BindView(R2.id.rl_calibration_leveling_xy_selection_right_standby_temperature)
    RelativeLayout mRlRightStandbyTemperature;
    @BindView(R2.id.et_calibration_leveling_xy_selection_right_standby_temperature_)
    EditText mEdRightStandbyTemperature;
    int mRightStandbyTemperature;
    @BindView(R2.id.rl_calibration_leveling_xy_selection_bed_printing_temperature)
    RelativeLayout mRlBedPrintingTemperature;
    @BindView(R2.id.et_calibration_leveling_xy_selection_bed_printing_temperature)
    TextView mEdBedPrintingTemperature;
    int mBedPrintingTemperature;

    @BindView(R2.id.cl_left_content)
    ConstraintLayout mClLeftContent;
    @BindView(R2.id.ly_calibration_right_select)
    ConstraintLayout mCLRightSelect;
    @BindView(R2.id.textView)
    TextView mTvTitle;

    private int mCurrentPosition;
    private CustomKeyboardUtil mCustomKeyboardUtil;

    public static Fragment newInstance() {
        return new A400CalibrationLevelingXYSelectionFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        helper = getServiceContainer().getService(IPreferences.class).getHelper();
        mCustomKeyboardUtil = new CustomKeyboardUtil(requireContext());
        initView();
    }

    private void initView() {
        mTvTitle.setText(R.string.a400_choose_a_material);
        Animation animLeftIn = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_in_left);
        mClLeftContent.setAnimation(animLeftIn);

        Animation animRightIn = AnimationUtils.loadAnimation(requireContext(),
                R.anim.push_in_right);
        mCLRightSelect.startAnimation(animRightIn);

        if (mSectionItems.size() > 0) {
            mSectionItems.clear();
            items.clear();
        }
        mlySections.setVisibility(View.VISIBLE);
        mSectionItems.addAll(getSections());
        mRvSections.setLayoutManager(new LinearLayoutManager(requireContext()));
        CalibrationModeSelectionAdapter adapter = new CalibrationModeSelectionAdapter(mSectionItems);
        mRvSections.setAdapter(adapter);
        VerticalSpaceItemDecoration decoration = new VerticalSpaceItemDecoration(DimensUtils.dp2px(12));
        mRvSections.addItemDecoration(decoration);
        mCurrentPosition = helper.getA400BevelingXYMaterialSelection();
        adapter.setSelection(mCurrentPosition);
        onSectionSelected(mCurrentPosition);
        adapter.setOnSectionSelectedListener(position -> {
            playNormalClickSound();
            if (mCurrentPosition == position) {
                return;
            } else {
                mCurrentPosition = position;
            }
            onSectionSelected(position);

            Animation animRightOut = AnimationUtils.loadAnimation(requireContext(),
                    R.anim.push_out_left);
            animRightOut.setDuration(150);
            mClLeftContent.setAnimation(animRightOut);
            animRightOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Animation animLeftIn = AnimationUtils.loadAnimation(requireContext(),
                            R.anim.push_in_left);
                    animLeftIn.setDuration(150);
                    mClLeftContent.setAnimation(animLeftIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

        });
        mCustomKeyboardUtil.bindKeyboardListener(mEdBedPrintingTemperature, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int temperature = getInputValue(StringToValueUtils.parseInt(s.toString()), 0, 80);
                    mEdBedPrintingTemperature.setText("" + temperature);
                }

            }
        });
        mEdBedPrintingTemperature.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(mEdBedPrintingTemperature.getText()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });

        mCustomKeyboardUtil.bindKeyboardListener(mEdLeftPrintingTemperature, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int temperature = getInputValue(StringToValueUtils.parseInt(s.toString()), 0, 300);
                    mEdLeftPrintingTemperature.setText("" + temperature);
                }

            }
        });
        mEdLeftPrintingTemperature.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(mEdLeftPrintingTemperature.getText()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });

        mCustomKeyboardUtil.bindKeyboardListener(mEdRightPrintingTemperature, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int temperature = getInputValue(StringToValueUtils.parseInt(s.toString()), 0, 300);
                    mEdRightPrintingTemperature.setText("" + temperature);
                }

            }
        });
        mEdRightPrintingTemperature.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(mEdRightPrintingTemperature.getText()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });

    }


    private void onSectionSelected(int position) {
        //save Perent
        helper.setA400BevelingXYMaterialSelection(position);
        switch (position) {
            case A400_LEVELING_XY_CALIBRATION_PLA:
                mTvXYCalibrationSelectionTitle.setText(R.string.all_material_PLA);
                updateView(false, 210, 210, 150, 150, 60);
                break;
            case A400_LEVELING_XY_CALIBRATION_PETG:
                mTvXYCalibrationSelectionTitle.setText(R.string.all_material_PETG);
                updateView(false, 230, 230, 150, 150, 80);
                break;
            case A400_LEVELING_XY_CALIBRATION_ABS:
                mTvXYCalibrationSelectionTitle.setText(R.string.all_material_ABS);
                updateView(false, 235, 235, 150, 150, 80);
                break;
            case A400_LEVELING_XY_CALIBRATION_CUSTOM:
                mTvXYCalibrationSelectionTitle.setText(R.string.a400_calibration_xy_mode_custom);
                updateView(true,
                        helper.getA400LevelingXYCalibrationLeftPrintingTemperature(),
                        helper.getA400LevelingXYCalibrationRightPrintingTemperature(),
                        helper.getA400LevelingXYCalibrationLeftStandbyTemperature(),
                        helper.getA400LevelingXYCalibrationRightStandbyTemperature(),
                        helper.getA400LevelingXYCalibrationBedPrintingTemperature());
                break;
            default:
                break;
        }

    }

    private void updateView(boolean enable, int leftPrintingTemperature, int rightPrintingTemperature, int leftStandbyTemperature, int rightStandbyTemperature, int bedPrintingTemperature) {
        mEdLeftPrintingTemperature.setEnabled(enable);
        mEdRightPrintingTemperature.setEnabled(enable);
        mEdLeftStandbyTemperature.setEnabled(enable);
        mEdRightStandbyTemperature.setEnabled(enable);
        mEdBedPrintingTemperature.setEnabled(enable);

        mEdLeftPrintingTemperature.setText(leftPrintingTemperature + "");
        mEdRightPrintingTemperature.setText(rightPrintingTemperature + "");
        mEdLeftStandbyTemperature.setText(leftStandbyTemperature + "");
        mEdRightStandbyTemperature.setText(rightStandbyTemperature + "");
        mEdBedPrintingTemperature.setText(bedPrintingTemperature + "");
    }

    private List<CalibrationModeSelectionItem> getSections() {
        items.add(new CalibrationModeSelectionItem(R.string.all_material_PLA, 0));
        items.add(new CalibrationModeSelectionItem(R.string.all_material_PETG, 0));
        items.add(new CalibrationModeSelectionItem(R.string.all_material_ABS, 0));
        items.add(new CalibrationModeSelectionItem(R.string.all_material_CUSTOM, 0));
        return items;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_calibration_leveling_xy_selection;
    }

    @OnClick(R2.id.tv_air_purifier_error_power_off_calibration_right_select_exit)
    public void onClickExit() {
        playNormalClickSound();
        onExit();
    }

    @OnClick(R2.id.bt_calibration_right_select_cancel)
    public void onClickCancel() {
        playNormalClickSound();
        onExit();
    }

    @OnClick(R2.id.bt_calibration_right_select_determine)
    public void onClickDetermine() {
        playNormalClickSound();
        onDetermine();

        onExit();
    }


    private void onDetermine() {
        int a400LevelingBedCalibrationMode = helper.getA400BevelingXYMaterialSelection();
        if (a400LevelingBedCalibrationMode == A400_LEVELING_XY_CALIBRATION_CUSTOM) {
            helper.setA400LevelingXYCalibrationLeftPrintingTemperature(StringToValueUtils.parseInt(mEdLeftPrintingTemperature.getText().toString()));
            helper.setA400LevelingXYCalibrationRightPrintingTemperature(StringToValueUtils.parseInt(mEdRightPrintingTemperature.getText().toString()));
//            helper.setA400LevelingXYCalibrationLeftStandbyTemperature(StringToValueUtils.parseInt(mEdLeftStandbyTemperature.getText().toString()));
//            helper.setA400LevelingXYCalibrationRightStandbyTemperature(StringToValueUtils.parseInt(mEdRightStandbyTemperature.getText().toString()));
            helper.setA400LevelingXYCalibrationBedPrintingTemperature(StringToValueUtils.parseInt(mEdBedPrintingTemperature.getText().toString()));
        }
    }

    private void onExit() {
        if (getActivity() != null) {
            Animation animLeftOut = AnimationUtils.loadAnimation(requireContext(),
                    R.anim.push_out_left);
            mClLeftContent.setAnimation(animLeftOut);

            Animation animRightOut = AnimationUtils.loadAnimation(requireContext(),
                    R.anim.push_out_right);
            mCLRightSelect.startAnimation(animRightOut);
            getActivity().finish();
        }
    }

    public int getInputValue(int value, int min, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }
}
