package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.text.MessageFormat;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;

public class ImportJob extends Job {

	private ProcessResult processResult = new ProcessResult(Status.OK_STATUS);
	private ProjectContext projectContext;
	private IProgressMonitor buildMonitor;
	private Shell parentShell;
	private IImportOperationFactory importOperationFactory;

	public ImportJob(ProjectContext projectContext, Shell parentShell, IImportOperationFactory importOperationFactory) {
		super(MessageFormat.format(Messages.Task_ImportProjectFromCf, projectContext.getProjectName()));
		
		this.projectContext = projectContext;
		this.parentShell = parentShell;
		this.importOperationFactory = importOperationFactory;
	}

	@Override
	protected IStatus run(IProgressMonitor progressMonitor) {

		String buildResult;
		String buildMessage;
		
		this.buildMonitor = progressMonitor;
		
		Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.Status_StartImport, projectContext.getProjectName())));

		Actions.createTempBase(projectContext, progressMonitor, processResult);
		Actions.loadConfigFromCf(projectContext, progressMonitor, processResult);
		Actions.dumpConfigToXml(projectContext, progressMonitor, processResult);
		Actions.importXmlToProject(importOperationFactory, projectContext, progressMonitor, processResult);

		if (processResult.statusIsOK()) {
			buildResult = Messages.Info_ImportFromCfIsDone;
			buildMessage = MessageFormat.format(Messages.Info_FileCfImportTo, projectContext.getCfFullName());

			Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.Status_EndImport, projectContext.getProjectName())));
			
		} else if (processResult.statusIsCancel()) {
			buildResult = MessageFormat.format(Messages.Status_ImportFromCfCancel, projectContext.getProjectName());
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

		buildMonitor.setTaskName(buildResult);

		Messages.showPostBuildMessage(parentShell, buildResult, buildMessage);

		projectContext.getTempDirs().deleteDirs(buildMonitor);

		return processResult.getStatus();
	}

}
