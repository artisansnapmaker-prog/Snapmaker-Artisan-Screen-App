package fabscreen.features.machinetools.calibration.a400platform.laser.w_1_6;

import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import fabscreen.features.machinetools.R;
import fabscreen.platform.core.ui.view.ViewUtils;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 1.6W laser 3 or 4 axis Touch Material to get material surface "height"
 */
public class CalibrationTouchMaterialFragment extends CalibrationJogFragment {
    public static Fragment newInstance() {
        return new CalibrationTouchMaterialFragment();
    }

    @Override
    protected void initView() {
        super.initView();
        mViewModel.startCalibration();
        setMainTitle(getString(R.string.calibration_manual_focus_calibration_title));
        setSubTitle(getString(getViewModel().isRotaryAvailable() ? R.string.calibration_manual_focus_calibration_one_four_axis_subtitle :
                R.string.calibration_manual_focus_calibration_one_subtitle));

        mTvCalibrationDescTitle.setText(getViewModel().isRotaryAvailable() ? R.string.calibration_manual_focus_calibration_one_four_axi_contents_title :
                R.string.calibration_manual_focus_calibration_one_content_title);

        mTvCalibrationDescContent.setText(getViewModel().isRotaryAvailable() ? R.string.calibration_manual_focus_calibration_one_four_axis_content :
                R.string.calibration_manual_focus_calibration_one_content);
        setProgress(1, 4);
        mBtnRunBoundary.setVisibility(View.GONE);

        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(getViewModel().isRotaryAvailable() ? R.drawable.pic_laser_manual_focus_calibration_0_5mm_pitch_touch_material_four_axis :
                        R.drawable.pic_laser_manual_focus_calibration_0_5mm_pitch_touch_material)
                .apply(options)
                .into(mIvCalibrationDesc);

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> ViewUtils.enableButtons((ViewGroup) requireView(), !isMoving), LogHelper::log);
    }

    @Override
    protected void goNext() {
        mViewModel.saveMaterialSurfaceZ()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    // Material surface z saved.
                    ((A400LaserManualFocusCalibrationActivity) requireActivity()).goToSetOrigin();
                }, LogHelper::log);
    }
}
