package fabscreen.features.print.j1platform;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.BaseViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintJ1AdjustmentLightingFragment extends BaseFragment {
    @BindView(R2.id.sbg_j1_print_adjustment_lighting)
    SegmentedButtonGroup mSbgLighting;

    @BindView(R2.id.tv_j1_print_adjustment_lighting_level)
    TextView mTvLightingLevel;

    IMachine mMachine;

    public static Fragment newInstance() {
        return new PrintJ1AdjustmentLightingFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMachine = ServiceContainer.getInstance().getService(IMachine.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();


    }

    private void initView() {
        mMachine.getMachineController().getEnclosure().getEnclosureStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(enclosureStatus -> {
                    int ledValue = enclosureStatus.getLedValue();
                    String level = "Off";
                    if (ledValue >= 125) {
                        level = "High";
                    } else if (ledValue != 0){
                        level = "Low";
                    } else {
                        level = "Off";
                    }
                    mTvLightingLevel.setText(level);
                });

        mSbgLighting.setOnClickedButtonListener(position -> {
            int lightingValue = 0;
            switch (position) {
                case 0:
                    lightingValue = 0;
                    break;
                case 1:
                    lightingValue = 125;
                    break;
                case 2:
                    lightingValue = 255;
                    break;
            }

            setLightingValue();
        });
    }

    void setLightingValue() {

    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_print_adjustment_lighting;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMachine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
            mMachine.getMachineController().getEnclosure().unsubscribeEnclosureInfo();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMachine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
            mMachine.getMachineController().getEnclosure().subscribeEnclosureInfo();
        }
    }

//    @OnClick(R2.id.btn_j1_print_z_offset_minus)
//    void onClickZOffsetDown() {
//        playNormalClickSound();
//    }
//
//    @OnClick(R2.id.btn_j1_print_z_offset_plus)
//    void onClickZOffsetUp() {
//        playNormalClickSound();
//    }
}
