package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com._1c.g5.ides.monitoring.IMonitoringEventDispatcher;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestrator;
import com._1c.g5.v8.dt.import_.IImportOperation;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;
import com._1c.g5.v8.dt.platform.services.core.infobases.sync.InfobaseSynchronizationException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ConfigurationFilesFormat;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ConfigurationFilesKind;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionArguments;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeVersionRequiredException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.impl.RuntimeExecutionCommandBuilder.ThickClientMode;
import com._1c.g5.v8.dt.platform.services.ui.infobases.sync.InfobaseUpdateDialogBasedCallback;
import com._1c.g5.v8.dt.platform.version.Version;

public class ImportProjectWorker extends BaseProjectWorker {

	public enum SupportChangeMode {
		DEFAULT, DISABLESUPPORT
	}

	private SupportChangeMode supportChangeMode;

	public ImportProjectWorker(Shell parentShell, ProjectInfo cfFileInfo, String projectName, Version projectVersion,
			SupportChangeMode supportChangeMode) {
		super(parentShell, cfFileInfo, projectName);

		this.projectV8version = projectVersion;
		this.supportChangeMode = supportChangeMode;

		this.projectType = getProjectTypeFromFileName(cfFileInfo.name);
		this.extName = getExtensionNameFromProject(getProjectReferenceFromWorkspace(projectName), this.projectType);

		JOB_TITLE = MessageFormat.format(Messages.Task_ImportProjectFromCf, projectName);
		JOB_IS_DONE_MESSAGE = MessageFormat.format(Messages.Info_ImportFromCfIsDone, projectName);
		JOB_IS_DONE_STATUS = MessageFormat.format(Messages.Status_EndImport, projectName);
		JOB_IS_ABORT_MESSAGE = MessageFormat.format(Messages.Status_OperationAbortByUser, projectName);

	}

    @Override
    public IStatus proceedJob(IProgressMonitor monitor) {

		Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.Status_StartImport, projectName)));

		createThickClientLauncher();

		IProject projectRef = getProjectReferenceFromWorkspace(projectName);

		if (projectInfo.useTempIB() || !projectRef.exists()) {
			if (projectInfo.useTempIB()) {
				createTempInfobase(monitor);
			}
			createTempXMLDir(monitor);
		} else {
			checkAssociateInfobaseToProject(monitor);

		}

		importConfigurationFromFile(projectInfo, monitor);

		if (projectInfo.useTempIB() || !projectRef.exists()) {

			exportConfigurationToFiles(monitor);

			ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(parentShell);
			Display.getDefault().asyncExec(() -> {
				try {
					progressDialog.run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InterruptedException {

							importXmlToProject(monitor);
							deleteTempInfobase(monitor);
							deleteTempDirs(monitor);

							if (Boolean.TRUE.equals(projectInfo.linkIBToProject())) {
								associateInfobaseToProject(projectInfo.getDeploymentInfobase(), projectRef, monitor);
							}
							showOperationResult();
						}
					});
				} catch (InvocationTargetException e) {
					jobStatus = Activator.createErrorStatus(e);
					Activator.log(jobStatus);
				} catch (InterruptedException e) { // NOSONAR
					jobStatus = new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.Status_OperationAbortByUser);
					Activator.log(Activator.createInfoStatus(Messages.Status_OperationAbortByUser));
				}

			});

		} else {
			pullInfobaseChanges(monitor);
			showOperationResult();
		}

		return jobStatus;

	}

	private void checkAssociateInfobaseToProject(IProgressMonitor monitor) {

		IProject projectRef = getProjectReferenceFromWorkspace(projectName);
		if (projectRef.exists() && Boolean.TRUE.equals(projectInfo.linkIBToProject())) {
			deployProjectToExistingInfobase(projectRef, projectInfo.getDeploymentInfobase(), true, true, monitor);
		}

	}

	private void pullInfobaseChanges(IProgressMonitor monitor) {

		if (!checkJobStatus(Messages.Actions_PullInfobaseChanges, monitor)) {
			return;
		}

        IMonitoringEventDispatcher monitoringEventDispatcher = getMonitoringEventDispatcher();
        IWorkspaceOrchestrator workspaceOrchestrator = getWorkspaceOrchestrator();
        IQualifiedNameFilePathConverter qualifiedNameFilePathConverter = getQualifiedNameFilePathConverter();

        InfobaseUpdateDialogBasedCallback updateCallback = new InfobaseUpdateDialogBasedCallback(parentShell,
            getV8projectManager(), getCompareEditorInputFactory(), getComparisonManager(), monitoringEventDispatcher,
            workspaceOrchestrator, qualifiedNameFilePathConverter);
		updateCallback.setAllowOverrideConflict(false);

		try {
			getInfobaseSynchronizationManager().pullInfobaseChanges(getProjectReferenceFromWorkspace(projectName),
                projectInfo.getDeploymentInfobase(), updateCallback, true, monitor);
		} catch (InfobaseSynchronizationException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
			return;
		}

		if (monitor.isCanceled()) {
			jobStatus = new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.Status_OperationAbortByUser);
			Activator.log(Activator.createInfoStatus(Messages.Status_OperationAbortByUser));
		}

	}

	private void importConfigurationFromFile(ProjectInfo sourceFile, IProgressMonitor monitor) {

		if (!checkJobStatus(Messages.Actions_LoadConfigFromCf, monitor)) {
			return;
		}

		try {
			switch (projectType) {
				case CONFIGURATION:
					importConfigurationFromCf(sourceFile);
					break;
				case EXTENSION:
					importExtensionFromCfe(sourceFile);
					break;
				default:
					break;
			}
		} catch (RuntimeExecutionException | RuntimeVersionRequiredException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}

	}

	private void importConfigurationFromCf(ProjectInfo sourceFile) throws RuntimeExecutionException {
		v8Launcher.second.importConfigurationFromCf(v8Launcher.first, projectInfo.getDeploymentInfobase(),
				buildArguments(projectInfo.getDeploymentInfobase()), sourceFile.getPath());
	}

	private void importExtensionFromCfe(ProjectInfo sourceFile)
			throws RuntimeExecutionException, RuntimeVersionRequiredException {

		V8ExtendedCommandBuilder command = new V8ExtendedCommandBuilder(v8Launcher.first.getFile(),
				ThickClientMode.DESIGNER);
		command.forInfobase(projectInfo.getDeploymentInfobase(), true).loadExtensionFromCfe(sourceFile.name, extName);

		V8ExtendedLauncher extendLauncher = new V8ExtendedLauncher(v8Launcher);
		extendLauncher.executeRuntimeProcessCommandEx(command, buildArguments(projectInfo.getDeploymentInfobase()),
				projectInfo.getDeploymentInfobase());

	}

	private void exportConfigurationToFiles(IProgressMonitor monitor) {

		if (!checkJobStatus(Messages.Actions_DumpConfigToXml, monitor)) {
			return;
		}

		try {
			switch (projectType) {
				case CONFIGURATION:
					exportConfigurationToXml();
					break;
				case EXTENSION:
					exportExtensionToXml();
					break;
				default:
					break;
			}
		} catch (RuntimeExecutionException | RuntimeVersionRequiredException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}

	}

	private void exportConfigurationToXml() throws RuntimeExecutionException {

		RuntimeExecutionArguments arguments = new RuntimeExecutionArguments();

		v8Launcher.second.exportConfigurationToXml(v8Launcher.first, projectInfo.getDeploymentInfobase(),
				ConfigurationFilesFormat.HIERARCHICAL, ConfigurationFilesKind.PLAIN_FILES, arguments, temptXMLPath);

		changeSupportMode(temptXMLPath);

	}

	private void exportExtensionToXml() throws RuntimeExecutionException, RuntimeVersionRequiredException {

		V8ExtendedCommandBuilder command = new V8ExtendedCommandBuilder(v8Launcher.first.getFile(),
				ThickClientMode.DESIGNER);
		command.forInfobase(projectInfo.getDeploymentInfobase(), true).dumpExtensionToXml(temptXMLPath, projectName);

		V8ExtendedLauncher extendLauncher = new V8ExtendedLauncher(v8Launcher);
		extendLauncher.executeRuntimeProcessCommandEx(command, buildArguments(projectInfo.getDeploymentInfobase()),
				projectInfo.getDeploymentInfobase());

	}

	public void importXmlToProject(IProgressMonitor monitor) {

		if (!checkJobStatus(Messages.Actions_ImportProjectFromXml, monitor)) {
			return;
		}

		IImportOperationFactory importOperationFactory = getImportOperationFactory();

		IImportOperation importOperation = null;
		switch (projectType) {
			case CONFIGURATION:
				importOperation = importOperationFactory.createImportConfigurationOperation(projectName,
						getProjectV8Version(), temptXMLPath);
				break;
			case EXTENSION:
				IProject project = null;
				importOperation = importOperationFactory.createImportExtensionOperation(projectName,
						getProjectV8Version(), temptXMLPath, project);
				break;
			default:
				return;
		}

		try {
			importOperation.run(monitor);
			Thread.sleep(2000);
		} catch (InvocationTargetException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		} catch (InterruptedException e) { // NOSONAR
			jobStatus = new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.Status_OperationAbortByUser);
			Activator.log(Activator.createInfoStatus(Messages.Status_OperationAbortByUser));
		}
	}

	private void changeSupportMode(Path xmlTempPath) {

		if (supportChangeMode == SupportChangeMode.DISABLESUPPORT) {
			String[] parentConfigurationsFiles = getParentConfigurationsFiles(xmlTempPath);
			for (String fileName : parentConfigurationsFiles) {
				File parentFile = new File(fileName);
				if (parentFile.exists()) {
					recursiveDelete(parentFile);
					Activator.log(Activator.createInfoStatus(
							MessageFormat.format(Messages.Info_FileDelete, parentFile.getAbsolutePath())));
				} else {
					Activator.log(Activator.createInfoStatus(Messages.Info_FileSupportNotFound));
				}
			}
		}
	}

}
