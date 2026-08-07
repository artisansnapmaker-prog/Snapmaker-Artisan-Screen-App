package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.BaseCalibrationProgressFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CalibrationEngravingFragment extends BaseCalibrationProgressFragment {
    @BindView(R2.id.iv_engrave_pic)
    ImageView mIvPic;

    @BindView(R2.id.iv_help)
    ImageView mIvHelp;

    public static Fragment newInstance() {
        return new CalibrationEngravingFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        setMainTitle(getString(R.string.calibration_manual_focus_calibration_title));
        setSubTitle(getString(R.string.calibration_manual_focus_calibration_three_subtitle));
        mIvHelp.setVisibility(View.GONE);
        setProgress(3, 4);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(getViewModel().isRotaryAvailable() ? R.drawable.pic_laser_manual_focus_calibration_0_5mm_pitch_engrave_calibration_pattern_four_axis
                        : R.drawable.pic_laser_manual_focus_calibration_0_5mm_pitch_engrave_calibration_pattern)
                .apply(options)
                .into(mIvPic);
        getViewModel().doEngraving()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success ->
                                ((A400LaserManualFocusCalibrationActivity) requireActivity()).goToChooseLine()
                        , LogHelper::log);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_manual_focus_calibration_engraving;
    }

    @Override
    protected A400LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(A400LaserCalibrationViewModel.class);
    }
}
