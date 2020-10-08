package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.nio.file.Path;
import java.text.MessageFormat;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com._1c.g5.v8.dt.export.ExportException;
import com._1c.g5.v8.dt.export.IExportService;
import com._1c.g5.v8.dt.export.IExportServiceRegistry;
import com._1c.g5.v8.dt.export.IExportStrategy;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ConfigurationFilesFormat;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionArguments;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeVersionRequiredException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.impl.RuntimeExecutionCommandBuilder.ThickClientMode;
import com._1c.g5.v8.dt.platform.version.Version;

public class ExportProjectWorker extends BaseProjectWorker {
	
	private IProject project;
	
	public IProject getProject() {
		return project;
	}
	
	private IExportServiceRegistry exportServiceRegistry;
	private boolean fullLoad;
	
	public ExportProjectWorker(Shell parentShell, ProjectInfo cfFileInfo, IProject projectRef,
			boolean loadFullConfiguration, IExportServiceRegistry exportServiceRegistry) {
		
		super(parentShell, cfFileInfo, projectRef.getName());
		
		this.project = projectRef;
		this.exportServiceRegistry = exportServiceRegistry;
		this.projectV8version = getV8VersionFromProject(this.project);
		this.fullLoad = loadFullConfiguration;
		
		this.projectType = getProjectTypeFromProject(this.project); // Перенести в setExtensionName, избавиться от
																	// переменной projectType
		this.extName = getExtensionNameFromProject(this.project, this.projectType);
		
		JOB_TITLE = MessageFormat.format(Messages.Task_BuildCfFromProject, projectName);
		JOB_IS_DONE_MESSAGE = MessageFormat.format(Messages.Info_CfBuildIsDone, cfFileInfo.name);
		JOB_IS_DONE_STATUS = MessageFormat.format(Messages.Status_EndExport, projectName);
		JOB_IS_ABORT_MESSAGE = MessageFormat.format(Messages.Status_OperationAbortByUser, projectName);
		
	}
	
	public void exportProjectToXml(IProgressMonitor monitor) {
		
		if (!checkJobStatus(MessageFormat.format(Messages.Status_StartExport, projectName), monitor)) {
			return;
		}
		
		createTempXMLDir(monitor);
		
		if (!checkJobStatus(Messages.Actions_DumpConfigToXml, monitor)) {
			return;
		}
		
		Version version = getRuntimeVersionSupport().getRuntimeVersion(this.getProject());
		
		IExportService exportService = null;
		try {
			exportService = exportServiceRegistry.getExportService(version);
		} catch (ExportException e) {
			jobStatus = Activator.createErrorStatus(e.getLocalizedMessage(), e);
			Activator.log(jobStatus);
			return;
		}
		
		Configuration configuration = getConfigurationProvider().getConfiguration(this.getProject());
		
		IStatus exportResult = exportService.work(configuration, temptXMLPath,
				IExportStrategy.DEFAULT.exportExternalProperties(configuration),
				IExportStrategy.DEFAULT.exportExternalProperties(configuration), monitor);
		
		if (!exportResult.isOK()) {
			
			jobStatus = new Status(exportResult.getSeverity(), Activator.PLUGIN_ID, exportResult.getMessage());
			
			if (exportResult.getSeverity() == IStatus.CANCEL) {
				Activator.log(Activator.createInfoStatus(exportResult.getMessage()));
			} else {
				Activator.log(Activator.createErrorStatus(exportResult.getMessage()));
			}
		}
		
	}
	
	public IStatus proceedJob(IProgressMonitor monitor) {
		
		if (!projectInfo.useTempIB()
				&& !checkJobStatus(MessageFormat.format(Messages.Status_StartExport, projectName), monitor)) {
			showOperationResult();
			return jobStatus;
		}
		
		createThickClientLauncher();
		
		if (projectInfo.useTempIB()) {
			createTempInfobase(monitor);
		}
		
		deployProjectToInfobase(monitor);
		
		exportConfigurationToCf(projectInfo, monitor);
		
		if (projectInfo.useTempIB()) {
			deleteTempInfobase(monitor);
			deleteTempDirs(monitor);
		}
		
		showOperationResult();
		
		return jobStatus;
	}
	
	private void deployProjectToInfobase(IProgressMonitor monitor) {
		
		if (projectInfo.useTempIB()) {
			
			if (!checkJobStatus(Messages.Actions_LoadConfigFromXml, monitor)) {
				return;
			}
			
			try {
				
				RuntimeExecutionArguments arguments = new RuntimeExecutionArguments();
				
				switch (projectInfo.getFileType()) {
					case SIMPLE:
						v8Launcher.second.importConfigurationFromXml(v8Launcher.first,
								projectInfo.getDeploymentInfobase(), ConfigurationFilesFormat.HIERARCHICAL, arguments,
								temptXMLPath);
						break;
					
					case DISTRIBUTION:
						v8Launcher.second.importConfigurationFromXml(v8Launcher.first,
								projectInfo.getDeploymentInfobase(), ConfigurationFilesFormat.HIERARCHICAL, arguments,
								temptXMLPath);
						
						if (!checkJobStatus(Messages.Actions_UpdateInfobase, monitor)) {
							return;
						}
						v8Launcher.second.updateInfobase(v8Launcher.first, projectInfo.getDeploymentInfobase(), null,
								arguments);
						break;
					
					case EXTENSION:
						loadExtensionFromXml(temptXMLPath, extName);
						break;
					
					default:
						break;
				}
				
			} catch (RuntimeExecutionException | RuntimeVersionRequiredException e) {
				jobStatus = Activator.createErrorStatus(e);
			}
			
		} else {
			deployProjectToExistingInfobase(project, projectInfo.getDeploymentInfobase(), fullLoad,
					projectInfo.linkIBToProject(), monitor);
		}
	}
	
	private void exportConfigurationToCf(ProjectInfo destinationFile, IProgressMonitor monitor) {
		
		if (!checkJobStatus(Messages.Actions_DumpConfigToCf, monitor)) {
			return;
		}
		
		if (!destinationFile.checkAndCreateParentDir()) {
			jobStatus = Activator.createErrorStatus(
					MessageFormat.format(Messages.Status_ErrorCreateParentDir, projectInfo.getParentDirPath()));
			Activator.log(jobStatus);
			return;
		}
		
		try {
			switch (destinationFile.getFileType()) {
				case SIMPLE:
					exportConfigurationToCfSimple(destinationFile);
					break;
				case DISTRIBUTION:
					exportConfigurationToCfDistribution(destinationFile);
					break;
				case EXTENSION:
					exportExtensionToCfe(destinationFile, extName);
					break;
				default:
					break;
			}
		} catch (RuntimeExecutionException | RuntimeVersionRequiredException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}
		
	}
	
	private void exportConfigurationToCfSimple(ProjectInfo destinationFile) throws RuntimeExecutionException {
		
		v8Launcher.second.exportConfigurationToCf(v8Launcher.first, projectInfo.getDeploymentInfobase(),
				buildArguments(projectInfo.getDeploymentInfobase()), destinationFile.getPath());
		
	}
	
	private void exportConfigurationToCfDistribution(ProjectInfo destinationFile)
			throws RuntimeExecutionException, RuntimeVersionRequiredException {
		
		V8ExtendedCommandBuilder command = new V8ExtendedCommandBuilder(v8Launcher.first.getLaunchable(),
				ThickClientMode.DESIGNER);
		command.forInfobase(projectInfo.getDeploymentInfobase(), true).dumpConfigurationToCfDistr(destinationFile.name);
		
		V8ExtendedLauncher extendLauncher = new V8ExtendedLauncher(v8Launcher);
		extendLauncher.executeRuntimeProcessCommandEx(command, buildArguments(projectInfo.getDeploymentInfobase()),
				projectInfo.getDeploymentInfobase());
		
	}
	
	private void exportExtensionToCfe(ProjectInfo destinationFile, String extensionName)
			throws RuntimeExecutionException, RuntimeVersionRequiredException {
		
		V8ExtendedCommandBuilder command = new V8ExtendedCommandBuilder(v8Launcher.first.getLaunchable(),
				ThickClientMode.DESIGNER);
		command.forInfobase(projectInfo.getDeploymentInfobase(), true).dumpExtensionToCfe(destinationFile.name,
				extensionName);
		
		V8ExtendedLauncher extendLauncher = new V8ExtendedLauncher(v8Launcher);
		extendLauncher.executeRuntimeProcessCommandEx(command, buildArguments(projectInfo.getDeploymentInfobase()),
				projectInfo.getDeploymentInfobase());
		
	}
	
	private void loadExtensionFromXml(Path sourceFolder, String extensionName)
			throws RuntimeExecutionException, RuntimeVersionRequiredException {
		
		V8ExtendedCommandBuilder command = new V8ExtendedCommandBuilder(v8Launcher.first.getLaunchable(),
				ThickClientMode.DESIGNER);
		command.forInfobase(projectInfo.getDeploymentInfobase(), true).loadExtensionFromXml(sourceFolder,
				extensionName);
		
		V8ExtendedLauncher extendLauncher = new V8ExtendedLauncher(v8Launcher);
		extendLauncher.executeRuntimeProcessCommandEx(command, buildArguments(projectInfo.getDeploymentInfobase()),
				projectInfo.getDeploymentInfobase());
		
	}
	
}
