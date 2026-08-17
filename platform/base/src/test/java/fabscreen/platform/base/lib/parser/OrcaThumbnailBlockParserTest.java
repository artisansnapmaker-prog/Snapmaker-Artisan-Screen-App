package fabscreen.platform.base.lib.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

public class OrcaThumbnailBlockParserTest {
    @Test
    public void parsesSplitOrcaThumbnail() {
        OrcaThumbnailBlockParser parser = new OrcaThumbnailBlockParser();

        assertFalse(parser.consumeLine("; THUMBNAIL_BLOCK_START"));
        assertTrue(parser.consumeLine("; thumbnail begin 600x600 12"));
        assertTrue(parser.consumeLine("; QUJDREVG"));
        assertTrue(parser.consumeLine("; R0hJ"));
        assertTrue(parser.consumeLine("; thumbnail end"));

        OrcaThumbnailBlockParser.Result result = parser.takeCompleted();
        assertEquals(600, result.getWidth());
        assertEquals(600, result.getHeight());
        assertEquals("QUJDREVGR0hJ", result.getEncodedData());
        assertNull(parser.takeCompleted());
    }

    @Test
    public void supportsNamedFormatAndCaseInsensitiveMarkers() {
        OrcaThumbnailBlockParser parser = new OrcaThumbnailBlockParser();

        assertTrue(parser.consumeLine(" ; THUMBNAIL_PNG BEGIN 10x20 4"));
        assertTrue(parser.consumeLine("; YWJj"));
        assertTrue(parser.consumeLine("; thumbnail_png END"));

        OrcaThumbnailBlockParser.Result result = parser.takeCompleted();
        assertEquals(10, result.getWidth());
        assertEquals(20, result.getHeight());
        assertEquals("YWJj", result.getEncodedData());
    }

    @Test
    public void rejectsLengthMismatchAndRecoversForNextBlock() {
        OrcaThumbnailBlockParser parser = new OrcaThumbnailBlockParser();

        parser.consumeLine("; thumbnail begin 20x20 8");
        parser.consumeLine("; YWJj");
        parser.consumeLine("; thumbnail end");
        assertNull(parser.takeCompleted());

        parser.consumeLine("; thumbnail begin 20x20 4");
        parser.consumeLine("; YWJj");
        parser.consumeLine("; thumbnail end");
        assertEquals("YWJj", parser.takeCompleted().getEncodedData());
    }

    @Test
    public void rejectsUnsafeDimensionsAndInvalidPayload() {
        OrcaThumbnailBlockParser parser = new OrcaThumbnailBlockParser();

        parser.consumeLine("; thumbnail begin 4096x4096 4");
        parser.consumeLine("; YWJj");
        parser.consumeLine("; thumbnail end");
        assertNull(parser.takeCompleted());

        parser.consumeLine("; thumbnail begin 10x10 4");
        parser.consumeLine("; !!@@");
        parser.consumeLine("; thumbnail end");
        assertNull(parser.takeCompleted());
    }

    @Test
    public void parsesRealOrcaFixtureWhenProvided() throws Exception {
        String fixturePath = System.getenv("ORCA_GCODE_FIXTURE");
        Assume.assumeTrue(fixturePath != null && Files.isRegularFile(Paths.get(fixturePath)));

        OrcaThumbnailBlockParser parser = new OrcaThumbnailBlockParser();
        OrcaThumbnailBlockParser.Result result = null;
        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(fixturePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (parser.consumeLine(line)) {
                    OrcaThumbnailBlockParser.Result completed = parser.takeCompleted();
                    if (completed != null) {
                        result = completed;
                    }
                }
            }
        }

        assertEquals(600, result.getWidth());
        assertEquals(600, result.getHeight());
        assertEquals(47_784, result.getEncodedData().length());
        byte[] png = Base64.getDecoder().decode(result.getEncodedData());
        assertEquals(35_837, png.length);
        assertEquals("452b577b0762c7bcf94be7a5e8e5f098cbb463f6310e13ed08cf705401a9ac72",
                toHex(MessageDigest.getInstance("SHA-256").digest(png)));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }
}
