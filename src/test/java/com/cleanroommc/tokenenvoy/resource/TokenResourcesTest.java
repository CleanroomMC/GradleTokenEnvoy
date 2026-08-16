package com.cleanroommc.tokenenvoy.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenResourcesTest {

    @Test
    void treatsKnownBinariesAsBinary() {
        assertTrue(TokenResources.isBinary("assets/textures/x.png"));
        assertTrue(TokenResources.isBinary("data.bin"));
        assertTrue(TokenResources.isBinary("sound.OGG"));
    }

    @Test
    void treatsTextResourcesAsText() {
        assertFalse(TokenResources.isBinary("mcmod.info"));
        assertFalse(TokenResources.isBinary("pack.mcmeta"));
        assertFalse(TokenResources.isBinary("assets/lang/en_us.lang"));
        assertFalse(TokenResources.isBinary("mixins.mod.json"));
    }

}
