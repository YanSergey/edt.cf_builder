package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.import_.IImportService;
import com._1c.g5.v8.dt.import_.IImportServiceRegistry;
import com._1c.g5.v8.dt.import_.ImportException;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.version.Version;
import com.google.inject.Inject;
import com._1c.g5.v8.dt.core.platform.IConfigurationProjectManager;
import com._1c.g5.v8.dt.export.ExportException;
import com._1c.g5.v8.dt.export.IExportService;
import com._1c.g5.v8.dt.export.IExportStrategy;
import com._1c.g5.v8.dt.import_.IImportOperation;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;

public class CfImportHandler extends AbstractHandler {
	
	@Inject
	private IImportServiceRegistry importServiceRegistry;
	@Inject
	private IImportOperationFactory importOperationFactory;
	@Inject
	private IConfigurationProjectManager configurationProjectManager;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Version version = Version.V8_3_14;

		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(windowInfo.getShell());

		Display.getDefault().asyncExec(() -> {
			try {

				progressDialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						
						//Path importPath = Paths.get("C:\\0 Test\\tempXML");
						Path importPath = Paths.get("C:\\0 Test\\demo-xml");
						String projectName = "newProjImp";

						/*
						// раскомменировать, если будет крашиться при пересоздании проекта
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

						boolean exist = project.exists();
						SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
						if (exist) {

							try {
								project.delete(true, true, subMonitor.newChild(4));
							} catch (CoreException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						*/

						IImportOperation importOperation = importOperationFactory.createImportConfigurationOperation(projectName, version, importPath);
						importOperation.run(monitor);
						

						/*
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
						IProject newProjImp = null;

						try {
							boolean exist = project.exists();
							SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
							if (exist) {

								project.delete(true, true, subMonitor.newChild(4));
							}
							subMonitor.setWorkRemaining(4);
							if (subMonitor.isCanceled()) {

								throw new InterruptedException();
							}
							configurationProjectManager.create(projectName, version, (Configuration) null,
									subMonitor.newChild(1));
							if (monitor.isCanceled()) {

								throw new InterruptedException();

							}
						} catch (CoreException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// bootstrap.registerImport(project);

						IImportService importService = null;
						try {
							importService = importServiceRegistry.getImportService(version);
						} catch (ImportException | CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						IStatus status = importService.work(project, importPath, monitor);
						*/
						
						
						/*
						try {
							newProjImp = configurationProjectManager.create(projectName, version, (Configuration) null, monitor);
						} catch (CoreException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						IImportService importService = null;
						
						try {
							importService = importServiceRegistry.getImportService(version);
						} catch (ImportException | CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						IStatus status = importService.work(newProjImp, importPath, monitor);
						 */


						
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

}
