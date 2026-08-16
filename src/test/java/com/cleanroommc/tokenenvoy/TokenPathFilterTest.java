package com.cleanroommc.tokenenvoy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenPathFilterTest {

    @Test
    void emptyIncludesAcceptEverything() {
        TokenPathFilter filter = resources(List.of(), List.of());
        assertTrue(filter.accepts("resources.json"));
        assertTrue(filter.accepts("assets/lang/en_us.lang"));
    }

    @Test
    void emptyPathIsRejected() {
        assertFalse(resources(List.of(), List.of()).accepts(""));
        assertFalse(resources(List.of(), List.of()).accepts((String) null));
        assertFalse(resources(List.of(), List.of()).accepts("   "));
    }

    @Test
    void doesNotApplyGradleDefaultExcludes() {
        TokenPathFilter filter = resources(List.of(), List.of());
        assertTrue(filter.accepts("notes~"));
        assertTrue(filter.accepts(".gitkeep"));
    }

    @Test
    void plainFileNameMatchesOnlyRoot() {
        TokenPathFilter filter = resources(List.of("resources.json"), List.of());
        assertTrue(filter.accepts("resources.json"));
        assertFalse(filter.accepts("nested/resources.json"));
        assertFalse(filter.accepts("mcmod.json"));
    }

    @Test
    void doubleStarMatchesFileNameInAnyDirectory() {
        TokenPathFilter filter = resources(List.of("**/resources.json"), List.of());
        assertTrue(filter.accepts("resources.json"));
        assertTrue(filter.accepts("nested/resources.json"));
        assertFalse(filter.accepts("resources.txt"));
    }

    @Test
    void plainClassFileNameMatchesOnlyRoot() {
        TokenPathFilter filter = classes(List.of("Tokens.class"), List.of());
        assertTrue(filter.accepts("Tokens.class"));
        assertFalse(filter.accepts("com/example/Tokens.class"));
        assertFalse(filter.accepts("com/example/Tokens$Inner.class"));
    }

    @Test
    void doubleStarClassFileNameMatchesNestedOutput() {
        TokenPathFilter filter = classes(List.of("**/Tokens.class"), List.of());
        assertTrue(filter.accepts("Tokens.class"));
        assertTrue(filter.accepts("com/example/Tokens.class"));
        assertFalse(filter.accepts("com/example/Tokens$Inner.class"));
        assertFalse(filter.accepts("com/example/Other.class"));
    }

    @Test
    void exactRelativePathMatchesOnlyThatPath() {
        TokenPathFilter filter = classes(List.of("com/example/Tokens.class"), List.of());
        assertTrue(filter.accepts("com/example/Tokens.class"));
        assertFalse(filter.accepts("Tokens.class"));
        assertFalse(filter.accepts("other/example/Tokens.class"));
    }

    @Test
    void fqcnIsCanonicalizedForClasses() {
        TokenPathFilter dotted = classes(List.of("com.example.Tokens"), List.of());
        assertTrue(dotted.accepts("com/example/Tokens.class"));
        assertFalse(dotted.accepts("com/example/Other.class"));
        assertFalse(dotted.accepts("Tokens.class"));

        TokenPathFilter withSuffix = classes(List.of("com.example.Tokens.class"), List.of());
        assertTrue(withSuffix.accepts("com/example/Tokens.class"));

        TokenPathFilter simpleName = classes(List.of("Tokens"), List.of());
        assertTrue(simpleName.accepts("Tokens"));
        assertFalse(simpleName.accepts("Tokens.class"));
        assertFalse(simpleName.accepts("com/example/Tokens.class"));
    }

    @Test
    void resourceFqcnIsNotRewritten() {
        TokenPathFilter filter = resources(List.of("com.example.Tokens"), List.of());
        assertFalse(filter.accepts("com/example/Tokens.class"));
        assertTrue(filter.accepts("com.example.Tokens"));
    }

    @Test
    void starGlobStaysInOneSegment() {
        TokenPathFilter filter = resources(List.of("*.json"), List.of());
        assertTrue(filter.accepts("resources.json"));
        assertFalse(filter.accepts("assets/resources.json"));
    }

    @Test
    void doubleStarGlobCrossesDirectories() {
        TokenPathFilter filter = resources(List.of("**/*.json"), List.of());
        assertTrue(filter.accepts("resources.json"));
        assertTrue(filter.accepts("assets/resources.json"));
        assertFalse(filter.accepts("resources.txt"));
    }

    @Test
    void doubleStarWithoutSlashIsOneSegmentSuffix() {
        TokenPathFilter filter = resources(List.of("**.lang"), List.of());
        assertTrue(filter.accepts("en_us.lang"));
        assertFalse(filter.accepts("assets/lang/en_us.lang"));
    }

    @Test
    void questionMarkMatchesOneCharacter() {
        TokenPathFilter filter = resources(List.of("?.txt"), List.of());
        assertTrue(filter.accepts("a.txt"));
        assertFalse(filter.accepts("ab.txt"));
        assertFalse(filter.accepts("a/b.txt"));
    }

    @Test
    void nestedDirectoryGlob() {
        TokenPathFilter filter = resources(List.of("assets/**/*.lang"), List.of());
        assertTrue(filter.accepts("assets/lang/en_us.lang"));
        assertTrue(filter.accepts("assets/en_us.lang"));
        assertFalse(filter.accepts("data/en_us.lang"));
        assertFalse(filter.accepts("assets"));
    }

    @Test
    void excludesWinOverIncludes() {
        TokenPathFilter filter = resources(List.of("**/*.json", "resources.json"), List.of("skip.json", "secret/**"));
        assertTrue(filter.accepts("resources.json"));
        assertTrue(filter.accepts("data.json"));
        assertTrue(filter.accepts("nested/data.json"));
        assertFalse(filter.accepts("skip.json"));
        assertTrue(filter.accepts("nested/skip.json"));
        assertFalse(filter.accepts("secret/keep.json"));
    }

    @Test
    void doubleStarExcludeMatchesAnyDirectory() {
        TokenPathFilter filter = resources(List.of("**/*.json"), List.of("**/skip.json"));
        assertTrue(filter.accepts("resources.json"));
        assertFalse(filter.accepts("skip.json"));
        assertFalse(filter.accepts("nested/skip.json"));
    }

    @Test
    void excludeWithoutIncludeSkipsOnlyMatches() {
        TokenPathFilter filter = classes(List.of(), List.of("**/internal/**"));
        assertTrue(filter.accepts("com/example/Tokens.class"));
        assertFalse(filter.accepts("com/example/internal/Hidden.class"));
    }

    @Test
    void normalizesBackslashesAndDotSlash() {
        TokenPathFilter filter = resources(List.of("assets/**/*.json"), List.of());
        assertTrue(filter.accepts("assets\\resources.json"));
        assertTrue(filter.accepts("./assets/resources.json"));
        assertTrue(filter.accepts("/assets/resources.json"));
    }

    @Test
    void doubleStarAtEndMatchesDescendants() {
        TokenPathFilter filter = resources(List.of("assets/**"), List.of());
        assertTrue(filter.accepts("assets"));
        assertTrue(filter.accepts("assets/x"));
        assertTrue(filter.accepts("assets/lang/en_us.lang"));
        assertFalse(filter.accepts("data/x"));
    }

    private static TokenPathFilter classes(List<String> includes, List<String> excludes) {
        return TokenPathFilter.of(includes, excludes, TokenPathFilter.Kind.CLASSES);
    }

    private static TokenPathFilter resources(List<String> includes, List<String> excludes) {
        return TokenPathFilter.of(includes, excludes, TokenPathFilter.Kind.RESOURCES);
    }

}
