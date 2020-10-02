package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.text.MessageFormat;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;

import ru.yanygin.dt.cfbuilder.plugin.ui.PlatformV8Commands.V8CommandTypes;

public class BuildDistrCfJob extends Job {

	private ProcessResult processResult = new ProcessResult(Status.OK_STATUS);

	private ProjectContext projectContext;
	private Shell parentShell;

	public BuildDistrCfJob(ProjectContext projectContext, Shell parentShell) {
		super(MessageFormat.format(Messages.Task_BuildCfFromProject, projectContext.getProjectName()));

		this.projectContext = projectContext;
		this.parentShell = parentShell;
	}

	@Override
	protected IStatus run(IProgressMonitor progressMonitor) {

		Actions.runPlatformV8Command(V8CommandTypes.CREATEINFOBASE, projectContext, processResult, progressMonitor);
		Actions.runPlatformV8Command(V8CommandTypes.LOADCONFIGFROMFILES, projectContext, processResult, progressMonitor);
		Actions.runPlatformV8Command(V8CommandTypes.UPDATEDBCONFIG, projectContext, processResult, progressMonitor);
		Actions.runPlatformV8Command(V8CommandTypes.CREATEDISTRCF, projectContext, processResult, progressMonitor);

		String buildResult;
		String buildMessage;

		if (processResult.statusIsOK()) {
			buildResult = Messages.Info_CfBuildIsDone;
			buildMessage = Messages.Info_FileCfSaveIs
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(projectContext.getCfFileInfo().FULLNAME);

			Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.Status_EndBuild, projectContext.getProjectName())));
			
		} else if (processResult.statusIsCancel()) {
			buildResult = MessageFormat.format(Messages.Status_CfBuildCancel, projectContext.getProjectName());
			buildMessage = buildResult;

			Activator.log(Activator.createInfoStatus(buildResult));

		} else {
			buildResult = Messages.Status_OperationAbort;
			buildMessage = Messages.Status_OperationAbort;

			Activator.log(Activator.createErrorStatus(buildResult));

			if (!processResult.getOutput().isEmpty()) {
				Activator.log(Activator.createErrorStatus(processResult.getOutput()));
				buildMessage = buildMessage
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(processResult.getOutput());
			}

			String outLog = Actions.readOutLogFile(projectContext.getTempDirs().getLogFilePath());
			if (!outLog.isEmpty()) {
				Activator.log(Activator.createErrorStatus(outLog));
				buildMessage = buildMessage
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(outLog);
			}
		}

		progressMonitor.setTaskName(buildResult);
		Messages.showPostBuildMessage(parentShell, buildResult, buildMessage);

		projectContext.getTempDirs().deleteDirs(progressMonitor);

		return processResult.getStatus();
	}

}
