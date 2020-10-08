package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.export.IExportServiceRegistry;
import com._1c.g5.v8.dt.platform.services.ui.SelectionContextProject;
import com.google.inject.Inject;

import ru.yanygin.dt.cfbuilder.plugin.ui.JobDialog.DialogType;

public class ExportHandler extends AbstractHandler {
	
	@Inject
	private IExportServiceRegistry exportServiceRegistry;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}
		
		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		IProject selectProject = SelectionContextProject.getContextProject(windowInfo.getActivePage());
		if (selectProject == null) {
			Activator.log(Activator.createErrorStatus(Messages.Status_ErrorGetProject));
			return null;
		}
		
		JobDialog exportDialog;
		try {
			exportDialog = new JobDialog(windowInfo.getShell(), DialogType.EXPORT, selectProject,
					exportServiceRegistry);
		} catch (Exception e) {
			Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			return null;
		}
		
		int dialogResult = exportDialog.open();
		if (dialogResult != 0) {
			return null;
		}
		
		return null;
		
	}
	
}
