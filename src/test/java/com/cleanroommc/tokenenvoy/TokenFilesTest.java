package com.cleanroommc.tokenenvoy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenFilesTest {

    @Test
    void readsPropertiesAndInterpolatesValues() {
        Map<String, String> tokens = TokenFiles.read(
                """
                VERSION=${mod_version}
                MOD_ID=plain
                """,
                Map.of("mod_version", "4.0.0")
        );
        assertEquals("4.0.0", tokens.get("VERSION"));
        assertEquals("plain", tokens.get("MOD_ID"));
    }

    @Test
    void leavesUnknownPlaceholdersInPlace() {
        assertEquals("${missing}", TokenFiles.interpolate("${missing}", Map.of("other", "x")));
    }

    @Test
    void interpolatesMultiplePlaceholders() {
        assertEquals("a-b", TokenFiles.interpolate("${one}-${two}", Map.of("one", "a", "two", "b")));
    }

}
