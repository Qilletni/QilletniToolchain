package dev.qilletni.toolchain.command;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.toolchain.qll.*;
import dev.qilletni.toolchain.utils.FileUtil;
import dev.qilletni.toolchain.LogSetup;
import dev.qilletni.toolchain.config.QilletniInfoParser;
import dev.qilletni.toolchain.utils.ProgressDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "build", description = "Build a Qilletni library")
public class CommandBuild implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBuild.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Parameters(description = "The root directory of the project", index = "0", defaultValue = ".")
    public Path projectRoot;

    // Ends in .qll: use as file. Otherwise, use as a destination directory
    @CommandLine.Option(names = {"--output-file", "-o"}, description = "The directory or file name of the build .qll")
    public Path outputFilePath;

    @CommandLine.Option(names = {"--no-build-jar", "-n"}, description = "Qilletni should not rebuild build the native .jar")
    public boolean noBuildJar;
    
    @CommandLine.Option(names = {"--gradle-verbose", "-g"}, description = "Verbose Gradle output")
    public boolean verboseGradleOutput;

    @CommandLine.Option(names = {"--log-port", "-p"}, defaultValue = "-1", description = "The port to use for logging")
    private int logPort;

    @Override
    public Integer call() throws IOException {
        if (logPort > 0) {
            LogSetup.setupLogSocket(logPort);
        }

        LOGGER.debug("Called build! {}", this);

        LOGGER.debug("Project root: {}", projectRoot);

        LOGGER.debug("Output file/directory: {}", outputFilePath);

        ProgressDisplay.info("Building Qilletni library...");

        var sourcePath = projectRoot.resolve("qilletni-src");
        var buildDirectory = projectRoot.resolve("build");

        var qilletniInfo = QilletniInfoParser.readQilletniInfo(sourcePath);

        LOGGER.debug("Qilletni Info = {}", qilletniInfo);

        var qilletniSourceHandler = new QilletniSourceHandler();

        var qllBuildPath = buildDirectory.resolve("ql-build").resolve(qilletniInfo.name());

        FileUtil.clearAndCreateDirectory(qllBuildPath);

        if (GradleProjectHelper.isGradleProject(projectRoot)) {
            ProgressDisplay.info("Building native jar...");

            var gradleProjectHelper = GradleProjectHelper.createProjectHelper(projectRoot).orElseThrow(() -> new RuntimeException("Unable to configure Gradle project"));
            var gradleJarOptional = gradleProjectHelper.findProjectJar(verboseGradleOutput);
            
            if (gradleJarOptional.isPresent()) {
                LOGGER.debug("Project jar will be extracted from: {}", gradleJarOptional);

                var gradleJar = gradleJarOptional.get();

                // Build the jar if it doesn't exist, or if it's not told to NOT rebuild
                if (!Files.exists(gradleJar) || !noBuildJar) {
                    LOGGER.debug("Building Java .jar with shadowJar task");
                    gradleProjectHelper.runShadowJarTask(verboseGradleOutput);
                }

                // Copy it if it's been created
                if (Files.exists(gradleJar)) {
                    QllJarExtractor.copyExtractedJar(gradleJar, qllBuildPath);
                } else {
                    ProgressDisplay.warn("The expected native jar path was identified but the file does not exist.");
                }
            } else {
                ProgressDisplay.error("Unable to find native jar in Gradle project");
                return 1;
            }
        }

        qilletniSourceHandler.moveQilletniSource(qllBuildPath, sourcePath);

        QllInfoGenerator.writeQllInfo(new QllInfo(qilletniInfo), qllBuildPath);

        var defaultQllFileName = "%s-%s.qll".formatted(qilletniInfo.name(), qilletniInfo.version().getVersionString());
        Path destinationFile;

        if (outputFilePath != null) {
            if (outputFilePath.getFileName().toString().endsWith(".qll")) {
                Files.createDirectories(outputFilePath.getParent());
                Files.deleteIfExists(outputFilePath);
                destinationFile = outputFilePath;
            } else {
                // Is a parent directory
                Files.createDirectories(outputFilePath);
                destinationFile = outputFilePath.resolve(defaultQllFileName);
            }
        } else {
            var outDir = projectRoot.resolve("build").resolve("ql-build");
            Files.createDirectories(outDir);
            destinationFile = outDir.resolve(defaultQllFileName);
        }

        LOGGER.debug("Writing package to: {}", destinationFile);

        var qllPackager = new QllPackager();

        qllPackager.packageQll(qllBuildPath, destinationFile);

        LOGGER.info("Built library to {}", destinationFile.toAbsolutePath());

        ProgressDisplay.success("Library built successfully!\n  Destination: %s".formatted(destinationFile));

        return 0;
    }

    @Override
    public String toString() {
        return "CommandBuildArgs{" +
                "helpRequested=" + helpRequested +
                '}';
    }
}
