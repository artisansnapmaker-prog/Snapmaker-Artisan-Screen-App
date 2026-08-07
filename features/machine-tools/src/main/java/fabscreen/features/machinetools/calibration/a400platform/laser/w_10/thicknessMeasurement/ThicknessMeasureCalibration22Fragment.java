package fabscreen.features.machinetools.calibration.a400platform.laser.w_10.thicknessMeasurement;

import static fabscreen.platform.core.ui.common.Laser10wThicknessCalibrationViewModel.FIRST_CAPTURE;
import static fabscreen.platform.core.ui.common.Laser10wThicknessCalibrationViewModel.MEASURE_CAPTURE;
import static fabscreen.platform.core.ui.common.Laser10wThicknessCalibrationViewModel.SECOND_CAPTURE;

import android.os.Bundle;
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
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.common.CalibrationCaptureResult;
import fabscreen.platform.core.ui.common.Laser10wThicknessCalibrationViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ThicknessMeasureCalibration22Fragment extends A400CalibrationBaseFragment {

    @BindView(R2.id.auto_thickness_measure_calibration_22_content)
    TextView mTvPointCount;
    @BindView(R2.id.auto_thickness_measure_calibration_22_image)
    ImageView mIvImage;
    private Laser10wThicknessCalibrationViewModel mViewModel;

    public static Fragment newInstance() {
        return new ThicknessMeasureCalibration22Fragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        moveAndUpdateView(FIRST_CAPTURE);
    }

    private void initView() {
        setTitle(R.string.a400_calibration_thickness_measure_4_1_title);
        mTvTopBarContent.setText(R.string.a400_calibration_thickness_measure_4_1_content);
        mTvPointCount.setText(R.string.a400_calibration_thickness_measure_calibrating);
        mGuideProgressBar.setMax(3);
        mGuideProgressBar.setProgress(3);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_laser_thickness_calibration_auto_calibration_01)
                .apply(options)
                .into(mIvImage);
    }

    private void moveAndUpdateView(int which) {
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        switch (which) {
            case FIRST_CAPTURE:
                Glide.with(requireContext())
                        .load(R.drawable.pic_laser_thickness_calibration_auto_calibration_01)
                        .apply(options)
                        .into(mIvImage);
                mTvPointCount.setText(R.string.a400_calibration_thickness_measure_calibrating);
                break;
            case SECOND_CAPTURE:
                Glide.with(requireContext())
                        .load(R.drawable.pic_laser_thickness_calibration_auto_calibration_02)
                        .apply(options)
                        .into(mIvImage);
                mTvPointCount.setText(R.string.a400_calibration_thickness_measure_calibrating);
                break;
            case MEASURE_CAPTURE:
                mTvPointCount.setText(R.string.a400_calibration_thickness_measure_verifying);
                break;
            default:
                break;
        }
        mViewModel.moveCameraPosition(which)
                .flatMap(aBoolean -> aBoolean ? mViewModel.takePhoto(which) : Observable.just(new CalibrationCaptureResult()))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::onGetCaptureResult, e -> {
                    LogHelper.log(e);
                    showFailDialog();
                });
    }

    private void onGetCaptureResult(CalibrationCaptureResult result) {
        Logger.d("result >>" + result);
        if (result.isSuccess) {
            switch (result.which) {
                case FIRST_CAPTURE:
                case SECOND_CAPTURE:
                    moveAndUpdateView(result.which + 1);
                    break;
                case MEASURE_CAPTURE:
                    mViewModel.switchAFAssistLight(false);
                    mViewModel.exitCalibration();
                    mViewModel.setExposeTime(0);
                    finishActivityWithResultOk();
                    break;
                default:
                    break;
            }
        } else {
            showFailDialog();
        }

    }

    private void showFailDialog() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setType(DecisionDialog.ERROR_TYPE)
                .setPic(R.drawable.ic_tips_error_dark)
                .setTitle(R.string.a400_calibration_thickness_measure_failed)
                .setContent(R.string.a400_calibration_thickness_measure_failed_content)
                .setFirstTv(getActivity().getResources().getString(R.string.all_quit), R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.switchAFAssistLight(false);
                    mViewModel.exitCalibration();
                    mViewModel.setExposeTime(0);
                    requireActivity().finish();
                })
                .setSecondTv(getActivity().getResources().getString(R.string.all_retry), R.color.select_dialog_red_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.switchAFAssistLight(true);
                    moveAndUpdateView(FIRST_CAPTURE);
                })
                .show();
    }

    @Override
    protected Laser10wThicknessCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(Laser10wThicknessCalibrationViewModel.class);
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_thickness_measure_calibration_41;
    }

}
