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
	
	public static String Actions_CreateTempBase;
	public static String Actions_CreateTempDir;
	public static String Actions_DeleteTempBase;
	public static String Actions_LoadConfigFromXml;
	public static String Actions_DumpConfigToXml;
	public static String Actions_LoadConfigFromCf;
	public static String Actions_DumpConfigToCf;
	public static String Actions_ImportProjectFromXml;
	public static String Actions_UpdateInfobase;
	public static String Actions_DeployProjectToInfobase;
	public static String Actions_ClearingTemp;
	public static String Actions_PullInfobaseChanges;
	public static String Actions_SetAssociateInfobaseToProject;
	
	public static String Status_StartExport;
	public static String Status_EndExport;
	public static String Status_StartImport;
	public static String Status_EndImport;
	
	public static String Status_OperationAbortByUser;
	public static String Status_CfBuildCancel;
	public static String Status_ImportFromCfCancel;
	public static String Status_Error;
	public static String Status_ErrorGetProject;
	public static String Status_ErrorCreateParentDir;
	public static String Status_IncorrectDialogType;
	
	public static String Info_DataIsPreparing;
	public static String Info_CfBuildIsDone;
	public static String Info_FileCfSaveIs;
	public static String Info_ImportFromCfIsDone;
	public static String Info_ErrorDeleteFile;
	public static String Info_FileSupportNotFound;
	public static String Info_FileDelete;
	public static String Info_ErrorIdentifyingProjectType;
	
	public static String Filter_CF_Files;
	public static String Filter_CFE_Files;
	public static String Filter_CF_CFE_Files;
	
	public static String Dialog_SetCfLocation;
	public static String Dialog_ImportProjectFromCf;
	public static String Dialog_ExportProjectToCf;
	public static String Dialog_ProjectName;
	public static String Dialog_ProjectReference;
	public static String Dialog_ProjectExists;
	public static String Dialog_V8Version;
	public static String Dialog_CfPath;
	public static String Dialog_View;
	public static String Dialog_SupportlabelText;
	public static String Dialog_SupportModeDefault;
	public static String Dialog_SupportModeDisable;
	public static String Dialog_SetNewProjectProperties;
	public static String Dialog_SetExportProperties;
	public static String Dialog_UseTempIB;
	public static String Dialog_ShowOnlyAssociatedIB;
	public static String Dialog_LoadFullConfiguration;
	public static String Dialog_InfoBase;
	public static String Dialog_AssociateAfterDeploy;
	public static String Dialog_CreateDistributionCffile;
	public static String Dialog_SelectInfobase;
	public static String Dialog_ParentProjectNotSet;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages() {
	}
	
	public static void showPostBuildMessage(Shell parentShell, String buildMessage) {
		showPostBuildMessage(parentShell, buildMessage, buildMessage);
	}
	
	public static void showPostBuildMessage(Shell parentShell, String buildTitle, String buildMessage) {
		Display.getDefault().asyncExec(() -> MessageDialog.openInformation(parentShell, buildTitle, buildMessage));
	}
	
}
