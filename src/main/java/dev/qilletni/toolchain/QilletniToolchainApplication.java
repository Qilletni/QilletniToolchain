package dev.qilletni.toolchain;

import dev.qilletni.toolchain.command.CommandBuild;
import dev.qilletni.toolchain.command.CommandDoc;
import dev.qilletni.toolchain.command.CommandInit;
import dev.qilletni.toolchain.command.CommandPersist;
import dev.qilletni.toolchain.command.CommandRun;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.stream.Collectors;

@CommandLine.Command(name = "qilletni", version = "v1.0.0-SNAPSHOT", subcommands = {CommandRun.class, CommandBuild.class, CommandDoc.class, CommandInit.class, CommandPersist.class})
public class QilletniToolchainApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniToolchainApplication.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"--version"}, versionHelp = true, description = "Display the version")
    private boolean versionRequested = false;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose logging output")
    public void setVerbose(boolean verbose) {
        QilletniToolchainApplication.verbose = verbose;
        if (verbose) {
            System.setProperty("VERBOSE", "DEBUG");
            // Reconfigure log4j2 to pick up the new system property
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.reconfigure();
        }
    }

    public static void main(String[] args) {
        var application = new QilletniToolchainApplication();

        if (args.length == 0) {
            LOGGER.error("Invalid command!");
            return;
        }

        LOGGER.info("Executing command with args:  {}", Arrays.stream(args).map("'%s'"::formatted).collect(Collectors.joining(" ")));

        new CommandLine(application)
                .execute(args);
    }

    private static boolean verbose = false;

    public static boolean isVerbose() {
        return verbose;
    }
}
