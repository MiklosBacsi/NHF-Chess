package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests GameSaver's parsing methods (extracting values from json strings).
 * @see GameSaver
 * @author Miklós Bácsi
 */
class GameSaverTest {

    // Parsing a standard positive integer
    @Test
    void testExtractInt_Positive() {
        String json = "\"sr\": 6, \"sc\": 4";
        int result = GameSaver.extractInt(json, "sr");
        assertEquals(6, result);
    }

    // Parsing a negative integer (Crucial for Drop moves)
    @Test
    void testExtractInt_Negative() {
        String json = "\"sr\": -1, \"sc\": -1";
        int result = GameSaver.extractInt(json, "sr");
        assertEquals(-1, result);
    }

    // Parsing a string value with whitespace handling
    @Test
    void testExtractValue() {
        String json = "\"t\": \"DROP\", \"d\":\"ROOK\"";
        String result = GameSaver.extractValue(json, "t");
        assertEquals("DROP", result);
    }
}