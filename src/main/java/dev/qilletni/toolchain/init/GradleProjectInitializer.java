package dev.qilletni.toolchain.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

public class GradleProjectInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GradleProjectInitializer.class);
    
    private final Path projectDir;
    private final String projectName;
    private final String packageName;
    private final String className;
    private final String gradleVersion;
    private final String tagVersion;

    public GradleProjectInitializer(Path projectDir, String projectName, String packageName, String className, String gradleVersion) {
        this.projectDir = projectDir;
        this.projectName = projectName;
        this.packageName = packageName;
        this.className = className;
        this.gradleVersion = gradleVersion;

        var tagVersion = gradleVersion;
        if (gradleVersion.indexOf(".") == gradleVersion.lastIndexOf(".")) { // Turn 8.0 into 8.0.0
            tagVersion += ".0";
        }
        
        this.tagVersion = tagVersion;
    }

    public void initializeProject() throws IOException, URISyntaxException {
        // Create Project Structure
        createDirectories();

        // Generate Files
        generateBuildGradle();
        generateSettingsGradle();
        generateMainClass();

        // Set Up Gradle Wrapper
        setupGradleWrapper();
        
        LOGGER.debug("Project initialized successfully");
    }

    private void createDirectories() throws IOException {
        var packagePath = packageName.replace('.', '/');
        String[] dirs = {
                "src/main/java/" + packagePath,
                "gradle/wrapper"
        };

        for (var dir : dirs) {
            Path dirPath = projectDir.resolve(dir);
            Files.createDirectories(dirPath);
        }
    }

    private void generateBuildGradle() throws IOException {
        String content = """
            plugins {
                id 'java'
                id 'com.gradleup.shadow' version '8.3.11'
            }

            group = '%s'
            version = '1.0.0'

            sourceCompatibility = JavaVersion.VERSION_22
            targetCompatibility = JavaVersion.VERSION_22

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                testImplementation platform('org.junit:junit-bom:5.9.1')
                testImplementation 'org.junit.jupiter:junit-jupiter'

                compileOnly 'dev.qilletni.api:qilletni-api:1.0.0-SNAPSHOT'
            }

            shadowJar {
                configurations = [project.configurations.runtimeClasspath]
                archiveClassifier.set('')
            }

            test {
                useJUnitPlatform()
            }
            """.formatted(packageName);

        var filePath = projectDir.resolve("build.gradle");
        Files.writeString(filePath, content);
    }

    private void generateSettingsGradle() throws IOException {
        var content = """
            rootProject.name = '%s'
            """.formatted(projectName);
        var filePath = projectDir.resolve("settings.gradle");
        Files.writeString(filePath, content);
    }

    private void generateMainClass() throws IOException {
        var content = """
            package %s;

            public class %s {
                public String sayGoodbye() {
                    return "Goodbye, World!";
                }
            }
            """.formatted(packageName, className);

        var packagePath = packageName.replace('.', '/');
        Path filePath = projectDir.resolve("src/main/java/" + packagePath + "/" + className + ".java");
        Files.writeString(filePath, content);
    }

    private void setupGradleWrapper() throws IOException, URISyntaxException {
        // Generate gradle-wrapper.properties
        var propertiesContent = """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\\://services.gradle.org/distributions/gradle-%s-bin.zip
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
            """.formatted(gradleVersion);

        var propertiesPath = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        Files.writeString(propertiesPath, propertiesContent);

        // Generate gradlew and gradlew.bat scripts
        generateGradlewScripts();

        // Download gradle-wrapper.jar
        downloadGradleWrapperJar();
    }

    private void downloadGradleWrapperJar() throws IOException, URISyntaxException {
        var wrapperJarUrl = "https://raw.githubusercontent.com/gradle/gradle/v%s/gradle/wrapper/gradle-wrapper.jar".formatted(tagVersion);
        var wrapperJarPath = projectDir.resolve("gradle/wrapper/gradle-wrapper.jar");

        LOGGER.debug("Downloading Gradle {} from {}", gradleVersion, wrapperJarUrl);
        try (var  in = new URI(wrapperJarUrl).toURL().openStream()) {
            Files.copy(in, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void generateGradlewScripts() throws IOException, URISyntaxException {
        
        var gradlewUrl = "https://raw.githubusercontent.com/gradle/gradle/refs/tags/v%s/gradlew".formatted(tagVersion);
        var gradlewBatUrl = "%s.bat".formatted(gradlewUrl);

        LOGGER.debug("Downloading Gradle scripts from {}", gradlewUrl);
        
        var gradlewPath = projectDir.resolve("gradlew");
        try (var in = new URI(gradlewUrl).toURL().openStream()) {
            Files.copy(in, gradlewPath, StandardCopyOption.REPLACE_EXISTING);

            // Make the script executable (Unix systems)
            try {
                Set<PosixFilePermission> perms = EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE
                );
                Files.setPosixFilePermissions(gradlewPath, perms);
            } catch (UnsupportedOperationException e) {
                // Ignore exception on unsupported file systems
            }
        }

        var gradleBatPath = projectDir.resolve("gradlew.bat");
        try (var in = new URI(gradlewBatUrl).toURL().openStream()) {
            Files.copy(in, gradleBatPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
