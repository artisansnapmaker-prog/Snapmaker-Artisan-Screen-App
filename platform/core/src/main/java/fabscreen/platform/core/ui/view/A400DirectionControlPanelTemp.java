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
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.platform.base.helper.ClickHelper;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;
import fabscreen.platform.core.ui.data.MoveController;

@SuppressLint("NonConstantResourceId")
public class A400DirectionControlPanelTemp extends ConstraintLayout {

    //    @BindView(R2.id.sbg_control_steps)
//    SegmentedButtonGroup mSbgControlSteps;
    @BindView(R2.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R2.id.btn_calibration_move_up)
    Button mBtnMoveUp;
    @BindView(R2.id.btn_calibration_move_down)
    Button mBtnMoveDown;
    @BindView(R2.id.tv_calibration_move_type)
    TextView mTvMoveType;

    private OnDirectionClickListener mDirectionListener;
    private float[] mWidths = {0.02f, 0.1f, 1f, 5f};
    private float mStepWidth = mWidths[1];
    private String[] mTabs;
    private Context mContext;

    public A400DirectionControlPanelTemp(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_calibration_control_z_temp, this);
        ButterKnife.bind(this, view);

        setLinTabValue();
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mStepWidth = mWidths[tab.getPosition()];
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
        mTabs = new String[]{mContext.getString(R.string.all_0_02mm)
                , mContext.getString(R.string.all_0_1mm), mContext.getString(R.string.all_1mm)
                , mContext.getString(R.string.all_5mm)};
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

    public A400DirectionControlPanelTemp setStepWidths(float width0, float width1, float width2, float width3) {
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
        if (ClickHelper.isFastDoubleClick(id)) {
            return;
        }
        if (id == R.id.btn_calibration_move_up) {
            mDirectionListener.onDirectionClicked(Direction.UP, mStepWidth);
        } else if (id == R.id.btn_calibration_move_down) {
            mDirectionListener.onDirectionClicked(Direction.DOWN, mStepWidth);
        }
    }

    public void refreshMoveState(MoveController.Direction direction) {
        refreshViewByMovingState(direction);
    }

    public void refreshViewByMovingState(Direction direction) {
        if (direction == null) return;
        Logger.d("refresh moving state: direction=%s", direction);
        mBtnMoveUp.setEnabled(false);
        mBtnMoveDown.setEnabled(false);
        switch (direction) {

            case UP:
                mBtnMoveUp.setEnabled(false);
                mBtnMoveUp.setBackgroundResource(R.drawable.ic_a400_control_up_activated);
                break;
            case DOWN:
                mBtnMoveDown.setEnabled(false);
                mBtnMoveDown.setBackgroundResource(R.drawable.ic_a400_control_down_activated);
                break;
            case IDLE:
                mBtnMoveUp.setEnabled(true);
                mBtnMoveDown.setEnabled(true);
                mBtnMoveUp.setBackgroundResource(R.drawable.control_btn_up_background);
                mBtnMoveDown.setBackgroundResource(R.drawable.control_btn_down_background);
                break;
            default:
                mBtnMoveUp.setBackgroundResource(R.drawable.control_btn_up_background);
                mBtnMoveDown.setBackgroundResource(R.drawable.control_btn_down_background);
                mBtnMoveUp.setEnabled(false);
                mBtnMoveDown.setEnabled(false);
                break;
        }
    }

    public void setOnDirectionClickListener(OnDirectionClickListener listener) {
        mDirectionListener = listener;
    }

    public interface OnDirectionClickListener {
        void onDirectionClicked(Direction direction, float stepWidth);
    }
}
