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

import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;

@SuppressLint("NonConstantResourceId")
public class XYZControlPanelFourRange extends ConstraintLayout {

    @BindView(R2.id.sbg_control_steps)
    SegmentedButtonGroup mSbgControlSteps;
    @BindView(R2.id.sv_control_panel_xy)
    A400SteeringView mSvControlXY;
    @BindView(R2.id.btn_control_panel_z_plus)
    Button mBtnControlZPlus;
    @BindView(R2.id.btn_control_panel_z_minus)
    Button mBtnControlZMinus;
    @BindView(R2.id.tv_control_panel_z_title)
    TextView mTvControlZTitle;
    private OnDirectionClickListener mDirectionListener;
    private float[] mWidths = {0.1f, 1f, 10f, 100f};
    private float mStepWidth = mWidths[1];

    public XYZControlPanelFourRange(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_control_panel_xyz_axes_4_rang_mini, this);
        ButterKnife.bind(this, view);
        mSbgControlSteps.setPosition(1, false);
        mSbgControlSteps.setOnPositionChangedListener(position -> {
            if (mWidths.length < 4) return;
            mStepWidth = mWidths[position];
        });
        // steering view
        mSvControlXY.setOnDirectionClickedListener(state -> {
            switch (state) {
                case SteeringView.DIRECTION_UP:
                    mDirectionListener.onDirectionClicked(Direction.FORWARD, mStepWidth);
                    break;
                case SteeringView.DIRECTION_DOWN:
                    mDirectionListener.onDirectionClicked(Direction.BACKWARD, mStepWidth);
                    break;
                case SteeringView.DIRECTION_LEFT:
                    mDirectionListener.onDirectionClicked(Direction.LEFT, mStepWidth);
                    break;
                case SteeringView.DIRECTION_RIGHT:
                    mDirectionListener.onDirectionClicked(Direction.RIGHT, mStepWidth);
                    break;
            }
        });
    }

    public XYZControlPanelFourRange setStepWidths(float width0, float width1, float width2, float width3) {
        mWidths[0] = width0;
        mWidths[1] = width1;
        mWidths[2] = width2;
        mWidths[3] = width3;
        return this;
    }

    public void setXYEnabled(boolean enabled) {
        mSvControlXY.setEnabled(enabled);
    }

    public void setZEnabled(boolean enabled) {
        mBtnControlZPlus.setEnabled(enabled);
        mBtnControlZMinus.setEnabled(enabled);
    }

    @OnClick({R2.id.btn_control_panel_z_plus, R2.id.btn_control_panel_z_minus})
    void onPanelItemClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_control_panel_z_plus) {
            mDirectionListener.onDirectionClicked(Direction.UP, mStepWidth);
        } else if (id == R.id.btn_control_panel_z_minus) {
            mDirectionListener.onDirectionClicked(Direction.DOWN, mStepWidth);
        }
    }

    public void refreshViewByMovingState(Direction direction) {
        if (direction == null) return;
        Logger.d("refresh moving state: direction=%s", direction);
        mSvControlXY.setEnabled(false);
        mBtnControlZPlus.setEnabled(false);
        mBtnControlZMinus.setEnabled(false);
        mTvControlZTitle.setEnabled(false);
        switch (direction) {
            case FORWARD:
                mSvControlXY.setDirection(SteeringView.DIRECTION_UP);
                break;
            case BACKWARD:
                mSvControlXY.setDirection(SteeringView.DIRECTION_DOWN);
                break;
            case LEFT:
                mSvControlXY.setDirection(SteeringView.DIRECTION_LEFT);
                break;
            case RIGHT:
                mSvControlXY.setDirection(SteeringView.DIRECTION_RIGHT);
                break;
            case UP:
                mSvControlXY.setEnabled(false);
                mBtnControlZPlus.setBackgroundResource(R.drawable.ic_a400_control_up_activated);
                break;
            case DOWN:
                mSvControlXY.setEnabled(false);
                mBtnControlZMinus.setBackgroundResource(R.drawable.ic_a400_control_down_activated);
                break;
            case DISABLE:
                mSvControlXY.setEnabled(false);
                mBtnControlZPlus.setEnabled(false);
                mBtnControlZMinus.setEnabled(false);
                break;
            case IDLE:
                mSvControlXY.setDirection(SteeringView.DIRECTION_IDLE);
                mSvControlXY.setEnabled(true);
                mBtnControlZPlus.setEnabled(true);
                mBtnControlZMinus.setEnabled(true);
                mTvControlZTitle.setEnabled(true);
                mBtnControlZPlus.setBackgroundResource(R.drawable.control_btn_up_background);
                mBtnControlZMinus.setBackgroundResource(R.drawable.control_btn_down_background);
                break;
            default:
                mSvControlXY.setEnabled(false);
                mBtnControlZPlus.setBackgroundResource(R.drawable.control_btn_up_background);
                mBtnControlZMinus.setBackgroundResource(R.drawable.control_btn_down_background);
                mBtnControlZPlus.setEnabled(false);
                mBtnControlZMinus.setEnabled(false);
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
