package fabscreen.features.settings.a400.moduleassistant.replacehotend;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;

public class ReplaceHotendRestartingFragment extends BaseFragment {

    @BindView(R2.id.iv_progress_pic)
    ImageView mIvProgressPic;
    @BindView(R2.id.tv_progress_desc)
    TextView mTvProgressDesc;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_hotend_inprogress_display;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        mTvProgressDesc.setText(R.string.replace_hotend_mainboard_updating_tips);
        mIvProgressPic.setBackground(null);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext()).load(R.drawable.pic_a400_mainboard_restart).apply(options).into(mIvProgressPic);
    }
}
