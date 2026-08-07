package fabscreen.features.machinetools.calibration.j1Platform.levelingZ;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.J1CalibrationBaseFragment;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.FDMController;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.prop.FloatProp;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import fabscreen.platform.core.ui.view.HorizontalPrecisionIndicator;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LevelingZAuxiliaryCalibrationFragment extends J1CalibrationBaseFragment {
    @BindView(R2.id.horizontal_indicator)
    HorizontalPrecisionIndicator mHpiZValue;
    @BindView(R2.id.top_bar_back)
    Button mBtBack;
    @BindView(R2.id.btn_next)
    Button mBtNext;
    @BindView(R2.id.tv_leveling_bed_auxiliary_calibration_content)
    TextView mTvShowCount;
    @BindView(R2.id.tv_leveling_bed_auxiliary_calibration_title)
    TextView mTvShowTitle;
    FDMController fdmController;
    @BindView(R2.id.rectangle_2)
    ImageView mIvShowImage;

    public static Fragment newInstance() {
        return new LevelingZAuxiliaryCalibrationFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fdmController = ServiceContainer.getInstance().getService(IMachine.class).getFDMController();
        initView();
        initData();
    }

    private void initData() {
        fabMoving.show();
        fdmController.setCalibrationMode(50)
                .flatMap(success -> success.isSuccess() ? fdmController.startZAuxiliaryCalibration(3) : Observable.just(success))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    fabMoving.dismiss();
                    if (success.isSuccess()) {
                        initOperation();
                    } else {
                        errorBack("setCalibrationMode", success.resultProp.getValue());
                    }
                }, LogHelper::log);
    }

    private void initView() {
        Glide.with(this)
                .load(R.drawable.pic_leveling_z_calibration_auxiliary_content)
                .into(mIvShowImage);
        mTvShowTitle.setText(R.string.leveling_Z_calibration_auxiliary_title);
        mTvShowCount.setText(R.string.leveling_Z_calibration_auxiliary_content);
        mHpiZValue.setPrecisionIndicatorListener(isValid -> {
            mBtNext.setEnabled(isValid);
        });
        mBtNext.setEnabled(false);
    }


    private void initOperation() {
        fdmController.watchHeightDifferenceState()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        BaseStructure baseStructure = (BaseStructure) responseStructure.dataProp;
                        int index = ((UInt8Prop) (baseStructure.getProp("index"))).getValue();
                        float heightDifference = ((FloatProp) (baseStructure.getProp("heightDifference"))).getValue();
                        // FIXME: Z Offset fine tune direction is opposite with the widget.
                        //  Solution: make widget direction is configurable.
                        mHpiZValue.setValue(-heightDifference);
//                    } else if (responseStructure.resultProp.getValue().equals(200)) {
//                        DecisionDialog.create(requireContext())
//                                .setDialogStatus(DecisionDialog.BTN_TWO, false, false, false, true)
//                                .setContent(R.string.j1_calibration_fail_to_obtain_data)
//                                .setSecondTv(R.string.all_retry, R.color.select_dialog_orange_txt, (dialog, which) -> {
//                                    fdmController.setGetHeightDifferenceState(true)
//                                            .flatMap(responseStructure1 ->
//                                                    responseStructure1.isSuccess() ?
//                                                            fdmController.subscribeGetHeightDifference() : Observable.just(responseStructure1))
//                                            .observeOn(AndroidSchedulers.mainThread())
//                                            .as(bindToLifecycle())
//                                            .subscribe(resultStructure -> {
//                                                if (resultStructure.isSuccess()) {
//                                                    dialog.dismiss();
//                                                }
//                                            });
//                                })
//                                .setFirstTv(R.string.all_quit, R.color.select_dialog_grey_txt, ((dialog, which) -> {
//                                    ServiceContainer.getInstance().getService(IMachine.class).getFDMController()
//                                            .exitCalibration(false)
//                                            .observeOn(AndroidSchedulers.mainThread())
//                                            .as(bindToLifecycle())
//                                            .subscribe(success -> {
//                                                if (success.isSuccess()) {
//                                                    dialog.dismiss();
//                                                    ServiceContainer.getInstance().getService(IRouter.class).routeToCalibrationPage().startAndClear(getContext());
//                                                    finishActivityWithResultOk();
//                                                }
//                                            });
//                                })).show();
//                    }
                    } else {
                        errorBack("initOperation", responseStructure.resultProp.getValue());
                    }
                });

        fdmController.setGetHeightDifferenceState(true)
                .flatMap(responseStructure ->
                        responseStructure.isSuccess() ?
                                fdmController.subscribeGetHeightDifference() : Observable.just(responseStructure))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(resultStructure -> {
                });
    }


    @OnClick(R2.id.btn_next)
    public void onClickNext() {
        playNormalClickSound();
        fdmController.unSubscribeGetZHeightDifference()
                .flatMap(success -> fdmController.setGetHeightDifferenceState(false))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success.isSuccess()) {
                        if (getActivity() == null) return;
                        ((LevelingZAuxiliaryCalibrationActivity) getActivity()).gotoRestoringMachine();
                    } else {
                        errorBack("setGetHeightDifferenceState", success.resultProp.getValue());
                    }
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_auxiliary_calibration_get_height_difference;
    }

}
