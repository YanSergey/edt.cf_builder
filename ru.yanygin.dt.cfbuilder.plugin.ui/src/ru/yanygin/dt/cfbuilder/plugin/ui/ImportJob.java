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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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

import com._1c.g5.v8.dt.export.ExportException;
import com._1c.g5.v8.dt.export.IExportService;
import com._1c.g5.v8.dt.export.IExportServiceRegistry;
import com._1c.g5.v8.dt.import_.IImportOperation;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.version.Version;

public class ImportJob extends Job {

	private ProcessResult processResult = new ProcessResult(Status.OK_STATUS);
//	private IStatus status = Status.OK_STATUS;
//	private String processOutput;
	
	private TempDirs tempDirs;
	private ProjectContext projectContext;
	private IProgressMonitor buildMonitor;
	private IWorkbenchWindow windowInfo;
	private IImportOperationFactory importOperationFactory;

	public ImportJob(ProjectContext projectContext, IWorkbenchWindow windowInfo, TempDirs tempDirs, IImportOperationFactory importOperationFactory) {
		//super(Messages.CfBuild_Build_Project_Name.replace("%projectName%", projectContext.getProjectName()));
		super(MessageFormat.format(Messages.CfBuild_Build_Project_Name, projectContext.getProjectName()));
		
		this.projectContext = projectContext;
		this.windowInfo = windowInfo;
		this.tempDirs = tempDirs;
		this.importOperationFactory = importOperationFactory;
	}

	@Override
	protected IStatus run(IProgressMonitor progressMonitor) {

		String buildResult;
		String buildMessage;
		
		this.buildMonitor = progressMonitor;

		Actions.createTempBase(projectContext, tempDirs, progressMonitor, processResult);
		Actions.loadConfigFromCf(projectContext, tempDirs, progressMonitor, processResult);
		Actions.dumpConfigToXml(projectContext, tempDirs, progressMonitor, processResult);
		importXmlToProject();
//		loadConfig();
//		dumpConfig();

		if (processResult.statusIsOK()) {
			buildResult = Messages.CfBuild_Done;
			buildMessage = Messages.CfBuild_File_CF_Save_Is
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(projectContext.getCfFullName());

			//Activator.log(Activator.createInfoStatus(Messages.CfBuild_End_Build.replace("%projectName%", projectContext.getProjectName())));
			Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.CfBuild_End_Build, projectContext.getProjectName())));
			
		} else if (processResult.statusIsCancel()) {
			//buildResult = Messages.CfBuild_Cancel.replace("%projectName%", projectContext.getProjectName());
			buildResult = MessageFormat.format(Messages.CfBuild_Cancel, projectContext.getProjectName());
			buildMessage = buildResult;
			
			Activator.log(Activator.createInfoStatus(buildResult));
			
		} else {
			buildResult = Messages.CfBuild_Abort;
			buildMessage = Messages.CfBuild_Abort;
			
			Activator.log(Activator.createErrorStatus(buildResult));

			if (!processResult.getOutput().isEmpty()) {
				Activator.log(Activator.createErrorStatus(processResult.getOutput()));
				buildMessage = buildMessage
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(processResult.getOutput());
			}

			String outLog = Actions.readOutLogFile(tempDirs.getLogFilePath());
			if (!outLog.isEmpty()) {
				Activator.log(Activator.createErrorStatus(outLog));
				buildMessage = buildMessage
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(outLog);
			}
		}

		buildMonitor.setTaskName(buildResult);

		Messages.showPostBuildMessage(windowInfo, buildResult, buildMessage);

		tempDirs.deleteDirs(buildMonitor);

		return processResult.getStatus();
	}

//	private boolean checkBuildState(String taskName) {
//		if (processResult.statusIsOK() & buildMonitor.isCanceled()) {
//			processResult.setResult(Status.CANCEL_STATUS);
//			//String infoMessage = Messages.CfBuild_Cancel.replace("%projectName%", projectContext.getProjectName());
//			String infoMessage = MessageFormat.format(Messages.CfBuild_Cancel, projectContext.getProjectName());
//			buildMonitor.setTaskName(infoMessage);
//			//Activator.log(Activator.createInfoStatus(infoMessage));
//			return false;
//		}
//
//		if (processResult.statusIsOK()) {
//			buildMonitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
//			Activator.log(Activator.createInfoStatus(taskName));
//			return true;
//		}
//
//		return false;
//	}
//
//	private void createTempBase() {
//
//		if (!Actions.checkBuildState(projectContext, Messages.CfBuild_Create_Base, buildMonitor, processResult))
//			return;
//
//		Map<String, String> environmentVariables = new HashMap<>();
//		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
//		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
//		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());
//		
//		environmentVariables.put("cfName",				projectContext.getCfFullName());
//
//		String command = "%PLATFORM_1C_PATH% CREATEINFOBASE File=%BASE_1C_PATH% /Out %LOGFILE%";
//
//		//runCommand(command, environmentVariables, proces);
//		processResult = Actions.runCommand(command, environmentVariables);
//	}
//
//	private void loadConfigFromCf() {
//
//		if (!checkBuildState(Messages.CfBuild_Load_Config))
//			return;
//
//		Map<String, String> environmentVariables = new HashMap<>();
//		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
//		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
//		environmentVariables.put("cfName",				projectContext.getCfFullName());
//		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());
//
//		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadCfg \"%cfName%\" /Out %LOGFILE%";
//
//		//runCommand(command, environmentVariables, proces);
//		processResult = Actions.runCommand(command, environmentVariables);
//
//	}
//
//	private void dumpConfigInXml() {
//
//		if (!checkBuildState(Messages.CfBuild_Dump_Config))
//			return;
//
//		File buildDir = new File(projectContext.getCfLocation());
//		if (!buildDir.exists()) {
//			buildDir.mkdir();
//		}
//
//		Map<String, String> environmentVariables = new HashMap<>();
//		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
//		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
//		environmentVariables.put("outXmlDir",			tempDirs.getXmlPath());
//		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());
//
//		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /DumpConfigToFiles %outXmlDir% /Out %LOGFILE%";
//
//		//runCommand(command, environmentVariables, proces);
//		processResult = Actions.runCommand(command, environmentVariables);
//
//	}

	private void importXmlToProject() {

		//if (!checkBuildState(Messages.CfBuild_Import_Project))
		if (!Actions.checkBuildState(projectContext, Messages.CfBuild_Create_Base, buildMonitor, processResult))
			return;

//		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(windowInfo.getShell());
		
		Version version = Version.V8_3_14;
		Path importPath = Paths.get(tempDirs.getXmlPath());
		
		
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectContext.getProjectName());

		boolean exist = project.exists();
		SubMonitor subMonitor = SubMonitor.convert(buildMonitor, 100);
		if (exist) {

			try {
				project.delete(true, true, subMonitor.newChild(4));
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		
		
		IImportOperation importOperation = importOperationFactory.createImportConfigurationOperation(projectContext.getProjectName(), version, importPath);
		try {
			importOperation.run(buildMonitor);
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		
//		Display.getDefault().asyncExec(() -> {
//			try {
//
//				progressDialog.run(true, true, new IRunnableWithProgress() {
//					@Override
//					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//						
//						//Path importPath = Paths.get("C:\\0 Test\\tempXML");
////						Path importPath = Paths.get("C:\\0 Test\\demo-xml");
////						String projectName = "newProjImp";
//
//						/*
//						// раскомменировать, если будет крашиться при пересоздании проекта
//						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
//
//						boolean exist = project.exists();
//						SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
//						if (exist) {
//
//							try {
//								project.delete(true, true, subMonitor.newChild(4));
//							} catch (CoreException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//						*/
//						
//						Version version = Version.V8_3_14;
//						Path importPath = Paths.get(tempDirs.getXmlPath());
//						
//						IImportOperation importOperation = importOperationFactory.createImportConfigurationOperation(projectContext.getProjectName(), version, importPath);
//						importOperation.run(monitor);
//						
//
//						/*
//						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
//						IProject newProjImp = null;
//
//						try {
//							boolean exist = project.exists();
//							SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
//							if (exist) {
//
//								project.delete(true, true, subMonitor.newChild(4));
//							}
//							subMonitor.setWorkRemaining(4);
//							if (subMonitor.isCanceled()) {
//
//								throw new InterruptedException();
//							}
//							configurationProjectManager.create(projectName, version, (Configuration) null,
//									subMonitor.newChild(1));
//							if (monitor.isCanceled()) {
//
//								throw new InterruptedException();
//
//							}
//						} catch (CoreException e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						}
//						// bootstrap.registerImport(project);
//
//						IImportService importService = null;
//						try {
//							importService = importServiceRegistry.getImportService(version);
//						} catch (ImportException | CoreException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						IStatus status = importService.work(project, importPath, monitor);
//						*/
//						
//						
//						/*
//						try {
//							newProjImp = configurationProjectManager.create(projectName, version, (Configuration) null, monitor);
//						} catch (CoreException e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						}
//						IImportService importService = null;
//						
//						try {
//							importService = importServiceRegistry.getImportService(version);
//						} catch (ImportException | CoreException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//
//						IStatus status = importService.work(newProjImp, importPath, monitor);
//						 */
//
//
//						
//						Thread.sleep(2000);
//						progressDialog.close();
//
//					}
//
//				});
//			} catch (InvocationTargetException | InterruptedException e) {
//				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
//				e.printStackTrace();
//			}
//		});
//		
		
	}
	
	/*
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
	*/

}
