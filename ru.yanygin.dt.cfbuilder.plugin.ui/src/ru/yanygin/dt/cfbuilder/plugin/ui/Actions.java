package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;

import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.MatchingRuntimeNotFound;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;

public class Actions {

	public static String findRuntimeDesignerPath(IProject project, IRuntimeVersionSupport runtimeVersionSupport,
			IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager) {
		
		Version version = runtimeVersionSupport.getRuntimeVersion(project);

		IResolvableRuntimeInstallation resolvableRuntimeInstallation = resolvableRuntimeInstallationManager.getDefault(
				"com._1c.g5.v8.dt.platform.services.core.runtimeType.EnterprisePlatform", version.toString());

		RuntimeInstallation currentRuntime;
		try {
			currentRuntime = resolvableRuntimeInstallation.get();
		} catch (MatchingRuntimeNotFound e) {
			e.printStackTrace();
			return null;
		}
		return "\"".concat(currentRuntime.getInstallLocation().toString()).concat("\\1cv8.exe\"");
	}

	public static HashMap<String, String> askCfLocationPath(IWorkbenchWindow windowInfo) {
		
		FileDialog saveDialog = new FileDialog(windowInfo.getShell(), SWT.SAVE);
		saveDialog.setText(Messages.CfBuild_Set_CF_Location);

		String[] filterExt = { "*.cf" };
		String[] filterNames = { Messages.CfBuild_1C_Files };
		saveDialog.setFilterExtensions(filterExt);
		saveDialog.setFilterNames(filterNames);

		HashMap<String, String> cfNameMap = new HashMap<>();

		cfNameMap.put("cfFullName", saveDialog.open());
		cfNameMap.put("cfLocation", saveDialog.getFilterPath());

		return cfNameMap;
	}

}
