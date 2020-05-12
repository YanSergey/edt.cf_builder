package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.runtime.Plugin;

import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.wiring.AbstractServiceAwareModule;
import com._1c.g5.v8.dt.export.IExportOperationFactory;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;

public class ExternalDependenciesModule extends AbstractServiceAwareModule {

	public ExternalDependenciesModule(Plugin bundle) {
		super(bundle);
	}

	@Override
	protected void doConfigure() {
		bind(IRuntimeVersionSupport.class).toService();
		bind(IResolvableRuntimeInstallationManager.class).toService();
		bind(IExportOperationFactory.class).toService();
		bind(IConfigurationProvider.class).toService();
	}

}
