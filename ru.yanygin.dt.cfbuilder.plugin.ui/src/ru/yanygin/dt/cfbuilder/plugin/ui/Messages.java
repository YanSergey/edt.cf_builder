package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Данный класс - представитель локализации механизма строк в Eclipse.
 */
class Messages extends NLS {
    private static final String BUNDLE_NAME = "ru.yanygin.dt.cfbuilder.plugin.ui.messages";

    public static String CfBuild_Start_Build;
    public static String CfBuild_End_Build;
    
    public static String CfBuild_Error_Save_Texts;
    public static String CfBuild_Error_Get_Project;
    public static String CfBuild_Error_Find_Platform;
    public static String CfBuild_Error_Create_Temp;
    public static String CfBuild_Error_Delete_Temp;
    public static String CfBuild_Unknown_Error;
    
    public static String CfBuild_Build_Project_Name;
    public static String CfBuild_Run_Convertion;
    public static String CfBuild_Convertion_Done;
    public static String CfBuild_Create_Base;
    public static String CfBuild_Load_Config;
    public static String CfBuild_Dump_Config;
    public static String CfBuild_Clean_Temp;
    
    public static String CfBuild_Abort;
    public static String CfBuild_Cancel;
    public static String CfBuild_Done;
    public static String CfBuild_Set_CF_Location;
    public static String CfBuild_Set_CF_Error;
    public static String CfBuild_File_CF_Save_Is;
    public static String CfBuild_1C_Files;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
