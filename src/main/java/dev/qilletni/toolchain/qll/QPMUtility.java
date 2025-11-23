package dev.qilletni.toolchain.qll;

import dev.qilletni.toolchain.PathUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class QPMUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(QPMUtility.class);

    public static boolean isQPMInstalled() {
        return Files.exists(getQPMBinary());
    }

    private static Path getQPMBinary() {
        var qpmPath = Objects.requireNonNullElse(System.getenv("QPM_PATH"), Paths.get(System.getProperty("user.home"), ".qilletni").toAbsolutePath().toString());
        var qilletniDir = Paths.get(qpmPath);

        // Determine whether to use gradlew or gradlew.bat based on OS
        return System.getProperty("os.name").toLowerCase().contains("win")
                ? qilletniDir.resolve("qpm.bat")
                : qilletniDir.resolve("qpm");
    }

    /**
     * Runs the `qpm install` command in a given directory
     *
     * @return The process result containing exit code and output
     */
    public static GradleProjectHelper.ProcessResult runQPMInstall(boolean verboseOutput, Path workingDir) {
        try {
            var qpmPath = getQPMBinary();

            // Build the command
            var command = new ArrayList<String>();
            command.add(qpmPath.toAbsolutePath().toString());
            command.add("install");

            var processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDir.toFile());

            var process = processBuilder.start();

            var stdOut = new StringBuilder();
            var stdErr = new StringBuilder();

            try (var reader = process.inputReader()) {
                reader.lines().forEach(str -> {
                    if (verboseOutput) {
                        System.out.println(str);
                    }

                    stdOut.append(str);
                });
            }

            try (var reader = process.errorReader()) {
                reader.lines().forEach(str -> {
                    if (verboseOutput) {
                        System.err.println(str);
                    }

                    stdErr.append(str);
                });
            }

            boolean completed = process.waitFor(1, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                return new GradleProjectHelper.ProcessResult(-1, "Process timed out after 1 minute", stdErr.toString());
            }

            return new GradleProjectHelper.ProcessResult(process.exitValue(), stdOut.toString(), stdErr.toString());

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while running `qpm install`", e);
            return new GradleProjectHelper.ProcessResult(-1, "", e.getMessage());
        }
    }

}
