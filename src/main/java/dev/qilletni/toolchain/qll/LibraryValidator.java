package dev.qilletni.toolchain.qll;

import dev.qilletni.api.lib.qll.QilletniInfoData;
import dev.qilletni.api.lib.qll.QllInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LibraryValidator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryValidator.class);
    
    private final List<QllInfo> libraries;

    public LibraryValidator(List<QllInfo> libraries) {
        this.libraries = libraries;
    }
    
    private boolean hasDependencyMet(QllInfo qllInfo, QilletniInfoData.Dependency dependency) {
        for (var library : libraries) {
            if (library.scope().equals(dependency.scope()) && library.name().equals(dependency.name())) {
                if (dependency.version().permitsVersion(library.version())) {
                    LOGGER.info("[{}] Dependency '{}' version {} matches required version {}", qllInfo.name(), dependency.name(), library.version(), dependency.version());
                    return true;
                }
                
                LOGGER.info("[{}] Dependency '{}' version {} does not match required version {}", qllInfo.name(), dependency.name(), library.version(), dependency.version());
                return false;
            }
        }
        
        LOGGER.error("[{}] Dependency '{}' not found! (Version {})", qllInfo.name(), dependency.name(), dependency.version().getVersionString());
        return false;
    }
    
    public boolean validate() {
        var valid = true;
        
        for (var library : libraries) {
            if (!library.dependencies().stream().allMatch(dependency -> hasDependencyMet(library, dependency))) {
                LOGGER.error("[{}] Library dependencies not met!", library.name());
                valid = false;
            }
        }
        
        return valid;
    }
}
