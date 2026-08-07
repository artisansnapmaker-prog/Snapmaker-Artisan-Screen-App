package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.common.jogger.XYZJogViewModel;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400XYZBControlPanel;
import fabscreen.platform.core.ui.view.StepIntroductionDialog;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class WorkPrepareWithJogFragment extends BaseFragment {

    @BindView(R2.id.iv_main_pic)
    ImageView mIvMainPic;
    @BindView(R2.id.vp_main_pic)
    VideoPlayerIJK mVpMainPic;
    @BindView(R2.id.tv_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_desc)
    TextView mTvDesc;
    @BindView(fabscreen.platform.core.R2.id.xyzb_calibration_control)
    A400XYZBControlPanel mXYZBCalibrationControl;

    boolean isShowVideo = false;
    private XYZJogViewModel mViewModel;
    private DecisionDialog mDecisionDialog;
    private boolean isFirst;
    private boolean mHasZ;
    private boolean mNeedShowTip = false;

    private StepIntroductionDialog mFocusLeverHelperDialog;

    private BehaviorSubject<Boolean> mShouldEnableButtonsSubject = BehaviorSubject.create();

    public static Fragment newInstance(Bundle bundle) {
        Fragment fragment = new WorkPrepareWithJogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public Observable<Boolean> getButtonsEnableObservable() {
        return mShouldEnableButtonsSubject.hide();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_laser_z_jog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle == null) return;
        int pic = bundle.getInt("pic", 0);
        String title = bundle.getString("title");
        int descId = bundle.getInt("desc");
        String vpPath = bundle.getString("videoPath", "");
        mHasZ = bundle.getBoolean("hasZ", true);
        mNeedShowTip = bundle.getBoolean("tip", false);
        if (!vpPath.isEmpty()) {
            mVpMainPic.setVisibility(View.VISIBLE);
            mVpMainPic.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + vpPath);
            mVpMainPic.setLooping(true);
            isShowVideo = true;
        }
        if (pic != 0) {
            mIvMainPic.setImageResource(pic);
        }
        mTvTitle.setText(title);
        mTvDesc.setText(descId);

        mViewModel = getViewModel();
        isFirst = true;
        initView();
        watchMovingState();

    }

    protected void initView() {

        mDecisionDialog = DecisionDialog.create(requireContext()).setFirstTv(requireContext().getString(R.string.all_confirm),
                fabscreen.platform.core.R.color.select_dialog_blue_txt, (dialog, which) -> {
                    dialog.dismiss();
                });

        mXYZBCalibrationControl.setRotaryStuffVisibility(mViewModel.isRotaryAvailable());
        mXYZBCalibrationControl.setOnDirectionClickListener(new A400XYZBControlPanel.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                playNormalClickSound();
                mViewModel.moveToPosition(direction)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(responseStructure -> {
                            if (!responseStructure.isSuccess()) {
                                mDecisionDialog.setContent(getString(R.string.all_error_dialog_linear_module_moving_limitation_desc) + responseStructure.resultProp.getValue())
                                        .show();
                            }
                        });
            }

            @Override
            public void onPositionChange(int position) {
                mViewModel.changeStepWidth(position);
            }

            @Override
            public void changPanel(int position) {

            }
        });

        if ((mViewModel.mMachine.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.LASER && !mViewModel.isRotaryAvailable() && !mHasZ)) {
            mXYZBCalibrationControl.hasZ(false);
        }

        mViewModel.getMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> mShouldEnableButtonsSubject.onNext(!isMoving), LogHelper::log);

        String videoResPath = "/laser_20w_how_to_use_focus_lever.webm";
        int leverDescResId = R.string.a400_print_laser_40w_how_to_use_focus_lever_helper_desc;
        if (mViewModel.mMachine.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.LASER) {
            switch (mViewModel.mMachine.getLaserController().getHeadType()) {
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    videoResPath = "/laser_20w_how_to_use_focus_lever.webm";
                    leverDescResId = R.string.a400_print_laser_40w_how_to_use_focus_lever_helper_desc;
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    videoResPath = "/laser_2w_how_to_use_focus_lever.webm";
                    leverDescResId = R.string.a400_print_laser_2w_how_to_use_focus_lever_helper_desc;
                    break;
                default:
                    break;
            }
        }

        mFocusLeverHelperDialog = StepIntroductionDialog.create(requireContext())
                .setTitle(R.string.a400_print_laser_40w_how_to_use_focus_lever_helper_title)
                .setContent(leverDescResId)
                .setVideo(videoResPath)
                .setOnClickBack(v -> mFocusLeverHelperDialog.dismiss());
        mFocusLeverHelperDialog.setCanceledOnTouchOutSide(false);
        if (mNeedShowTip) {
            mFocusLeverHelperDialog.show();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (isShowVideo) {
            mVpMainPic.setLooping(false);
            mVpMainPic.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isShowVideo) {
            mVpMainPic.setLooping(true);
            mVpMainPic.start();
        }
    }

    private void watchMovingState() {
        mViewModel.getMoveStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshByMovingState, LogHelper::log);
    }

    private void refreshByMovingState(MoveController.Direction direction) {
        if (isFirst) {
            isFirst = false;
            return;
        }
        mXYZBCalibrationControl.refreshMoveState(direction);
    }

    @Override
    protected XYZJogViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(XYZJogViewModel.class);
    }

}
