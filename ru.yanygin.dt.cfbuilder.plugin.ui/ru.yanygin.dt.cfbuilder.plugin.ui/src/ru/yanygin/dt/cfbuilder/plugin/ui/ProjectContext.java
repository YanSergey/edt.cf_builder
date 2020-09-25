package ru.yanygin.dt.cfbuilder.plugin.ui;

import com._1c.g5.v8.dt.platform.version.Version;

import ru.yanygin.dt.cfbuilder.plugin.ui.ImportJob.SupportMode;

public class ProjectContext {

	private String projectName;

	private String platformV8Path;

	private CfFileInfo cfFileInfo;

	private TempDirs tempDirs;

	private Version v8version;

	private SupportMode supportMode;

	public String getProjectName() {
		return projectName;
	}

	public String getPlatformV8Path() {
		return platformV8Path;
	}

	public CfFileInfo getCfFileInfo() {
		return cfFileInfo;
	}

	public TempDirs getTempDirs() {
		return tempDirs;
	}

	public Version getV8version() {
		return v8version;
	}

	public SupportMode getSupportMode() {
		return supportMode;
	}
	
	public ProjectContext(String projectName, String platformPath, CfFileInfo cfFileInfo, TempDirs tempDirs,
			Version version, SupportMode supportMode) {

		this(projectName, platformPath, cfFileInfo, tempDirs);
		this.v8version = version;
		this.supportMode = supportMode;
	}

	public ProjectContext(String projectName, String platformPath, CfFileInfo cfFileInfo, TempDirs tempDirs) {

		this.projectName = projectName;
		this.platformV8Path = platformPath;
		this.cfFileInfo = cfFileInfo;
		this.tempDirs = tempDirs;
	}

	public String[] getParentConfigurationsFiles() {
		String[] parentConfigurationsFiles = new String[2];
		
		parentConfigurationsFiles[0] = tempDirs.getXmlPath().concat("\\Ext\\ParentConfigurations.bin");
		parentConfigurationsFiles[1] = tempDirs.getXmlPath().concat("\\Ext\\ParentConfigurations");
		
		return parentConfigurationsFiles;
	}

}
