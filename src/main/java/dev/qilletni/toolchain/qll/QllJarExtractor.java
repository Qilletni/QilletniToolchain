package dev.qilletni.toolchain.qll;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.pkgutil.manifest.models.ResolvedPackage;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QllJarExtractor {
    
    private final List<URL> extractedJars = new ArrayList<>();

    /**
     * Takes a .qll and extracts the jar, preparing it for class loading. The jar file will be named the .qll name,\
     * with the .qll extension replaced with .jar
     *
     * @param resolvedPackage The package the jar belongs to
     * @param qllPath         The path of the .qll library
     * @param destinationPath The directory to place the .jar file in
     */
    public void extractJarTo(ResolvedPackage resolvedPackage, Path qllPath, Path destinationPath) {
        var destinationFile = destinationPath.resolve("%s-%s.jar".formatted(resolvedPackage.name().replace('/', '_'), resolvedPackage.version()));
        
        try (var fileSystem = FileSystems.newFileSystem(qllPath)) {
            var path = fileSystem.getPath("native.jar");
            
            // Not all libraries have native methods
            if (Files.notExists(path)) {
                return;
            }
            
            Files.copy(path, destinationFile);

            extractedJars.add(destinationFile.toUri().toURL());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies a local library jar to a given path.
     *
     * @param localJarPath The local path of the jar. This is determined by Gradle
     * @param destinationPath The directory to place the .jar file in
     * @param localQllInfo The {@link QllInfo} of the local library
     * @throws IOException
     */
    public void copyLocalNativeJar(Path localJarPath, Path destinationPath, QllInfo localQllInfo) throws IOException {
        var destinationJar = destinationPath.resolve("%s-%s.jar".formatted(localQllInfo.name(), localQllInfo.version().getVersionString()));

        Files.copy(localJarPath, destinationJar);

        extractedJars.add(localJarPath.toUri().toURL());
    }
    
    public URLClassLoader createClassLoader() {
        return new URLClassLoader(extractedJars.toArray(URL[]::new));
    }
    
    private String createJarName(String qllName) {
        if (!qllName.endsWith(".qll")) {
            return qllName;
        }
        
        return qllName.substring(0, qllName.length() - 4) + ".jar";
    }
    
}
