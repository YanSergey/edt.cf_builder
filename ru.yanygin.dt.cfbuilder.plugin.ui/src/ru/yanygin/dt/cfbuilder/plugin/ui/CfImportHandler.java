package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.import_.IImportService;
import com._1c.g5.v8.dt.import_.IImportServiceRegistry;
import com._1c.g5.v8.dt.import_.ImportException;
//import com._1c.g5.v8.dt.internal.core.V8ConfigurationNature;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.ui.SelectionContextProject;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;
import com.google.inject.Inject;
import com._1c.g5.v8.dt.core.ICoreConstants;
import com._1c.g5.v8.dt.core.platform.IConfigurationProjectManager;
import com._1c.g5.v8.dt.export.ExportException;
import com._1c.g5.v8.dt.export.IExportService;
import com._1c.g5.v8.dt.export.IExportStrategy;
import com._1c.g5.v8.dt.import_.IImportOperation;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;

public class CfImportHandler extends AbstractHandler {
	
	@Inject
	private IRuntimeVersionSupport runtimeVersionSupport;
	@Inject
	private IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager;
	@Inject
	private IImportServiceRegistry importServiceRegistry;
	@Inject
	private IImportOperationFactory importOperationFactory;
	@Inject
	private IConfigurationProjectManager configurationProjectManager;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPage page = windowInfo.getActivePage();

		IProject project = SelectionContextProject.getContextProject(page);
//		if (project == null) {
//			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Error_Get_Project));
//			return null;
//		}
		
		//ImportDialog importDialog = new ImportDialog(windowInfo.getShell());
		
		
		// Имя проекта, версию и путь к cf узнаем у пользователя
		String projectName;
		if (project == null) {
			projectName = "newImportProject";
		} else {
			projectName = project.getName();
		}
		
		List<Version> supportedVersion = runtimeVersionSupport.getSupportedVersions();
		
		HashMap<String, String> cfNameInfo = Actions.askCfLocationPath(windowInfo.getShell(), SWT.OPEN);
		if (cfNameInfo.get("cfFullName") == null) {
			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Set_CF_Error));
			return null;
		}
		/*importDialog.setInitialProperties(projectName, supportedVersion);
		
		int dialogResult = importDialog.open();
		Version version = importDialog.getProjectSelectedVersion();
		HashMap<String, String> cfNameInfo = importDialog.getProjectSelectedcfNameInfo();*/
		Version version = Version.V8_3_14;
		
		
		//
		

		TempDirs tempDirs = new TempDirs();
		if (!tempDirs.createTempDirs()) {
			Messages.showPostBuildMessage(windowInfo, Messages.CfBuild_Error_Create_Temp);
			return null;
		}
		
		String platformPath = Actions.findEnterpriseRuntimePathFromVersion(version, runtimeVersionSupport, resolvableRuntimeInstallationManager);
		if (platformPath == null) {
			//Activator.log(Activator.createErrorStatus(Messages.CfBuild_Error_Find_Platform.replace("%platformVersion%", version.toString())));
			Activator.log(Activator.createErrorStatus(MessageFormat.format(Messages.CfBuild_Error_Find_Platform, version.toString())));
			return null;
		}

		ProjectContext projectContext = new ProjectContext(projectName, platformPath, cfNameInfo);

		ImportJob importJob = new ImportJob(projectContext, windowInfo, tempDirs, importOperationFactory);
		importJob.schedule();
		
		/*IProjectDescription d = project.getDescription();
		String[] nId = d.getNatureIds();
		
		String f = ICoreConstants.V8_CONFIGURATION_NATURE;
		f = new com._1c.g5.v8.dt.internal.core.V8ConfigurationNature();*/
//		V8ConfigurationNature nat = new com._1c.g5.v8.dt.internal.core.V8ConfigurationNature();
		
		
//		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(windowInfo.getShell());
//		
//		Display.getDefault().asyncExec(() -> {
//			try {
//
//				progressDialog.run(true, true, new IRunnableWithProgress() {
//					@Override
//					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//						
//						//Path importPath = Paths.get("C:\\0 Test\\tempXML");
//						Path importPath = Paths.get("C:\\0 Test\\demo-xml");
//						String projectName = "newProjImp";
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
//						IImportOperation importOperation = importOperationFactory.createImportConfigurationOperation(projectName, version, importPath);
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

		
		return null;
	}

}
