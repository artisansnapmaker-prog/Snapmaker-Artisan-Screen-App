package fabscreen.features.machinetools.calibration.j1Platform.calibrationCheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

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
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.controller.FDMController;

public class CalibrationCheckInfoFragment extends J1CalibrationBaseFragment {
    @BindView(R2.id.btn_next)
    Button mBtNext;
    @BindView(R2.id.iv_show_image)
    ImageView mIvShowImage;
    FDMController fdmController;


    public static Fragment newInstance() {
        return new CalibrationCheckInfoFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fdmController = ServiceContainer.getInstance().getService(IMachine.class).getFDMController();
        initView();

    }

    private void initView() {
        Glide.with(this)
                .load(R.drawable.gif_j1calibration_check_info)
                .into(mIvShowImage);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_auxiliary_calibration_check;
    }

    @OnClick(R2.id.btn_next)
    void onClickNext() {
        playNormalClickSound();
        ((CalibrationCheckCalibrationActivity) getActivity()).gotoPrintFragment();
    }


    @OnClick(R2.id.tv_calibration_load_filament)
    public void onCheckLoadFilament() {
        playNormalClickSound();
        ServiceContainer.getInstance().getService(IRouter.class).routeToControlPage().start(getContext());
    }
}
