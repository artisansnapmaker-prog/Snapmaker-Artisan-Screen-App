package fabscreen.platform.core.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;
import fabscreen.platform.core.ui.common.a400jogpanel.JogPanelViewPagerAdapter;
import fabscreen.platform.core.ui.data.MoveController;


public class A400XYZBControlPanel extends ConstraintLayout {

    @BindView(R2.id.vp_calibration_control)
    ViewPager2 mVpControl;
    @BindView(R2.id.iv_calibration_control_first)
    ImageView mIvFirstIndicator;
    @BindView(R2.id.iv_calibration_control_second)
    ImageView mIvSecondIndicator;
    @BindView(R2.id.tab_layout)
    TabLayout mTabLayout;

    private OnDirectionClickListener mDirectionListener;
    private Context mContext;
    private JogPanelViewPagerAdapter mViewPageAdapter;
    private float[] mWidths = {0.1f, 1f, 10f, 100f};
    private float mStepWidth = mWidths[1];
    private int mViewPageCuter = 0;
    private String[] mTabs;

    public A400XYZBControlPanel(@NonNull Context context) {
        super(context);
        init(context);
    }

    public A400XYZBControlPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_calibration_control_xyzb, this);
        mContext = context;
        ButterKnife.bind(this, view);

        setLinTabValue();

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (mDirectionListener != null) {
                    mStepWidth = mWidths[tab.getPosition()];
                    mDirectionListener.onPositionChange(tab.getPosition());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mTabLayout.selectTab(mTabLayout.getTabAt(1));

        List<Integer> vpTypeList = new ArrayList<>();
        vpTypeList.add(JogPanelViewPagerAdapter.XYZ_TYPE);
        vpTypeList.add(JogPanelViewPagerAdapter.B_TYPE);
        mViewPageAdapter = new JogPanelViewPagerAdapter(vpTypeList);
        mVpControl.setAdapter(mViewPageAdapter);
        mVpControl.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (mViewPageCuter != position) {
                    mViewPageCuter = position;
                    changeSign(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        mViewPageAdapter.setOnItemClickListener(new JogPanelViewPagerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MoveController.Direction direction) {
                if (mDirectionListener != null) {
                    mDirectionListener.onDirectionClicked(direction, mStepWidth);
                }
            }
        });

    }

    public void hasZ(boolean isVisibility) {
        mViewPageAdapter.setZVisibility(isVisibility);
    }

    public void setLinTabValue() {
        mTabs = new String[]{mContext.getString(R.string.all_0_1mm)
                , mContext.getString(R.string.all_1mm), mContext.getString(R.string.all_10mm)
                , mContext.getString(R.string.all_100mm)};
        if (mTabLayout.getTabCount() > 0) {
            for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                mTabLayout.getTabAt(i).setText(mTabs[i]);
            }
        } else {
            for (int i = 0; i < mTabs.length; i++) {
                mTabLayout.addTab(mTabLayout.newTab().setText(mTabs[i]));
            }
        }

    }

    public void setRotateTabValue() {
        mTabs = new String[]{mContext.getString(R.string.all_0_2degree)
                , mContext.getString(R.string.all_1degree), mContext.getString(R.string.all_10degree)
                , mContext.getString(R.string.all_90degree)};
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            mTabLayout.getTabAt(i).setText(mTabs[i]);
        }
    }

    public void setStepWidths(float width0, float width1, float width2, float width3) {
        mWidths[0] = width0;
        mWidths[1] = width1;
        mWidths[2] = width2;
        mWidths[3] = width3;
    }

    private void changeSign(int position) {
        mIvFirstIndicator.setBackgroundResource(position == 0 ? R.drawable.ic_view_pager_normal : R.drawable.ic_view_pager_select);
        mIvSecondIndicator.setBackgroundResource(position == 1 ? R.drawable.ic_view_pager_normal : R.drawable.ic_view_pager_select);
        if (position == 0) {
            setLinTabValue();
        } else {
            setRotateTabValue();
        }
        mTabLayout.selectTab(mTabLayout.getTabAt(1));
        mDirectionListener.onPositionChange(position);
        if (mDirectionListener != null) {
            mDirectionListener.changPanel(position);
        }
    }

    public void setRotaryStuffVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        mIvFirstIndicator.setVisibility(visibility);
        mIvSecondIndicator.setVisibility(visibility);
        mVpControl.setUserInputEnabled(visible);
    }


    public void setOnDirectionClickListener(OnDirectionClickListener listener) {
        mDirectionListener = listener;
    }

    public void onMachineMoving(Boolean isHoming) {
        mViewPageAdapter.onMachineMoving(isHoming ? MoveController.Direction.DISABLE : MoveController.Direction.IDLE);
    }

    public void refreshMoveState(MoveController.Direction direction) {
        if (mViewPageAdapter == null) return;
        mViewPageAdapter.onMachineMoving(direction);
    }

    public interface OnDirectionClickListener {
        void onDirectionClicked(MoveController.Direction direction, float stepWidth);

        void onPositionChange(int position);

        void changPanel(int position);
    }
}
