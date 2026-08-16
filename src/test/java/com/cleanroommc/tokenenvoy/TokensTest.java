package com.cleanroommc.tokenenvoy;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokensTest {

    @Test
    void test() {
        assertEquals("@{VERSION}", Tokens.placeholder("VERSION"));
        assertEquals("1.2.3", Tokens.replace("@{VERSION}", Map.of("VERSION", "1.2.3")));
        assertEquals("mod=example", Tokens.replace("mod=@{MOD_ID}", Map.of("MOD_ID", "example")));
    }

    @Test
    void doesNotReplaceTokenlessNames() {
        assertEquals("VERSION", Tokens.replace("VERSION", Map.of("VERSION", "1.2.3")));
    }

    @Test
    void skipsEmptyKeysAndNullValues() {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("", "nope");
        tokens.put("MISSING", null);
        tokens.put("OK", "yes");
        assertEquals("yes", Tokens.replace("@{OK}", tokens));
        assertEquals("@{MISSING}", Tokens.replace("@{MISSING}", tokens));
    }

    @Test
    void leavesNullInputAlone() {
        assertNull(Tokens.replace(null, Map.of("A", "b")));
    }

}
