package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.prop.UInt16Prop;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.FabSeekBar;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintA400AdjustmentFlowRateFragment extends BaseFragment {
    private static final int FLOW_RATE_MAX_VALUE = 200;
    private static final int FLOW_RATE_MIN_VALUE = 10;

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

    private NewPrintController mNewPrintController;
    public static final int LEFT_NOZZLE_TYPE = 101;
    public static final int RIGHT_NOZZLE_TYPE = 102;
    private int mNozzleType;
    private boolean mIsChange = false;

    public static Fragment newInstance(int nozzleType) {
        PrintA400AdjustmentFlowRateFragment flowRateFragment = new PrintA400AdjustmentFlowRateFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("nozzleType", nozzleType);
        flowRateFragment.setArguments(bundle);
        return flowRateFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNozzleType = requireArguments().getInt("nozzleType", LEFT_NOZZLE_TYPE);

        mNewPrintController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print_adjustment_flow_rate;
    }

    private void initView() {
        mTvSettingName.setText(mNozzleType == LEFT_NOZZLE_TYPE ?
                R.string.a400_print_print_setting_left_flow_rate_title :
                R.string.a400_print_print_setting_right_flow_rate_title);
        mTvName.setText(mNozzleType == LEFT_NOZZLE_TYPE ? R.string.a400_print_settings_left_nozzle_flow_rate : R.string.a400_print_settings_right_nozzle_flow_rate);

        mFabSeekBar.setMin(FLOW_RATE_MIN_VALUE);
        mFabSeekBar.setMax(FLOW_RATE_MAX_VALUE);

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

    @OnClick(R2.id.btn_print_adjustment_cancel)
    public void onClickCancel() {
        changeState(false);
    }

    @OnClick(R2.id.btn_print_adjustment_confirm)
    public void onClickConfirm() {
        mNewPrintController.setFDMFlowRate(0, mNozzleType == LEFT_NOZZLE_TYPE ? 0 : 1, (int) mFabSeekBar.getProgress())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    changeState(false);
                }, e -> {
                    changeState(false);
                    LogHelper.log(e);
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        changeState(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getProgress(0, mNozzleType == LEFT_NOZZLE_TYPE ? 0 : 1);
    }

    private void changeState(boolean isChange) {
        mIsChange = isChange;
        getProgress(0, mNozzleType == LEFT_NOZZLE_TYPE ? 0 : 1);
        mBtnCancel.setVisibility(mIsChange ? View.VISIBLE : View.INVISIBLE);
        mBtnConfirm.setVisibility(mIsChange ? View.VISIBLE : View.INVISIBLE);
    }

    void getProgress(int index, int extruderIndex) {
        mNewPrintController.getFDMFlowRate(index, extruderIndex)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle()).subscribe(responseStructure -> {
                    BaseStructure baseStructure = (BaseStructure) responseStructure.dataProp;
                    int key = (int) baseStructure.getProp("key").getValue();
                    ArrayList<UInt16Prop> flowRateList = (ArrayList<UInt16Prop>) baseStructure.getProp("flowRateArray").getValue();
                    Logger.d("key %d, arrayList flowRate %s", key, flowRateList.toString());
                    if (mIsChange) return;
                    mTvProgressValue.setText(flowRateList.get(extruderIndex).getValue() + "%");
                    mFabSeekBar.setProgress(flowRateList.get(extruderIndex).getValue());
                });
    }
}
