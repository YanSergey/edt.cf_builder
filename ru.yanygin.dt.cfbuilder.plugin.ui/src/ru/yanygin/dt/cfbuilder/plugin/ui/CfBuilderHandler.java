package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.export.IExportServiceRegistry;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.ui.SelectionContextProject;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;
import com.google.inject.Inject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;

public class CfBuilderHandler extends AbstractHandler {

	@Inject
	private IRuntimeVersionSupport runtimeVersionSupport;
	@Inject
	private IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager;
	@Inject
	private IExportServiceRegistry exportServiceRegistry;
	@Inject
	private IConfigurationProvider configurationProvider;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}

		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		Shell parentShell = windowInfo.getShell();
		IWorkbenchPage page = windowInfo.getActivePage();

		Map<String, String> cfNameInfo = Actions.askCfLocationPath(parentShell, SWT.SAVE);
		if (cfNameInfo == null) {
			Activator.log(Activator.createInfoStatus(Messages.Status_CancelFileCfSelestion));
			Messages.showPostBuildMessage(parentShell, Messages.Status_CancelFileCfSelestion);
			return null;
		}

		IProject project = SelectionContextProject.getContextProject(page);
		if (project == null) {
			Activator.log(Activator.createErrorStatus(Messages.Status_ErrorGetProject));
			Messages.showPostBuildMessage(parentShell, Messages.Status_ErrorGetProject);
			return null;
		}

		TempDirs tempDirs = new TempDirs();
		if (!tempDirs.createTempDirs()) {
			Activator.log(Activator.createErrorStatus(Messages.Status_ErrorCreateTempDirs));
			Messages.showPostBuildMessage(parentShell, Messages.Status_ErrorCreateTempDirs);
			return null;
		}

		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(parentShell);

		Display.getDefault().asyncExec(() -> {
			try {

				progressDialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

						String projectName = project.getLocation().toFile().getName();
						Version version = runtimeVersionSupport.getRuntimeVersion(project);
						Configuration configuration = configurationProvider.getConfiguration(project);
						Path exportPath = Paths.get(tempDirs.getXmlPath());

						Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.Status_StartBuild, projectName)));

						IStatus status = Actions.exportProjectToXml(exportServiceRegistry, version, configuration,
								exportPath, SubMonitor.convert(monitor));

						if (!status.isOK()) {

							if (status.getSeverity() == IStatus.CANCEL) {
								String title = MessageFormat.format(Messages.Status_CfBuildCancel, projectName);
								Activator.log(Activator.createInfoStatus(title));
								Messages.showPostBuildMessage(parentShell, title);
							} else {
								String title = Messages.Status_UnknownError;
								Activator.log(Activator.createErrorStatus(title));
								Messages.showPostBuildMessage(parentShell, title, status.getMessage());
							}
							return;
						}

						String platformPath = Actions.findEnterpriseRuntimePathFromProject(project, runtimeVersionSupport, resolvableRuntimeInstallationManager);
						if (platformPath == null) {
							String errorText = MessageFormat.format(Messages.Status_ErrorFindPlatform, version.toString());
							Activator.log(Activator.createErrorStatus(errorText));
							Messages.showPostBuildMessage(parentShell, errorText);
							return;
						}

						ProjectContext projectContext = new ProjectContext(projectName, platformPath, cfNameInfo, tempDirs);

						BuildJob buildJob = new BuildJob(projectContext, parentShell);
						buildJob.schedule();

						Thread.sleep(2000);
						progressDialog.close();

					}

				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
				Messages.showPostBuildMessage(parentShell, Messages.Status_UnknownError, e.getLocalizedMessage());
			}
		});

		return null;

	}

}
