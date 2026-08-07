package fabscreen.features.guide.j1;

import androidx.fragment.app.Fragment;

import butterknife.OnClick;
import fabscreen.features.guide.R;
import fabscreen.features.guide.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.FabConfirm;

public class GuideJ1InfoFragment extends BaseFragment {

    public static Fragment newInstance() {
        return new GuideJ1InfoFragment();
    }


    @OnClick(R2.id.btn_next)
    public void onClickNext() {
        playNormalClickSound();
        ((J1GuideActivity) requireActivity()).checkNext();
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_guide_info;
    }
}
