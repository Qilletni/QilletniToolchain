package dev.qilletni.toolchain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionProviderTest {

    @Test
    void versionLinesIncludeToolchainAndEmbeddedComponentVersions() throws Exception {
        var lines = new VersionProvider().getVersion();

        assertEquals(6, lines.length);
        assertTrue(lines[0].startsWith("qilletni "), "First line should start with 'qilletni '");

        var joined = String.join("\n", lines);
        assertTrue(joined.contains("qilletni-core:"), "Should report the embedded qilletni-core version");
        assertTrue(joined.contains("qilletni-api:"), "Should report the embedded qilletni-api version");
        assertTrue(joined.contains("qilletni-pkgutil:"), "Should report the embedded qilletni-pkgutil version");
        assertTrue(joined.contains("qilletni-docgen:"), "Should report the embedded qilletni-docgen version");
        assertTrue(joined.contains("commit:"), "Should report the source commit");

        assertTrue(!lines[0].endsWith(" unknown"), "generateVersionInfo should have produced a real toolchain version");
    }

    @Test
    void loadPropertiesNeverThrowsWhenResourceMissing() {
        assertDoesNotThrow(() -> new VersionProvider().loadProperties());
    }
}
