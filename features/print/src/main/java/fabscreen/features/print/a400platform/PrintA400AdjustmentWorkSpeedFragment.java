package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.FabSeekBar;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintA400AdjustmentWorkSpeedFragment extends BaseFragment {
    private static final int WORK_SPEED_MAX_VALUE = 500;
    private static final int WORK_SPEED_MIN_VALUE = 10;

    @BindView(R2.id.pragress)
    FabSeekBar mFabSeekBar;
    @BindView(R2.id.tv_progress_value)
    TextView mTvProgressValue;
    @BindView(R2.id.btn_print_adjustment_cancel)
    Button mBtnCancel;
    @BindView(R2.id.btn_print_adjustment_confirm)
    Button mBtnConfirm;
    @BindView(R2.id.tv_progress_name)
    TextView mTvName;
    @BindView(R2.id.tv_print_setting_name)
    TextView mTvSettingName;
    private boolean mIsChange = false;

    public static Fragment newInstance() {
        return new PrintA400AdjustmentWorkSpeedFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController()
                .getTookHeadSpeedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(workSpeedList -> {
                    if (workSpeedList.isEmpty()) return;
                    if (mIsChange) return;
                    mTvProgressValue.setText(workSpeedList.get(0) + "%");
                    mFabSeekBar.setProgress(workSpeedList.get(0));
                }, LogHelper::log);
//        updateWorkSpeed();
    }

    private void initView() {
        mTvSettingName.setText(R.string.a400_print_print_setting_part_work_speed_title);
        mFabSeekBar.setMin(WORK_SPEED_MIN_VALUE);
        mFabSeekBar.setMax(WORK_SPEED_MAX_VALUE);

        // listen seekbar
        mFabSeekBar.setOnProgressChangeListener(new FabSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(FabSeekBar fabSeekBar, float progress) {
                mTvProgressValue.setText(((int) progress) + "%");
            }

            @Override
            public void onStartTrackingTouch(FabSeekBar fabSeekBar, float progress) {
                changeState(true);
            }

            @Override
            public void onStopTrackingTouch(FabSeekBar fabSeekBar, float progress) {
            }
        });

    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print_adjustment_work_speed;
    }


    @OnClick(R2.id.btn_print_adjustment_cancel)
    public void onClickCancel() {
        changeState(false);
    }

    @OnClick(R2.id.btn_print_adjustment_confirm)
    public void onClickConfirm() {
        NewPrintController newPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
        newPrintController
                .setPrintWorkSpeed(ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType, 0, 0, (int) mFabSeekBar.getProgress())
                .flatMap(responseStructure -> {
                    if (ServiceContainer.getInstance().getService(IMachine.class).getFDMController().getHeadType() == Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER) {
                        return newPrintController.setPrintWorkSpeed(IMachine.WorkType.FDM, 0, 1, (int) mFabSeekBar.getProgress());
                    } else {
                        return Observable.just(responseStructure);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    changeState(false);
                }, e -> {
                    changeState(false);
                    LogHelper.log(e);
                });
    }

//    void updateWorkSpeed() {
//        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController()
//                .getExtruderWorkSpeed(ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType, 0)
//                .observeOn(AndroidSchedulers.mainThread())
//                .as(bindToLifecycle())
//                .subscribe(responseStructure -> {
//
//                });
//    }

    private void changeState(boolean isChange) {
        mIsChange = isChange;
//        updateWorkSpeed();
        mBtnCancel.setVisibility(mIsChange ? View.VISIBLE : View.INVISIBLE);
        mBtnConfirm.setVisibility(mIsChange ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().subscribeTookHeadSpeed();
    }

    @Override
    public void onPause() {
        super.onPause();
        changeState(false);
        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().unSubscribeTookHeadSpeed();
    }
}