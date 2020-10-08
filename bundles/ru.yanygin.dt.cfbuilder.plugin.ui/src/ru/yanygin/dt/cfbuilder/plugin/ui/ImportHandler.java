package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.platform.services.ui.SelectionContextProject;
import ru.yanygin.dt.cfbuilder.plugin.ui.JobDialog.DialogType;

public class ImportHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		String projectName = "";
		IProject project = SelectionContextProject.getContextProject(windowInfo.getActivePage());
		if (project != null) {
			projectName = project.getName();
		}
		
		JobDialog importDialog;
		try {
			importDialog = new JobDialog(windowInfo.getShell(), DialogType.IMPORT, projectName);
		} catch (Exception e) {
			Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			return null;
		}
		
		int dialogResult = importDialog.open();
		if (dialogResult != 0) {
			return null;
		}
		
		return null;
	}
	
}
