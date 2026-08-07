package fabscreen.features.machinetools.calibration.a400platform;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_IDLE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.machinetools.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.TOOLS_CALIBRATION_A400)
public class A400CalibrationActivity extends BaseActivity {

    View mFloatView;
    ImageView mIvTopToast;
    TextView mTvTopToast;
    ConstraintLayout mClTopToast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        showPrintState();
        Fragment fragment = A400CalibrationFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIvTopToast != null) {
            MachineStatus status = ServiceContainer.getInstance().getService(IMachine.class)
                    .getMachineStatusSubjectHolder()
                    .getValue();
            boolean isIdle = SYSTEM_STATUS_IDLE.valueEquals(status.status);
            boolean isPrint = status.status <= 10;
            boolean is3DP = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM;
            mTvTopToast.setText(getString(R.string.a400_toast_operation_block_by_machine_desc, getString(isPrint && is3DP ? R.string.a400_toast_operation_block_by_machine_printing : R.string.a400_toast_operation_block_by_machine_working)));
            mFloatView.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CalibrationType.Z_CALI_AUTO:
                case CalibrationType.BED_LEVELING_AUTO:
                case CalibrationType.DUAL_EXTRUDER_XY:
                case CalibrationType.THK_MEASURE:
                case CalibrationType.CAMERA_CALI:
                case CalibrationType.AXIS_CENTRAL_CALI:
                case CalibrationType.laser_MANUAL_FOCUS_CALIBRATION:
                    mRouter.routeToCalibrationComplete(requestCode).start(this);
                    break;
            }
        }
    }


    private void showPrintState() {
        ViewGroup rootView = (ViewGroup) this.findViewById(android.R.id.content).getRootView();
        mFloatView = LayoutInflater.from(this).inflate(fabscreen.platform.base.R.layout.view_a400_top_icon_toast, rootView, false);
        rootView.addView(mFloatView);
        mIvTopToast = mFloatView.findViewById(R.id.iv_top_toast);
        mTvTopToast = mFloatView.findViewById(R.id.tv_top_toast);
        mClTopToast = mFloatView.findViewById(R.id.cl_top_toast);
        mIvTopToast.setImageResource(R.drawable.pic_a400_warning_68x68);
        mTvTopToast.setText(R.string.a400_toast_operation_block_by_machine_desc);
        mTvTopToast.setTextColor(getResources().getColor(R.color.palette_white_pure, null));
        mTvTopToast.setTextSize(24);
        ServiceContainer.getInstance().getService(IMachine.class)
                .getMachineStatusSubjectHolder()
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    boolean isIdle = SYSTEM_STATUS_IDLE.valueEquals(status.status);
                    boolean isPrint = status.status <= 10;
                    boolean is3DP = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM;
                    mTvTopToast.setText(getString(R.string.a400_toast_operation_block_by_machine_desc, getString(isPrint && is3DP ? R.string.a400_toast_operation_block_by_machine_printing : R.string.a400_toast_operation_block_by_machine_working)));
                    mClTopToast.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                    mFloatView.setVisibility(isIdle ? View.INVISIBLE : View.VISIBLE);
                }, LogHelper::log);
    }

    public static class CalibrationType {
        // 3dp
        public static final int Z_CALI_MANUAL = 0x01;
        public static final int Z_CALI_AUTO = 0x02;
        public static final int Z_CALI_AUTO_SENSOR = 0x03;
        public static final int BED_LEVELING_MANUAL = 0x04;
        public static final int BED_LEVELING_AUTO = 0x05;
        public static final int DUAL_EXTRUDER_XY = 0x06;

        // laser-10w-3axis
        public static final int THK_MEASURE = 0x07;
        public static final int PLATFORM_HEIGHT_CALI = 0x08;
        public static final int CAMERA_CALI = 0x09;

        // laser-10w-4axis
        public static final int AXIS_CENTRAL_CALI = 0x0a;

        //laser-1.6w-3axis
        public static final int laser_MANUAL_FOCUS_CALIBRATION = 0x11;
    }
}
