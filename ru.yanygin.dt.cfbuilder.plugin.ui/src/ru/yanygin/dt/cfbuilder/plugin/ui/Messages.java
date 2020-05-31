package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Данный класс - представитель локализации механизма строк в Eclipse.
 */
class Messages extends NLS {
	private static final String BUNDLE_NAME = "ru.yanygin.dt.cfbuilder.plugin.ui.messages"; //$NON-NLS-1$

	public static String Task_BuildCfFromProject;
	public static String Task_ImportProjectFromCf;
	
	public static String Actions_Set_CF_Location;
	public static String Actions_Create_TempBase;
	public static String Actions_Load_ConfigFromXml;
	public static String Actions_Dump_ConfigToXml;
	public static String Actions_Load_ConfigFromCf;
	public static String Actions_Dump_ConfigToCf;
	public static String Actions_Import_ProjectFromXml;
	public static String Actions_ClearingTemp;
	
	public static String Status_StartBuild;
	public static String Status_EndBuild;
	public static String Status_StartImport;
	public static String Status_EndImport;
	
	public static String Status_OperationAbort;
	public static String Status_CfBuildCancel;
	public static String Status_ImportFromCfCancel;
	public static String Status_UnknownError;
	public static String Status_CancelFileCfSelestion;
	public static String Status_ErrorGetProject;
	public static String Status_ErrorCreateTempDirs;

	public static String Status_ErrorFindPlatform;
	public static String Status_ErrorDeleteTemp;

	public static String Info_DataIsPreparing;
	public static String Info_CfBuildIsDone;
	public static String Info_FileCfSaveIs;
	public static String Info_ImportFromCfIsDone;
	public static String Info_FileCfImportTo;
	
	public static String Filter_1C_Files;

	public static String Dialog_ImportProjectFromCf;
	public static String Dialog_ProjectName;
	public static String Dialog_ProjectExists;
	public static String Dialog_V8Version;
	public static String Dialog_CfPath;
	public static String Dialog_View;


	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
	
	public static void showPostBuildMessage(Shell parentShell, String buildMessage) {
		showPostBuildMessage(parentShell, buildMessage, buildMessage);
	}
	
	public static void showPostBuildMessage(Shell parentShell, String buildTitle, String buildMessage) {
		Display.getDefault()
				.asyncExec(() -> MessageDialog.openInformation(parentShell, buildTitle, buildMessage));
	}

}
