package fabscreen.features.machinetools.calibration.j1Platform;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.view.BaseFragment;

public class J1CalibrationModeFragment extends BaseFragment {
    @BindView(R2.id.iv_show_image)
    ImageView mIvShowImage;
    @BindView(R2.id.tv_calibration_mode_content)
    TextView mTvCalibrationModeContent;
    @BindView(R2.id.rl_calibration_switch_mode)
    RelativeLayout mRlCalibrationMode;
    @BindView(R2.id.sw_calibration_switch_mode)
    Switch mSwCalibrationMode;
    IPreferences.Helper mHelper;
    private final J1CalibrationMode.J1CalibrationModeIndex mCalibrationModeIndex;
    private boolean mIsAuxiliary;
    private J1CalibrationMode mJ1CalibrationMode;
    private final boolean mIsGuide;

    public J1CalibrationModeFragment(J1CalibrationMode.J1CalibrationModeIndex calibrationModeInxde, boolean isGuide) {
        mCalibrationModeIndex = calibrationModeInxde;
        mIsGuide = isGuide;
    }

    public static Fragment newInstance(J1CalibrationMode.J1CalibrationModeIndex calibrationModeInxde, boolean isGuide) {
        return new J1CalibrationModeFragment(calibrationModeInxde, isGuide);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHelper = getServiceContainer().getService(IPreferences.class).getHelper();
        getIsAuxiliary();
        if (mIsGuide) {
            mIsAuxiliary = true;
        }
        getCalibrationMode();
        initView();
    }

    private void initView() {
        Glide.with(this).load(mJ1CalibrationMode.getCalibrationModeImageId()).into(mIvShowImage);
        mTvCalibrationModeContent.setText(mJ1CalibrationMode.getCalibrationModeContentId());
        mSwCalibrationMode.setChecked(mJ1CalibrationMode.isAuxiliary());
//        if (mCalibrationModeIndex == CALIBRATION_CHECK || mIsGuide) {
//            mRlCalibrationMode.setVisibility(View.INVISIBLE);
//        } else {
//            mRlCalibrationMode.setVisibility(View.VISIBLE);
//        }
    }

    private void getCalibrationMode() {
        switch (mCalibrationModeIndex) {
            case HEATED_BED_LEVELING:
                mJ1CalibrationMode = mIsAuxiliary ?
                        new J1CalibrationMode(
                                mCalibrationModeIndex,
                                R.drawable.gif_calibration_j1_heated_bed_leveing_auxiliary_content,
                                R.string.calibration_J1_heated_bed_leveing_auxiliary_content,
                                RoutePath.TOOLS_CALIBRATION_J1_3DP_LEVELING_BED_AUXILIARY,
                                mIsAuxiliary)
                        :
                        new J1CalibrationMode(
                                mCalibrationModeIndex,
                                R.drawable.ic_j1_calibration,
                                R.string.calibration_J1_heated_bed_leveing_auxiliary_content,
                                RoutePath.TOOLS_CALIBRATION_J1_3DP_LEVELING_BED,
                                mIsAuxiliary);
                break;
            case Z_OFFSET_CALIBRATION:
                mJ1CalibrationMode = mIsAuxiliary ?
                        new J1CalibrationMode(
                                mCalibrationModeIndex,
                                R.drawable.gif_calibration_j1_z_offset_calibration_content,
                                R.string.calibration_J1_Z_offset_calibration_content,
                                RoutePath.TOOLS_CALIBRATION_J1_3DP_LEVELING_Z_AUXILIARY,
                                mIsAuxiliary)
                        :
                        new J1CalibrationMode(
                                mCalibrationModeIndex,
                                R.drawable.ic_j1_calibration,
                                R.string.calibration_J1_heated_bed_leveing_auxiliary_content,
                                RoutePath.TOOLS_CALIBRATION_J1_3DP_LEVELING_Z,
                                mIsAuxiliary);
                break;
            case XY_OFFSET_CALIBRATION:
                mJ1CalibrationMode = mIsAuxiliary ?
                        new J1CalibrationMode(
                                mCalibrationModeIndex,
                                R.drawable.gif_calibration_j1_xy_offset_auxiliary_calibration_content,
                                R.string.calibration_J1_XY_offset_auxiliary_calibration_content,
                                RoutePath.TOOLS_CALIBRATION_J1_3DP_LEVELING_XY_AUXILIARY,
                                mIsAuxiliary)
                        :
                        new J1CalibrationMode(
                                mCalibrationModeIndex,
                                R.drawable.ic_j1_calibration,
                                R.string.calibration_J1_XY_offset_calibration_content,
                                RoutePath.TOOLS_CALIBRATION_J1_3DP_LEVELING_XY,
                                mIsAuxiliary);
                break;
            case CALIBRATION_CHECK:
                mJ1CalibrationMode = new J1CalibrationMode(
                        mCalibrationModeIndex,
                        R.drawable.pic_model_check_360x408,
                        R.string.calibration_J1_calibration_check_content,
                        RoutePath.TOOLS_CALIBRATION_J1_3DP_CALIBRATION_CHECK,
                        mIsAuxiliary);
                break;
            default:
                mJ1CalibrationMode = null;
                break;
        }
    }

    private void getIsAuxiliary() {
        switch (mCalibrationModeIndex) {
            case HEATED_BED_LEVELING:
                mIsAuxiliary = mHelper.getHeatedBedLeveLingIsAuxiliary();
                break;
            case Z_OFFSET_CALIBRATION:
                mIsAuxiliary = mHelper.getZOffsetCalibrationIsAuxiliary();
                break;
            case XY_OFFSET_CALIBRATION:
                mIsAuxiliary = mHelper.getXYOffsetCalibrationIsAuxiliary();
                break;
            default:
                mIsAuxiliary = false;
        }
    }

    @OnCheckedChanged(R2.id.sw_calibration_switch_mode)
    public void SwitchChange(CompoundButton view, boolean isCheck) {
        if (isCheck != mIsAuxiliary) {
            mIsAuxiliary = isCheck;
            switch (mCalibrationModeIndex) {
                case HEATED_BED_LEVELING:
                    mHelper.setHeatedBedLeveLingIsAuxiliary(mIsAuxiliary);
                    break;
                case Z_OFFSET_CALIBRATION:
                    mHelper.setZOffsetCalibrationIsAuxiliary(mIsAuxiliary);
                    break;
                case XY_OFFSET_CALIBRATION:
                    mHelper.setXYOffsetCalibrationIsAuxiliary(mIsAuxiliary);
                    break;
                default:
            }
            getCalibrationMode();
            initView();
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_calibration_mode;
    }

    @OnClick(R2.id.btn_calibration_start)
    void onCalibrationStartClicked() {
        playNormalClickSound();
        if (mIsGuide) {
            ServiceContainer.getInstance().getService(IRouter.class)
                    .routeWithClassPath(mJ1CalibrationMode.getCalibrationModePath(), mIsGuide)
                    .start(getContext());
        } else {
            ServiceContainer.getInstance().getService(IRouter.class)
                    .routeWithClassPath(mJ1CalibrationMode.getCalibrationModePath())
                    .start(getContext());
        }

    }


}
