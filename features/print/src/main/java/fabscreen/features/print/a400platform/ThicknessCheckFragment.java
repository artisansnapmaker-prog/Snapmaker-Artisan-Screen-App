package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.features.print.a400platform.viewmodel.PrintReadyViewModel;
import fabscreen.platform.base.helper.EditTextHelper;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;

public class ThicknessCheckFragment extends BaseFragment {

    @BindView(R2.id.btn_thickness)
    Button mBtnThickness;
    @BindView(R2.id.tv_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_desc)
    TextView mTvDesc;
    @BindView(R2.id.tv_sub_title)
    TextView mTvSubTitle;
    @BindView(R2.id.iv_main_pic)
    ImageView mIvMainPic;

    private PrintReadyViewModel mViewModel;
    private CustomKeyboardUtil mCustomKeyboardUtil;

    public static Fragment newInstance() {
        return new ThicknessCheckFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
    }

    private void initView() {
        switch (mViewModel.getHeadType()) {
            case Module.ModuleType.HEAD_LASER_10W:
                mBtnThickness.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_4axis_input_material_diameter_title : R.string.a400_print_laser_input_material_thickness_input_hint);
                mTvSubTitle.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_4axis_material_diameter_subtitle : R.string.a400_print_laser_input_material_thickness_subtitle);
                mTvTitle.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_4axis_input_material_diameter_title : R.string.a400_print_laser_input_material_thickness_title);
                mTvDesc.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_4axis_material_diameter_content : R.string.a400_print_laser_input_material_thickness_message_desc);
                break;
            case Module.ModuleType.HEAD_LASER_2W_INFRARED:
            case Module.ModuleType.HEAD_LASER_20W:
            case Module.ModuleType.HEAD_LASER_40W:
                mBtnThickness.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_4axis_input_material_diameter_title : R.string.a400_print_laser_input_material_thickness_input_hint);
                mTvSubTitle.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_4axis_material_diameter_subtitle : R.string.a400_print_laser_input_material_thickness_subtitle);
                mTvTitle.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_4axis_input_material_diameter_title : R.string.a400_print_laser_input_material_thickness_title);
                mTvDesc.setText(mViewModel.getIsRotaryAvailable() ? R.string.a400_print_laser_40w_4axis_material_diameter_content : R.string.a400_print_laser_input_material_thickness_message_desc);
                break;
            case Module.ModuleType.HEAD_LASER:
            default:
                break;
        }
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(mViewModel.getIsRotaryAvailable() ? R.drawable.pic_a400_laser_four_axis_input_thickness : R.drawable.pic_a400_laser_input_thickness)
                .apply(options)
                .into(mIvMainPic);

        mCustomKeyboardUtil = new CustomKeyboardUtil(requireContext());
        mCustomKeyboardUtil.bindKeyboardListener(mBtnThickness, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.isEmpty()) {
                    input = EditTextHelper.fixNumberInputSinglePoint(s).toString();
                    if (input.length() > 8) {
                        input = input.substring(0, 7);
                    }
                    float value;
                    try {
                        value = getInputValue(Float.parseFloat(input),
                                mViewModel.getIsRotaryAvailable() ? 1f : 0.01f,
                                mViewModel.getIsRotaryAvailable() ? 300f : 400f);
                    } catch (Exception e) {
                        value = mViewModel.getIsRotaryAvailable() ? 1f : 0.01f;
                    }
                    mBtnThickness.setTextColor(ContextCompat.getColor(requireContext(), R.color.palette_white_pure));
                    mBtnThickness.setText(value + "");
                    mViewModel.saveMaterialThickness(value);
                }
            }
        });

        float initValue = mViewModel.getMaterialThicknessValue();
        if (initValue >= 0) {
            mBtnThickness.setTextColor(ContextCompat.getColor(requireContext(), R.color.palette_white_pure));
            mBtnThickness.setText(initValue + "");
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_thickness_check;
    }

    @Override
    protected PrintReadyViewModel getViewModel() {
        return getViewModelProvider().get(PrintReadyViewModel.class);
    }

    public float getInputValue(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    @OnClick(R2.id.btn_thickness)
    void onThinkClick() {
        playSwitchSound();
        mCustomKeyboardUtil.showKeyboard(mBtnThickness, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
    }
}
