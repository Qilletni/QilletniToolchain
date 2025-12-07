package dev.qilletni.toolchain.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class QllExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QllExtractor.class);

    public static Optional<Path> extractToTmp(Path qllPath) throws IOException {
        var tempDirectory = Files.createTempDirectory("ql-doc").resolve(qllPath.getFileName());
        Files.createDirectories(tempDirectory);

        try (var fileSystem = FileSystems.newFileSystem(qllPath)) {
            var qllInfo = fileSystem.getPath("qll.info");
            var qilletniSrc = fileSystem.getPath("qilletni-src");

            if (Files.notExists(qllInfo) || Files.notExists(qilletniSrc)) {
                return Optional.empty();
            }

            Files.copy(qllInfo, tempDirectory.resolve("qll.info"));

            var targetDir = tempDirectory.resolve("qilletni-src");

            try (var stream = Files.walk(qilletniSrc)) {
                stream.forEach(source -> {
                    var dest = targetDir.resolve(qilletniSrc.relativize(source).toString());
                    try {
                        Files.copy(source, dest);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy " + source + " to " + dest, e);
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(tempDirectory);
    }

}
