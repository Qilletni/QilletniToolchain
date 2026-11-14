package dev.qilletni.toolchain.qll;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.api.lib.qll.Version;
import dev.qilletni.impl.lib.LibrarySourceFileResolver;
import dev.qilletni.pkgutil.manifest.LockFile;
import dev.qilletni.pkgutil.manifest.models.ResolvedPackage;
import dev.qilletni.toolchain.VersionProvider;
import dev.qilletni.toolchain.config.QilletniInfoParser;
import dev.qilletni.toolchain.logging.ProgressDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class QllLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(QllLoader.class);
    
    private final VersionProvider versionProvider;
    
    public QllLoader() throws IOException {
        this.versionProvider = new VersionProvider();
    }

    public Optional<QllInfo> loadQll(LibrarySourceFileResolver librarySourceFileResolver, Path qllPath) throws IOException, URISyntaxException {
        LOGGER.debug("Loading {}", qllPath);

        QllInfo qllInfo;

        var zipUri = URI.create("jar:" + qllPath.toUri());

        try (var zipFile = new ZipFile(qllPath.toFile());
             var zipFs = FileSystems.newFileSystem(zipUri, Collections.emptyMap())) {
            qllInfo = QllInfoGenerator.readPackagedQllInfo(zipFile.getInputStream(zipFile.getEntry("qll.info")));
            
            if (!validateQilletniVersion(qllInfo)) {
                return Optional.empty();
            }

            LOGGER.debug("qllInfo = {}", qllInfo);

            var srcPathInZip = zipFs.getPath("qilletni-src");

            var sourceMap = new HashMap<String, String>();

            zipFile.stream().filter(Predicate.not(ZipEntry::isDirectory))
                    .forEach(entry -> {
                        String normalizedName = entry.getName().replace("\\", "/");

                        // Get a Path object *within the Zip FileSystem*
                        // This Path object will correctly use '/' separators internally
                        Path entryPathInZip = zipFs.getPath(normalizedName);

                        Path fileName = entryPathInZip.getFileName();
                        Path parentDir = entryPathInZip.getParent(); // null for entries in root

                        // Determine the "root" component (first directory name)
                        var root = "";
                        if (entryPathInZip.getNameCount() > 1) {
                            // Get the first path element (e.g., "src" from "src/core.ql")
                            root = entryPathInZip.getName(0).toString();
                        }

                        // Handle cases where the entry might not be under "src"
                        var relative = "N/A";

                        try {
                            if (parentDir != null && parentDir.startsWith(srcPathInZip)) {
                                relative = srcPathInZip.relativize(entryPathInZip).toString();
                            } else if (entryPathInZip.startsWith(srcPathInZip) && entryPathInZip.getNameCount() == 1) {
                                // Handle files directly inside src, like "src/somefile.ql"
                                relative = entryPathInZip.getFileName().toString(); // Or srcPathInZip.relativize(entryPathInZip)
                            } else if (entryPathInZip.startsWith(srcPathInZip)) {
                                relative = srcPathInZip.relativize(entryPathInZip).toString();
                            }

                            // If it's not under src, keep relative="N/A" or handle differently
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Cannot relativize {} against {}", entryPathInZip, srcPathInZip);
                        }

                        // Logging using Path components
                        // This is SUPER verbose, ignore it
//                        LOGGER.debug("Entry: {} (Path: {}) is of: {}, {} ({})",
//                                entry.getName(),
//                                entryPathInZip,
//                                fileName,
//                                root,
//                                relative);

                        if (fileName.toString().endsWith(".ql") && root.equals("qilletni-src")) {
                            try (var is = zipFile.getInputStream(entry)) {
                                sourceMap.put(relative.replace("\\", "/"), new String(is.readAllBytes()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

            librarySourceFileResolver.addLibraryResolver(qllInfo.name(), sourceMap::get);
        }

        return Optional.of(qllInfo);
    }

    /**
     * Used when running files in the <code>examples</code> directory of a library. This acts like the parent project is
     * the library, for usage without building/packing the library.
     *
     * @param librarySourceFileResolver The {@link LibrarySourceFileResolver} to register the library with
     * @param projectRootPath           The root path of the project (i.e. the parent of the <code>qilletni-src</code>)
     * @return The created {@link QllInfo}
     */
    public Optional<QllInfo> loadLocalLibrary(LibrarySourceFileResolver librarySourceFileResolver, Path projectRootPath) throws IOException {
        LOGGER.debug("Loading local {}", projectRootPath);

        var srcPath = projectRootPath.resolve("qilletni-src");

        var qllInfo = new QllInfo(QilletniInfoParser.readQilletniInfo(srcPath));
        
        if (!validateQilletniVersion(qllInfo)) {
            return Optional.empty();
        }

        // key: Qilletni file path,  value = actual file path
        var sourceMap = new HashMap<String, Path>();
        var sourceCache = new HashMap<String, String>();

        try (var walk = Files.walk(srcPath)) {
            walk.filter(Predicate.not(Files::isDirectory))
                    .filter(path -> path.getFileName().toString().endsWith(".ql"))
                    .forEach(entry -> {
                        String relative = srcPath.relativize(entry).toString().replace("\\", "/");

                        LOGGER.debug("Entry: {} is of: {}, {}", entry, entry.getFileName(), relative);
                        sourceMap.put(relative, entry);
                    });
        }

        librarySourceFileResolver.addLibraryDiskResolver(qllInfo.name(), sourceMap::get);
        librarySourceFileResolver.addLibraryResolver(qllInfo.name(), importPath ->
                sourceCache.computeIfAbsent(importPath, path -> {
                    var actualPath = sourceMap.get(path);

                    if (actualPath == null) {
                        return null;
                    }

                    try {
                        return Files.readString(actualPath);
                    } catch (IOException e) {
                        LOGGER.error("Failed to read local library file: {}", actualPath, e);
                        return null;
                    }
                }));

        return Optional.of(qllInfo);
    }

    public List<ResolvedPackage> getResolvedPackages(Path lockfilePath) throws IOException {
        var parse = LockFile.parse(lockfilePath);
        return new ArrayList<>(parse.getPackages().values());
    }

    /**
     * Checks if the library's required Qilletni version matches Qilletni version being used.
     * 
     * @param qllInfo The QllInfo of the library being checked
     * @return If the version is valid. If false, the library can not be loaded
     */
    private boolean validateQilletniVersion(QllInfo qllInfo) {
        if (qllInfo.qilletniVersion() == null) {
            return true;
        }
        
        if (!qllInfo.qilletniVersion().permitsVersion(versionProvider.getQilletniCoreVersion())) {
            ProgressDisplay.error("Qilletni version mismatch: %s@%s requires Qilletni version %s, but got %s", qllInfo.name(), qllInfo.version().getVersionString(), qllInfo.qilletniVersion().getVersionWithSpecifier(), versionProvider.getQilletniCoreVersion().getVersionString());
            return false;
        }

        return true;
    }
}
