package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.MatchingRuntimeNotFound;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;

public class Actions {

	public static String findEnterpriseRuntimePathFromProject(IProject project, IRuntimeVersionSupport runtimeVersionSupport,
			IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager) {
		
		Version version = runtimeVersionSupport.getRuntimeVersion(project);

		return findEnterpriseRuntimePathFromVersion(version, runtimeVersionSupport, resolvableRuntimeInstallationManager);
//		IResolvableRuntimeInstallation resolvableRuntimeInstallation = resolvableRuntimeInstallationManager.getDefault(
//				"com._1c.g5.v8.dt.platform.services.core.runtimeType.EnterprisePlatform", version.toString());
//
//		RuntimeInstallation currentRuntime;
//		try {
//			currentRuntime = resolvableRuntimeInstallation.get();
//		} catch (MatchingRuntimeNotFound e) {
//			e.printStackTrace();
//			return null;
//		}
//		return "\"".concat(currentRuntime.getInstallLocation().toString()).concat("\\1cv8.exe\"");
	}

	public static String findEnterpriseRuntimePathFromVersion(Version version, IRuntimeVersionSupport runtimeVersionSupport,
			IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager) {
		
//		Version version = runtimeVersionSupport.getRuntimeVersion(project);

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
		saveDialog.setText(Messages.CfBuild_Set_CF_Location);

		String[] filterExt = { "*.cf" };
		String[] filterNames = { Messages.CfBuild_1C_Files };
		saveDialog.setFilterExtensions(filterExt);
		saveDialog.setFilterNames(filterNames);

		HashMap<String, String> cfNameInfo = new HashMap<>();

		cfNameInfo.put("cfFullName", saveDialog.open());
		cfNameInfo.put("cfLocation", saveDialog.getFilterPath());

		return cfNameInfo;
	}

	public static boolean checkBuildState(ProjectContext projectContext, String taskName, IProgressMonitor buildMonitor, ProcessResult processResult) {
		if (processResult.statusIsOK() & buildMonitor.isCanceled()) {
			processResult.setResult(Status.CANCEL_STATUS);
			//String infoMessage = Messages.CfBuild_Cancel.replace("%projectName%", projectContext.getProjectName());
			String infoMessage = MessageFormat.format(Messages.CfBuild_Cancel, projectContext.getProjectName());
			buildMonitor.setTaskName(infoMessage);
			//Activator.log(Activator.createInfoStatus(infoMessage));
			return false;
		}

		if (processResult.statusIsOK()) {
			buildMonitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
			Activator.log(Activator.createInfoStatus(taskName));
			return true;
		}

		return false;
	}

	public static void createTempBase(ProjectContext projectContext, TempDirs tempDirs, IProgressMonitor buildMonitor, ProcessResult processResult) {

		if (!checkBuildState(projectContext, Messages.CfBuild_Create_Base, buildMonitor, processResult))
			return;

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());

		String command = "%PLATFORM_1C_PATH% CREATEINFOBASE File=%BASE_1C_PATH% /Out %LOGFILE%";

		//runCommand(command, environmentVariables, proces);
		runCommand(command, environmentVariables, processResult);
	}
	
	public static void loadConfigFromXml(ProjectContext projectContext, TempDirs tempDirs, IProgressMonitor buildMonitor, ProcessResult processResult) {

		if (!checkBuildState(projectContext, Messages.CfBuild_Load_Config, buildMonitor, processResult))
//		if (!checkBuildState(Messages.CfBuild_Load_Config))
			return;

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
		environmentVariables.put("outXmlDir",			tempDirs.getXmlPath());
		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());

		//String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadConfigFromFiles %outXmlDir% /UpdateDBCfg /Out %LOGFILE%";
		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadConfigFromFiles %outXmlDir% /Out %LOGFILE%";

		//runCommand(command, environmentVariables, proces);
		runCommand(command, environmentVariables, processResult);

	}
	
	public static void loadConfigFromCf(ProjectContext projectContext, TempDirs tempDirs, IProgressMonitor buildMonitor, ProcessResult processResult) {

		if (!checkBuildState(projectContext, Messages.CfBuild_Load_Config, buildMonitor, processResult))
			return;

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
		environmentVariables.put("cfName",				projectContext.getCfFullName());
		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());

		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadCfg \"%cfName%\" /Out %LOGFILE%";

		//runCommand(command, environmentVariables, proces);
		runCommand(command, environmentVariables, processResult);

	}

	public static void dumpConfigToXml(ProjectContext projectContext, TempDirs tempDirs, IProgressMonitor buildMonitor, ProcessResult processResult) {

		if (!checkBuildState(projectContext, Messages.CfBuild_Dump_Config, buildMonitor, processResult))
			return;

		File buildDir = new File(projectContext.getCfLocation());
		if (!buildDir.exists()) {
			buildDir.mkdir();
		}

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
		environmentVariables.put("outXmlDir",			tempDirs.getXmlPath());
		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());

		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /DumpConfigToFiles %outXmlDir% /Out %LOGFILE%";

		//runCommand(command, environmentVariables, proces);
		runCommand(command, environmentVariables, processResult);

	}

	public static void dumpConfigToCf(ProjectContext projectContext, TempDirs tempDirs, IProgressMonitor buildMonitor, ProcessResult processResult) {

		if (!checkBuildState(projectContext, Messages.CfBuild_Dump_Config, buildMonitor, processResult))
//		if (!checkBuildState(Messages.CfBuild_Dump_Config))
			return;

		File buildDir = new File(projectContext.getCfLocation());
		if (!buildDir.exists()) {
			buildDir.mkdir();
		}

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
		environmentVariables.put("cfName",				projectContext.getCfFullName());
		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());

		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /DumpCfg \"%cfName%\" /Out %LOGFILE%";

		//runCommand(command, environmentVariables, proces);
		runCommand(command, environmentVariables, processResult);

	}

	public static void runCommand(String command, Map<String, String> environmentVariables, ProcessResult processResult) {
		
		IStatus status = Status.OK_STATUS;
		String processOutput = "";
		
		Process process;
		ProcessBuilder processBuilder = new ProcessBuilder();

		Map<String, String> env = processBuilder.environment();
		environmentVariables.forEach((k, v) -> env.put(k, v));

		processBuilder.command("cmd.exe", "/c", command);
		try {
			process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "windows-1251"));// cp866, UTF-8

			String line;
			while ((line = reader.readLine()) != null) {
				processOutput = processOutput.concat(System.lineSeparator()).concat(line);
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Abort);
				Activator.log(Activator.createErrorStatus(Messages.CfBuild_Abort));
			}

		} catch (IOException | InterruptedException e) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Unknown_Error.concat(processOutput), e));
		}
		
		//return new ProcessResult(status, processOutput);
		processResult.setResult(status, processOutput);

	}
	
	public static ProcessResult runCommand(List<String> commands, Map<String, String> environmentVariables) {
		
		IStatus status = Status.OK_STATUS;
		String processOutput = "";
		
		Process process;
		ProcessBuilder processBuilder = new ProcessBuilder();

		Map<String, String> env = processBuilder.environment();
		environmentVariables.forEach((k, v) -> env.put(k, v));

//		processBuilder.command("cmd.exe", "/c", commands);
		processBuilder.command(commands);
		try {
			process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "windows-1251"));// cp866, UTF-8

			String line;
			while ((line = reader.readLine()) != null) {
				processOutput = processOutput.concat(System.lineSeparator()).concat(line);
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Abort);
				Activator.log(Activator.createErrorStatus(Messages.CfBuild_Abort));
			}

		} catch (IOException | InterruptedException e) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Unknown_Error.concat(processOutput), e));
		}
		
		return new ProcessResult(status, processOutput);


	}
	
	public static String readOutLogFile(String fileName) {
		String contents = "";

		if (new File(fileName).exists()) {
			try {
				contents = new String(Files.readAllBytes(Paths.get(fileName)), Charset.forName("Windows-1251"));
			} catch (IOException e) {
				//status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			}
		}

		return contents;
	}

}
