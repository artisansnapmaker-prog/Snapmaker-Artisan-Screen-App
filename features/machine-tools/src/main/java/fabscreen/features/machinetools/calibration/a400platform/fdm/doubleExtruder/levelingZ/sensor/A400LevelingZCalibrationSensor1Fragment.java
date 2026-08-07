package fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingZ.sensor;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.features.machinetools.calibration.a400platform.fdm.doubleExtruder.levelingZ.A400LevelingZViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400LevelingZCalibrationSensor1Fragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.iv_leveling_z_ico)
    ImageView mIvIco;
    @BindView(R2.id.tv_leveling_z_content)
    TextView mTvContent;
    @BindView(R2.id.top_bar_back)
    Button mBtnBack;
    private A400LevelingZViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400LevelingZCalibrationSensor1Fragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        mViewModel.checkHome()
                .flatMap(aBoolean -> mViewModel.setCalibrationMode(54))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        mViewModel.A400LevelingZSensorCalibration(0);
                    } else {
                        requireActivity().finish();
                    }
                }, LogHelper::log);
    }

    private void initView() {
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        mGuideProgressBar.setMax(4);
        mGuideProgressBar.setVisibility(View.VISIBLE);
        setTitle(R.string.calibration_Z_offset_calibration_title);
        mBtnBack.setVisibility(View.GONE);
        mTvTopBarContent.setText(getString(R.string.a400_calibration_z_leveling_sensor_height, getString(R.string.a400_left), 1, 4));
        mIvIco.setImageResource(R.drawable.pic_leveling_z_left);
        mTvContent.setText(R.string.a400_calibration_z_leveling_auto_left);
        mGuideProgressBar.setProgress(1);
        mGuideProgressBar.invalidate();

        mViewModel.getResultObservable()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    switch (result) {
                        case 0:
                            setTitle(R.string.calibration_Z_offset_calibration_title);
                            mIvIco.setImageResource(R.drawable.pic_leveling_z_right);
                            mTvContent.setText(R.string.a400_calibration_z_leveling_auto_right);
                            mTvTopBarContent.setText(getString(R.string.a400_calibration_z_leveling_sensor_height, getString(R.string.a400_right), 2, 4));
                            mGuideProgressBar.setProgress(2);
                            mGuideProgressBar.invalidate();
                            mViewModel.A400LevelingZSensorCalibration(1);
                            break;
                        case 1:
                            if (getActivity() == null) return;
                            ((A400LevelingZCalibrationSensorActivity) requireActivity()).initialHeightCalibration();
                            break;
                        default:
                            break;
                    }
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_leveling_z_calibration_1;
    }

    @Override
    protected A400LevelingZViewModel getViewModel() {
        return getViewModelProvider().get(A400LevelingZViewModel.class);
    }


}
