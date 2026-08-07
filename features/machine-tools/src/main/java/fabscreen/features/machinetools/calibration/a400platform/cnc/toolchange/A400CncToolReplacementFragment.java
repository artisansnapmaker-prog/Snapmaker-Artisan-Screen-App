package fabscreen.features.machinetools.calibration.a400platform.cnc.toolchange;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.features.machinetools.calibration.a400platform.cnc.manualTool.A400CncManualToolViewModel;

public class A400CncToolReplacementFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.fragment_calibration_image)
    ImageView mIvImage;
    @BindView(R2.id.fragment_calibration_title)
    TextView mTvTitle;
    @BindView(R2.id.fragment_calibration_content)
    TextView mTvContent;
    @BindView(R2.id.bt_a400_calibration_submit)
    Button mBtnCalibration;

    private A400CncManualToolViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400CncToolReplacementFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(A400CncManualToolViewModel.class);
        initView();
    }

    private void initView() {
        setTitle(R.string.a400_calibration_cnc_tool_change);
        mTvTopBarContent.setText(R.string.calibration_cnc_tool_change_second_steps);
//        mTvTitle.setText("更换刀具");
        mTvContent.setText(R.string.a400_change_tool_message);
        mBtnCalibration.setText(R.string.all_next);
        mGuideProgressBar.setMax(3);
        mGuideProgressBar.setProgress(2);
        mGuideProgressBar.setVisibility(View.VISIBLE);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));

        if (mViewModel.is200wCnc()) {
            Glide.with(requireContext())
                    .load(mViewModel.isFourAxis() ?
                            R.drawable.pic_cnc_200w_four_axis_bit_assistant_tchange_bit
                            : R.drawable.pic_cnc_200w_bit_assistant_tchange_bit
                    )
                    .apply(options)
                    .into(mIvImage);
        } else {
            Glide.with(requireContext())
                    .load(mViewModel.isFourAxis() ?
                            R.drawable.pic_cnc_50w_four_axis_bit_assistant_tchange_bit
                            : R.drawable.pic_cnc_50w_bit_assistant_tchange_bit
                    )
                    .apply(options)
                    .into(mIvImage);
        }
    }

    @OnClick(R2.id.bt_a400_calibration_submit)
    public void onClickSubmit() {
        playNormalClickSound();
        ((A400CncToolChangeAssistantActivity) requireActivity()).gotoSetZ();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_cnc_manual_tool_replacement;
    }

}
