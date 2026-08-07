package fabscreen.features.machinetools.calibration.a400platform.cnc.manualTool.basic;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.features.machinetools.calibration.a400platform.cnc.manualTool.A400CncManualToolViewModel;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400XYZBControlPanel;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

public class A400CncManualToolBasicFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.vp_main_pic)
    VideoPlayerIJK mVpMainPic;
    @BindView(R2.id.cv_main_pic)
    CardView mCdMainPic;
    @BindView(R2.id.fragment_calibration_content)
    TextView mTvContent;
    @BindView(R2.id.top_bar_content)
    TextView mTvSubTitle;
    @BindView(R2.id.fragment_calibration_title)
    TextView mTvMessageTitle;
    @BindView(R2.id.cp_a400_calibration_move)
    A400XYZBControlPanel mXYZControlPanel;
    //    @BindView(R2.id.tab_layout)
//    TabLayout mTabLayout;
    PublishSubject<Boolean> mIsMovePopUpSubject = PublishSubject.create();

    private A400CncManualToolViewModel mViewModel;
    private DecisionDialog mDecisionDialog;

    public static Fragment newInstance() {
        return new A400CncManualToolBasicFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        watchMovingState();
        checkHome().observeOn(AndroidSchedulers.mainThread()).as(bindToLifecycle()).subscribe();
    }

    public Observable<Boolean> checkHome() {
        IMachine service = ServiceContainer.getInstance().getService(IMachine.class);
        if (!service.getMachineStatusSubjectHolder().getValue().isHomed) {
            mIsMovePopUpSubject.onNext(true);
            return service.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> service.getMachineController().home(0))
                    .flatMap(integer -> service.getMachineController().updateCoordinateSystem(1))
                    .doOnNext(machineStatus -> {
                        mIsMovePopUpSubject.onNext(false);
                    })
                    .flatMap(machineStatus -> Observable.just(machineStatus.isHomed));
        } else {
            return Observable.just(true);
        }
    }

    private void initView() {
        setTitle(R.string.a400_manual_tool_title);
        mTvSubTitle.setText(R.string.a400_manual_tool_subheading);
        mTvMessageTitle.setText(R.string.a400_manual_tool_message_title);
        mTvContent.setText(R.string.a400_manual_tool_message);
        mGuideProgressBar.setMax(1);
        mGuideProgressBar.setProgress(1);
        mGuideProgressBar.setVisibility(View.VISIBLE);
        mCdMainPic.setVisibility(View.VISIBLE);
        mVpMainPic.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + (mViewModel.is200wCnc() ? "/CNC_3x_200W_Set_XY_Origin.webm" : "/CNC_3x_Normal_Set_XY_Origin.webm"));
        mVpMainPic.setLooping(true);
        mDecisionDialog = DecisionDialog.create(requireContext()).setDialogStatus(DecisionDialog.BTN_ONE,
                false, false, false, true);
        mXYZControlPanel.setRotaryStuffVisibility(mViewModel.isRotaryAvailable());
        mXYZControlPanel.setStepWidths(0.1f, 1f, 10f, 100f);
        mXYZControlPanel.setOnDirectionClickListener(new A400XYZBControlPanel.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                playNormalClickSound();
                mViewModel.move(direction, stepWidth)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(responseStructure -> {
                            if (responseStructure.isGeneralError()) {
                                mDecisionDialog.setContent(getString(R.string.debug_machine_move_restricted))
                                        .setFirstTv(requireContext().getString(R.string.all_confirm),
                                                R.color.select_dialog_blue_txt, (dialog, which) -> {
                                                    dialog.dismiss();
                                                }).show();
                            }
                        }, LogHelper::log);
            }

            @Override
            public void onPositionChange(int position) {
                mViewModel.changeStepWidth(position);
            }

            @Override
            public void changPanel(int position) {

            }
        });

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mXYZControlPanel.setEnabled(!isMoving);
                });
        mIsMovePopUpSubject.observeOn(AndroidSchedulers.mainThread()).as(bindToLifecycle()).subscribe(aBoolean -> {
            if (aBoolean) {
                fabLoading.show();
            } else {
                fabLoading.dismiss();
            }
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
    }

    private void watchMovingState() {
        mViewModel.getMoveStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshByMovingState, LogHelper::log);
    }

    private void refreshByMovingState(MoveController.Direction direction) {
        mXYZControlPanel.refreshMoveState(direction);
    }

    @Override
    public void onPause() {
        super.onPause();
        mVpMainPic.setLooping(false);
        mVpMainPic.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVpMainPic.setLooping(true);
        mVpMainPic.start();
    }

    @OnClick(R2.id.bt_a400_calibration_submit)
    public void onClickSubmit() {
        playNormalClickSound();
        mViewModel.setWorkOrigin()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success.isSuccess()) {
                        ((A400CncManualToolBasicActivity) requireActivity()).gotoCncManualToolComplete();
                    }
                }, LogHelper::log);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_cnc_manual_tool_basic;
    }

    @Override
    protected A400CncManualToolViewModel getViewModel() {
        return getViewModelProvider().get(A400CncManualToolViewModel.class);
    }
}
