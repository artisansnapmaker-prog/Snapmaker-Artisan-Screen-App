package fabscreen.features.settings.a400.moduleassistant.replacemodule;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.core.ui.base.BaseProgressFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceModuleRestartFragment extends BaseProgressFragment {
    @BindView(R2.id.iv_restart)
    ImageView mIvRestart;
    private ReplaceModuleViewModel mViewModel;

    public static Fragment newInstance() {
        return new ReplaceModuleRestartFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceModuleViewModel.class);
        initView();
    }

    private void initView() {
        setMainTitle(getString(R.string.replace_module_title));
        setSubTitle(getString(R.string.replace_module_initialize_2_3));
        setProgress(2, 3);
        setIfShowClose(false);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_a400_mainboard_restart)
                .apply(options)
                .into(mIvRestart);
        mViewModel.getMachineRestartObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> goToConfirmation(), LogHelper::log);
    }

    private void goToConfirmation() {
        if (requireActivity() instanceof A400ReplaceModuleActivity) {
            ((A400ReplaceModuleActivity) requireActivity()).goToConfirmation();
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_module_restart;
    }
}
