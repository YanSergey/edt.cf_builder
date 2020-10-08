package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.runtime.Plugin;

import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IRuntimeComponentExecutor;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.wiring.AbstractServiceAwareModule;
import com._1c.g5.v8.dt.export.IExportOperationFactory;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;
import com._1c.g5.v8.dt.import_.IImportServiceRegistry;
import com._1c.g5.v8.dt.core.platform.IConfigurationProjectManager;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.core.platform.IDependentProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;

public class ExternalDependenciesModule extends AbstractServiceAwareModule {
	
	public ExternalDependenciesModule(Plugin bundle) {
		super(bundle);
	}
	
	@Override
	protected void doConfigure() {
		bind(IRuntimeVersionSupport.class).toService();
		bind(IResolvableRuntimeInstallationManager.class).toService();
		bind(IExportOperationFactory.class).toService();
		bind(IImportServiceRegistry.class).toService();
		bind(IImportOperationFactory.class).toService();
		bind(IConfigurationProvider.class).toService();
		bind(IConfigurationProjectManager.class).toService();
		
		bind(IInfobaseManager.class).toService();
		bind(IV8Project.class).toService();
		bind(IV8ProjectManager.class).toService();
		bind(IDependentProject.class).toService();
		
		bind(IRuntimeComponentExecutor.class).toService();
	}
	
}
