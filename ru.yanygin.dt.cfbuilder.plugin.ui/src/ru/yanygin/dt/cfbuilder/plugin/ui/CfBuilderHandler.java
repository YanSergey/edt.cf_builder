package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.MatchingRuntimeNotFound;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.ui.SelectionContextProject;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;
import com.google.inject.Inject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

public class CfBuilderHandler extends AbstractHandler {

	@Inject
	private IRuntimeVersionSupport runtimeVersionSupport;
    @Inject
    private IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
        if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
            return null;
        }
        
		IWorkbenchWindow windowInfo = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        IWorkbenchPage page = windowInfo.getActivePage();

        IProject project = SelectionContextProject.getContextProject(page);
		
        if (project == null) {
    		Activator.log(Activator.createErrorStatus(Messages.CfBuild_Error_Get_Project));
			return null;
		}
		
        String projectName = project.getLocation().toFile().getName();
        String projectPath = project.getLocation().toFile().getAbsolutePath();	
        
        String platformPath = setRuntimeDesignerPath(project);
        if (platformPath == null) {
    		Activator.log(Activator.createErrorStatus(Messages.CfBuild_Error_Find_Platform));
			return null;
        }
        
        String edtVersion = identifyEdtVersion();
        
        HashMap<String, String> cfName = askCfLocationPath(windowInfo);
		
		if (cfName.get("cfFullName") == null) {
    		Activator.log(Activator.createErrorStatus(Messages.CfBuild_Set_CF_Error));
			return null;
		}

		
		ProjectContext projectContext = new ProjectContext(projectName,
															projectPath,
															platformPath,
															edtVersion,
															cfName);
		
		BuildJob buildJob = new BuildJob(projectContext, windowInfo);
		buildJob.schedule();
		
		return null;
	
	}
	
	private String setRuntimeDesignerPath(IProject project) {
		Version ver = runtimeVersionSupport.getRuntimeVersion(project);
		
        IResolvableRuntimeInstallation resolvableRuntimeInstallation = resolvableRuntimeInstallationManager.getDefault(
                "com._1c.g5.v8.dt.platform.services.core.runtimeType.EnterprisePlatform", ver.toString());
        
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
	
}
