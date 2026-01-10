package dev.qilletni.toolchain.docs;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.docgen.DocGenerator;
import dev.qilletni.toolchain.logging.ProgressDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class DocumentationOrchestrator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationOrchestrator.class);

    private final DocGenerator docGenerator;

    public DocumentationOrchestrator(Path cacheDirectory, Path outputDirectory) {
        this.docGenerator = new DocGenerator(cacheDirectory, outputDirectory);
    }
    
    public int beginDocGen(QllInfo qllInfo, Path inputDirectory) {
        LOGGER.debug("Generating docs for: {}", qllInfo.name());

        try {
            ProgressDisplay.info("Generating docs...");
            docGenerator.generateDocs(inputDirectory, qllInfo);

            ProgressDisplay.info("Regenerating global index...");
            docGenerator.regenerateGlobalIndex();

            ProgressDisplay.success("Generated docs");
        } catch (IOException e) {
            LOGGER.error("Failed to generate docs for: {}", qllInfo.name(), e);
            return 1;
        }
        
        return 0;
    }

    public int regenerateAllPackages() {
        try {
            docGenerator.regenerateAllCachedDocs();

            docGenerator.regenerateGlobalIndex();
        } catch (IOException e) {
            LOGGER.error("Failed to regenerate all packages' docs", e);
            return 1;
        }

        return 0;
    }
    
}
