package fabscreen.platform.core.ui.view.customkeyboard;

import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fabscreen.platform.base.helper.SoundUtil;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.core.R;

public class CustomKeyboardUtil {
    public static int mKeyboardType = 1;// 默认
    public static final int INPUT_TYPE_NUMBER_DECIMAL = 1; // T9 Like Number keyboard
    public static final int INPUT_TYPE_QWERTY_ABC = 6;// QWERTY layout alphabetic keyboard
    public static final int INPUT_TYPE_QWERTY_NUMBER_SYMBOL = 8; // QWERTY layout number/symbol keyboard
    public static final int INPUT_TYPE_QWERTY_SYMBOL = 7;// 标点键盘

    public static final int KEYBOARD_SHOW = 1;
    public static final int KEYBOARD_HIDE = 2;

    private Context mContext;
    private Handler showHandler;
    private CustomKeyboardUtil mCustomKeyboardUtil;
    private IAppService mApp = ServiceContainer.getInstance().getService(IAppService.class);

    private Map<View, TextWatcher> mBindingViews = new LinkedHashMap<>();

    public boolean isCapitalUpper = false;// 是否大写
    public boolean isShow = false;
    private int mMaxLimitLength = -1;

    // Views
    private FrameLayout mRootView;
    private FabKeyBoardView keyboardView;
    private LinearLayout layoutView;
    private LinearLayout keyboardLayout;
    private EditText mInnerEditText;
    private View mCurrentBindView;

    public static Keyboard abcKeyboard; // 字母键盘
    public static Keyboard symbolKeyboard; // 字母键盘
    public static Keyboard numKeyboard; // 数字键盘
    public static Keyboard currentKeyboard; //提供给 keyboardView 进行画

    // Listeners
    InputFinishListener inputOver;
    KeyBoardStateChangeListener keyBoardStateChangeListener;
    private TextWatcher mCurrentViewListener;

    /**
     * 最新构造方法，现在都用这个
     *
     * @param context //* @param rootView rootView 需要是LinearLayout,以适应键盘
     */
    public CustomKeyboardUtil(Context context) {
        this.mContext = context;
        this.mRootView = ((Activity) mContext).getWindow().getDecorView().findViewById(android.R.id.content);
        initKeyboardView();
        mCustomKeyboardUtil = this;
    }

    public void setInputOverListener(InputFinishListener listener) {
        this.inputOver = listener;
    }

    public int getInputType() {
        return mKeyboardType;
    }

    public boolean getKeyboardState() {
        return this.isShow;
    }

    public static Keyboard getKeyboardType() {
        return currentKeyboard;
    }

    public void setPreInputText(String inputText) {
        if (isShow) return;

        if (!mInnerEditText.getText().toString().isEmpty()) {
            mInnerEditText.getText().clear();
        } else {
            mInnerEditText.setText("");
        }
        int start = mInnerEditText.getSelectionStart();
        int length = mInnerEditText.getText().length();
        mInnerEditText.getText().replace(start, length == 0 ? start : length - 1, inputText);
        Editable eText = mInnerEditText.getText();
        mInnerEditText.setSelection(eText.length());
    }

    private void setMyKeyboard(Keyboard newKeyboard) {
        currentKeyboard = newKeyboard;
//        List<Key> keys = currentKeyboard.getKeys();
//        for (Key k: keys) {
//            if (k.codes[0] == 200200) {
//                k.codes[0] = 200210;
//            }
//        }
        keyboardView.setKeyboard(newKeyboard);
    }

    private boolean isAlphabetic(String str) {
        String wordstr = "abcdefghijklmnopqrstuvwxyz";
        return wordstr.contains(str.toLowerCase());
    }

    public void setNumberInputType(int inputType) {
        if (inputType == InputType.TYPE_CLASS_NUMBER || inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL) {
            mInnerEditText.setInputType(inputType);
        }
    }

    public void setMaxLength(int length) {
        mMaxLimitLength = length;
    }

    public int getMaxLength() {
        return  mMaxLimitLength;
    }

    private void initKeyboardType() {
        switch (mKeyboardType) {
            case INPUT_TYPE_NUMBER_DECIMAL:
                mInnerEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                initKeyboard(R.id.keyboard_view_t9like_num);
                keyboardView.setPreviewEnabled(false);
                numKeyboard = new Keyboard(mContext, R.xml.fab_softkeyboard_nums_decimal);
                setMyKeyboard(numKeyboard);
                break;
            case INPUT_TYPE_QWERTY_ABC:
                initKeyboard(R.id.keyboard_view_qwerty);
                mInnerEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                keyboardView.setPreviewEnabled(false);
                abcKeyboard = new Keyboard(mContext, R.xml.fab_softkeyboard_qwerty_alphabet);
                setMyKeyboard(abcKeyboard);
                break;
            case INPUT_TYPE_QWERTY_NUMBER_SYMBOL:
                initKeyboard(R.id.keyboard_view_qwerty);
                mInnerEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                keyboardView.setPreviewEnabled(false);
                symbolKeyboard = new Keyboard(mContext, R.xml.fab_softkeyboard_qwerty_num_symbol);
                setMyKeyboard(symbolKeyboard);
                break;
            case INPUT_TYPE_QWERTY_SYMBOL:
                // TODO
                initKeyboard(R.id.keyboard_view_qwerty);
                mInnerEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                keyboardView.setPreviewEnabled(true);
                symbolKeyboard = new Keyboard(mContext, R.xml.fab_softkeyboard_qwerty_extend_symbol);
                setMyKeyboard(symbolKeyboard);
                break;
        }
    }

    private void initKeyboardView() {
        if (mRootView.findViewById(R.id.ll_custom_keyboard) != null) {
            keyboardLayout = mRootView.findViewById(R.id.ll_custom_keyboard);
        } else {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            keyboardLayout = (LinearLayout) inflater.inflate(R.layout.view_custom_keyboard, null);
            mRootView.addView(keyboardLayout);
        }
        mInnerEditText = keyboardLayout.findViewById(R.id.et_keyboard_inner_edit);
        mInnerEditText.setClickable(false);
        mInnerEditText.requestFocus();
        keyboardLayout.setVisibility(View.GONE);
//        keyBoardLayout.setBackgroundColor(mActivity.getResources().getColor(R.color.product_list_bac));
//        initLayoutHeight(keyBoardLayout);
        layoutView = keyboardLayout;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        keyboardLayout.setLayoutParams(layoutParams);
    }

    private void initKeyboard(int keyBoardViewID) {
        keyboardView = mRootView.findViewById(keyBoardViewID);
        keyboardView.setVisibility(View.VISIBLE);
        keyboardView.setEnabled(true);
        keyboardView.setOnKeyboardActionListener(onKeyboardActionListener);
//        keyboardView.setOnTouchListener((v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                v.performClick();
//            }
//            return false;
//        });
    }

    public boolean setKeyboardCursorNew(EditText edit) {
        // TODO: set TargetEditText? Can we delete args "edit"?
//        mTargetEditText = edit;
        boolean flag = false;

        // Try hiding system keyboard if FabKeyboard wasn't open
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        boolean isOpen = imm.isActive();
        if (isOpen) {
//            if (imm.hideSoftInputFromWindow(mTargetEditText.getWindowToken(), 0))
//                flag = true;
        }

        // Reflex EditText method to set EditText focus.
        int currentVersion = android.os.Build.VERSION.SDK_INT;
        String methodName = null;
        if (currentVersion >= 16) {
            // Android 4.2 or upper
            methodName = "setShowSoftInputOnFocus";
        } else if (currentVersion >= 14) {
            // Android 4.0 or lower
            methodName = "setSoftInputShownOnFocus";
        }

        if (methodName == null) {
            // Could not get any method to set focus. Normally it should not happened.
//            mTargetEditText.setInputType(InputType.TYPE_NULL);
        } else {
            Class<EditText> cls = EditText.class;
            Method setShowSoftInputOnFocus;
            try {
                setShowSoftInputOnFocus = cls.getMethod(methodName, boolean.class);
                setShowSoftInputOnFocus.setAccessible(true);
                setShowSoftInputOnFocus.invoke(edit, false);
            } catch (NoSuchMethodException e) {
                edit.setInputType(InputType.TYPE_NULL);
                e.printStackTrace();
            } catch (IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    public void hideSystemKeyBoard() {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(keyboardLayout.getWindowToken(), 0);
    }

    public void hideAllKeyBoard() {
        hideSystemKeyBoard();
        hideKeyboard();
    }

    class finishListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            hideKeyboard();
        }
    }

    private OnKeyboardActionListener onKeyboardActionListener = new OnKeyboardActionListener() {
        @Override
        public void swipeUp() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void onText(CharSequence text) {
            // TODO: check code here.
            SoundUtil.playSound(mApp.getSoundPool(), mApp.getSoundIdByResourceId(fabscreen.platform.base.R.raw.sound_click));
            if (mInnerEditText == null) return;
            Editable editable = onChangeText(mInnerEditText.getText());
            int start = mInnerEditText.getSelectionStart();
            int end = mInnerEditText.getSelectionEnd();
            String temp = editable.subSequence(0, start) + text.toString() + editable.subSequence(start, editable.length());
            // Check input legit
            if (mMaxLimitLength > 0 && temp.length() > mMaxLimitLength) {
                return;
            }
            mInnerEditText.setText(temp);
            Editable eText = mInnerEditText.getText();
            Selection.setSelection(eText, start + 1);
        }

        @Override
        public void onRelease(int primaryCode) {
            if (mKeyboardType != CustomKeyboardUtil.INPUT_TYPE_QWERTY_NUMBER_SYMBOL
                    && (primaryCode == Keyboard.KEYCODE_SHIFT)) {
                keyboardView.setPreviewEnabled(true);
            }
        }

        @Override
        public void onPress(int primaryCode) {
            if (mKeyboardType == CustomKeyboardUtil.INPUT_TYPE_QWERTY_NUMBER_SYMBOL
                    || mKeyboardType == CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL) {
                keyboardView.setPreviewEnabled(false);
                return;
            }
            if (primaryCode == Keyboard.KEYCODE_SHIFT
                    || primaryCode == Keyboard.KEYCODE_DELETE
                    || primaryCode == 123123
                    || primaryCode == 456456
                    || primaryCode == 200100
                    || primaryCode == 200200
                    || primaryCode == 200210
                    || primaryCode == 300100
                    || primaryCode == 32) {
                keyboardView.setPreviewEnabled(false);
                return;
            }
            keyboardView.setPreviewEnabled(true);
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            SoundUtil.playSound(mApp.getSoundPool(), mApp.getSoundIdByResourceId(fabscreen.platform.base.R.raw.sound_click));
            Editable editable = mInnerEditText.getText();
            if (editable.length() > 0) {
                Selection.setSelection(editable, editable.length());
            }
            int start = mInnerEditText.getSelectionStart();
            int end = mInnerEditText.getSelectionEnd();

            switch (primaryCode) {
                case Keyboard.KEYCODE_CANCEL:
                    hideKeyboard();
                    if (inputOver != null) {
//                        inputOver.inputHasOver(primaryCode, mTargetEditText);
                    }
                    break;
                case Keyboard.KEYCODE_DELETE:
                    if (editable != null && editable.length() > 0) {
                        if (start > 0) {
                            editable.delete(start - 1, start);
                        }
                    }
                    break;
                case Keyboard.KEYCODE_SHIFT:
                    switchCaps();
                    // Not good here.
                    keyboardView.setKeyboard(abcKeyboard);
                    break;
                case Keyboard.KEYCODE_DONE:
//                    if (keyboardView.getRightType() == 4) {
//                    hideKeyboardLayout();
//                    if (inputOver != null)
//                        inputOver.inputHasOver(keyboardView.getRightType(), ed);
//                } else if (keyboardView.getRightType() == 5) {
//                    // 下一个监听
//
//                    if (inputOver != null)
//                        inputOver.inputHasOver(keyboardView.getRightType(), ed);
//                }
                    break;
                case 0:
                    // Space
                    // FIXME: fix unused space on empty text.
                    break;
                case 200100:
                    // Cancel
                    // TODO: original string
//                    mTargetEditText.setText("");
                    mInnerEditText.setText("");
                    hideKeyboard();
                    break;
                case 200200:
                    // Done
                    if (mCurrentViewListener != null) {
                        mCurrentViewListener.afterTextChanged(editable);
                    }
                    hideKeyboard();
                    break;
                case 200210:
                    // Done but disabled;
                    break;
                case 300100:
                    //
                    isCapitalUpper = false;
                    showKeyboard(mCurrentBindView, INPUT_TYPE_QWERTY_SYMBOL);
                    break;
                case 123123:
                    // change keyboard
                    isCapitalUpper = false;
                    showKeyboard(mCurrentBindView, INPUT_TYPE_QWERTY_NUMBER_SYMBOL);
                    break;
                case 456456:
                    isCapitalUpper = false;
                    showKeyboard(mCurrentBindView, INPUT_TYPE_QWERTY_ABC);
                    break;
                case 789789:
                    isCapitalUpper = false;
                    showKeyboard(mCurrentBindView, INPUT_TYPE_QWERTY_SYMBOL);
                    break;
                case 741741:
                    showKeyboard(mCurrentBindView, INPUT_TYPE_QWERTY_ABC);
                    break;
                default:
                    if (mMaxLimitLength > 0 && editable.length() + 1 > mMaxLimitLength) {
                        return;
                    }
                    editable.replace(editable.length(), editable.length(), Character.toString((char) primaryCode));
                    break;
            }
        }
    };

    /**
     * 键盘大小写切换
     */
    private void switchCaps() {
        List<Key> keyList = abcKeyboard.getKeys();
        if (isCapitalUpper) {
            // Caps off
            isCapitalUpper = false;
            for (Key key : keyList) {
                if (key.label != null && isAlphabetic(key.label.toString())) {
                    key.label = key.label.toString().toLowerCase();
                    key.codes[0] = key.codes[0] + 32;
                }
            }
        } else {
            // Caps on and lock
            isCapitalUpper = true;
            for (Key key : keyList) {
                if (key.label != null && isAlphabetic(key.label.toString())) {
                    key.label = key.label.toString().toUpperCase();
                    key.codes[0] = key.codes[0] - 32;
                }
            }
        }
    }

    private Editable onChangeText(Editable e) {
        return e;
    }

    private void hideKeyboardLayout() {
        if (keyboardView != null) {
            int visibility = keyboardView.getVisibility();
            if (visibility == View.VISIBLE) {
                keyboardView.setVisibility(View.INVISIBLE);
            }
        }
        if (layoutView != null) {
            layoutView.setVisibility(View.GONE);
        }
    }

    public void hideKeyboard() {
        if (getKeyboardState()) {
            if (keyboardLayout != null) {
                keyboardLayout.setVisibility(View.GONE);
            }
            if (keyBoardStateChangeListener != null) {
//                keyBoardStateChangeListener.KeyBoardStateChange(KEYBOARD_HIDE, mTargetEditText);
            }
            isShow = false;
            hideKeyboardLayout();
            // Reset inner EditText and attributes
            isCapitalUpper = false;
            mCurrentBindView = null;
            mInnerEditText.getText().clear();
            mMaxLimitLength = -1;
        }
    }

    private Key getCodes(int i) {
        return keyboardView.getKeyboard().getKeys().get(i);
    }

    // Show keyboard
    private void showKeyboardView() {
        // Hide previous keyboard first.
        if (keyboardView != null) {
            keyboardView.setVisibility(View.GONE);
        }

        initKeyboardType();
        isShow = true;
        keyboardView.setVisibility(View.VISIBLE);
    }

    private void realShowKeyboard() {
        this.realShowKeyboard(null);
    }

    private void realShowKeyboard(EditText editText) {
        if (keyboardLayout != null) {
            keyboardLayout.setVisibility(View.VISIBLE);
        }

        showKeyboardView();

        if (keyBoardStateChangeListener != null) {
            keyBoardStateChangeListener.KeyBoardStateChange(KEYBOARD_SHOW, editText);
        }
    }

    public void showKeyboard(View view, int keyboardType) {
        mKeyboardType = keyboardType;
        if (mBindingViews.get(view) == null || view == null) {
            // View did not bind keyboard before or view that didn't exists.
            return;
        } else {
            mCurrentViewListener = mBindingViews.get(view);
            mCurrentBindView = view;
        }

        if (keyboardLayout != null && keyboardLayout.getVisibility() == View.VISIBLE) {
            Log.d("KeyboardUtil", "visible");
        }
//        if (view instanceof EditText) {
//            if (setKeyboardCursorNew((EditText) view)) {
//                Handler showHandler = new Handler();
//                showHandler.postDelayed(this::realShowKeyboard, 400);
//            } else {
//                realShowKeyboard();
//            }
//        } else {
        realShowKeyboard();
//        }

    }

    @Deprecated
    public void showKeyboard(final EditText editText, int keyBoardType, boolean isUser) {
        // TODO: check here.
        if (getKeyboardState()
                && mKeyboardType == keyBoardType) {
            return;
        }

        mKeyboardType = keyBoardType;
        if (keyboardLayout != null && keyboardLayout.getVisibility() == View.VISIBLE) {
            Log.d("KeyboardUtil", "visible");
        }

        if (setKeyboardCursorNew(editText)) {
            Handler showHandler = new Handler();
            showHandler.postDelayed(() -> realShowKeyboard(editText), 400);
        } else {
            realShowKeyboard(editText);
        }
    }

    // Implement interface
    // TODO: refactor this.
    public interface InputFinishListener {
        void inputHasOver(int onclickType, EditText editText);
    }

    /**
     * 监听键盘变化
     */
    // TODO: Check this interface definition and usage.
    public interface KeyBoardStateChangeListener {
        void KeyBoardStateChange(int state, EditText editText);
    }

    public void setKeyBoardStateChangeListener(KeyBoardStateChangeListener listener) {
        this.keyBoardStateChangeListener = listener;
    }

    // Use this method to bind keyboard listener first
    public boolean bindKeyboardListener(View view, TextWatcher watcher) {
        boolean result = false;
        if (!mBindingViews.containsKey(view)) {
            mBindingViews.put(view, watcher);
//            Logger.d("bind view on keyboard " + view.toString());
            bindViewDetachedEvent(view);
            result = true;
        } else {
            // FIXME
            // Already have view binding?
            mBindingViews.replace(view, watcher);
            result = true;
        }
        return result;
    }

    private void bindViewDetachedEvent(View view) {
        if (view != null) {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {

                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (mBindingViews.get(v) != null) {
//                        Logger.d("remove mapping " + v.toString());
                        mBindingViews.remove(v);
                    }
                }
            });
        }
    }
}
