package com.nvlad.yii2support.common;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathMappingSettings;
import com.jetbrains.php.config.commandLine.PhpCommandSettings;
import com.jetbrains.php.config.commandLine.PhpCommandSettingsBuilder;
import com.jetbrains.php.remote.interpreter.PhpRemoteSdkAdditionalData;
import com.jetbrains.php.run.remote.PhpRemoteInterpreterManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class YiiCommandLineUtil {
    private static final boolean is2016 = ApplicationInfo.getInstance().getMajorVersion().equals("2016");

    private static final String[] knownErrors = new String[]{
            "userName must not be null",
            "Auth cancel",
            "PHP home is not specified or invalid.",
    };

    public static GeneralCommandLine create(Project project, String command) throws ExecutionException {
        return create(project, command, (String[]) null);
    }

    public static GeneralCommandLine create(Project project, String command, String... parameters) throws ExecutionException {
        return create(project, command, Arrays.asList(parameters));
    }

    public static GeneralCommandLine create(Project project, String command, List<String> parameters) throws ExecutionException {
        parameters.add("--color");

        String yiiRootPath = YiiApplicationUtils.getYiiRootPath(project);
        PhpCommandSettings commandSettings = commandSettings(project, command, parameters);
        GeneralCommandLine commandLine = commandSettings.createGeneralCommandLine();
        commandLine.setWorkDirectory(yiiRootPath);

        return commandLine;
    }

    @Nullable
    public static ProcessHandler configureHandler(Project project, String command, List<String> parameters) throws ExecutionException {
        parameters.add("--color");

        PhpCommandSettings commandSettings = commandSettings(project, command, parameters);
        GeneralCommandLine commandLine = commandSettings.createGeneralCommandLine();
        if (commandSettings.isRemote()) {
            PhpRemoteInterpreterManager interpreterManager = PhpRemoteInterpreterManager.getInstance();
            if (interpreterManager == null) {
                return null;
            }

            PhpRemoteSdkAdditionalData additionalData = (PhpRemoteSdkAdditionalData) commandSettings.getAdditionalData();
            if (additionalData == null) {
                return null;
            }

            try {
                Method getRemoteProcessHandler = getMethod(interpreterManager);
                if (is2016) {
                    return (ProcessHandler) getRemoteProcessHandler
                            .invoke(interpreterManager, project, "Unknown string", additionalData, commandLine);
                } else {
                    PathMappingSettings.PathMapping[] pathMappings = new PathMappingSettings.PathMapping[0];
                    if (getRemoteProcessHandler.getParameterCount() == 5) {
                        return (ProcessHandler) getRemoteProcessHandler
                                .invoke(interpreterManager, project, additionalData, commandLine, true, pathMappings);

                    }

                    return (ProcessHandler) getRemoteProcessHandler
                            .invoke(interpreterManager, project, additionalData, commandLine, pathMappings);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                processError(e);
            }

            return null;
        }

        return new OSProcessHandler(commandLine);
    }

    public static void processError(Throwable e) {
        final String message;
        if (e instanceof InvocationTargetException) {
            message = ((InvocationTargetException) e).getTargetException().getMessage();
        } else {
            message = e.getMessage();
        }

        if (e instanceof InvocationTargetException || ArrayUtil.contains(message, knownErrors)) {
            SwingUtilities.invokeLater(() -> Messages.showErrorDialog(message, "Execution Error"));

            return;
        }

        throw new RuntimeException(e);
    }

    private static Method getMethod(PhpRemoteInterpreterManager manager) throws NoSuchMethodException {
        for (Method method : manager.getClass().getMethods()) {
            if (method.getName().equals("getRemoteProcessHandler")) {
                return method;
            }
        }

        throw new NoSuchMethodException("getRemoteProcessHandler");
    }

    private static PhpCommandSettings commandSettings(Project project, String command, List<String> parameters) throws ExecutionException {
        String yiiRootPath = YiiApplicationUtils.getYiiRootPath(project);
        PhpCommandSettings commandSettings = PhpCommandSettingsBuilder.create(project, false);
        if (YiiApplicationUtils.getAppTemplate(project) == YiiApplicationTemplate.StarterKit) {
            commandSettings.setScript(yiiRootPath + "/console/yii");
        } else {
            commandSettings.setScript(yiiRootPath + "/yii");
        }

        commandSettings.addArgument(command);
        commandSettings.addArguments(parameters);

        return commandSettings;
    }
}
