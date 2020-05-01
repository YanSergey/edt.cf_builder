package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;

public class ProjectContext {
	
	private String projectName;
	private String projectPath;
	
	private String platformPath;
	private String edtVersion;
	
	private String cfFullName;
	private String cfLocation;
	
	public String getProjectName() {
		return projectName;
	}
	
	public String getProjectPath() {
		return projectPath;
	}
	
	public String getPlatformPath() {
		return platformPath;
	}
	
	public String getEdtVersion() {
		return edtVersion;
	}
	
	public String getCfFullName() {
		return cfFullName;
	}
	
	public String getCfLocation() {
		return cfLocation;
	}
	
	public ProjectContext(String projectName,
						  String projectPath,
						  String platformPath,
						  String edtVersion,
						  HashMap<String, String> cfName) {
		
		this.projectName 	= projectName;
		this.projectPath 	= projectPath;
		this.platformPath 	= platformPath;
		this.edtVersion 	= edtVersion;
		this.cfFullName 	= cfName.get("cfFullName");
		this.cfLocation 	= cfName.get("cfLocation");
        
	}
	
}
