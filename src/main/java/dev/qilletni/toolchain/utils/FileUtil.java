package dev.qilletni.toolchain.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileUtil {

    public static void deleteDirectory(Path path) {
        try {
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void clearAndCreateDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                deleteDirectory(directory);
            }
            
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
