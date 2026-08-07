package fabscreen.platform.base.helper;

public class StringToValueUtils {
    public static int parseInt(String str) {
        if (str == null || str.isEmpty()) return Integer.MIN_VALUE;
        int value = 0;
        for (int i = 0; i < str.length(); i++) {
            int digit = Character.getNumericValue(str.charAt(i));
            if (value <= Integer.MAX_VALUE / 10)
                value = value * 10 + digit;
            else {
                value = Integer.MAX_VALUE;
                break;
            }
        }
        return value;
    }
}
