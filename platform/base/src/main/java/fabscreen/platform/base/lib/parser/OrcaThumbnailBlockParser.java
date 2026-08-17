package fabscreen.platform.base.lib.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects Prusa/Orca-style multi-line thumbnail blocks without depending on Android APIs.
 *
 * <p>The encoded length in the begin marker is the number of Base64 characters, not the
 * decoded image size.</p>
 */
public final class OrcaThumbnailBlockParser {
    public static final int MAX_IMAGE_DIMENSION = 2048;
    public static final long MAX_IMAGE_PIXELS = 4_000_000L;
    public static final int MAX_ENCODED_CHARACTERS = 8 * 1024 * 1024;

    private static final Pattern BEGIN_PATTERN = Pattern.compile(
            "^\\s*;\\s*thumbnail(?:_(?:png|jpe?g))?\\s+begin\\s+(\\d+)x(\\d+)\\s+(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern END_PATTERN = Pattern.compile(
            "^\\s*;\\s*thumbnail(?:_(?:png|jpe?g))?\\s+end\\s*$",
            Pattern.CASE_INSENSITIVE);

    private boolean collecting;
    private boolean invalid;
    private int width;
    private int height;
    private int declaredEncodedLength;
    private StringBuilder encodedData;
    private Result completed;

    /**
     * Consumes a line when it starts or belongs to a thumbnail block.
     *
     * @return true when the line was part of thumbnail syntax; false for normal G-code/header
     *         processing.
     */
    public boolean consumeLine(String line) {
        if (line == null) {
            resetCollection();
            return false;
        }

        Matcher beginMatcher = BEGIN_PATTERN.matcher(line);
        if (beginMatcher.matches()) {
            startBlock(beginMatcher);
            return true;
        }

        if (!collecting) {
            return false;
        }

        if (END_PATTERN.matcher(line).matches()) {
            if (!invalid
                    && encodedData != null
                    && encodedData.length() == declaredEncodedLength) {
                completed = new Result(width, height, encodedData.toString());
            }
            resetCollection();
            return true;
        }

        String trimmed = line.trim();
        if (!trimmed.startsWith(";")) {
            resetCollection();
            return false;
        }

        if (invalid) {
            return true;
        }

        String payload = trimmed.substring(1).trim();
        if (payload.isEmpty()) {
            return true;
        }
        if (!isBase64(payload)
                || encodedData.length() + payload.length() > declaredEncodedLength
                || encodedData.length() + payload.length() > MAX_ENCODED_CHARACTERS) {
            invalid = true;
            return true;
        }

        encodedData.append(payload);
        return true;
    }

    /** Returns the most recently completed valid block once. */
    public Result takeCompleted() {
        Result result = completed;
        completed = null;
        return result;
    }

    public void reset() {
        completed = null;
        resetCollection();
    }

    private void startBlock(Matcher matcher) {
        resetCollection();
        collecting = true;

        try {
            long parsedWidth = Long.parseLong(matcher.group(1));
            long parsedHeight = Long.parseLong(matcher.group(2));
            long parsedLength = Long.parseLong(matcher.group(3));
            long pixels = parsedWidth * parsedHeight;

            invalid = parsedWidth <= 0
                    || parsedHeight <= 0
                    || parsedWidth > MAX_IMAGE_DIMENSION
                    || parsedHeight > MAX_IMAGE_DIMENSION
                    || pixels <= 0
                    || pixels > MAX_IMAGE_PIXELS
                    || parsedLength <= 0
                    || parsedLength > MAX_ENCODED_CHARACTERS;
            if (!invalid) {
                width = (int) parsedWidth;
                height = (int) parsedHeight;
                declaredEncodedLength = (int) parsedLength;
                encodedData = new StringBuilder(Math.min(declaredEncodedLength, 64 * 1024));
            }
        } catch (NumberFormatException e) {
            invalid = true;
        }
    }

    private void resetCollection() {
        collecting = false;
        invalid = false;
        width = 0;
        height = 0;
        declaredEncodedLength = 0;
        encodedData = null;
    }

    private static boolean isBase64(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean valid = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '+'
                    || c == '/'
                    || c == '=';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    public static final class Result {
        private final int width;
        private final int height;
        private final String encodedData;

        private Result(int width, int height, String encodedData) {
            this.width = width;
            this.height = height;
            this.encodedData = encodedData;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getEncodedData() {
            return encodedData;
        }
    }
}
