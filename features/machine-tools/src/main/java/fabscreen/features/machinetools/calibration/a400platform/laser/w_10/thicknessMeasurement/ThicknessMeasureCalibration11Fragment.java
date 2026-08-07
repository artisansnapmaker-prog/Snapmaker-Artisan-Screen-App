package fabscreen.features.machinetools.calibration.a400platform.laser.w_10.thicknessMeasurement;

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
import fabscreen.features.machinetools.calibration.a400platform.laser.w_10.platformHeight.TouchPlatformViewModel;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400DirectionControlPanel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ThicknessMeasureCalibration11Fragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.cp_a400_calibration_move)
    A400DirectionControlPanel mCpMove;
    @BindView(R2.id.fragment_calibration_content)
    TextView mTvContent;
    @BindView(R2.id.fragment_calibration_title)
    TextView mTvTitle;
    private TouchPlatformViewModel mViewModel;
    private boolean isSelect = false;
    @BindView(R2.id.bt_a400_calibration_submit)
    Button mBtnSubmit;
    @BindView(R2.id.top_bar_back)
    Button mBtnBack;
    @BindView(R2.id.fragment_calibration_image)
    ImageView mIVImage;


    public static Fragment newInstance() {
        return new ThicknessMeasureCalibration11Fragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        mViewModel.checkHome()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        mViewModel.initToolheadPosition();
                    }
                });

    }

    private void initView() {
        setTitle(R.string.a400_calibration_thickness_measure_title);
        mTvTopBarContent.setText(R.string.a400_calibration_thickness_measure_content);
        mTvTitle.setText(R.string.a400_calibration_thickness_measure_second_title);
        mTvTitle.setVisibility(View.VISIBLE);
        mTvContent.setText(R.string.a400_calibration_platform_height_calibration_10w_content);
        mIvIco.setVisibility(View.GONE);
        mGuideProgressBar.setMax(3);
        mGuideProgressBar.setProgress(1);
        mGuideProgressBar.invalidate();
        mGuideProgressBar.setVisibility(View.VISIBLE);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_laser_thickness_calibration_measure_platform_height)
                .apply(options)
                .into(mIVImage);
        mCpMove.setOnDirectionClickListener(new A400DirectionControlPanel.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                playNormalClickSound();
                mViewModel.moveXYZByStep(direction, stepWidth);

            }

            @Override
            public void onPositionChange(int position) {
//                mViewModel.changeStepWidth(position);
            }

            @Override
            public void changPanel(int position) {

            }
        });
        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mCpMove.setEnabled(!isMoving);
                    mBtnBack.setEnabled(!isMoving);
                    mBtnSubmit.setEnabled(!isMoving);
                });

        mViewModel.getIsMachineMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    if (isMoving) {
                        if (requireActivity() instanceof ThicknessMeasureCalibrationActivity) {
                            ((ThicknessMeasureCalibrationActivity) requireActivity()).showDialog();
                        }
                    } else {
                        if (requireActivity() instanceof ThicknessMeasureCalibrationActivity) {
                            if (!isSelect) {
                                ((ThicknessMeasureCalibrationActivity) requireActivity()).dismissDialog();
                            }
                        }
                    }
                });
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_cnc_manual_tool_change_2;
    }


    @Override
    protected TouchPlatformViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(TouchPlatformViewModel.class);
    }


    @OnClick(R2.id.bt_a400_calibration_submit)
    void onClickNext() {
        playNormalClickSound();
        isSelect = true;
        mViewModel.savePlatformZOffset()
                .flatMap(responseStructure -> mViewModel.upLiftToolhead())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (getActivity() == null) return;
                    ((ThicknessMeasureCalibrationActivity) getActivity()).gotToThicknessMeasureCalibration21();
                }, LogHelper::log);
    }
}
