package fabscreen.features.machinetools.calibration.a400platform.laser.w_40.platformHeight;

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
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400DirectionControlPanel;
import fabscreen.platform.core.ui.view.StepIntroductionDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400Calibration40WLaserPlatformHeightFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.cp_a400_calibration_move)
    A400DirectionControlPanel mCpMove;
    @BindView(R2.id.fragment_calibration_content)
    TextView mTvContent;
    @BindView(R2.id.bt_a400_calibration_submit)
    Button mBtnSubmit;
    @BindView(R2.id.top_bar_back)
    Button mBtnBack;
    @BindView(R2.id.fragment_calibration_image)
    ImageView mIVImage;

    private TouchPlatform40WViewModel mViewModel;

    private StepIntroductionDialog mFocusLeverHelperDialog;
    private boolean mNeedShowTip = true;

    public static Fragment newInstance() {
        return new A400Calibration40WLaserPlatformHeightFragment();
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
        setTitle(R.string.a400_calibration_platform_height_calibration_10w_title);
        mTvContent.setText(R.string.a400_calibration_platform_height_calibration_40w_content);
        mTvTopBarContent.setVisibility(View.GONE);
        mGuideProgressBar.setMax(1);
        mGuideProgressBar.setProgress(1);
        mGuideProgressBar.invalidate();
        mGuideProgressBar.setVisibility(View.VISIBLE);
        mBtnSubmit.setText(R.string.all_save);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_laser_40w_platform_height_calibration)
                .apply(options)
                .into(mIVImage);
        mCpMove.setStepWidths(0.1f, 1f, 10f, 100f)
                .setOnDirectionClickListener(new A400DirectionControlPanel.OnDirectionClickListener() {
                    @Override
                    public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                        mViewModel.moveXYZByStep(direction, stepWidth);

                    }

                    @Override
                    public void onPositionChange(int position) {
//                        mViewModel.changeStepWidth(position);
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
                        fabLoading.show();
                    } else {
                        fabLoading.dismiss();
                    }
                });

        mFocusLeverHelperDialog = StepIntroductionDialog.create(requireContext())
                .setTitle(R.string.a400_calibration_laser_40w_how_to_use_focus_lever_helper_title)
                .setContent(R.string.a400_calibration_laser_40w_how_to_use_focus_lever_helper_desc)
                .setVideo("/laser_20w_how_to_use_focus_lever.webm")
                .setOnClickBack(v -> mFocusLeverHelperDialog.dismiss());
        mFocusLeverHelperDialog.setCanceledOnTouchOutSide(false);
        if (mNeedShowTip) {
            mFocusLeverHelperDialog.show();
            mNeedShowTip = false;
        }
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_cnc_manual_tool_change_2;
    }


    @Override
    protected TouchPlatform40WViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(TouchPlatform40WViewModel.class);
    }


    @OnClick(R2.id.bt_a400_calibration_submit)
    void onClickNext() {
        playNormalClickSound();
        mViewModel.savePlatformZOffset()
                .flatMap(result -> mViewModel.exitCalibration(result.isSuccess()))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (getActivity() == null) return;
                    IPreferences.Helper helper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();
                    long machineSn = helper.getA400MachineSn();
                    if (helper.getA400MachineStep(machineSn) == 0) {
                        finishActivityWithResultOk();
                    } else {
                        ((A400Calibration40WLaserPlatformHeightCalibrationActivity) getActivity()).gotoComplete();
                    }
                });
    }
}
