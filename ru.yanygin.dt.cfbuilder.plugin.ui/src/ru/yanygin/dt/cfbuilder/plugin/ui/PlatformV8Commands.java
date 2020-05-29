package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;

public class PlatformV8Commands {

	public enum V8CommandTypes {CREATEINFOBASE, LOADCONFIGFROMFILES, LOADCONFIGFROMCF, DUMPCONFIGTOFILES, DUMPCONFIGTOCF};

	public static HashMap<String, String> getPlatformV8Command(V8CommandTypes commandType) {
		
		HashMap<String, String> command = new HashMap<>();
		
		switch (commandType) {
			case CREATEINFOBASE:
				command.put("command", "%PLATFORM_1C_PATH% CREATEINFOBASE File=%BASE_1C_PATH% /Out %LOGFILE%");
				command.put("actionMessage", Messages.Actions_Create_TempBase);
				break;
			case LOADCONFIGFROMFILES:
				command.put("command", "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadConfigFromFiles %outXmlDir% /Out %LOGFILE%");
				command.put("actionMessage", Messages.Actions_Load_ConfigFromXml);
				break;
			case LOADCONFIGFROMCF:
				command.put("command", "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadCfg \"%cfName%\" /Out %LOGFILE%");
				command.put("actionMessage", Messages.Actions_Load_ConfigFromCf);
				break;
			case DUMPCONFIGTOFILES:
				command.put("command", "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /DumpConfigToFiles %outXmlDir% /Out %LOGFILE%");
				command.put("actionMessage", Messages.Actions_Dump_ConfigToXml);
				break;
			case DUMPCONFIGTOCF:
				command.put("command", "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /DumpCfg \"%cfName%\" /Out %LOGFILE%");
				command.put("actionMessage", Messages.Actions_Dump_ConfigToCf);
				break;
		}
		return command;
		
		
	}
}
