package rc3.januscompiler.utils;

import org.junit.jupiter.api.Test;
import rc3.lib.utils.StringUtils;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void testCorrectEscapes() {
        assertEquals("\\b", StringUtils.escape("\b"));
        assertEquals("\\t", StringUtils.escape("\t"));
        assertEquals("\\n", StringUtils.escape("\n"));
        assertEquals("\\f", StringUtils.escape("\f"));
        assertEquals("\\r", StringUtils.escape("\r"));
        assertEquals("\\\"", StringUtils.escape("\""));
        assertEquals("\\'", StringUtils.escape("'"));
        assertEquals("\\\\", StringUtils.escape("\\"));
    }

    @Test
    void testUnescapedIsUnchanged() {
        String input = "Hello World!";

        assertEquals(input, StringUtils.escape(input));
    }
}