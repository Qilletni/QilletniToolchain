package dev.qilletni.toolchain;

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

    @Override
    public String[] getVersion() throws IOException {
        var properties = loadProperties();

        return new String[]{
                "qilletni " + properties.getProperty("toolchain.version", "unknown"),
                "  qilletni-core:    " + properties.getProperty("qilletni.core.version", "unknown"),
                "  qilletni-api:     " + properties.getProperty("qilletni.api.version", "unknown"),
                "  qilletni-pkgutil: " + properties.getProperty("qilletni.pkgutil.version", "unknown"),
                "  qilletni-docgen:  " + properties.getProperty("qilletni.docgen.version", "unknown"),
                "  commit:           " + properties.getProperty("commit", "unknown"),
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
}
