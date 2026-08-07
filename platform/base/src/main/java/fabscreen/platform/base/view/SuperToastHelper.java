package fabscreen.platform.base.view;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;

public class SuperToastHelper {
    // Toast show 1.75s as default
    public static int TOAST_SHOW_SHORT = 1750;
    // Long period showing toast was set 17s.
    public static int TOAST_SHOW_LONG = 17000;

    private int mDrawableId;
    private String mMessageId;
    private String mTitleId;
    private int mShowTime;
    private int mJ1UsbLogo;
    private boolean mCloseabledDisplay = false;

    private SuperToastHelper(int drawableId, String message, String title, int showTime, int j1UsbLogo, boolean isCloseable) {
        mDrawableId = drawableId;
        mMessageId = message;
        mTitleId = title;
        mShowTime = showTime;
        mJ1UsbLogo = j1UsbLogo;
        mCloseabledDisplay = isCloseable;
    }

    public void showToast(Context context) {
        Intent intent = new Intent(context, SuperToastActivity.class);
        intent.putExtra(SuperToastActivity.TOAST_MESSAGE, mMessageId);
        intent.putExtra(SuperToastActivity.TOAST_DRAWABLE, mDrawableId);
        intent.putExtra(SuperToastActivity.TOAST_TITLE, mTitleId);
        intent.putExtra(SuperToastActivity.TOAST_J1_USB, mJ1UsbLogo);
        intent.putExtra(SuperToastActivity.TOAST_SHOW_TIME, mShowTime);
        intent.putExtra(SuperToastActivity.TOAST_CLOSEABLE_DISPLAY, mCloseabledDisplay);
        context.startActivity(intent);
    }

    public static final class Builder {
        // Default is -1,will not be displayed
        private int mDrawableId = SuperToastActivity.VIEW_DONE;
        private String mMessageId;
        private String mTitleId;
        private int mShowTime = SuperToastActivity.DEFAULT_SHOW_TIME;
        private int mUSBDrawableId = SuperToastActivity.VIEW_DONE;

        // Permanent display was disabled as default.
        private boolean mIsCloseableDisplay = false;


        public Builder() {
        }

        public SuperToastHelper.Builder setTitle(String title) {
            mTitleId = title;
            return this;
        }

        public SuperToastHelper.Builder setDrawable(@DrawableRes int drawableId) {
            mDrawableId = drawableId;
            return this;
        }

        public SuperToastHelper.Builder setMessage(String message) {
            mMessageId = message;
            return this;
        }

        public SuperToastHelper.Builder setShowTime(int showTime) {
            mShowTime = showTime;
            return this;
        }

        public SuperToastHelper.Builder setCloseableDisplay(boolean isCloseable) {
            mIsCloseableDisplay = isCloseable;
            return this;
        }

        // FIXME: Fix usb dialog issue for j1.
        public SuperToastHelper.Builder setToastForSingleLogo(@DrawableRes int drawableId) {
            mUSBDrawableId = drawableId;
            return this;
        }


        public SuperToastHelper build() {
            return new SuperToastHelper(mDrawableId, mMessageId, mTitleId, mShowTime, mUSBDrawableId, mIsCloseableDisplay);
        }
    }

}
