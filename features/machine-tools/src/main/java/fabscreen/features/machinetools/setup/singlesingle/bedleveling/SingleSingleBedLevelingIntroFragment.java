package fabscreen.features.machinetools.setup.singlesingle.bedleveling;

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
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.view.BaseFragment;

public class SingleSingleBedLevelingIntroFragment extends BaseFragment {
    @BindView(R2.id.iv_setup_intro)
    ImageView mIvIntro;

    public static Fragment newInstance() {
        return new SingleSingleBedLevelingIntroFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(R.drawable.pic_initialize_single_extrusion_module_heated_bed_leveling)
                .apply(options)
                .into(mIvIntro);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_setup_intro;
    }

    @OnClick(R2.id.btn_start)
    void onStartClicked() {
        playNormalClickSound();
        ((SingleSingleBedLevelingActivity) requireActivity()).goToBedLeveling();
    }
}
