package fabscreen.features.settings.a400.moduleassistant.replacehotend;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.RotateButtonView;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceHotendTempDashboardFragment extends BaseFragment {
    public static final String KEY_OPERATION = "operation";
    public static final int SET_TEMP = 1;
    public static final int COOLING_DOWN = 2;
    private ReplaceHotendViewModel mViewModel;

    @BindView(R2.id.rbv_left)
    RotateButtonView mRbvLeft;
    @BindView(R2.id.rbv_right)
    RotateButtonView mRbvRight;
    @BindView(R2.id.tv_temp_l)
    TextView mTvTempL;
    @BindView(R2.id.tv_temp_r)
    TextView mTvTempR;
    @BindView(R2.id.tv_cooling_down_tip)
    TextView mTvCoolingDownTip;
    @BindView(R2.id.group_view_for_set_temp)
    Group mGroupViewForSetTemp;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_hotend_temp_dashboard;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceHotendViewModel.class);
        initView();
    }

    private void initView() {
        int operation = requireArguments().getInt(KEY_OPERATION);
        if (operation == SET_TEMP) {
            mRbvLeft.setMax(300);
            mRbvLeft.setMin(160);
            mRbvLeft.setIncrementalInterval(5);
            mRbvRight.setMax(300);
            mRbvRight.setMin(160);
            mRbvRight.setIncrementalInterval(5);
            mGroupViewForSetTemp.setVisibility(View.VISIBLE);
            mRbvLeft.setColor1Progress(mViewModel.getUserSelectTempL());
            mRbvRight.setColor1Progress(mViewModel.getUserSelectTempR());
            mRbvLeft.setUseColor2(false);
            mRbvRight.setUseColor2(false);
            responseToUserTouch();
        } else if (operation == COOLING_DOWN) {
            mRbvLeft.setMax(300);
            mRbvLeft.setMin(0);
            mRbvRight.setMax(300);
            mRbvRight.setMin(0);
            mTvCoolingDownTip.setVisibility(View.VISIBLE);
            mRbvLeft.setTouchable(false);
            mRbvRight.setTouchable(false);
            observeNozzleTemp();
        }

    }

    private void responseToUserTouch() {

        mViewModel.getUserSelectTempObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(temps -> refreshTempValue(temps[0], temps[1]), LogHelper::log);

        mRbvLeft.setCrollerChangeListener(new RotateButtonView.OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(RotateButtonView croller, float progress) {
                mViewModel.setUserSelectedTempL(progress);
            }

            @Override
            public void onStartTrackingTouch(RotateButtonView croller, float progress) {

            }

            @Override
            public void onStopTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setUserSelectedTempL(progress);
            }
        });

        mRbvRight.setCrollerChangeListener(new RotateButtonView.OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(RotateButtonView croller, float progress) {
                mViewModel.setUserSelectedTempR(progress);
            }

            @Override
            public void onStartTrackingTouch(RotateButtonView croller, float progress) {

            }

            @Override
            public void onStopTrackingTouch(RotateButtonView croller, float progress) {
                mViewModel.setUserSelectedTempR(progress);
            }
        });
    }

    private void observeNozzleTemp() {
        mViewModel.getNozzleTempObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshDashboard, LogHelper::log);
    }

    private void refreshDashboard(ReplaceHotendViewModel.NozzleTemp temps) {
        mRbvLeft.setColor2Progress(temps.l);
        mRbvRight.setColor2Progress(temps.r);
        refreshTempValue(temps.l, temps.r);
    }

    private void refreshTempValue(int tempL, int tempR) {
        mTvTempL.setText(String.valueOf(tempL));
        mTvTempR.setText(String.valueOf(tempR));
    }

    @OnClick(R2.id.btn_next)
    void onNextClicked() {
        playNormalClickSound();
        mViewModel.heatUpNozzle();
    }
}
