package fabscreen.platform.core.ui.common;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FakeLanguageLoadingActivity extends BaseActivity {
    @BindView(R2.id.iv_bg)
    ImageView mIvBg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_language_loading);
        ButterKnife.bind(this);

        int resId = getIntent().getIntExtra("resId", R.drawable.pic_a400_change_language_bg);
        Glide.with(this).load(resId).dontAnimate().into(mIvBg);
        AndroidSchedulers.mainThread().scheduleDirect(this::finish, 1, TimeUnit.SECONDS);
    }
}
