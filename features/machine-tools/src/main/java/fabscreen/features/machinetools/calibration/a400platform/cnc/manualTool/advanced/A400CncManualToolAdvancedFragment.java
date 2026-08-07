package fabscreen.features.machinetools.calibration.a400platform.cnc.manualTool.advanced;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.features.machinetools.calibration.a400platform.cnc.manualTool.A400CncManualToolViewModel;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400CoordinatesPanel;
import fabscreen.platform.core.ui.view.A400XYZBControlPanel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

import static fabscreen.platform.core.ui.data.MoveController.Direction.IDLE;

public class A400CncManualToolAdvancedFragment extends A400CalibrationBaseFragment {
    @BindView(R2.id.xyzb_calibration_control)
    A400XYZBControlPanel mXYZBCalibrationControl;
    @BindView(R2.id.cp_a400_calibration_coordinates)
    A400CoordinatesPanel mCoordinatesPanel;

    PublishSubject<Boolean> mIsMovePopUpSubject = PublishSubject.create();
    private A400CncManualToolViewModel mViewModel;
    private DecisionDialog mDecisionDialog;

    public static Fragment newInstance() {
        return new A400CncManualToolAdvancedFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        checkHome().observeOn(AndroidSchedulers.mainThread()).as(bindToLifecycle()).subscribe();
        watchMovingState();
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
        List<String> mList = new ArrayList<>();
        mList.add(getString(R.string.a400_work_coordinates));
        mList.add(getString(R.string.a400_machine_coordinates));
        mCoordinatesPanel.setBAxisVisibility(mViewModel.isRotaryAvailable());
        mCoordinatesPanel.setRunBoundaryVisibility(false);
        mCoordinatesPanel.setCoordinatesList(mList)
                .setOnDirectionClickListener(new A400CoordinatesPanel.OnCoordinatesOnClickListener() {
                    @Override
                    public void onDirectionClicked(int type, int viewId) {
                        playNormalClickSound();
                        switch (type) {
                            case A400CoordinatesPanel.X_TYPE:
                                Vector mXVector = new Vector();
                                mXVector.setX(0);
                                mViewModel.setOrigin(mXVector, viewId);
                                break;

                            case A400CoordinatesPanel.Y_TYPE:
                                Vector mYVector = new Vector();
                                mYVector.setY(0);
                                mViewModel.setOrigin(mYVector, viewId);
                                break;

                            case A400CoordinatesPanel.Z_TYPE:
                                Vector mZVector = new Vector();
                                mZVector.setZ(0);
                                mViewModel.setOrigin(mZVector, viewId);
                                break;
                            case A400CoordinatesPanel.B_TYPE:
                                Vector mBVector = new Vector();
                                mBVector.setB(0);
                                mViewModel.setOrigin(mBVector, viewId);
                                break;
                            case A400CoordinatesPanel.XYZ_TYPE:
                                Vector mXYZVector = new Vector();
                                mXYZVector.setX(0);
                                mXYZVector.setY(0);
                                mXYZVector.setZ(0);
                                if (mViewModel.isRotaryAvailable()) {
                                    mXYZVector.setB(0);
                                }
                                mViewModel.setOrigin(mXYZVector, viewId);
                                break;
                        }
                    }

                    @Override
                    public void onPopupOnClicked(int position) {
                        mViewModel.setCoordinateType(position);
                    }

                    @Override
                    public void onClickRunBoundary() {

                    }
                });
        updateCoordinateView();

        setTitle(R.string.a400_manual_tool_title);
        mTvTopBarContent.setText(mViewModel.isRotaryAvailable() ? R.string.a400_manual_tool_four_axis_subheading : R.string.a400_manual_tool_subheading);
        mGuideProgressBar.setMax(1);
        mGuideProgressBar.setProgress(1);
        mGuideProgressBar.setVisibility(View.VISIBLE);
        mDecisionDialog = DecisionDialog.create(requireContext()).setDialogStatus(DecisionDialog.BTN_ONE,
                false, false, false, true);

        mXYZBCalibrationControl.setRotaryStuffVisibility(mViewModel.isRotaryAvailable());
        mXYZBCalibrationControl.setOnDirectionClickListener(new A400XYZBControlPanel.OnDirectionClickListener() {
            @Override
            public void onDirectionClicked(MoveController.Direction direction, float stepWidth) {
                playNormalClickSound();
                mViewModel.moveToPosition(direction)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(responseStructure -> {
                            if (responseStructure.isGeneralError()) {
                                mDecisionDialog.setContent(getString(R.string.all_error_dialog_linear_module_moving_limitation_desc))
                                        .setFirstTv(requireContext().getString(R.string.all_confirm),
                                                R.color.select_dialog_blue_txt, (dialog, which) -> {
                                                    dialog.dismiss();
                                                }).show();
                            }
                        });
            }

            @Override
            public void onPositionChange(int position) {
                mViewModel.changeStepWidth(position);
            }

            @Override
            public void changPanel(int position) {
                mCoordinatesPanel.scrollCoordinate(position);
            }
        });

        mViewModel.getMoveStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshByMovingState, LogHelper::log);

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

    private void updateCoordinateView() {
        mViewModel.getCoordinateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(vector -> {
                    mCoordinatesPanel.setCoordinatesValue(
                            String.format(Locale.US, "%.2f", vector.getX()),
                            String.format(Locale.US, "%.2f", vector.getY()),
                            String.format(Locale.US, "%.2f", vector.getZ()),
                            String.format(Locale.US, "%.2f", vector.getB())
                    );
                }, LogHelper::log);
    }

    private void watchMovingState() {
        mViewModel.getMoveStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshByMovingState, LogHelper::log);
    }

    private void refreshByMovingState(MoveController.Direction direction) {
        mXYZBCalibrationControl.refreshMoveState(direction);
        mCoordinatesPanel.setViewEnable(direction == IDLE);
    }


    @OnClick(R2.id.bt_a400_calibration_submit)
    public void onClickSubmit() {
        playNormalClickSound();
        mViewModel.setWorkOrigin()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        ((A400CncManualToolAdvancedActivity) requireActivity()).gotoCncManualToolComplete();
                    }
                }, LogHelper::log);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_cnc_manual_tool_advanced;
    }

    @Override
    protected A400CncManualToolViewModel getViewModel() {
        return getViewModelProvider().get(A400CncManualToolViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.subscribeCoordinate();
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.unSubscribeCoordinate();
    }
}
