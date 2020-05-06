package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

public class BuildJob extends Job {

	private int numSubTask = 0;
	private IStatus status = Status.OK_STATUS;
	private TempDirs tempDirs = new TempDirs();
	private String processOutput;
	private ProjectContext projectContext;
	private SubMonitor buildMonitor;
	private SubMonitor progressBar;

	private IWorkbenchWindow windowInfo;

	public BuildJob(ProjectContext projectContext, IWorkbenchWindow windowInfo) {
		super(Messages.CfBuild_Build_Project_Name.replace("%projectName%", projectContext.getProjectName()));
		this.projectContext = projectContext;
		this.windowInfo = windowInfo;
	}

	@Override
	protected IStatus run(IProgressMonitor m) {

		String buildResult;
		String buildMessage;

		buildMonitor = SubMonitor.convert(m, 4);

		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(windowInfo.getShell());

		Display.getDefault().asyncExec(() -> {
			try {
				Thread.sleep(5000);
				if (status != Status.OK_STATUS) {
					return;
				}

				progressDialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

						progressBar = SubMonitor.convert(monitor);
						progressBar.beginTask(Messages.CfBuild_Run_Convertion, 60);

						while (numSubTask < 2 && status == Status.OK_STATUS) {
							if (progressBar.isCanceled()) {
								buildMonitor.setCanceled(true);
								progressBar.setTaskName(Messages.CfBuild_Cancel);
								status = Status.CANCEL_STATUS;
								return;
							}

							progressBar.worked(1);
							Thread.sleep(1000);
						}
						progressBar.done();

						if (numSubTask == 2 && status == Status.OK_STATUS) {
							progressBar.subTask(Messages.CfBuild_Convertion_Done);
							Thread.sleep(5000);
						}
						progressDialog.close();
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			}
		});

		Activator.log(Activator.createInfoStatus(Messages.CfBuild_Start_Build));

		if (tempDirs.createTempDirs()) {

			convertProjectToXml();
			createTempBase();
			loadConfig();
			dumpConfig();

			if (status == Status.OK_STATUS) {
				Activator.log(Activator.createInfoStatus(Messages.CfBuild_End_Build));

				buildResult = Messages.CfBuild_Done;
				buildMessage = Messages.CfBuild_File_CF_Save_Is
										.concat(System.lineSeparator())
										.concat(System.lineSeparator())
										.concat(projectContext.getCfFullName());

			} else if (status == Status.CANCEL_STATUS) {
				Activator.log(Activator.createInfoStatus(Messages.CfBuild_Cancel));

				buildResult = Messages.CfBuild_Cancel.replace("%projectName%", projectContext.getProjectName());
				buildMessage = buildResult;

			} else {
				String outLog = readOutLogFile(tempDirs.getLogFilePath());
				Activator.log(Activator.createErrorStatus(processOutput));
				Activator.log(Activator.createErrorStatus(outLog));

				buildResult = Messages.CfBuild_Abort;
				buildMessage = Messages.CfBuild_Abort;

				if (!processOutput.isEmpty()) {
					buildMessage = buildMessage
									.concat(System.lineSeparator())
									.concat(System.lineSeparator())
									.concat(processOutput);
				}
				if (!outLog.isEmpty()) {
					buildMessage = buildMessage
									.concat(System.lineSeparator())
									.concat(System.lineSeparator())
									.concat(outLog);
				}
			}

			buildMonitor.setTaskName(buildResult);

			showPostBuildMessage(buildResult, buildMessage);

			buildMonitor.beginTask(Messages.CfBuild_Clean_Temp, IProgressMonitor.UNKNOWN);
			tempDirs.deleteDirs();
			tempDirs = null;
			buildMonitor.done();
		} else {
			buildResult = Messages.CfBuild_Error_Create_Temp;
			buildMessage = Messages.CfBuild_Error_Create_Temp;

			showPostBuildMessage(buildResult, buildMessage);
		}

		return status;
	}

	private void showPostBuildMessage(String buildResult, String buildMessage) {
		Display.getDefault()
				.asyncExec(() -> MessageDialog.openInformation(windowInfo.getShell(), buildResult, buildMessage));
	}

	private boolean checkBuildState(String taskName) {
		if (buildMonitor.isCanceled()) {
			buildMonitor.setTaskName("Cancel");
			status = Status.CANCEL_STATUS;
			return false;
		}

		numSubTask++;
		buildMonitor.setTaskName(taskName);
		Activator.log(Activator.createInfoStatus(taskName));

		return (status == Status.OK_STATUS);
	}

	private void convertProjectToXml() {

		if (!checkBuildState(Messages.CfBuild_Run_Convertion))
			return;

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("workspaceDir",	tempDirs.getWorkspacePath());
		environmentVariables.put("outXmlDir",		tempDirs.getXmlPath());
		environmentVariables.put("projectDir",		projectContext.getProjectPath());

		String command = "ring -l warn edt@" + projectContext.getEdtVersion()
				+ " workspace export --project %projectDir% --configuration-files %outXmlDir% --workspace-location %workspaceDir%";
		runCommand(command, environmentVariables);

	}

	private void createTempBase() {

		if (!checkBuildState(Messages.CfBuild_Create_Base))
			return;

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());

		String command = "%PLATFORM_1C_PATH% CREATEINFOBASE File=%BASE_1C_PATH% /Out %LOGFILE%";

		runCommand(command, environmentVariables);

	}

	private void loadConfig() {

		if (!checkBuildState(Messages.CfBuild_Load_Config))
			return;

		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
		environmentVariables.put("outXmlDir",			tempDirs.getXmlPath());
		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());

		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadConfigFromFiles %outXmlDir% /UpdateDBCfg /Out %LOGFILE%";

		runCommand(command, environmentVariables);

	}

	private void dumpConfig() {

		if (!checkBuildState(Messages.CfBuild_Dump_Config))
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

		runCommand(command, environmentVariables);

	}

	private void runCommand(String command, Map<String, String> environmentVariables) {

		processOutput = "";
		Process process;
		ProcessBuilder processBuilder = new ProcessBuilder();

		Map<String, String> env = processBuilder.environment();
		environmentVariables.forEach((k, v) -> env.put(k, v));

		processBuilder.command("cmd.exe", "/c", command);
		try {
			process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "windows-1251"));// cp866
																														// UTF-8

			String line;
			while ((line = reader.readLine()) != null) {
				processOutput = processOutput.concat(System.lineSeparator()).concat(line);
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Abort);
				Activator.log(Activator.createErrorStatus(Messages.CfBuild_Abort));
			}
			buildMonitor.worked(1);

		} catch (IOException | InterruptedException e) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Unknown_Error.concat(processOutput), e));
		}

	}

	private String readOutLogFile(String fileName) {
		String contents = "";

		if (new File(fileName).exists()) {
			try {
				contents = new String(Files.readAllBytes(Paths.get(fileName)), Charset.forName("Windows-1251"));
			} catch (IOException e) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			}
		}

		return contents;
	}

}
