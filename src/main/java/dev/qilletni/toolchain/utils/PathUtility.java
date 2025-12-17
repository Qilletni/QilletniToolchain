package dev.qilletni.toolchain.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtility {

    public static Path getDependencyPath() throws IOException {
        var userHome = System.getProperty("user.home");

        var qilletniDir = Paths.get(userHome, ".qilletni", "packages");

        Files.createDirectories(qilletniDir);

        return qilletniDir;
    }

    public static  Path getCachePath() throws IOException {
        var userHome = System.getProperty("user.home");

        var qilletniDir = Paths.get(userHome, ".qilletni", "doc-cache");

        Files.createDirectories(qilletniDir);

        return qilletniDir;
    }
    
}
