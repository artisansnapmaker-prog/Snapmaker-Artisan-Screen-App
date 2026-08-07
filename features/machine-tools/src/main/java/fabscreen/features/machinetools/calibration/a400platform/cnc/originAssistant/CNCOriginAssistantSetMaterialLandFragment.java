package fabscreen.features.machinetools.calibration.a400platform.cnc.originAssistant;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.platform.base.Constants;
import fabscreen.platform.base.helper.EditTextHelper;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CNCOriginAssistantSetMaterialLandFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.et_cnc_origin_assistant_set_material_diameter)
    EditText mEtWorkpieceDiameter;
    @BindView(R2.id.et_cnc_origin_assistant_set_material_length)
    EditText mEtWorkpieceLength;
    @BindView(R2.id.tv_cnc_origin_assistant_set_material_diameter_tip)
    TextView mTvDiameterTip;
    @BindView(R2.id.tv_cnc_origin_assistant_set_material_length_tip)
    TextView mTvLengthTip;
    @BindView(R2.id.btn_cnc_origin_assistant_set_material_next)
    Button mBtnNext;
    @BindView(R2.id.view_guide_progress_bar)
    LinearProgressIndicator mGuideProgressBar;
    @BindView(R2.id.iv_origin_assistant_set_material)
    ImageView mIvPic;
    private A400CNCOriginAssistantViewModel mViewModel;

    private CustomKeyboardUtil mCustomKeyboardUtil;

    public static CNCOriginAssistantSetMaterialLandFragment newInstance() {
        return new CNCOriginAssistantSetMaterialLandFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();

        if (getActivity() != null) {
//            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_origin_assistant_set_materia_landl;
    }

    @Override
    protected A400CNCOriginAssistantViewModel getViewModel() {
        return getViewModelProvider().get(A400CNCOriginAssistantViewModel.class);
    }


    private void hideKeyboard() {
        if (getView() == null) return;
        if (getContext() == null) return;

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isActive()) {
            imm.hideSoftInputFromWindow(getView().getApplicationWindowToken(), 0);
        }
    }

    private void initView() {
        // We need to disabled next button first before user start inputting.
        mBtnNext.setEnabled(false);
        mGuideProgressBar.setMax(9);
        mGuideProgressBar.setProgress(1);
        setTitle(R.string.calibration_cnc_origin_assistant);
        setContent(R.string.a400_cnc_origin_set_material_title);

        initKeyboardBinding();
        initEditText();


        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_cnc_origin_assistant_material_settings_360x240)
                .apply(options)
                .into(mIvPic);
        // Skip the first value.
        mViewModel.getWorkpieceDiameterTipObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    switch (tip) {
                        case TIP_OK:
                            mTvDiameterTip.setVisibility(TextView.GONE);
                            mTvDiameterTip.setText("");
                            mEtWorkpieceDiameter.setBackgroundResource(R.drawable.bg_snap_black_round);
                            break;
                        case TIP_NOT_POSITIVE_NUMBER:
                            mTvDiameterTip.setVisibility(TextView.VISIBLE);
                            mTvDiameterTip.setText(R.string.cnc_origin_assistant_input_tip_not_positive);
                            mEtWorkpieceDiameter.setBackgroundResource(R.drawable.bg_set_material_error);
                            break;
                        case TIP_EMPTY:
                            mTvDiameterTip.setVisibility(TextView.VISIBLE);
                            mTvDiameterTip.setText(R.string.cnc_origin_assistant_input_tip_empty);
                            mEtWorkpieceDiameter.setBackgroundResource(R.drawable.bg_set_material_error);
                            break;
                        default:
                            break;
                    }
                });

        // Skip the first value.
        mViewModel.getWorkpieceLengthTipObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    switch (tip) {
                        case TIP_OK:
                            mTvLengthTip.setVisibility(TextView.GONE);
                            mTvLengthTip.setText("");
                            mEtWorkpieceLength.setBackgroundResource(R.drawable.bg_snap_black_round);
                            break;
                        case TIP_NOT_POSITIVE_NUMBER:
                            mTvLengthTip.setVisibility(TextView.VISIBLE);
                            mTvLengthTip.setText(R.string.cnc_origin_assistant_input_tip_not_positive);
                            mEtWorkpieceLength.setBackgroundResource(R.drawable.bg_set_material_error);
                            break;
                        case TIP_EMPTY:
                            mTvLengthTip.setVisibility(TextView.VISIBLE);
                            mTvLengthTip.setText(R.string.cnc_origin_assistant_input_tip_empty);
                            mEtWorkpieceLength.setBackgroundResource(R.drawable.bg_set_material_error);
                            break;
                        default:
                            break;
                    }
                });

        mViewModel.getMaterialInputReady()
                .debounce(200, Constants.TIME_UNIT)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isReady -> mBtnNext.setEnabled(isReady));
    }


    private void initKeyboardBinding() {
        mCustomKeyboardUtil = new CustomKeyboardUtil(requireContext());

        // Binding Diameter EditText
        mCustomKeyboardUtil.bindKeyboardListener(mEtWorkpieceDiameter, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = EditTextHelper.fixNumberInputSinglePoint(s).toString();
                mViewModel.setWorkpieceDiameterInput(input);
                mEtWorkpieceDiameter.setText(s);
            }
        });

        // Binding Length EditText
        mCustomKeyboardUtil.bindKeyboardListener(mEtWorkpieceLength, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = EditTextHelper.fixNumberInputSinglePoint(s).toString();
                mViewModel.setWorkpieceLengthInput(input);
                mEtWorkpieceLength.setText(s);
            }
        });
    }


    private void initEditText() {
        // Limit the length of input.
        mEtWorkpieceLength.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        mEtWorkpieceDiameter.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

        if (getViewModel().getWorkpieceDiameter() > 0) {
            mEtWorkpieceDiameter.setText(String.valueOf(getViewModel().getWorkpieceDiameter()));
        }

        if (getViewModel().getWorkpieceLength() > 0) {
            mEtWorkpieceLength.setText(String.valueOf(getViewModel().getWorkpieceLength()));
        }

        mEtWorkpieceDiameter.setShowSoftInputOnFocus(false);

        mEtWorkpieceLength.setShowSoftInputOnFocus(false);
    }

    @OnClick(R2.id.btn_cnc_origin_assistant_set_material_next)
    void onClickNext() {
        playNormalClickSound();
        if (getActivity() != null) {
            ((CncOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantInstallMaterialFragment();
        }
    }

    @OnClick(R2.id.et_cnc_origin_assistant_set_material_diameter)
    void onClickDiameter() {
        playNormalClickSound();
        mCustomKeyboardUtil.hideSystemKeyBoard();
        mCustomKeyboardUtil.setPreInputText(mEtWorkpieceDiameter.getText().toString());
        mCustomKeyboardUtil.showKeyboard(mEtWorkpieceDiameter, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_CLASS_NUMBER);
    }

    @OnClick(R2.id.et_cnc_origin_assistant_set_material_length)
    void onTouchLength() {
        playNormalClickSound();
        mCustomKeyboardUtil.hideSystemKeyBoard();
        mCustomKeyboardUtil.setPreInputText(mEtWorkpieceLength.getText().toString());
        mCustomKeyboardUtil.showKeyboard(mEtWorkpieceLength, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
        mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_CLASS_NUMBER);
    }
}
