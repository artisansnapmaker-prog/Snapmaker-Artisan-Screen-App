package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6.rotary;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
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
import fabscreen.platform.base.helper.EditTextHelper;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CentralAxisInputDiameterFragment extends A400CalibrationBaseFragment {
    public static Fragment newInstance() {
        return new CentralAxisInputDiameterFragment();
    }

    @BindView(R2.id.iv_calibration_desc)
    ImageView mIvPic;
    @BindView(R2.id.et_diameter)
    TextView mEtDiameter;
    @BindView(R2.id.tv_calibration_desc_content)
    TextView mTvContent;
    CalibrationCentralAxisViewModel mViewModel;
    private IPreferences.Helper helper;
    private long mMachineSN;
    private CustomKeyboardUtil mCustomKeyboardUtil;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(CalibrationCentralAxisViewModel.class);
        helper = getServiceContainer().getService(IPreferences.class).getHelper();
        mCustomKeyboardUtil = new CustomKeyboardUtil(requireContext());
        initView();
    }

    private void initView() {
        setTitle(R.string.a400_central_axis_calibration);
        mTvTopBarContent.setText(R.string.a400_set_material_diameter_1_2);
        mGuideProgressBar.setMax(2);
        mGuideProgressBar.setProgress(1);
        mMachineSN = helper.getA400MachineSn();
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_laser_central_axis_calibration_cylinder_material_set_material_diameter_479x359)
                .apply(options)
                .into(mIvPic);

        switch (mViewModel.getMachineToolHead()) {
            case Module.ModuleType.HEAD_LASER:
                break;
            case Module.ModuleType.HEAD_LASER_10W:
                if (helper.getA400MachineStep(mMachineSN) == 0) {
                    mTvContent.setText(R.string.a400_four_axis_calibration_central_axis_content);
                } else {
                    mTvContent.setText(R.string.a400_calibration_4axis_central_axis_content);
                }
                break;
            case Module.ModuleType.HEAD_LASER_2W_INFRARED:
            case Module.ModuleType.HEAD_LASER_20W:
            case Module.ModuleType.HEAD_LASER_40W:
                mTvContent.setText(R.string.a400_calibration_4axis_central_axis_content);
                break;
            default:
                break;
        }

        mCustomKeyboardUtil.bindKeyboardListener(mEtDiameter, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                float value;
                if (!TextUtils.isEmpty(input)) {
                    try {
                        input = EditTextHelper.fixNumberInputSinglePoint(s).toString();
                        if (input.length() > 8) {
                            input = input.substring(0, 7);
                        }
                        value = getInputValue(Float.parseFloat(input), 1, 300);
                    } catch (Exception e) {
                        value = 1;
                    }
                    mEtDiameter.setText(String.valueOf(value));
                }

            }
        });

        mEtDiameter.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(mEtDiameter.getText()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
        });
    }

    public float getInputValue(float value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_calibration_central_axis;
    }

    @OnClick(R2.id.btn_next)
    void onNextClicked() {
        playNormalClickSound();
        mViewModel.setMaterialDiameter(Float.parseFloat(mEtDiameter.getText().toString().trim()));
        ((CalibrationCentralAxisActivity) requireActivity()).goToTouchMaterial();
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
////                                if (ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().modelId ==IMachine.Product.A400){
//                                    MoveController.getInstance().stepToPosition(MoveController.Direction.UP, 100);
////                                }
                                requireActivity().setResult(Activity.RESULT_CANCELED);
                                requireActivity().finish();
                            }, LogHelper::log);
                }));
        fabBackConfirm.show();
    }

}
