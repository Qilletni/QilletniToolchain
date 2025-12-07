package dev.qilletni.toolchain;

import dev.qilletni.toolchain.command.CommandBuild;
import dev.qilletni.toolchain.command.CommandDoc;
import dev.qilletni.toolchain.command.CommandInit;
import dev.qilletni.toolchain.command.CommandPersist;
import dev.qilletni.toolchain.command.CommandRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "qilletni", version = "v1.0.0-SNAPSHOT", subcommands = {CommandRun.class, CommandBuild.class, CommandDoc.class, CommandInit.class, CommandPersist.class})
public class QilletniToolchainApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniToolchainApplication.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true, description = "Display the version")
    private boolean versionRequested = false;

    public static void main(String[] args) {
        var application = new QilletniToolchainApplication();

        if (args.length == 0) {
            LOGGER.error("Invalid command!");
            return;
        }

        new CommandLine(application)
                .execute(args);
    }
}
