package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com._1c.g5.v8.dt.export.ExportException;
import com._1c.g5.v8.dt.export.IExportService;
import com._1c.g5.v8.dt.export.IExportServiceRegistry;
import com._1c.g5.v8.dt.export.IExportStrategy;
import com._1c.g5.v8.dt.import_.IImportOperation;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.MatchingRuntimeNotFound;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;

import ru.yanygin.dt.cfbuilder.plugin.ui.PlatformV8Commands.V8CommandTypes;

public class Actions {

	public static String findEnterpriseRuntimePathFromProject(IProject project,
			IRuntimeVersionSupport runtimeVersionSupport,
			IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager) {

		Version version = runtimeVersionSupport.getRuntimeVersion(project);
		return findEnterpriseRuntimePathFromVersion(version, runtimeVersionSupport, resolvableRuntimeInstallationManager);
	}

	public static String findEnterpriseRuntimePathFromVersion(Version version,
			IRuntimeVersionSupport runtimeVersionSupport,
			IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager) {

		IResolvableRuntimeInstallation resolvableRuntimeInstallation = resolvableRuntimeInstallationManager.getDefault(
				"com._1c.g5.v8.dt.platform.services.core.runtimeType.EnterprisePlatform", version.toString());

		RuntimeInstallation currentRuntime;
		try {
			currentRuntime = resolvableRuntimeInstallation.get();
		} catch (MatchingRuntimeNotFound e) {
			e.printStackTrace();
			return null;
		}
		return "\"".concat(currentRuntime.getInstallLocation().toString()).concat("\\1cv8.exe\"");
	}

	public static HashMap<String, String> askCfLocationPath(Shell parentShell, int style) {

		FileDialog saveDialog = new FileDialog(parentShell, style);
		saveDialog.setText(Messages.Actions_Set_CF_Location);

		String[] filterExt = { "*.cf" };
		String[] filterNames = { Messages.Filter_1C_Files };
		saveDialog.setFilterExtensions(filterExt);
		saveDialog.setFilterNames(filterNames);

		HashMap<String, String> cfNameInfo = null;

		String cfFullName = saveDialog.open();
		if (cfFullName != null) {
			cfNameInfo = new HashMap<>();
			cfNameInfo.put("cfFullName", cfFullName);
			cfNameInfo.put("cfLocation", saveDialog.getFilterPath());
		}

		return cfNameInfo;
	}

	public static boolean checkBuildState(ProjectContext projectContext, String taskName, IProgressMonitor buildMonitor,
			ProcessResult processResult) {
		if (processResult.statusIsOK() & buildMonitor.isCanceled()) {
			processResult.setResult(Status.CANCEL_STATUS);

			String infoMessage = MessageFormat.format(Messages.Status_CfBuildCancel, projectContext.getProjectName());
			buildMonitor.setTaskName(infoMessage);
			return false;
		}

		if (processResult.statusIsOK()) {
			buildMonitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
			Activator.log(Activator.createInfoStatus(taskName));
			return true;
		}

		return false;
	}

	public static void runPlatformV8Command(V8CommandTypes commandType, ProjectContext projectContext,
			ProcessResult processResult, IProgressMonitor buildMonitor) {

		HashMap<String, String> v8command = PlatformV8Commands.getPlatformV8Command(commandType);

		if (!checkBuildState(projectContext, v8command.get("actionMessage"), buildMonitor, processResult))
			return;

		if (commandType == V8CommandTypes.DUMPCONFIGTOCF) {
			File buildDir = new File(projectContext.getCfLocation());
			if (!buildDir.exists()) {
				buildDir.mkdir();
			}
		}

		IStatus status = Status.OK_STATUS;
		String processOutput = "";

		Process process;
		ProcessBuilder processBuilder = new ProcessBuilder();

		Map<String, String> env = processBuilder.environment();

		env.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		env.put("BASE_1C_PATH",		projectContext.getTempDirs().getOnesBasePath());
		env.put("LOGFILE",			projectContext.getTempDirs().getLogFilePath());
		env.put("XMLDIR",			projectContext.getTempDirs().getXmlPath());
		env.put("CFNAME",			projectContext.getCfFullName());

		processBuilder.command("cmd.exe", "/c", v8command.get("command"));
		try {
			process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "windows-1251"));

			String line;
			while ((line = reader.readLine()) != null) {
				processOutput = processOutput.concat(System.lineSeparator()).concat(line);
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_OperationAbort);
				Activator.log(Activator.createErrorStatus(Messages.Status_OperationAbort));
			}

		} catch (IOException | InterruptedException e) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_UnknownError);
			Activator.log(Activator.createErrorStatus(Messages.Status_UnknownError.concat(processOutput), e));
		}

		processResult.setResult(status, processOutput);

	}

	public static IStatus exportProjectToXml(IExportServiceRegistry exportServiceRegistry, Version version,
			Configuration configuration, Path exportPath, SubMonitor progressBar) {
		IStatus status;

		progressBar.subTask(Messages.Info_DataIsPreparing);

		IExportService exportService = null;
		try {
			exportService = exportServiceRegistry.getExportService(version);
		} catch (ExportException ex) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_UnknownError);
			Activator.log(Activator.createErrorStatus(ex.getLocalizedMessage(), ex));
			ex.printStackTrace();
		}
		boolean exportSubordinatesObjects = IExportStrategy.DEFAULT.exportExternalProperties(configuration);
		boolean exportExternalProperties = IExportStrategy.DEFAULT.exportExternalProperties(configuration);
		status = exportService.work(configuration, exportPath, exportSubordinatesObjects, exportExternalProperties,
				progressBar);
		return status;
	}

	public static void importXmlToProject(IImportOperationFactory importOperationFactory, ProjectContext projectContext,
			ProcessResult processResult, IProgressMonitor buildMonitor) {

		if (!Actions.checkBuildState(projectContext, Messages.Actions_Import_ProjectFromXml, buildMonitor,
				processResult))
			return;

		IStatus status = Status.OK_STATUS;
		String processOutput = "";

		Path importPath = Paths.get(projectContext.getTempDirs().getXmlPath());

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectContext.getProjectName());
		if (project.exists()) {

			try {
				SubMonitor subMonitor = SubMonitor.convert(buildMonitor, 100);
				project.delete(true, true, subMonitor.newChild(4));
			} catch (CoreException e) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_UnknownError);
				e.printStackTrace();
			}
		}

		IImportOperation importOperation = importOperationFactory.createImportConfigurationOperation(
				projectContext.getProjectName(), projectContext.getVersion(), importPath);
		try {
			importOperation.run(buildMonitor);
		} catch (InvocationTargetException | InterruptedException e) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_UnknownError);
			e.printStackTrace();
		}

		processResult.setResult(status, processOutput);

	}

	public static void runCommand(String command, Map<String, String> environmentVariables,
			ProcessResult processResult) {

		IStatus status = Status.OK_STATUS;
		String processOutput = "";

		Process process;
		ProcessBuilder processBuilder = new ProcessBuilder();

		Map<String, String> env = processBuilder.environment();
		environmentVariables.forEach((k, v) -> env.put(k, v));

		processBuilder.command("cmd.exe", "/c", command);
		try {
			process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "windows-1251"));

			String line;
			while ((line = reader.readLine()) != null) {
				processOutput = processOutput.concat(System.lineSeparator()).concat(line);
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_OperationAbort);
				Activator.log(Activator.createErrorStatus(Messages.Status_OperationAbort));
			}

		} catch (IOException | InterruptedException e) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_UnknownError);
			Activator.log(Activator.createErrorStatus(Messages.Status_UnknownError.concat(processOutput), e));
		}

		processResult.setResult(status, processOutput);

	}

	public static String readOutLogFile(String fileName) {
		String contents = "";

		if (new File(fileName).exists()) {
			try {
				contents = new String(Files.readAllBytes(Paths.get(fileName)), Charset.forName("Windows-1251"));
			} catch (IOException e) {
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			}
		}

		return contents;
	}

}
