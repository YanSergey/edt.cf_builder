package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;
import com.google.common.io.Files;

import ru.yanygin.dt.cfbuilder.plugin.ui.BaseProjectWorker.ProjectType;

public class ProjectInfo {
	
	public static final String FILEEXTENSION_CONFIGURATION = "cf";
	public static final String FILEEXTENSION_EXTENSION = "cfe";
	private static final String DOT = ".";
	
	public final String name;
	private final ConfigurationFileType fileType;
	private boolean useTempIB;
	private InfobaseReference deploymentInfobase;
	private Boolean linkIBToProject;
	
	public Path getPath() {
		return Paths.get(name);
	}
	
	public boolean useTempIB() {
		return useTempIB;
	}
	
	public InfobaseReference getDeploymentInfobase() {
		return deploymentInfobase;
	}
	
	public void setDeploymentInfobase(InfobaseReference deploymentInfobase) {
		this.deploymentInfobase = deploymentInfobase;
	}
	
	public Boolean linkIBToProject() {
		return linkIBToProject;
	}
	
	public final ConfigurationFileType getFileType() {
		return fileType;
	}
	
	public Path getParentDirPath() {
		return getPath().getParent();
	}
	
	public boolean checkAndCreateParentDir() {
		File parentDir = getParentDirPath().toFile();
		return parentDir.exists() || parentDir.mkdir();
	}
	
	protected enum ConfigurationFileType {
		SIMPLE, DISTRIBUTION, EXTENSION
	}
	
	public ProjectInfo(String name, boolean useTempIB, InfobaseReference selectedInfobase, boolean linkIBToProject,
			boolean createDistributionCf) {
		
		this.name = name;
		this.useTempIB = useTempIB;
		this.setDeploymentInfobase(selectedInfobase);
		this.linkIBToProject = linkIBToProject;
		String fileExt = Files.getFileExtension(name);
		
		if (fileExt.equalsIgnoreCase(ProjectInfo.FILEEXTENSION_CONFIGURATION)) {
			if (!createDistributionCf) {
				this.fileType = ConfigurationFileType.SIMPLE;
			} else {
				this.fileType = ConfigurationFileType.DISTRIBUTION;
			}
		} else if (fileExt.equalsIgnoreCase(ProjectInfo.FILEEXTENSION_EXTENSION)) {
			this.fileType = ConfigurationFileType.EXTENSION;
		} else {
			this.fileType = null;
		}
		
	}
	
	public static String changeFileExtension(String oldFileName, ProjectType projectType) {
		
		if (checkFileExtensionOfProject(oldFileName, projectType)) {
			return oldFileName;
		}
		
		String newFileName = oldFileName;
		
		if (oldFileName.contains(DOT)) {
			newFileName = oldFileName.substring(0, oldFileName.lastIndexOf(DOT));
		}
		
		return newFileName.concat(DOT).concat(getFileExtensionByProjectType(projectType));
		
	}
	
	public static boolean checkFileExtensionOfProject(String oldFileName, ProjectType projectType) {
		
		return projectType == ProjectType.ALL
				|| Files.getFileExtension(oldFileName).equals(getFileExtensionByProjectType(projectType));
	}
	
	private static String getFileExtensionByProjectType(ProjectType projectType) {
		switch (projectType) {
			case CONFIGURATION:
			case ALL:
				return FILEEXTENSION_CONFIGURATION;
			case EXTENSION:
				return FILEEXTENSION_EXTENSION;
		}
		return null;
	}
}
