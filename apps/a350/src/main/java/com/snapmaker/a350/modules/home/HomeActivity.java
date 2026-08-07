package com.snapmaker.a350.modules.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.R2;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.features.home.LauncherFragment;
import fabscreen.platform.base.Constants;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.view.BaseActivity;

@Route(path = RoutePath.FABSCREEN_INDEX)
public class HomeActivity extends BaseActivity {
    @BindView(R2.id.view_pager)
    ViewPager mViewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        initView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(Constants.KEY_IS_FORCE_BACK_HOME, false)) {
            gotoPage(0);
        }
    }

    private void initView() {
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
//                        return HomeFragment.newInstance();
                    case 1:
                        return LauncherFragment.newInstance();
                    default:
                        return null;
                }
            }
        });
        mViewPager.setOffscreenPageLimit(2);
    }

    public void gotoPage(int position) {
        mViewPager.setCurrentItem(position);
    }
}
