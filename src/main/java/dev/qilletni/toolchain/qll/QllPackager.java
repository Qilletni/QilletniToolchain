package dev.qilletni.toolchain.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class QllPackager {

    private static final Logger LOGGER = LoggerFactory.getLogger(QllPackager.class);

    public void packageQll(Path qllDirectoryPath, Path qllDestination) throws IOException {
        try (
                var fos = Files.newOutputStream(qllDestination);
                var zos = new ZipOutputStream(fos);
                var walking = Files.walk(qllDirectoryPath)) {

            LOGGER.debug("Packaging QLL from {} to: {}", qllDirectoryPath.toAbsolutePath(), qllDestination.toAbsolutePath());
            
            walking.filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        var zipEntry = new ZipEntry(qllDirectoryPath.relativize(path).toString());

                        LOGGER.debug("zip entry: {}  (original path: {})", zipEntry.getName(), path);

                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }
}
