package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.text.MessageFormat;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;
import com.google.inject.Inject;

import ru.yanygin.dt.cfbuilder.plugin.ui.ImportJob.SupportMode;

import com._1c.g5.v8.dt.import_.IImportOperationFactory;

public class CfImportHandler extends AbstractHandler {

	@Inject
	private IRuntimeVersionSupport runtimeVersionSupport;
	@Inject
	private IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager;
	@Inject
	private IImportOperationFactory importOperationFactory;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		Shell parentShell = windowInfo.getShell();
		IWorkbenchPage page = windowInfo.getActivePage();

		String projectName = "";
//		IProject project = SelectionContextProject.getContextProject(page);
//		if (project != null) {
//			projectName = project.getName();
//		}

		List<Version> supportedVersion = runtimeVersionSupport.getSupportedVersions();

		ImportDialog importDialog = new ImportDialog(windowInfo.getShell());
		importDialog.setInitialProperties(projectName, supportedVersion);

		int dialogResult = importDialog.open();
		if (dialogResult != 0) {
			Activator.log(Activator.createInfoStatus(Messages.Status_CancelFileCfSelestion));
			Messages.showPostBuildMessage(parentShell, Messages.Status_CancelFileCfSelestion);
			return null;
		}
		Version version = importDialog.getProjectSelectedVersion();
		CfFileInfo cfFileInfo = importDialog.getProjectSelectedCfNameInfo();
		projectName = importDialog.getNewProjectName();
		SupportMode supportMode = importDialog.getSupportMode();

		TempDirs tempDirs = new TempDirs();
		if (!tempDirs.createTempDirs()) {
			Activator.log(Activator.createErrorStatus(Messages.Status_ErrorCreateTempDirs));
			Messages.showPostBuildMessage(parentShell, Messages.Status_ErrorCreateTempDirs);
			return null;
		}

		String platformPath = Actions.findEnterpriseRuntimePathFromVersion(version, resolvableRuntimeInstallationManager);
		if (platformPath == null) {
			String errorText = MessageFormat.format(Messages.Status_ErrorFindPlatform, version.toString());
			Activator.log(Activator.createErrorStatus(errorText));
			Messages.showPostBuildMessage(parentShell, errorText);
			return null;
		}

		ProjectContext projectContext = new ProjectContext(projectName, platformPath, cfFileInfo, tempDirs, version, supportMode);

		ImportJob importJob = new ImportJob(projectContext, parentShell, importOperationFactory);
		importJob.schedule();

		return null;
	}

}
