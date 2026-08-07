package fabscreen.platform.core.ui.view;

import static fabscreen.platform.core.ui.data.MoveController.Direction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;
import fabscreen.platform.core.ui.data.MoveController;

@SuppressLint("NonConstantResourceId")
public class A400DirectionControlPanel extends ConstraintLayout {

    @BindView(R2.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R2.id.btn_calibration_move_up)
    Button mBtnMoveUp;
    @BindView(R2.id.btn_calibration_move_down)
    Button mBtnMoveDown;
    @BindView(R2.id.tv_calibration_move_type)
    TextView mTvMoveType;

    private OnDirectionClickListener mDirectionListener;
    private float[] mWidths = {0.1f, 1f, 10f, 100f};
    private float mStepWidth = mWidths[1];
    private String[] mTabs;
    private Context mContext;

    public A400DirectionControlPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_calibration_control_z, this);
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

    public A400DirectionControlPanel setStepWidths(float width0, float width1, float width2, float width3) {
        mWidths[0] = width0;
        mWidths[1] = width1;
        mWidths[2] = width2;
        mWidths[3] = width3;
        return this;
    }

    public void setTypeVisibility(int visibility) {
        mTvMoveType.setVisibility(visibility);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mTabLayout.setEnabled(enabled);
        mBtnMoveUp.setEnabled(enabled);
        mBtnMoveDown.setEnabled(enabled);
    }

    @OnClick({R2.id.btn_calibration_move_up, R2.id.btn_calibration_move_down})
    void onPanelItemClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_calibration_move_up) {
            mDirectionListener.onDirectionClicked(Direction.UP, mStepWidth);
        } else if (id == R.id.btn_calibration_move_down) {
            mDirectionListener.onDirectionClicked(Direction.DOWN, mStepWidth);
        }
    }

    public void setOnDirectionClickListener(OnDirectionClickListener listener) {
        mDirectionListener = listener;
    }

    public interface OnDirectionClickListener {
        void onDirectionClicked(MoveController.Direction direction, float stepWidth);

        void onPositionChange(int position);

        void changPanel(int position);
    }
}
