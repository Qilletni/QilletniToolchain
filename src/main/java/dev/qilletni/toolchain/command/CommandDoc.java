package dev.qilletni.toolchain.command;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.toolchain.FileUtil;
import dev.qilletni.toolchain.PathUtility;
import dev.qilletni.toolchain.config.QilletniInfoParser;
import dev.qilletni.toolchain.docs.DocumentationOrchestrator;
import dev.qilletni.toolchain.qll.QllExtractor;
import dev.qilletni.toolchain.qll.QllInfoGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "doc", description = "Generated HTML docs for Qilletni")
public class CommandDoc implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDoc.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Parameters(description = "Either a .qll or a source qilletni-src directory", index = "0")
    public Path sourcePath;

    @CommandLine.Option(names = {"--output-file", "-o"}, description = "The directory to put the generated docs in")
    public Path outputFilePath;

    @CommandLine.Option(names = {"--cache-path", "-c"}, description = "The directory containing the cache of the docs")
    public Path cachePath;
    
    @Override
    public Integer call() throws Exception {
        LOGGER.debug("Generating docs from: {}", sourcePath);
        LOGGER.debug("Doc output path: {}", outputFilePath);
        
        if (cachePath == null) {
            cachePath = PathUtility.getCachePath();
        }
        
        LOGGER.debug("Cache path: {}", cachePath);

        QllInfo qllInfo;
        Path extractedDir = null;

        try {
            if (sourcePath.getFileName().toString().endsWith(".qll")) {
                Optional<Path> path = QllExtractor.extractToTmp(sourcePath);
                if (path.isEmpty()) {
                    LOGGER.error("Unable to extract QLL from {}", sourcePath);
                    return 1;
                }

                extractedDir = path.get();

                sourcePath = extractedDir.resolve("qilletni-src");
                qllInfo = QllInfoGenerator.readPackagedQllInfo(Files.newInputStream(extractedDir.resolve("qll.info")));
            } else {
                qllInfo = new QllInfo(QilletniInfoParser.readQilletniInfo(sourcePath));
            }

            var documentationOrchestrator = new DocumentationOrchestrator();
            return documentationOrchestrator.beginDocGen(qllInfo, cachePath, sourcePath, outputFilePath);
        } finally {
            if (extractedDir != null) {
                FileUtil.deleteDirectory(extractedDir);
            }
        }
    }
}
