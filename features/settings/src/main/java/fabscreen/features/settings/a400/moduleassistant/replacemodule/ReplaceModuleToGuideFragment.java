package fabscreen.features.settings.a400.moduleassistant.replacemodule;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;

public class ReplaceModuleToGuideFragment extends BaseFragment {

    @BindView(R2.id.tv_desc)
    TextView mTvDesc;
    @BindView(R2.id.btn_to_guide)
    Button mBtnToGuide;

    public static Fragment newInstance() {
        return new ReplaceModuleToGuideFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
        playProcedureCompleteSound();
    }

    private void initView() {
        mTvDesc.setText(R.string.replace_module_complete_desc);
        mBtnToGuide.setText(R.string.a400_settings_replace_module_to_guide_desc);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_module_to_guide;
    }

    @OnClick({R2.id.btn_to_guide})
    @Override
    public void onClick(View view) {
        super.onClick(view);
        mRouter.routeToGuideMilestone().start(requireActivity());
        requireActivity().finish();
    }
}
