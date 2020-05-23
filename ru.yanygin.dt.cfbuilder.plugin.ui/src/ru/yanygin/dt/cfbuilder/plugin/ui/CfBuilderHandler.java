package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.export.ExportException;
import com._1c.g5.v8.dt.export.IExportOperation;
import com._1c.g5.v8.dt.export.IExportService;
import com._1c.g5.v8.dt.export.IExportStrategy;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.MatchingRuntimeNotFound;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.ui.SelectionContextProject;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;
import com.google.inject.Inject;
import com.sun.source.tree.ExportsTree;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;

public class CfBuilderHandler extends AbstractHandler {

	@Inject
	private IRuntimeVersionSupport runtimeVersionSupport;
	@Inject
	private IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager;
	@Inject
	private com._1c.g5.v8.dt.export.IExportServiceRegistry exportServiceRegistry;
	@Inject
	private com._1c.g5.v8.dt.export.IExportOperationFactory exportOperationFactory;
	@Inject
	private IConfigurationProvider configurationProvider;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}
		
		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPage page = windowInfo.getActivePage();
		
		HashMap<String, String> cfNameInfo = Actions.askCfLocationPath(windowInfo.getShell(), SWT.SAVE);
		if (cfNameInfo.get("cfFullName") == null) {
			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Set_CF_Error));
			return null;
		}

		IProject project = SelectionContextProject.getContextProject(page);
		if (project == null) {
			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Error_Get_Project));
			return null;
		}
		
		Version version = runtimeVersionSupport.getRuntimeVersion(project);
		Configuration configuration = configurationProvider.getConfiguration(project);
		
		TempDirs tempDirs = new TempDirs();
		if (!tempDirs.createTempDirs()) {
			Messages.showPostBuildMessage(windowInfo, Messages.CfBuild_Error_Create_Temp);
			return null;
		}

		
		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(windowInfo.getShell());

		Display.getDefault().asyncExec(() -> {
			try {

				progressDialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						
						String projectName = project.getLocation().toFile().getName();
						String projectPath = project.getLocation().toFile().getAbsolutePath();
						
						Path exportPath = Paths.get(tempDirs.getXmlPath());


						SubMonitor progressBar = SubMonitor.convert(monitor);
						progressBar.subTask(Messages.CfBuild_Run_Convertion);
						
						//Activator.log(Activator.createInfoStatus(Messages.CfBuild_Start_Build.replace("%projectName%", projectName)));
						Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.CfBuild_Start_Build, projectName)));

						IExportService exportService = null;
						IStatus status;
						try {
							exportService = exportServiceRegistry.getExportService(version);
						} catch (ExportException ex) {
							status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
							Activator.log(Activator.createErrorStatus(ex.getLocalizedMessage(), ex));
							ex.printStackTrace();
						}
						boolean exportSubordinatesObjects = IExportStrategy.DEFAULT.exportExternalProperties(configuration);
						boolean exportExternalProperties = IExportStrategy.DEFAULT.exportExternalProperties(configuration);
						status = exportService.work(configuration, exportPath, exportSubordinatesObjects, exportExternalProperties, progressBar);
						
//						IExportOperation op = exportOperationFactory.createExportOperation(exportPath, configuration);
//						IExportOperation op = exportOperationFactory.createExportOperation(exportPath, version, configuration);
//						IExportOperation op = exportOperationFactory.createExportOperation(exportPath, version, IExportStrategy.DEFAULT, configuration);
//						op.run(progressBar);

						
						
						if (!status.isOK()) {
							Activator.log(Activator.createErrorStatus(Messages.CfBuild_Error));
							Messages.showPostBuildMessage(windowInfo, Messages.CfBuild_Error, status.getMessage());
							return;
						}

						//String platformPath = getRuntimeDesignerPath(project);
						String platformPath = Actions.findEnterpriseRuntimePathFromProject(project, runtimeVersionSupport, resolvableRuntimeInstallationManager);
						if (platformPath == null) {
							//Activator.log(Activator.createErrorStatus(Messages.CfBuild_Error_Find_Platform.replace("%platformVersion%", version.toString())));
							Activator.log(Activator.createErrorStatus(MessageFormat.format(Messages.CfBuild_Error_Find_Platform, version.toString())));
							return;
						}

						//String edtVersion = identifyEdtVersion();

//						ProjectContext projectContext = new ProjectContext(projectName, projectPath, platformPath, cfNameInfo);
						ProjectContext projectContext = new ProjectContext(projectName, platformPath, cfNameInfo);

						BuildJob buildJob = new BuildJob(projectContext, windowInfo, tempDirs);
						buildJob.schedule();

						Thread.sleep(2000);
						progressDialog.close();

					}

				});
			} catch (InvocationTargetException | InterruptedException e) {
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
				e.printStackTrace();
			}
		});


		return null;

	}

	/*
	private String getRuntimeDesignerPath(IProject project) {
		Version version = runtimeVersionSupport.getRuntimeVersion(project);
		
		IResolvableRuntimeInstallation resolvableRuntimeInstallation = resolvableRuntimeInstallationManager
				.getDefault("com._1c.g5.v8.dt.platform.services.core.runtimeType.EnterprisePlatform", version.toString());

		RuntimeInstallation currentRuntime;
		try {
			currentRuntime = resolvableRuntimeInstallation.get();
		} catch (MatchingRuntimeNotFound e) {
			e.printStackTrace();
			return null;
		}
		return "\"".concat(currentRuntime.getInstallLocation().toString()).concat("\\1cv8.exe\"");
	}

	private HashMap<String, String> askCfLocationPath(IWorkbenchWindow windowInfo) {
		FileDialog saveDialog = new FileDialog(windowInfo.getShell(), SWT.SAVE);
		saveDialog.setText(Messages.CfBuild_Set_CF_Location);

		String[] filterExt = { "*.cf" };
		String[] filterNames = { Messages.CfBuild_1C_Files };
		saveDialog.setFilterExtensions(filterExt);
		saveDialog.setFilterNames(filterNames);

		HashMap<String, String> cfName = new HashMap<>();

		cfName.put("cfFullName", saveDialog.open());
		cfName.put("cfLocation", saveDialog.getFilterPath());

		return cfName;
	}

	private String identifyEdtVersion() {
		org.osgi.framework.Version platformVersion = Platform.getProduct().getDefiningBundle().getVersion();

		if (platformVersion.getMinor() == 17)
			return "2020.1.0";
		if (platformVersion.getMinor() == 18)
			return "2020.2.0";
		if (platformVersion.getMinor() == 19)
			return "2020.3.0";

		return String.valueOf(platformVersion.getMajor())
				.concat(".")
				.concat(String.valueOf(platformVersion.getMinor()))
				.concat(".")
				.concat(String.valueOf(platformVersion.getMicro()));
	}
	*/
	
}
