package fabscreen.platform.base.helper;

import androidx.annotation.MainThread;

/**
 * 处理多次点击
 */
public class ClickHelper {
    private static long lastClickTime = 0;
    private static int lastButtonId = 0;

    /**
     * 
     * @param viewId 点击事件的控件ID
     * @return
     */
    @MainThread
    public static boolean isFastDoubleClick(int viewId) {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (lastButtonId == viewId && 0 < timeD && timeD < 300) {
            return true;
        }
        lastClickTime = time;
        lastButtonId = viewId;
        return false;
    }

}
