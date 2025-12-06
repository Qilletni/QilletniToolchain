package dev.qilletni.toolchain.config;

import dev.qilletni.api.lib.qll.ComparableVersion;
import dev.qilletni.api.lib.qll.QilletniInfoData;
import dev.qilletni.api.lib.qll.Version;
import dev.qilletni.toolchain.exceptions.InvalidVersionException;
import dev.qilletni.toolchain.exceptions.QilletniInfoFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class QilletniInfoParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniInfoParser.class);

    /**
     * The name of the Qilletni program/library info file, which has an extension in the
     * {@link #QILLETNI_FILE_EXTENSIONS} array.
      */
    private static final String QILLETNI_INFO = "qilletni_info";

    /**
     * The potential extensions of the file of name {@link #QILLETNI_INFO}.
     */
    private static final String[] QILLETNI_FILE_EXTENSIONS = {"yml", "yaml"};

    /**
     * Locates a {@link #QILLETNI_INFO} file directly in the given directory, and returns its path.
     * 
     * @param directory The direct parent of the info file
     * @return The {@code qilletni_info} file, if it exists
     */
    public static Optional<Path> findQilletniInfoFile(Path directory) {
        for (var fileExtension : QILLETNI_FILE_EXTENSIONS) {
            var qilletniInfoFile = directory.resolve(String.format("%s.%s", QILLETNI_INFO, fileExtension));

            if (Files.exists(qilletniInfoFile)) {
                return Optional.of(qilletniInfoFile);
            }
        }
        
        return Optional.empty();
    }

    /**
     * Reads the {@link #QILLETNI_INFO} file in the given Qilletni source parent directory.
     * 
     * @param qilletniDirectory The parent directory of the {@link #QILLETNI_INFO} file and all {@code .ql} source
     *                          files.
     * @return The read {@link #QILLETNI_INFO} file data
     * @throws IOException
     */
    public static QilletniInfoData readQilletniInfo(Path qilletniDirectory) throws IOException {
        var qilletniInfoFile = findQilletniInfoFile(qilletniDirectory)
                .orElseThrow(() -> new FileNotFoundException(QILLETNI_INFO + " file not found! in " + qilletniDirectory.toString()));

        var yaml = new Yaml();
        Map<String, Object> obj = yaml.load(Files.newInputStream(qilletniInfoFile));

        var nameString = (String) Objects.requireNonNull(obj.get("name"), "'name' required in qilletni_info");
        var authorString = (String) Objects.requireNonNull(obj.get("author"), "'author' required in qilletni_info");
        var description = (String) obj.getOrDefault("description", "");
        
        var version = Version.parseVersionString((String) Objects.requireNonNull(obj.get("version"), "'version' required in qilletni_info"))
                .orElseThrow(() -> new InvalidVersionException("Invalid version"));

        var sourceUrl = (String) obj.getOrDefault("source_url", "");

        var providerClass = (String) obj.getOrDefault("provider", null);
        var nativeBindFactoryClass = (String) obj.getOrDefault("native_bind_factory", null);
        var nativeClasses = (List<String>) obj.getOrDefault("native_classes", Collections.emptyList());
        var autoImportFiles = (List<String>) obj.getOrDefault("auto_import", Collections.emptyList());
        var dependencies = obj.getOrDefault("dependencies", null);

        List<QilletniInfoData.Dependency> dependencyList = new ArrayList<>();

        if (dependencies instanceof Map depMap) {
            Map<String, String> dependenciesMap = (Map<String, String>) depMap;
            dependencyList = dependenciesMap.entrySet().stream().map(entry -> {
                var packageName = entry.getKey().startsWith("@") ? entry.getKey().substring(1) : entry.getKey();

                var dependencyVersion = ComparableVersion.parseComparableVersionString(entry.getValue())
                        .orElseThrow(() -> new QilletniInfoFormatException("Invalid version for dependency " + packageName));

                var nameScope = parsePackageName(packageName);

                return new QilletniInfoData.Dependency(nameScope.scope, nameScope.name, dependencyVersion);
            }).toList();
        }

        var nameScope = parsePackageName(nameString);

        return new QilletniInfoData(nameScope.scope, nameScope.name, version, authorString, description, sourceUrl, providerClass, nativeBindFactoryClass, nativeClasses, autoImportFiles, dependencyList);
    }

    record PackageName(String scope, String name) {}

    private static PackageName parsePackageName(String fullName) {
        var splitName = fullName.split("/");

        if (splitName.length != 2) {
            return new PackageName("", splitName[0]);
        }

        return new PackageName(splitName[0], splitName[1]);
    }
}
