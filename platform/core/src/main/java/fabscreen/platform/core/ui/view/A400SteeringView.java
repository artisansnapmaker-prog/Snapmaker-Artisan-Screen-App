package fabscreen.platform.core.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.HashMap;

import fabscreen.platform.core.R;

public class A400SteeringView extends SteeringView {
    //    private boolean mFrozen = false;
    private final HashMap<Integer, Drawable> mDisabledDrawables = new HashMap<>();
    private final HashMap<Integer, Drawable> mEnabledDrawables = new HashMap<>();

    private int mH;
    private int mW;
    private Paint mPaint;

    public A400SteeringView(Context context) {
        super(context);
    }

    public A400SteeringView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
        initDrawables();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextSize(28);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initDrawables() {
        Bitmap frozenRight = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.pic_a400_frozen_panel_right);
        mDisabledDrawables.put(DIRECTION_UP, new BitmapDrawable(getResources(), rotateBitmap(frozenRight, -90)));
        mDisabledDrawables.put(DIRECTION_DOWN, new BitmapDrawable(getResources(), rotateBitmap(frozenRight, 90)));
        mDisabledDrawables.put(DIRECTION_LEFT, new BitmapDrawable(getResources(), rotateBitmap(frozenRight, -180)));
        mDisabledDrawables.put(DIRECTION_RIGHT, new BitmapDrawable(getResources(), frozenRight));
        mDisabledDrawables.put(DIRECTION_IDLE, AppCompatResources.getDrawable(getContext(), R.drawable.btn_a400_steering_view_disabled_400x400));

        mEnabledDrawables.put(DIRECTION_UP, AppCompatResources.getDrawable(getContext(), R.drawable.btn_a400_steering_view_up_400x400));
        mEnabledDrawables.put(DIRECTION_DOWN, AppCompatResources.getDrawable(getContext(), R.drawable.btn_a400_steering_view_down_400x400));
        mEnabledDrawables.put(DIRECTION_LEFT, AppCompatResources.getDrawable(getContext(), R.drawable.btn_a400_steering_view_left_400x400));
        mEnabledDrawables.put(DIRECTION_RIGHT, AppCompatResources.getDrawable(getContext(), R.drawable.btn_a400_steering_view_right_400x400));
        mEnabledDrawables.put(DIRECTION_IDLE, AppCompatResources.getDrawable(getContext(), R.drawable.btn_a400_steering_view_400x400));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawCenterText(canvas);

        setButtonBackgrounds();
    }

    private void setButtonBackgrounds() {
        switch (mDirection) {
            case DIRECTION_IDLE:
                setBackground(isEnabled() ? mEnabledDrawables.get(DIRECTION_IDLE) : mDisabledDrawables.get(DIRECTION_IDLE));
                break;
            case DIRECTION_UP:
                setBackground(isEnabled() ? mEnabledDrawables.get(DIRECTION_UP) : mDisabledDrawables.get(DIRECTION_UP));
                break;
            case DIRECTION_DOWN:
                setBackground(isEnabled() ? mEnabledDrawables.get(DIRECTION_DOWN) : mDisabledDrawables.get(DIRECTION_DOWN));
                break;
            case DIRECTION_LEFT:
                setBackground(isEnabled() ? mEnabledDrawables.get(DIRECTION_LEFT) : mDisabledDrawables.get(DIRECTION_LEFT));
                break;
            case DIRECTION_RIGHT:
                setBackground(isEnabled() ? mEnabledDrawables.get(DIRECTION_RIGHT) : mDisabledDrawables.get(DIRECTION_RIGHT));
                break;
        }
    }

    private void drawCenterText(Canvas canvas) {
        int textColor = isEnabled() ? 0xffc9c9c9 : 0xff4d4d4d;
        mPaint.setColor(textColor);

        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float textTop = fontMetrics.top;
        float textBottom = fontMetrics.bottom;
        float baselineCenterDistance = (textBottom - textTop) / 2 - textBottom;
        int baseLineY = (int) (mH / 2f + baselineCenterDistance);

        canvas.drawText("XY", mW / 2f, baseLineY, mPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mH = h;
        mW = w;
    }

    public void setDirection(int direction) {
        mDirection = direction;
        invalidate();
    }

    private Bitmap rotateBitmap(Bitmap src, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }
}
