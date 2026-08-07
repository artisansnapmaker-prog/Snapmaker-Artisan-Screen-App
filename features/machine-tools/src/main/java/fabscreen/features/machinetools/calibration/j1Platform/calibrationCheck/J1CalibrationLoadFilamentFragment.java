package fabscreen.features.machinetools.calibration.j1Platform.calibrationCheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.control.j1.J1FilamentControlFragment;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.HelpBean;
import fabscreen.platform.core.ui.view.HelpDialog;

public class J1CalibrationLoadFilamentFragment extends BaseFragment {
    @BindView(R2.id.btn_top_bar_help)
    Button mBtnHelp;
    @BindView(R2.id.tv_top_bar_help)
    TextView mTvHelp;

    public static Fragment newInstance() {
        return new J1CalibrationLoadFilamentFragment();
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_load_filament;
    }

    @OnClick({R2.id.btn_top_bar_help, R2.id.tv_top_bar_help})
    public void onClickHelp() {
        HelpDialog.create(requireContext(), getHelpList()).show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBtnHelp.setVisibility(View.VISIBLE);
        mTvHelp.setVisibility(View.VISIBLE);
        Fragment fragment = J1FilamentControlFragment.newInstance();
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction().replace(fabscreen.platform.core.R.id.fcv_detail, fragment).commit();
    }

    public List<HelpBean> getHelpList() {
        List<HelpBean> list = new ArrayList<>();
        list.add(new HelpBean(R.drawable.gif_help_content_1, getString(R.string.j1_how_to_load_filament_step_1)));
        list.add(new HelpBean(R.drawable.gif_help_content_2, getString(R.string.j1_how_to_load_filament_step_2)));
        list.add(new HelpBean(R.drawable.gif_help_content_3, getString(R.string.j1_how_to_load_filament_step_3)));
        list.add(new HelpBean(R.drawable.pic_help_content_4, getString(R.string.j1_how_to_load_filament_step_4)));
        list.add(new HelpBean(R.drawable.gif_help_content_5, getString(R.string.j1_how_to_load_filament_step_5)));
        list.add(new HelpBean(R.drawable.pic_help_content_6, getString(R.string.j1_how_to_load_filament_step_6)));
        list.add(new HelpBean(R.drawable.gif_help_content_7, getString(R.string.j1_how_to_load_filament_step_7)));
        return list;
    }


}
