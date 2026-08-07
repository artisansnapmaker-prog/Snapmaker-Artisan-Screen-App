package fabscreen.platform.core.ui.view.customkeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import java.util.List;

import fabscreen.platform.core.R;

public class FabKeyBoardView extends KeyboardView {
    private Context mContext;
    private int heightPixels;
    private float density;
    private static Keyboard mKeyBoard;

    public FabKeyBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        heightPixels = mContext.getResources().getDisplayMetrics().heightPixels;
        density = mContext.getResources().getDisplayMetrics().density;
    }

    public FabKeyBoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        heightPixels = mContext.getResources().getDisplayMetrics().heightPixels;
        density = mContext.getResources().getDisplayMetrics().density;
    }

    /**
     * 重新画一些按键
     */
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // TODO: refactor this, prevent get keyboard type from CustomKeyboardUtil
        mKeyBoard = CustomKeyboardUtil.getKeyboardType();
        List<Key> keys = mKeyBoard.getKeys();

        for (Key key : keys) {
            if (mKeyBoard.equals(CustomKeyboardUtil.numKeyboard)) {
                drawNumSpecialKey(key, canvas);
            } else if (mKeyBoard.equals(CustomKeyboardUtil.abcKeyboard)) {
                drawABCSpecialKey(key, canvas);
            } else if (mKeyBoard.equals(CustomKeyboardUtil.symbolKeyboard)) {
                drawSymbolSpecialKey(key, canvas);
            }
        }
    }

    //数字键盘
    private void drawNumSpecialKey(Key key, Canvas canvas) {
        if (key.codes[0] == -5) {
            drawKeyBackground(R.drawable.btn_keyboard_key_num_delete, canvas, key);
        }
        if (key.codes[0] == 200100) {
            drawKeyBackground(R.drawable.btn_keyboard_key2, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200200) {
            drawKeyBackground(R.drawable.btn_keyboard_key_done, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200210) {
            drawKeyBackground(R.drawable.btn_keyboard_key_done_disabled, canvas, key);
            drawText(canvas, key);
        }

//        // 右下角的按键
//        if (key.codes[0] == 0
//                || key.codes[0] == 741741
//                || key.codes[0] == 88
//                || (key.codes[0] == -4 && key.label != null)
//                || key.codes[0] == 46) {
//            drawKeyBackground(R.drawable.btn_keyboard_key2, canvas, key);
//            drawText(canvas, key);
//        }
    }

    //字母键盘特殊处理背景
    private void drawABCSpecialKey(Key key, Canvas canvas) {
        //TODO 待添加特殊处理
        if (key.codes[0] == -5) {
            drawKeyBackground(R.drawable.btn_keyboard_key_delete, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == -1) {
            drawKeyBackground(R.drawable.btn_keyboard_key_shift, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 123123) {
            drawKeyBackground(R.drawable.btn_keyboard_key2, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 32) {
            drawKeyBackground(R.drawable.btn_keyboard_key_space, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200100) {
            drawKeyBackground(R.drawable.btn_keyboard_key2, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200200) {
            drawKeyBackground(R.drawable.btn_keyboard_key_done, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200210) {
            drawKeyBackground(R.drawable.btn_keyboard_key_done_disabled, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 300100) {
            // temporary disabled
            drawKeyBackground(R.drawable.btn_keyboard_key_done_disabled, canvas, key);
            drawText(canvas, key);
        }

    }

    //标点键盘特殊处理背景
    private void drawSymbolSpecialKey(Key key, Canvas canvas) {
        if (key.codes[0] == 123123 || key.codes[0] == 456456 || key.codes[0] == 300100) {
            drawKeyBackground(R.drawable.btn_keyboard_key2, canvas, key);
            drawText(canvas, key);
        }

        if (key.codes[0] == -5) {
            drawKeyBackground(R.drawable.btn_keyboard_key_delete, canvas, key);
        }

        if (key.codes[0] == 32) {
            drawKeyBackground(R.drawable.btn_keyboard_key_space, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200100) {
            drawKeyBackground(R.drawable.btn_keyboard_key2, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200200) {
            drawKeyBackground(R.drawable.btn_keyboard_key_done, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 200210) {
            drawKeyBackground(R.drawable.btn_keyboard_key_done_disabled, canvas, key);
            drawText(canvas, key);
        }
        if (key.codes[0] == 300100) {
            // temporary disabled
            drawKeyBackground(R.drawable.btn_keyboard_key2, canvas, key);
            drawText(canvas, key);
        }
    }

    private void drawKeyBackground(int drawableId, Canvas canvas, Key key) {
        Drawable npd = AppCompatResources.getDrawable(mContext, drawableId);
        int[] drawableState = key.getCurrentDrawableState();
        if (npd != null) {
            if (key.codes[0] != 0) {
                npd.setState(drawableState);
            }
            npd.setBounds(key.x, key.y, key.x + key.width, key.y
                    + key.height);
            npd.draw(canvas);
        }
    }

    private void drawText(Canvas canvas, Key key) {
        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        // Special case，set '.' symbol more large size for a better visual experience.
        if (key.codes[0] == 46) {
            paint.setTextSize(70);
        } else {
            paint.setTextSize(40);
        }
        paint.setAntiAlias(true);

        paint.setColor(Color.BLACK);
        if (mKeyBoard.equals(CustomKeyboardUtil.numKeyboard)) {
            if (key.label != null) {
                paint.setColor(ContextCompat.getColor(mContext, R.color.palette_white_pure));
                paint.getTextBounds(key.label.toString(), 0, key.label.toString()
                        .length(), bounds);
                canvas.drawText(key.label.toString(), key.x + (key.width * 0.5f),
                        (key.y + key.height * 0.5f) + bounds.height() * 0.5f, paint);
            }  else if (key.codes[0] == -5) {
                key.icon.setBounds(key.x + (int) (0.4 * key.width), key.y + (int) (0.328
                        * key.height), key.x + (int) (0.6 * key.width), key.y + (int) (0.672
                        * key.height));
                key.icon.draw(canvas);
            }
        } else if (mKeyBoard.equals(CustomKeyboardUtil.abcKeyboard)) {
            if (key.label != null) {
                paint.setColor(ContextCompat.getColor(mContext, R.color.palette_white_pure));
                paint.getTextBounds(key.label.toString(), 0, key.label.toString()
                        .length(), bounds);
                canvas.drawText(key.label.toString(), key.x + (key.width * 0.5f),
                        (key.y + key.height * 0.5f) + bounds.height() * 0.5f, paint);
            }
        } else if (mKeyBoard.equals(CustomKeyboardUtil.symbolKeyboard)) {
            paint.setColor(ContextCompat.getColor(mContext, R.color.palette_white_pure));
            paint.getTextBounds(key.label.toString(), 0, key.label.toString()
                    .length(), bounds);
            canvas.drawText(key.label.toString(), key.x + (key.width * 0.5f),
                    (key.y + key.height * 0.5f) + bounds.height() * 0.5f, paint);
        }
    }
}
