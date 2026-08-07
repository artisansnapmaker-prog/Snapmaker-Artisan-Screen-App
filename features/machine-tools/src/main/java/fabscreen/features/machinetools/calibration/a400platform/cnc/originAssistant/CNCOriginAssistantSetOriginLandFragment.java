package fabscreen.features.machinetools.calibration.a400platform.cnc.originAssistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.presenter.ControlBAxisPanelWidgetPresenter;
import fabscreen.platform.core.ui.presenter.ControlXYZPanelWidgetPresenter;
import fabscreen.platform.core.ui.view.ControlPanelAdapter;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.core.ui.view.NoScrollViewPager;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class CNCOriginAssistantSetOriginLandFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.iv_cnc_origin_assistant_set_origin_instructions)
    ImageView mIvAssistInstructions;
    @BindView(R2.id.vp_cnc_origin_assistant_control_panels)
    NoScrollViewPager mVpControlPanels;
    @BindView(R2.id.top_bar_back)
    Button mBtnBack;
    @BindView(R2.id.btn_widget_set_origin_x1)
    Button mBtnSetOriginX1;
    @BindView(R2.id.btn_widget_set_origin_x2)
    Button mBtnSetOriginX2;
    @BindView(R2.id.btn_widget_set_origin_y)
    Button mBtnSetOriginY;
    @BindView(R2.id.btn_widget_set_origin_z)
    Button mBtnSetOriginZ;
    @BindView(R2.id.btn_widget_set_origin_b)
    Button mBtnSetOriginB;
    @BindView(R2.id.tv_cnc_origin_assistant_set_origin_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_cnc_origin_assistant_set_origin_content)
    TextView mTvContent;
    @BindView(R2.id.view_guide_progress_bar)
    LinearProgressIndicator mProgress;

    private List<View> mViews;
    private ControlPanelAdapter mPanelAdapter;
    private FileParsingDialog mMOveDialog;
    private String mMoveMsg;
    private A400CNCOriginAssistantViewModel mViewModel;
    private ControlXYZPanelWidgetPresenter mControlXYZPanelPresenter;
    private ControlBAxisPanelWidgetPresenter mControlBAxisPanelPresenter;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Integer> mIsMovePopUpSubject = BehaviorSubject.createDefault(0);

    public static CNCOriginAssistantSetOriginLandFragment newInstance() {
        return new CNCOriginAssistantSetOriginLandFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.calibration_cnc_origin_assistant);
        mProgress.setMax(9);
        mProgress.setProgress(5);
        mTvTitle.setText(R.string.cnc_origin_assistant_set_x1);
        mTvContent.setText(R.string.a400_cnc_origin_origin_assistant_set_x1_msg);
        setContent(R.string.a400_cnc_origin_origin_assistant_set_x1_subtitle);
        mMOveDialog = FileParsingDialog.create(requireContext());
        initView();
        checkHome().observeOn(AndroidSchedulers.mainThread()).as(bindToLifecycle()).subscribe(aBoolean -> start());

    }

    public Observable<Boolean> checkHome() {
        IMachine service = ServiceContainer.getInstance().getService(IMachine.class);
        if (!service.getMachineStatusSubjectHolder().getValue().isHomed) {
            mIsMovePopUpSubject.onNext(-1);
            return service.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> service.getMachineController().home(0))
                    .flatMap(integer -> service.getMachineController().updateCoordinateSystem(1))
                    .doOnNext(machineStatus -> {
                        mIsMovePopUpSubject.onNext(0);
                    })
                    .flatMap(machineStatus -> Observable.just(machineStatus.isHomed));
        } else {
            return service.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(machineStatus.isHomed));
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_origin_assistant_set_origin_land;
    }

    @Override
    protected A400CNCOriginAssistantViewModel getViewModel() {
        return getViewModelProvider().get(A400CNCOriginAssistantViewModel.class);
    }

    private void initView() {
        LayoutInflater inflater = getLayoutInflater();
        View controlXYZPanel = inflater.inflate(R.layout.widget_control_panel_xyz_axes_for_4axis, null);
        View controlBAxisPanel = inflater.inflate(R.layout.widget_control_panel_b_axis, null);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        mViews = new ArrayList<>();
        mViews.add(controlXYZPanel);
        mViews.add(controlBAxisPanel);
        mPanelAdapter = new ControlPanelAdapter(mViews);
        mVpControlPanels.setAdapter(mPanelAdapter);

        // linear control panel
        mControlXYZPanelPresenter = new ControlXYZPanelWidgetPresenter(disposables);
        mControlXYZPanelPresenter.bind(requireContext(), controlXYZPanel, 4);
        mControlXYZPanelPresenter.connect();

        // rotary control panel
        mControlBAxisPanelPresenter = new ControlBAxisPanelWidgetPresenter(disposables);
        mControlBAxisPanelPresenter.bind(requireContext(), controlBAxisPanel);
        mControlBAxisPanelPresenter.connect();

        mIsMovePopUpSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(integer -> {
                    mMovingEventSubject.onNext(true);
                    switch (integer) {
                        case 0:
                            mMovingEventSubject.onNext(false);
                            if (mMOveDialog != null && mMOveDialog.isShowing()) {
                                mMOveDialog.dismiss();
                            }
                            return;
                        case -1:
                            mMoveMsg = getString(R.string.a400_cnc_origin_origin_assistant_go_home);
                            break;
                        case 1:
                            mMoveMsg = getString(R.string.a400_cnc_origin_origin_assistant_set_x1_tip);
                            break;
                        case 2:
                            mMoveMsg = getString(R.string.a400_cnc_origin_origin_assistant_set_x2_tip);
                            break;
                        case 3:
                            mMoveMsg = getString(R.string.a400_cnc_origin_origin_assistant_set_y_tip);
                            break;
                        case 4:
                            mMoveMsg = getString(R.string.a400_cnc_origin_origin_assistant_set_z_tip);
                            break;
                        default:
                            break;
                    }
                    mMOveDialog.setContent(mMoveMsg);
                    mMOveDialog.show();
                });

        mControlXYZPanelPresenter.getMovingEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mControlBAxisPanelPresenter.getMovingEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mViewModel.getMovingObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent != 0);
                    mIsMovePopUpSubject.onNext(movingEvent);
                });

        mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mControlXYZPanelPresenter.setEnabled(!movingEvent);
                    mControlBAxisPanelPresenter.setEnabled(!movingEvent);
                    mBtnBack.setEnabled(!movingEvent);
                });

        mViewModel.getAssistPhaseObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(phase -> {
                    mBtnSetOriginX1.setVisibility((phase == A400CNCOriginAssistantViewModel.AssistPhase.X1 || phase == A400CNCOriginAssistantViewModel.AssistPhase.NOT_START)
                            ? Button.VISIBLE
                            : Button.GONE);
                    mBtnSetOriginX1.setEnabled(phase == A400CNCOriginAssistantViewModel.AssistPhase.X1);
                    mBtnSetOriginX2.setVisibility(phase == A400CNCOriginAssistantViewModel.AssistPhase.X2 ? Button.VISIBLE : Button.GONE);
                    mBtnSetOriginX2.setEnabled(phase == A400CNCOriginAssistantViewModel.AssistPhase.X2);
                    mBtnSetOriginY.setVisibility(phase == A400CNCOriginAssistantViewModel.AssistPhase.Y ? Button.VISIBLE : Button.GONE);
                    mBtnSetOriginY.setEnabled(phase == A400CNCOriginAssistantViewModel.AssistPhase.Y);
                    mBtnSetOriginZ.setVisibility(phase == A400CNCOriginAssistantViewModel.AssistPhase.Z ? Button.VISIBLE : Button.GONE);
                    mBtnSetOriginZ.setEnabled(phase == A400CNCOriginAssistantViewModel.AssistPhase.Z);
                    mBtnSetOriginB.setVisibility(phase == A400CNCOriginAssistantViewModel.AssistPhase.B ? Button.VISIBLE : Button.GONE);
                    mBtnSetOriginB.setEnabled(phase == A400CNCOriginAssistantViewModel.AssistPhase.B);
                    switch (phase) {
                        case NOT_START:
                        case X1:
                            mProgress.setProgress(5);
                            mTvTitle.setText(R.string.cnc_origin_assistant_set_x1);
                            mTvContent.setText(R.string.a400_cnc_origin_origin_assistant_set_x1_msg);
                            setContent(R.string.a400_cnc_origin_origin_assistant_set_x1_subtitle);
                            Glide.with(this)
                                    .load(R.drawable.pic_cnc_origin_assistant_x1)
                                    .apply(options)
                                    .into(mIvAssistInstructions);
                            break;
                        case X2:
                            mProgress.setProgress(6);
                            mTvTitle.setText(R.string.cnc_origin_assistant_set_x2);
                            mTvContent.setText(R.string.a400_cnc_origin_origin_assistant_set_x2_msg);
                            setContent(getString(R.string.a400_cnc_origin_origin_assistant_set_x2_subtitle));
                            Glide.with(this)
                                    .load(R.drawable.pic_cnc_origin_assistant_x2)
                                    .apply(options)
                                    .into(mIvAssistInstructions);
                            break;
                        case Y:
                            mProgress.setProgress(7);
                            mTvTitle.setText(R.string.cnc_origin_assistant_set_y);
                            mTvContent.setText(R.string.a400_cnc_origin_origin_assistant_set_y_msg);
                            setContent(getString(R.string.a400_cnc_origin_origin_assistant_set_y_subtitle));
                            Glide.with(this)
                                    .load(R.drawable.pic_cnc_origin_assistant_y)
                                    .apply(options)
                                    .into(mIvAssistInstructions);
                            break;
                        case Z:
                            mProgress.setProgress(8);
                            mTvTitle.setText(R.string.cnc_origin_assistant_set_z);
                            mTvContent.setText(R.string.a400_cnc_origin_origin_assistant_set_z_msg);
                            setContent(getString(R.string.a400_cnc_origin_origin_assistant_set_z_subtitle));
                            Glide.with(this)
                                    .load(R.drawable.pic_cnc_origin_assistant_z)
                                    .apply(options)
                                    .into(mIvAssistInstructions);
                            break;
                        case B:
                            mProgress.setProgress(9);
                            mTvTitle.setText(R.string.cnc_origin_assistant_set_b);
                            mTvContent.setText(R.string.a400_cnc_origin_origin_assistant_set_b_msg);
                            setContent(getString(R.string.a400_cnc_origin_origin_assistant_set_b_subtitle));
                            Glide.with(this)
                                    .load(R.drawable.pic_cnc_origin_assistant_b)
                                    .apply(options)
                                    .into(mIvAssistInstructions);

                            mVpControlPanels.setCurrentItem(1);
                            break;
                        case COMPLETE:
                            finish();
                            break;
                        case ERROR:
                            mMOveDialog.dismiss();
                            Logger.w("Origin assist error.");
                            break;
                        default:
                            break;
                    }
                });
    }

    private void start() {
        mViewModel.startOriginAssist();
    }

    private void finish() {
        mMovingEventSubject.onNext(true);
        MoveController.getInstance().stepToPosition(MoveController.Direction.UP, 5)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> {
                    mMovingEventSubject.onNext(false);
                    if (getActivity() != null) {
                        ((CncOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantCompleteFragment();
                    }
                }, e -> {
                    mMovingEventSubject.onNext(false);
                    LogHelper.log(e);
                });
    }

    public void dismissDialog() {
        mMOveDialog.dismiss();
    }

    @OnClick(R2.id.btn_widget_set_origin_x1)
    void onClickSetOriginX1() {
        playNormalClickSound();
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginX1()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R2.id.btn_widget_set_origin_x2)
    void onClickSetOriginX2() {
        playNormalClickSound();
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginX2()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R2.id.btn_widget_set_origin_y)
    void onClickSetOriginY() {
        playNormalClickSound();
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginY()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R2.id.btn_widget_set_origin_z)
    void onClickSetOriginZ() {
        playNormalClickSound();
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginZ()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R2.id.btn_widget_set_origin_b)
    void onClickSetOriginB() {
        playNormalClickSound();
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginB()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }
}
