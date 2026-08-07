package fabscreen.platform.core.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import fabscreen.platform.base.helper.SoundUtil;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.core.R;

public class FabSeekBar extends View {
    private int mBackgroundColor;
    private float mRadian;
    private int mNormalColor;
    private int mPressedColor;
    private int mDisabledColor;
    private float mThumbNormalSize;
    private float mThumbClickSize;
    private float mProgressHeight;
    private int mNormalThumbId;
    private int mPressedThumbId;
    private int mDisabledThumbId;
    private Bitmap mNormalThumbDrawable;
    private Bitmap mPressedThumbDrawable;
    private Bitmap mDisabledThumbDrawable;

    private int showMode;
    private Paint mBackgroundPint;
    private Paint mProgressPint;
    private int mH;
    private int mW;
    private float progress;
    private float minValue;
    private float maxValue;
    private OnProgressChangeListener mOnProgressChangeListener;
    private Canvas mBitmapCanvas;
    private Drawable mBitmapDrawable;
    private IAppService mApp;

    public FabSeekBar(Context context) {
        this(context, null);
    }

    public FabSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FabSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FabSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.FabSeekBar, defStyleAttr, defStyleRes);
        mBackgroundColor = typedArray.getColor(R.styleable.FabSeekBar_fsb_backgroundColor, Color.parseColor("#090909"));
        mRadian = typedArray.getDimension(R.styleable.FabSeekBar_fsb_radian, dp2px(83));
        mNormalColor = typedArray.getColor(R.styleable.FabSeekBar_fsb_normalColor, Color.parseColor("#6E6E6E"));
        mPressedColor = typedArray.getColor(R.styleable.FabSeekBar_fsb_pressedColor, Color.parseColor("#1A41F5"));
        mDisabledColor = typedArray.getColor(R.styleable.FabSeekBar_fsb_disabledColor, Color.parseColor("#2E2E2E"));
        mThumbNormalSize = typedArray.getDimension(R.styleable.FabSeekBar_fsb_thumbNormalSize, dp2px(32));
        mThumbClickSize = typedArray.getDimension(R.styleable.FabSeekBar_fsb_thumbClickSize, dp2px(40));
        mProgressHeight = typedArray.getDimension(R.styleable.FabSeekBar_fsb_progressHeight, 28);
        mNormalThumbId = typedArray.getResourceId(R.styleable.FabSeekBar_fsb_normalThumbDrawable, R.drawable.shape_seekbar_btn_normal);
        mPressedThumbId = typedArray.getResourceId(R.styleable.FabSeekBar_fsb_pressedThumbDrawable, R.drawable.shape_seekbar_btn_pressed);
        mDisabledThumbId = typedArray.getResourceId(R.styleable.FabSeekBar_fsb_disabledThumbDrawable, R.drawable.shape_seekbar_btn_disabled);
        minValue = typedArray.getDimension(R.styleable.FabSeekBar_fsb_min, 0);
        maxValue = typedArray.getDimension(R.styleable.FabSeekBar_fsb_max, 100);
        typedArray.recycle();
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    private void initialize(Context context) {
        mApp = ServiceContainer.getInstance().getService(IAppService.class);
        mNormalThumbDrawable = getNewDrawable(mNormalThumbId, (int) (mThumbNormalSize * 2), context);
        mPressedThumbDrawable = getNewDrawable(mPressedThumbId, (int) (mThumbClickSize * 2), context);
        mDisabledThumbDrawable = getNewDrawable(mDisabledThumbId, (int) (mThumbNormalSize * 2), context);

        mBackgroundPint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPint.setStyle(Paint.Style.FILL);
        mBackgroundPint.setStrokeCap(Paint.Cap.ROUND);
        mBackgroundPint.setColor(mBackgroundColor);

        mProgressPint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPint.setStyle(Paint.Style.FILL);
        mProgressPint.setStrokeCap(Paint.Cap.ROUND);

    }


    public Bitmap getNewDrawable(int restId, int dstSize, Context context) {
        Drawable mBitmapDrawable = ContextCompat.getDrawable(context, restId);
        mBitmapDrawable.setBounds(0, 0, dstSize, dstSize);
        Bitmap bitmap = Bitmap.createBitmap(mBitmapDrawable.getIntrinsicWidth(),
                mBitmapDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        mBitmapCanvas = new Canvas(bitmap);
        mBitmapDrawable.setBounds(0, 0, dstSize, dstSize);
        mBitmapDrawable.draw(mBitmapCanvas);
        return bitmap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mH = h;
        mW = w;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawProgress(canvas);
        drawThumb(canvas);

    }

    private void drawThumb(Canvas canvas) {
        Bitmap tempBitmap = null;
        float mPaddingX = 0;
        float mPaddingY = 0;
        switch (showMode) {
            case 0:
                tempBitmap = mNormalThumbDrawable;
                mPaddingX = progress * (mW - mThumbClickSize * 2);
                mPaddingY = mThumbClickSize - mThumbNormalSize;
                break;
            case 1:
                tempBitmap = mPressedThumbDrawable;
                mPaddingX = progress * (mW - mThumbClickSize * 2);
                break;
            case 3:
                tempBitmap = mDisabledThumbDrawable;
                mPaddingX = progress * (mW - mThumbClickSize * 2);
                mPaddingY = mThumbClickSize - mThumbNormalSize;
                break;
            default:
                break;
        }
        if (tempBitmap != null) {
            canvas.drawBitmap(tempBitmap, mPaddingX, mPaddingY, null);
        }
    }

    private void drawProgress(Canvas canvas) {
        switch (showMode) {
            case 0:
                mProgressPint.setColor(mNormalColor);
                break;
            case 1:
                mProgressPint.setColor(mPressedColor);
                break;
            case 3:
                mProgressPint.setColor(mDisabledColor);
                break;
            default:
                break;
        }
        canvas.drawRoundRect(new RectF(mThumbClickSize + 1,
                mThumbClickSize - mProgressHeight / 2f + 1,
                mThumbClickSize + progress * (mW - mThumbClickSize * 2),
                mThumbClickSize + mProgressHeight / 2f - 1), 85, 85, mProgressPint);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawRoundRect(new RectF(mThumbClickSize,
                mThumbClickSize - mProgressHeight / 2f,
                mW - mThumbClickSize,
                mThumbClickSize + mProgressHeight / 2f), 85, 85, mBackgroundPint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        float cx = event.getX();
        cx = Math.max(cx, mThumbClickSize);
        cx = Math.min(cx, mW - mThumbClickSize);
        float oldProgress = progress;
        progress = (cx - mThumbClickSize) / (mW - mThumbClickSize * 2);
        showMode = 1;
        mOnProgressChangeListener.onProgressChanged(this, progress * (maxValue - minValue) + minValue);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            soundView(oldProgress, progress);
            mOnProgressChangeListener.onStopTrackingTouch(this, progress * (maxValue - minValue) + minValue);
            showMode = 0;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            soundView(oldProgress, progress);
            mOnProgressChangeListener.onStartTrackingTouch(this, progress * (maxValue - minValue) + minValue);
        }
        invalidate();
        return true;
    }

    private void soundView(float old, float now) {
        if (old != now) {
            SoundUtil.playSound(mApp.getSoundPool(), mApp.getSoundIdByResourceId(R.raw.sound_rotate_button_se_move));
        }
    }

    public float getProgress() {
        return progress * (maxValue - minValue) + minValue;
    }

    public void setProgress(float progress1) {
        showMode = 0;
        progress = (progress1 - minValue) / (maxValue - minValue);
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        showMode = enabled ? 0 : 3;
        super.setEnabled(enabled);
    }


    public void setOnProgressChangeListener(OnProgressChangeListener onProgressChangeListener) {
        mOnProgressChangeListener = onProgressChangeListener;
    }

    public void setMax(float max) {
        maxValue = max;
        invalidate();
    }

    public void setMin(float min) {
        minValue = min;
        invalidate();
    }

    public interface OnProgressChangeListener {
        void onProgressChanged(FabSeekBar fabSeekBar, float progress);

        void onStartTrackingTouch(FabSeekBar fabSeekBar, float progress);

        void onStopTrackingTouch(FabSeekBar fabSeekBar, float progress);
    }

}
