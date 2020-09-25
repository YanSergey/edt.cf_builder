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

import ru.yanygin.dt.cfbuilder.plugin.ui.ImportJob.SupportMode;
import ru.yanygin.dt.cfbuilder.plugin.ui.PlatformV8Commands.V8CommandTypes;

public class Actions {
	
	private Actions() {
	}

	public static String findEnterpriseRuntimePathFromProject(IProject project,
			IRuntimeVersionSupport runtimeVersionSupport,
			IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager) {

		Version version = runtimeVersionSupport.getRuntimeVersion(project);
		return findEnterpriseRuntimePathFromVersion(version, resolvableRuntimeInstallationManager);
	}

	public static String findEnterpriseRuntimePathFromVersion(Version version,
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

	public static CfFileInfo askCfLocationPath(Shell parentShell, int style) {

		FileDialog fileDialog = new FileDialog(parentShell, style);
		fileDialog.setText(Messages.Actions_Set_CF_Location);

		String[] filterExt = { "*.cf" };
		String[] filterNames = { Messages.Filter_1C_Files };
		fileDialog.setFilterExtensions(filterExt);
		fileDialog.setFilterNames(filterNames);

		CfFileInfo cfFileInfo = null;
		String cfFullName = fileDialog.open();
		if (cfFullName != null) {
			
			cfFileInfo = new CfFileInfo(cfFullName, fileDialog.getFilterPath(), fileDialog.getFileName(),
					fileDialog.getFileName().replace(".cf", ""));
		}

		return cfFileInfo;
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

		Map<String, String> v8command = PlatformV8Commands.getPlatformV8Command(commandType);

		if (!checkBuildState(projectContext, v8command.get("actionMessage"), buildMonitor, processResult))
			return;

		if ((commandType == V8CommandTypes.DUMPCONFIGTOCF) || (commandType == V8CommandTypes.CREATEDISTRCF)) {
			File buildDir = new File(projectContext.getCfFileInfo().LOCATION);
			if (!buildDir.exists()) {
				buildDir.mkdir();
			}
		}

		IStatus status = Status.OK_STATUS;
		String processOutput = "";

		Process process;
		ProcessBuilder processBuilder = new ProcessBuilder();

		Map<String, String> env = processBuilder.environment(); // NOSONAR

		env.put("PLATFORM_1C_PATH",	projectContext.getPlatformV8Path());
		env.put("BASE_1C_PATH",		projectContext.getTempDirs().getOnesBasePath());
		env.put("LOGFILE",			projectContext.getTempDirs().getLogFilePath());
		env.put("XMLDIR",			projectContext.getTempDirs().getXmlPath());
		env.put("CFNAME",			projectContext.getCfFileInfo().FULLNAME);

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

		} catch (IOException | InterruptedException e) { // NOSONAR
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
			return status;
		}
		boolean exportSubordinatesObjects = IExportStrategy.DEFAULT.exportExternalProperties(configuration);
		boolean exportExternalProperties = IExportStrategy.DEFAULT.exportExternalProperties(configuration);
		status = exportService.work(configuration, exportPath, exportSubordinatesObjects, exportExternalProperties, progressBar);
		return status;
	}

	public static void importXmlToProject(IImportOperationFactory importOperationFactory, ProjectContext projectContext,
			ProcessResult processResult, IProgressMonitor buildMonitor) {

		if (!Actions.checkBuildState(projectContext, Messages.Actions_Import_ProjectFromXml, buildMonitor,
				processResult))
			return;

		changeSupportMode(projectContext);

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
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			}
		}

		IImportOperation importOperation = importOperationFactory.createImportConfigurationOperation(
				projectContext.getProjectName(), projectContext.getV8version(), importPath);
		try {
			importOperation.run(buildMonitor);
		} catch (InvocationTargetException | InterruptedException e) { // NOSONAR
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.Status_UnknownError);
			Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
		}

		processResult.setResult(status, processOutput);

	}

	private static void changeSupportMode(ProjectContext projectContext) {
		if (projectContext.getSupportMode() == SupportMode.DISABLESUPPORT) {
			String[] parentConfigurationsFiles = projectContext.getParentConfigurationsFiles();
			for (String fileName : parentConfigurationsFiles) {
				File parentFile = new File(fileName);
				if (parentFile.exists()) {
					TempDirs.recursiveDelete(parentFile);
					Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.Info_FileDelete, parentFile.getAbsolutePath())));
				} else {
					Activator.log(Activator.createInfoStatus(Messages.Info_FileSupportNotFound));
				}
			}
		}
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
