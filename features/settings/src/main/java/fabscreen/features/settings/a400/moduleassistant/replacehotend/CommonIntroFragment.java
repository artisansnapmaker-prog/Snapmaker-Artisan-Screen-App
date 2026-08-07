package fabscreen.features.settings.a400.moduleassistant.replacehotend;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.view.VideoPlayerIJK;

public class CommonIntroFragment extends BaseFragment {
    public static final String KEY_OPERATION = "operation";
    public static final int INTRO = 1;
    public static final int REPLACE = 2;
    private int mOperation;
    private ReplaceHotendViewModel mViewModel;

    @BindView(R2.id.iv_intro)
    ImageView mIvIntro;
    @BindView(R2.id.pv_intro)
    VideoPlayerIJK mPvIntro;
    @BindView(R2.id.tv_intro_title)
    TextView mTvIntroTitle;
    @BindView(R2.id.tv_intro_content)
    TextView mTvIntroContent;
    @BindView(R2.id.btn_start)
    Button mBtnStart;
    @BindView(R2.id.cv_main_pic)
    CardView mCvMainPic;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_common_intro_fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceHotendViewModel.class);
        initView();
    }

    private void initView() {
        mOperation = requireArguments().getInt(KEY_OPERATION);
        @DrawableRes int imgRes = 0;
        if (mOperation == INTRO) {
            mTvIntroContent.setText(R.string.replace_hotend_intro_content);
            mIvIntro.setVisibility(View.VISIBLE);
            mCvMainPic.setVisibility(View.INVISIBLE);
            mBtnStart.setText(R.string.all_start);
            RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
            Glide.with(requireContext())
                    .load(R.drawable.pic_replace_nozzle_01)
                    .apply(options)
                    .into(mIvIntro);
        } else if (mOperation == REPLACE) {
//            imgRes = R.drawable.yyy;
            mTvIntroTitle.setVisibility(View.VISIBLE);
            mTvIntroTitle.setText(R.string.replace_hotend_do_replace_title);
            mTvIntroContent.setText(R.string.replace_hotend_do_replace_content);
            mCvMainPic.setVisibility(View.VISIBLE);
            mIvIntro.setVisibility(View.INVISIBLE);
            initVideo();
            mBtnStart.setText(R.string.all_done);
        }

    }

    @OnClick(R2.id.btn_start)
    void onStartClicked() {
        playNormalClickSound();
        if (mOperation == INTRO) {
            if (requireActivity() instanceof ReplaceHotendActivity) {
                ((ReplaceHotendActivity) requireActivity()).goToReplaceHotendProcess();
            }
        } else if (mOperation == REPLACE) {
            mViewModel.restartMainboard();
        }
    }

    private void initVideo() {
        mPvIntro.setVideoPath(ServiceContainer.getInstance().getService(IAppService.class).getVideDir() + "/Replace_nozzle_replace_the_nozzle.webm");
        mPvIntro.setLooping(true);
        mPvIntro.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPvIntro.setLooping(false);
        mPvIntro.stop();
    }

}
