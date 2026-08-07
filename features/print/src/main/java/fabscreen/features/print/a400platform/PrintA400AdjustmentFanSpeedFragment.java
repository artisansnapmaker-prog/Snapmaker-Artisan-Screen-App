package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.features.print.j1platform.viewmodel.PrintJ1AdjustmentFanSpeedViewModel;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.entity.parts.Fan;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.FabSeekBar;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintA400AdjustmentFanSpeedFragment extends BaseFragment {
    private static final int FAN_SPEED_MIN_VALUE = 0;
    @BindView(R2.id.tv_progress_value)
    TextView mTvProgressValue;
    @BindView(R2.id.btn_print_adjustment_cancel)
    Button mBtnCancel;
    @BindView(R2.id.btn_print_adjustment_confirm)
    Button mBtnConfirm;
    @BindView(R2.id.tv_print_setting_name)
    TextView mTvName;

    private static final int FAN_SPEED_MAX_VALUE = 100;
    @BindView(R2.id.pragress)
    FabSeekBar mFabSeekBar;
    private boolean mIsChange = false;

    private PrintJ1AdjustmentFanSpeedViewModel mViewModel;

    public static Fragment newInstance() {
        return new PrintA400AdjustmentFanSpeedFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(PrintJ1AdjustmentFanSpeedViewModel.class);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print_adjustment_fan_speed;
    }

    private void initView() {
        mTvName.setText(R.string.a400_print_print_setting_part_cooling_fan_title);
        mFabSeekBar.setMax(FAN_SPEED_MAX_VALUE);
        mFabSeekBar.setMin(FAN_SPEED_MIN_VALUE);
        IMachine machine = ServiceContainer.getInstance().getService(IMachine.class);
        machine.getFDMController().getToolheadStatusSubjectHolder(0)
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolheadStatus -> {
                    if (mIsChange) return;
                    ArrayList<Fan> fanLists = (ArrayList<Fan>) fdmToolheadStatus.getFanList();
                    int extruder0Speed = fanLists.get(0).getSpeedLevel();
                    int e0SpeedPercent = (int) ((extruder0Speed / 255f) * 100);
                    mTvProgressValue.setText(e0SpeedPercent + "%");
                    mFabSeekBar.setProgress(e0SpeedPercent);
                });

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
    public void onPause() {
        super.onPause();
        mViewModel.unSubscribeFanChange();
        onClickCancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.subscribeFanChange();
        mViewModel.updateFDMInfo();
    }

    @OnClick(R2.id.btn_print_adjustment_cancel)
    public void onClickCancel() {
        changeState(false);
    }

    @OnClick(R2.id.btn_print_adjustment_confirm)
    public void onClickConfirm() {
        mViewModel.setFanSpeed(0, 0, (int) (mFabSeekBar.getProgress() / 100f * 255))
                .flatMap(responseStructure -> {
                    if (ServiceContainer.getInstance().getService(IMachine.class).getFDMController().getHeadType() == Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER) {
                        return mViewModel.setFanSpeed(0, 1, (int) (mFabSeekBar.getProgress() / 100f * 255));
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

    private void changeState(boolean isChange) {
        mIsChange = isChange;
        mViewModel.updateFDMInfo();
        mBtnCancel.setVisibility(mIsChange ? View.VISIBLE : View.INVISIBLE);
        mBtnConfirm.setVisibility(mIsChange ? View.VISIBLE : View.INVISIBLE);
    }
}
