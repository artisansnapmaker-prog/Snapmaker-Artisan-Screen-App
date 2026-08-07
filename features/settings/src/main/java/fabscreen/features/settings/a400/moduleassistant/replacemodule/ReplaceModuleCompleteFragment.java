package fabscreen.features.settings.a400.moduleassistant.replacemodule;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.view.BaseFragment;

public class ReplaceModuleCompleteFragment extends BaseFragment {

    @BindView(R2.id.tv_desc)
    TextView mTvDesc;
    @BindView(R2.id.sv_need_calibration)
    ScrollView mSvNeedCalibration;
    @BindView(R2.id.ll_need_calibration)
    LinearLayout mLlNeedCalibration;
    @BindView(R2.id.btn_calibrate)
    Button mBtnCalibrate;
    @BindView(R2.id.btn_skip)
    Button mBtnSkip;

    private ReplaceModuleViewModel mViewModel;

    public static Fragment newInstance() {
        return new ReplaceModuleCompleteFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceModuleViewModel.class);
        initView();
        playProcedureCompleteSound();
    }

    private void initView() {
        boolean needCalibration = mViewModel.needCalibrate();
        mSvNeedCalibration.setVisibility(needCalibration ? View.VISIBLE : View.INVISIBLE);
        mBtnCalibrate.setText(needCalibration ? getString(R.string.replace_module_calibrate_module) : getString(R.string.replace_module_home_screen_title));
        mBtnSkip.setText(needCalibration ? getString(R.string.all_skip_calibration) : getString(R.string.a400_settings_module_assist_title));
        mTvDesc.setText(needCalibration ? R.string.replace_module_need_calibration_desc : R.string.replace_module_complete_desc);
        if (mViewModel.needCalibrate()) {
            for (String name : mViewModel.getNeedCalibrateModuleList()) {
                TextView textView = new TextView(requireContext());
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                textView.setTextColor(Color.WHITE);
                textView.setText(name);
                textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), R.drawable.shape_a400_module_indicator, null), null, null, null);
                textView.setCompoundDrawablePadding((int) DimensUtils.dp2px(12));
                textView.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = 15;
                mLlNeedCalibration.addView(textView, lp);
            }
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_module_complete;
    }

    @OnClick({R2.id.btn_calibrate, R2.id.btn_skip})
    @Override
    public void onClick(View view) {
        super.onClick(view);
        playSwitchSound();
        int id = view.getId();
        if (id == R.id.btn_calibrate) {
            if (mViewModel.needCalibrate()) {
                mRouter.routeToCalibrationPage().start(requireContext());
                requireActivity().finish();
            } else {
                mRouter.backHome().start(requireContext());
            }
        } else if (id == R.id.btn_skip) {
            back();
        }
    }
}
