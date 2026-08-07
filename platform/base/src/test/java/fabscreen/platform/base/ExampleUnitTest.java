package fabscreen.platform.base;

import static org.junit.Assert.assertEquals;

import com.orhanobut.logger.Logger;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import okio.ByteString;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void hexStringEqual() {
        Logger.d("version: %s", ByteString.decodeHex("76312e31322e3400000000000000000000000000000000000000000000000000"));
        ByteString byteString = ByteString.decodeHex("76312e31322e3400000000000000000000000000000000000000000000000000");
        String version = new String(byteString.toByteArray(), StandardCharsets.UTF_8);
        assertEquals("v1.12.4", version.trim());
    }
}
