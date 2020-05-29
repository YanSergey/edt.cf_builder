package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;

import com._1c.g5.v8.dt.platform.version.Version;

public class ProjectContext {

	private String projectName;

	private String platformPath;

	private String cfFullName;
	private String cfLocation;

	private TempDirs tempDirs;

	private Version version;
	
	public String getProjectName() {
		return projectName;
	}

	public String getPlatformPath() {
		return platformPath;
	}

	public String getCfFullName() {
		return cfFullName;
	}

	public String getCfLocation() {
		return cfLocation;
	}

	public TempDirs getTempDirs() {
		return tempDirs;
	}

	public Version getVersion() {
		return version;
	}
	
	public ProjectContext(String projectName, String platformPath, HashMap<String, String> cfNameInfo, TempDirs tempDirs, Version version) {

		this(projectName, platformPath, cfNameInfo, tempDirs);
		this.version = version;

	}

	public ProjectContext(String projectName, String platformPath, HashMap<String, String> cfNameInfo, TempDirs tempDirs) {

		this.projectName = projectName;
		this.platformPath = platformPath;
		this.cfFullName = cfNameInfo.get("cfFullName");
		this.cfLocation = cfNameInfo.get("cfLocation");
		this.tempDirs = tempDirs;

	}

}
