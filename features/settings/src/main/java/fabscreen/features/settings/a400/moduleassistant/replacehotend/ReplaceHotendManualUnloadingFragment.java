package fabscreen.features.settings.a400.moduleassistant.replacehotend;

import static fabscreen.features.settings.a400.moduleassistant.replacehotend.ReplaceHotendViewModel.IDLE;
import static fabscreen.features.settings.a400.moduleassistant.replacehotend.ReplaceHotendViewModel.LEFT_BUSY;
import static fabscreen.features.settings.a400.moduleassistant.replacehotend.ReplaceHotendViewModel.RIGHT_BUSY;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceHotendManualUnloadingFragment extends BaseFragment {

    @BindView(R2.id.btn_unload_l)
    Button mBtnUnloadL;
    @BindView(R2.id.btn_unload_r)
    Button mBtnUnloadR;
    @BindView(R2.id.btn_next)
    Button mBtnNext;
    @BindView(R2.id.iv_unloading)
    VideoPlayerIJK mVpUnloading;

    private ReplaceHotendViewModel mViewModel;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_hotend_manual_unloading;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceHotendViewModel.class);
        initView();
        initVideo();
    }

    private void initView() {
        mViewModel.getExtrudeObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshView, LogHelper::log);
    }

    @OnClick(R2.id.btn_unload_l)
    void onClickUnloadL() {
        mViewModel.unloadFilament(0);
    }

    @OnClick(R2.id.btn_unload_r)
    void onClickUnloadR() {
        mViewModel.unloadFilament(1);
    }

    @OnClick(R2.id.btn_next)
    void onClickNext() {
        mViewModel.filamentClearConfirmed();
    }

    /**
     * Memo: There won't be two extruders extrude at same time.
     *
     * @param extrudeStatus -1 idle, 0 left extruding, 1 right extruding
     */
    private void refreshView(int extrudeStatus) {

        mBtnUnloadL.setEnabled(extrudeStatus == IDLE);
        mBtnUnloadR.setEnabled(extrudeStatus == IDLE);
        mBtnNext.setEnabled(extrudeStatus == IDLE);

        switch (extrudeStatus) {
            case IDLE:
                mBtnUnloadL.setBackgroundResource(R.drawable.selector_a400_filament_unload);
                mBtnUnloadR.setBackgroundResource(R.drawable.selector_a400_filament_unload);
                break;

            case LEFT_BUSY:
                mBtnUnloadL.setBackgroundResource(R.drawable.a400_filament_unload_select);
                break;

            case RIGHT_BUSY:
                mBtnUnloadR.setBackgroundResource(R.drawable.a400_filament_unload_select);
                break;
        }
    }

    private void initVideo() {
        mVpUnloading.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/Replace_hotend_manual_unloading.webm");
        mVpUnloading.setLooping(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mVpUnloading.setLooping(false);
        mVpUnloading.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVpUnloading.setLooping(true);
        mVpUnloading.start();
    }
}
