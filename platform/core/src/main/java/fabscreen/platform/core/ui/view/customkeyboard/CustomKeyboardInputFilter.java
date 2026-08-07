package fabscreen.platform.core.ui.view.customkeyboard;

import android.text.InputFilter;
import android.text.Spanned;

public interface CustomKeyboardInputFilter extends InputFilter {

    // reference: InputFilter.java > LengthFilter class
    public static class LengthRangeFilter implements CustomKeyboardInputFilter {
        private final int mMin;
        private final int mMax;

        public LengthRangeFilter(int min, int max) {
            mMin = min;
            mMax = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            int maxLimit = mMax - (dest.length() - (dend - dstart));
            int minLimit = (dest.length() - (dend - dstart)) - mMin;
            if (maxLimit < 0 || minLimit < 0) {
                return "";
            } else if (maxLimit >= end - start) {
                return null; // keep original
            } else {
                maxLimit += start;
                if (Character.isHighSurrogate(source.charAt(maxLimit - 1))) {
                    --maxLimit;
                    if (maxLimit == start) {
                        return "";
                    }
                }
                return source.subSequence(start, maxLimit);
            }
        }

        public int getMax() {
            return mMax;
        }

        public int getMin() {
            return mMin;
        }
    }

    public static class NumberRangeFilter implements CustomKeyboardInputFilter {
        private final int mMin;
        private final int mMax;

        public NumberRangeFilter(int min, int max) {
            mMin = min;
            mMax = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            return null;
        }
    }
}
