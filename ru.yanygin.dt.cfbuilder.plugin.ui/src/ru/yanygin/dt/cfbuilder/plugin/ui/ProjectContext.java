package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;

public class ProjectContext {

	private String projectName;
//	private String projectPath;

	private String platformPath;

	private String cfFullName;
	private String cfLocation;

	public String getProjectName() {
		return projectName;
	}

//	public String getProjectPath() {
//		return projectPath;
//	}

	public String getPlatformPath() {
		return platformPath;
	}


	public String getCfFullName() {
		return cfFullName;
	}

	public String getCfLocation() {
		return cfLocation;
	}

	public ProjectContext(String projectName, String platformPath, HashMap<String, String> cfNameMap) {
//	public ProjectContext(String projectName, String projectPath, String platformPath, HashMap<String, String> cfNameMap) {

		this.projectName = projectName;
//		this.projectPath = projectPath;
		this.platformPath = platformPath;
		this.cfFullName = cfNameMap.get("cfFullName");
		this.cfLocation = cfNameMap.get("cfLocation");

	}

}
