package fabscreen.features.print.a400platform;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;

import fabscreen.features.print.R;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.PRINT_SETTING)
public class A400PrintSettingActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        FrameLayout mFlContainer = findViewById(R.id.fragment_container);
        mFlContainer.setBackgroundResource(R.color.palette_black_transparent_20);
        getPrintControllerCallback();
        PrintA400AdjustmentContainerFragment fragment = new PrintA400AdjustmentContainerFragment();
        addFragment(R.id.fragment_container, fragment);
        overridePendingTransition(0, 0);
    }

    private void getPrintControllerCallback() {
        ServiceContainer.getInstance().getService(IMachine.class)
                .getNewPrintController()
                .getPrintEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(printEvent -> {
                    Intent intent = new Intent();
                    intent.putExtra("print_event_state", printEvent.getPrintEventState().ordinal());
                    intent.putExtra("error_code", printEvent.getErrorCode());
                    setResult(Activity.RESULT_FIRST_USER, intent);
                    switch (printEvent.getPrintEventState()) {
                        case FINISH_SUCCESS:
                        case STOP_SUCCESS:
                            finish();
                            break;
                        case STATE_SUCCESS:
                        case START_FAIL:
                        case PAUSE_SUCCESS:
                        case PAUSE_FAIL:
                        case RESUME_SUCCESS:
                        case RESUME_FAIL:
                        case POWER_LOSS_RESUME_SUCCESS:
                        case POWER_LOSS_RESUME_FAIL:
                        case FINISH_FAIL:
                        case OPEN_DOOR_PAUSE:
                        default:
                            break;
                    }
                }, LogHelper::log);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.push_alpha_out);
    }

    @Override
    public void onFinishSuccess(String fileName, int printTime) {
        // NoToDo
    }
}
