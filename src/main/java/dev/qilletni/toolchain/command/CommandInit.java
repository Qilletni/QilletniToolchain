package dev.qilletni.toolchain.command;

import dev.qilletni.toolchain.init.ProjectInit;
import dev.qilletni.toolchain.init.ProjectType;
import dev.qilletni.toolchain.utils.ProgressDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "init", description = "Initializes a Qilletni project")
public class CommandInit implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandInit.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Parameters(description = "The name of the project", index = "0")
    public String scopedProjectName;

    @CommandLine.Option(names = {"--dir", "-d"}, description = "The directory to initialize the project in. Default is the project name.", defaultValue = "")
    public String sourcePathStr;

    @CommandLine.Option(names = {"--author", "-a"}, description = "The author of the project. Defaults to the scope, if available", defaultValue = "me")
    public String authorName;

    @CommandLine.Option(names = {"--native-class", "-c"}, description = "The native canonical class name to initialize a project with. If no class is specified, native bindings will not be set up.")
    public String nativeClass;

    @CommandLine.Option(names = {"--type", "-t"}, description = "The type of the project to initialize, either 'library' or 'application'", defaultValue = "application")
    public String projectType;
    
    @Override
    public Integer call() throws Exception {
        String projectName = scopedProjectName;
        String projectScope = null;

        if (projectName != null) {
            var splitProjectName = projectName.split("/");
            if (splitProjectName.length == 2) {
                projectScope = splitProjectName[0];
                projectName = splitProjectName[1];

                if (authorName == null) {
                    authorName = projectScope;
                }
            } else if (splitProjectName.length > 2) {
                LOGGER.error("Invalid project name: {}", projectName);
            }
        }

        // If it it unset ("") and it's been given a project name
        if ("".equals(sourcePathStr) && projectName != null) {
            sourcePathStr = projectName;
        }

        var sourcePath = Paths.get(sourcePathStr);

        LOGGER.debug("Initializing in: {}", sourcePath);

        ProjectType projectTypeEnum;
        
        if ("library".equals(projectType)) {
            ProgressDisplay.info("Initializing a library project...");
            projectTypeEnum = ProjectType.LIBRARY;

            if (projectScope == null || projectScope.isEmpty()) {
                LOGGER.error("A scope must be provided for a library");
                return 1;
            }
        } else if ("application".equals(projectType)) {
            ProgressDisplay.info("Initializing an application project...");
            projectTypeEnum = ProjectType.APPLICATION;
        } else {
            ProgressDisplay.error("Invalid project type: %s", projectType);
            return 1;
        }

        ProjectInit.NativeInit nativeInit = null;
        
        if (nativeClass != null) {
            var splitClass = nativeClass.split("\\.");
            var packageName = String.join(".", Arrays.copyOf(splitClass, splitClass.length - 1));
            var className = splitClass[splitClass.length - 1];
            
            LOGGER.debug("Creating project with native package of {} and class name of {}", packageName, className);
            
            nativeInit = new ProjectInit.NativeInit(packageName, className);
        }

        new ProjectInit().initialize(sourcePath.toAbsolutePath(), projectName, projectScope, authorName, nativeInit, projectTypeEnum);

        
        return 0;
    }
}
