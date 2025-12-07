package dev.qilletni.toolchain.docs;

import dev.qilletni.api.lib.qll.QilletniInfoData;
import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.docgen.DocGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class DocumentationOrchestrator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationOrchestrator.class);
    
    public int beginDocGen(QllInfo qllInfo, Path cacheDirectory, Path inputDirectory, Path outputDirectory) {
        LOGGER.debug("Generating docs for: {}", qllInfo.name());

        try {
            var docGenerator = new DocGenerator(cacheDirectory, outputDirectory);
            docGenerator.generateDocs(inputDirectory, qllInfo);
            
            docGenerator.regenerateGlobalIndex();
        } catch (IOException e) {
            LOGGER.error("Failed to generate docs for: {}", qllInfo.name(), e);
            return 1;
        }
        
        return 0;
    }
    
}
