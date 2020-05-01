package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.runtime.Plugin;

import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.wiring.AbstractServiceAwareModule;

public class ExternalDependenciesModule extends AbstractServiceAwareModule {

	public ExternalDependenciesModule(Plugin bundle) {
		super(bundle);
	}

	@Override
	protected void doConfigure() {
		bind(IRuntimeVersionSupport.class).toService();
        bind(IResolvableRuntimeInstallationManager.class).toService();
	}

}
