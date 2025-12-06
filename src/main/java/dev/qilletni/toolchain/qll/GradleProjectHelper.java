package dev.qilletni.toolchain.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class GradleProjectHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradleProjectHelper.class);

    private final ProjectBuildSettings buildSettings;

    private GradleProjectHelper(ProjectBuildSettings buildSettings) {
        this.buildSettings = buildSettings;
    }

    public static Optional<GradleProjectHelper> createProjectHelper(Path projectRoot) {
        var propertiesFile = projectRoot.resolve(".qilletni_build.properties");

        if (Files.notExists(propertiesFile)) {
            LOGGER.debug("No .qilletni_build.properties file found in project root: {}", projectRoot.toAbsolutePath());

            return Optional.of(new GradleProjectHelper(new ProjectBuildSettings("", projectRoot.toAbsolutePath())));
        }

        try {
            var properties = new Properties();
            properties.load(Files.newInputStream(propertiesFile));

            var moduleName = Optional.ofNullable(properties.getProperty("gradle.module.name"))
                    .filter(s -> !s.isBlank())
                    .map(":%s"::formatted)
                    .orElse("");

            var rootDir = Optional.ofNullable(properties.getProperty("gradle.root.dir"))
                    .filter(s -> !s.isBlank())
                    .map(projectRoot::resolve)
                    .map(Path::toAbsolutePath)
                    .orElseGet(projectRoot::toAbsolutePath);

            LOGGER.debug("Creating GradleProjectHelper with moduleName='{}', rootDir='{}' from properties file: {}",
                    moduleName, rootDir, propertiesFile.toAbsolutePath());

            return Optional.of(new GradleProjectHelper(new ProjectBuildSettings(moduleName, rootDir)));
        } catch (IOException e) {
            LOGGER.error("Error reading .qilletni_build.properties file in project root: {}", projectRoot.toAbsolutePath(), e);
            return Optional.empty();
        }
    }


    /**
     * Runs a Gradle task with the specified arguments.
     *
     * @param task              The Gradle task to run
     * @param args              Additional arguments to pass to Gradle
     * @return The process result containing exit code and output
     */
    public ProcessResult runGradleTask(boolean verboseGradleOutput, String task, String... args) {
        try {
            // Determine whether to use gradlew or gradlew.bat based on OS
            var gradleWrapper = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "gradlew.bat"
                    : "./gradlew";

            // Build the command
            var command = new ArrayList<String>();
            command.add(gradleWrapper);
            command.add(task);

            Collections.addAll(command, args);

            var processBuilder = new ProcessBuilder(command);
            processBuilder.directory(buildSettings.rootDir.toFile());

            LOGGER.info("Running Gradle task: '{}' in directory: {}", String.join(" ", command), buildSettings.rootDir.toAbsolutePath());

            var process = processBuilder.start();

            var stdOut = new StringBuilder();
            var stdErr = new StringBuilder();

            try (var reader = process.inputReader()) {
                reader.lines().forEach(str -> {
                    if (verboseGradleOutput) {
                        System.out.println(str);
                    }
                    
                    stdOut.append(str).append('\n');
                });
            }

            try (var reader = process.errorReader()) {
                reader.lines().forEach(str -> {
                    if (verboseGradleOutput) {
                        System.err.println(str);
                    }

                    stdErr.append(str).append('\n');
                });
            }

            boolean completed = process.waitFor(1, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                return new ProcessResult(-1, "Process timed out after 1 minute", stdErr.toString().trim());
            }

            return new ProcessResult(process.exitValue(), stdOut.toString().trim(), stdErr.toString().trim());

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while running Gradle task: {}", task, e);
            return new ProcessResult(-1, "", e.getMessage());
        }
    }

    /**
     * Runs the shadowJar task.
     *
     * @param verboseGradleOutput Whether to print verbose output from Gradle
     * @return The process result containing exit code and output
     */
    public ProcessResult runShadowJarTask(boolean verboseGradleOutput) {
        return runGradleTask(verboseGradleOutput, "%s:shadowJar".formatted(buildSettings.moduleName()));
    }

    /**
     * Finds the jar file that will be created (or has been created) in the project in the given path.
     *
     * @param verboseGradleOutput Whether to print verbose output from Gradle
     * @return The path of the jar file
     */
    public Optional<Path> findProjectJar(boolean verboseGradleOutput) {
        Path jarFindScript = null;

        try {
            jarFindScript = Files.createTempFile("qilletni-gradle-jar-find", ".groovy");
            Files.writeString(jarFindScript, "gradle.taskGraph.afterTask { t, _ -> if (t.name == 'shadowJar') println t.archiveFile.get() }");

            // Run the shadowJar task with the script to find the jar file
            ProcessResult result = runGradleTask(
                    verboseGradleOutput,
                    "%s:shadowJar".formatted(buildSettings.moduleName()),
                    "--console=plain",
                    "--quiet",
                    "-I",
                    jarFindScript.toString()
            );

            if (result.exitCode != 0 || result.stdOut.isEmpty()) {
                if (!result.stdErr.isEmpty()) {
                    LOGGER.error("Gradle error output: {}", result.stdErr);
                }
                return Optional.empty();
            }

            return Optional.of(Path.of(result.stdOut));

        } catch (IOException e) {
            LOGGER.error("Error while finding the jar file", e);
            return Optional.empty();
        } finally {
            if (jarFindScript != null) {
                try {
                    Files.deleteIfExists(jarFindScript);
                } catch (IOException ignored) {}
            }
        }
    }

    public static boolean isGradleProject(Path pathToProjectRoot) {
        return Files.exists(pathToProjectRoot.resolve(".qilletni_build.properties")) ||
                Files.exists(pathToProjectRoot.resolve("build.gradle"));
    }

    /**
     * Represents the result of a process execution.
     */
    public record ProcessResult(int exitCode, String stdOut, String stdErr) {
        public boolean isSuccessful() {
            return exitCode == 0;
        }
    }

    /**
     * Holds settings for building a project, from the `.qilletni_build.properties` file.
     *
     * @param moduleName The name of the module, such as `qilletni-spotify`. If empty, the root project is used. This value will have a `:` prepended to it, if present.
     * @param rootDir The relative root directory of the project where the `gradle`/`gradle.bat` files are, such as `../`. If empty, the current directory is used.
     */
    private record ProjectBuildSettings(String moduleName, Path rootDir) {}
}
