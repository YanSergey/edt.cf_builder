package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import com.google.common.io.Files;

public class TempDirs {

	private String tempDirPath;
	private String xmlPath;
	private String onesBasePath;
	private String logFilePath;

	public String getXmlPath() {
		return xmlPath;
	}

	public String getOnesBasePath() {
		return onesBasePath;
	}

	public String getLogFilePath() {
		return logFilePath;
	}

	public TempDirs() {
		super();
	}

	public boolean createTempDirs() {

		this.tempDirPath = Files.createTempDir().getAbsolutePath();

		this.xmlPath		= tempDirPath + "\\xml";
		this.onesBasePath	= tempDirPath + "\\base";
		this.logFilePath	= tempDirPath + "\\out.log";

		if (new File(xmlPath).mkdir() | new File(onesBasePath).mkdir())
			return true;
		else {
			Activator.log(Activator.createErrorStatus(Messages.Status_ErrorCreateTempDirs));
			return false;
		}

	}

	public void deleteDirs(IProgressMonitor buildMonitor) {
		buildMonitor.beginTask(Messages.Actions_ClearingTemp, IProgressMonitor.UNKNOWN);
		recursiveDelete(new File(tempDirPath));
		buildMonitor.done();
	}

	private static void recursiveDelete(File file) {

		if (!file.exists())
			return;

		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				recursiveDelete(f);
			}
		}

		if (!file.delete())
//			Activator.log(Activator.createInfoStatus(Messages.CfBuild_Error_Delete_Temp.replace("%fileName%", file.getAbsolutePath())));
			Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.Status_ErrorDeleteTemp, file.getAbsolutePath())));
	}

}
