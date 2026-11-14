package dev.qilletni.toolchain;

import dev.qilletni.api.lib.qll.Version;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Backs {@code qilletni --version}. Reads this CLI's own version and the exact
 * embedded {@code qilletni-core}/{@code qilletni-api}/{@code qilletni-pkgutil}/
 * {@code qilletni-docgen} versions (plus the source commit, when available)
 * from the {@code version.properties} resource generated at build time - see
 * the {@code generateVersionInfo} task in {@code build.gradle}.
 */
public class VersionProvider implements IVersionProvider {

    private static final String RESOURCE_PATH = "/dev/qilletni/toolchain/version.properties";
    
    private final Properties versionProperties;

    public VersionProvider() throws IOException {
        this.versionProperties = loadProperties();
    }

    @Override
    public String[] getVersion() throws IOException {
        return new String[]{
                "qilletni " + versionProperties.getProperty("toolchain.version", "unknown"),
                "  qilletni-core:    " + versionProperties.getProperty("qilletni.core.version", "unknown"),
                "  qilletni-api:     " + versionProperties.getProperty("qilletni.api.version", "unknown"),
                "  qilletni-pkgutil: " + versionProperties.getProperty("qilletni.pkgutil.version", "unknown"),
                "  qilletni-docgen:  " + versionProperties.getProperty("qilletni.docgen.version", "unknown"),
                "  commit:           " + versionProperties.getProperty("commit", "unknown"),
        };
    }

    /**
     * Loads {@link #RESOURCE_PATH}, returning an empty {@link Properties} (never
     * throwing) if the generated resource is missing.
     */
    public Properties loadProperties() throws IOException {
        var properties = new Properties();

        try (InputStream inputStream = VersionProvider.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        }

        return properties;
    }

    public Version getToolchainVersion() {
        return Version.parseVersionString(versionProperties.getProperty("toolchain.version", "unknown"))
                .orElseThrow(() -> new IllegalStateException("Failed to parse toolchain.version"));
    }

    public Version getQilletniCoreVersion() {
        return Version.parseVersionString(versionProperties.getProperty("qilletni.core.version", "unknown"))
                .orElseThrow(() -> new IllegalStateException("Failed to parse qilletni.core.version"));
    }

    public Version getQilletniAPIVersion() {
        return Version.parseVersionString(versionProperties.getProperty("qilletni.api.version", "unknown"))
                .orElseThrow(() -> new IllegalStateException("Failed to parse qilletni.api.version"));
    }

    public Version getQilletniPkgUtilVersion() {
        return Version.parseVersionString(versionProperties.getProperty("qilletni.pkgutil.version", "unknown"))
                .orElseThrow(() -> new IllegalStateException("Failed to parse qilletni.pkgutil.version"));
    }

    public Version getQilletniDocgenVersion() {
        return Version.parseVersionString(versionProperties.getProperty("qilletni.docgen.version", "unknown"))
                .orElseThrow(() -> new IllegalStateException("Failed to parse qilletni.docgen.version"));
    }

    public String getCommit() {
        return versionProperties.getProperty("commit", "unknown");
    }
}
