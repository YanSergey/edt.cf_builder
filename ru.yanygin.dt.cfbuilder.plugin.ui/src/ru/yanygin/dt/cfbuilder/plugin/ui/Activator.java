package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ru.yanygin.dt.cfbuilder.plugin.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private static final String CF_BUILD_PREFIX = "[CF build] "; //$NON-NLS-1$
    private Injector injector;
    private BundleContext bundleContext;

	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		super.start(context);
        this.bundleContext = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}
	
    protected BundleContext getContext() {
        return bundleContext;
    }

	public static Activator getDefault() {
		return plugin;
	}
    
	public synchronized Injector getInjector() {
		if (injector == null)
			injector = createInjector();

		return injector;
    }

    private Injector createInjector() {
        try
        {
            return Guice.createInjector(new ExternalDependenciesModule(this));
        }
        catch (Exception e)
        {
            log(createErrorStatus("Failed to create injector for " //$NON-NLS-1$
                + getBundle().getSymbolicName(), e));
            throw new RuntimeException("Failed to create injector for " //$NON-NLS-1$
                + getBundle().getSymbolicName(), e);
        }
    }

	public static IStatus createInfoStatus(String message) {
		return new Status(IStatus.INFO, PLUGIN_ID, 0, CF_BUILD_PREFIX.concat(message), (Throwable) null);
	}
	
    public static IStatus createErrorStatus(String message, Throwable throwable) {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, CF_BUILD_PREFIX.concat(message), throwable);
    }
	
    public static IStatus createErrorStatus(String message) {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, CF_BUILD_PREFIX.concat(message), (Throwable) null);
    }    
    public static void log(IStatus status) {
        plugin.getLog().log(status);
    }
}
