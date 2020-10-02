package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;
import java.util.Map;

public class PlatformV8Commands {

	private static final String PLATFORM_PATH 		= "%PLATFORM_1C_PATH%";
	private static final String DESIGNER 			= "DESIGNER /F \"%BASE_1C_PATH%\"";
	private static final String CREATEINFOBASE 		= "CREATEINFOBASE File=\"%BASE_1C_PATH%\"";
	private static final String LOADCONFIGFROMFILES = "/LoadConfigFromFiles %XMLDIR%";
	private static final String LOADCONFIGFROMCF 	= "/LoadCfg \"%CFNAME%\"";
	private static final String DUMPCONFIGTOFILES 	= "/DumpConfigToFiles %XMLDIR%";
	private static final String DUMPCONFIGTOCF 		= "/DumpCfg \"%CFNAME%\"";
	private static final String UPDATEDBCONFIG		= "/UpdateDBCfg";
	private static final String CREATEDISTRCF		= "/CreateDistributionFiles -cffile \"%CFNAME%\"";
	private static final String LOGFILE 			= "/Out \"%LOGFILE%\"";

	public enum V8CommandTypes {
		CREATEINFOBASE, LOADCONFIGFROMFILES, LOADCONFIGFROMCF, DUMPCONFIGTOFILES, DUMPCONFIGTOCF, UPDATEDBCONFIG, CREATEDISTRCF 
	};

	public static Map<String, String> getPlatformV8Command(V8CommandTypes commandType) {

		Map<String, String> command = new HashMap<>();

		switch (commandType) {
			case CREATEINFOBASE:
				command.put("command", makeV8Command(PLATFORM_PATH, CREATEINFOBASE, LOGFILE));
				command.put("actionMessage", Messages.Actions_Create_TempBase);
				break;
			case LOADCONFIGFROMFILES:
				command.put("command", makeV8Command(PLATFORM_PATH, DESIGNER, LOADCONFIGFROMFILES, LOGFILE));
				command.put("actionMessage", Messages.Actions_Load_ConfigFromXml);
				break;
			case LOADCONFIGFROMCF:
				command.put("command", makeV8Command(PLATFORM_PATH, DESIGNER, LOADCONFIGFROMCF, LOGFILE));
				command.put("actionMessage", Messages.Actions_Load_ConfigFromCf);
				break;
			case DUMPCONFIGTOFILES:
				command.put("command", makeV8Command(PLATFORM_PATH, DESIGNER, DUMPCONFIGTOFILES, LOGFILE));
				command.put("actionMessage", Messages.Actions_Dump_ConfigToXml);
				break;
			case DUMPCONFIGTOCF:
				command.put("command", makeV8Command(PLATFORM_PATH, DESIGNER, DUMPCONFIGTOCF, LOGFILE));
				command.put("actionMessage", Messages.Actions_Dump_ConfigToCf);
				break;
			case UPDATEDBCONFIG:
				command.put("command", makeV8Command(PLATFORM_PATH, DESIGNER, UPDATEDBCONFIG, LOGFILE));
				command.put("actionMessage", Messages.Actions_Update_DB_Config);
				break;
			case CREATEDISTRCF:
				command.put("command", makeV8Command(PLATFORM_PATH, DESIGNER, CREATEDISTRCF, LOGFILE));
				command.put("actionMessage", Messages.Actions_Create_DistrCf);
				break;
		}
		return command;

	}

	private static String makeV8Command(String... commands) {
		return String.join(" ", commands);
	}
}
