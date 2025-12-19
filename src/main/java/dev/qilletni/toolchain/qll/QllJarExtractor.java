package dev.qilletni.toolchain.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QllJarExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QllJarExtractor.class);

    private final List<URL> extractedJars = new ArrayList<>();

    /**
     * Copy the contents of a given jar into the .qll build path, under `/native/`
     *
     * @param jarPath      The path to the .jar produced by Gradle
     * @param qllBuildPath The path of the directory to be packaged into a .qll
     * @throws IOException
     */
    public static void copyExtractedJar(Path jarPath, Path qllBuildPath) throws IOException {
        Path nativeDir = qllBuildPath.resolve("native");
        Files.createDirectories(nativeDir);

        try (var jarFs = java.nio.file.FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path root = jarFs.getPath("/");

            try (var walk = Files.walk(root)) {
                walk.forEach(source -> {
                    try {
                        Path destination = nativeDir.resolve(root.relativize(source).toString());

                        if (Files.isDirectory(source)) {
                            if (Files.notExists(destination)) {
                                Files.createDirectories(destination);
                            }
                        } else {
                            Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to extract " + source, e);
                    }
                });
            }
        }
    }

    public void registerInnerJar(Path qllPath) {
        try {
            var qllUrl = qllPath.toUri().toURL().toString();
            extractedJars.add(new URL("jar:" + qllUrl + "!/native/"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create URL for inner jar", e);
        }
    }

    /**
     * Copies a local library jar to a given path.
     *
     * @param localJarPath    The local path of the jar. This is determined by Gradle
     * @throws IOException
     */
    public void addLocalLibraryJar(Path localJarPath) throws IOException {
        extractedJars.add(localJarPath.toUri().toURL());
    }

    public URLClassLoader createClassLoader() {
        return new URLClassLoader(extractedJars.toArray(URL[]::new));
    }

}
