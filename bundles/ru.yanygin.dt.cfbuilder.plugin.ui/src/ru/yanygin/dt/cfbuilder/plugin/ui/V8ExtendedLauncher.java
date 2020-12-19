package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import com._1c.g5.v8.dt.common.FileUtil;
import com._1c.g5.v8.dt.common.Pair;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IDesignerSessionThickClientLauncher;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ILaunchableRuntimeComponent;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IThickClientLauncher;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionArguments;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeVersionRequiredException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.impl.AbstractRuntimeComponentExecutor;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.impl.RuntimeExecutionCommandBuilder;
import com._1c.g5.v8.dt.platform.services.model.InfobaseAccess;
import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com.google.common.base.Strings;

public class V8ExtendedLauncher extends AbstractRuntimeComponentExecutor {
	
	private Pair<ILaunchableRuntimeComponent, IThickClientLauncher> v8Launcher;
	
	public V8ExtendedLauncher(Pair<ILaunchableRuntimeComponent, IThickClientLauncher> v8Launcher) {
		this.v8Launcher = v8Launcher;
	}
	
	public String executeRuntimeProcessCommandEx(RuntimeExecutionCommandBuilder command,
			RuntimeExecutionArguments arguments, InfobaseReference infobase)
			throws RuntimeExecutionException, RuntimeVersionRequiredException {
		
		addAuthentication(command, arguments);
		
		closeDesignerSession(arguments, infobase);
		return super.executeRuntimeProcessCommand(command, v8Launcher.first.getInstallation(), infobase, arguments);
		
	}
	
	private void addAuthentication(RuntimeExecutionCommandBuilder command, RuntimeExecutionArguments arguments) {
		if (arguments.getAccess() == InfobaseAccess.OS) {
			command.osAuthentication(true);
		} else if (arguments.getAccess() == InfobaseAccess.INFOBASE) {
			command.osAuthentication(false);
			if (!Strings.isNullOrEmpty(arguments.getUsername())) {
				command.userName(arguments.getUsername());
			}
			if (!Strings.isNullOrEmpty(arguments.getPassword())) {
				command.userPassword(arguments.getPassword());
			}
		}
	}
	
	private void closeDesignerSession(RuntimeExecutionArguments arguments, InfobaseReference infobase)
			throws RuntimeExecutionException {
		
		((IDesignerSessionThickClientLauncher) v8Launcher.second).closeDesignerSession(v8Launcher.first, infobase,
				arguments);
		
	}
	
	@Override
	protected File createTempFile() throws IOException {
		return FileUtil.createTempFile("1cv8-", ".log");
	}
	
	@Override
	protected String readRuntimeLog(File logFile, RuntimeInstallation installation) throws IOException {
		return readOutLogFile(logFile);
	}
	
	protected String readOutLogFile(File fileName) {
		String contents = "";
		
		if (fileName.exists()) {
			try {
				contents = new String(Files.readAllBytes(Paths.get(fileName.getAbsolutePath())),
						Charset.forName("Windows-1251"));
			} catch (IOException e) {
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			}
		}
		
		return contents;
	}
}
