package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.File;
import java.nio.file.Path;

import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.impl.RuntimeExecutionCommandBuilder;
import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;

public class V8ExtendedCommandBuilder extends RuntimeExecutionCommandBuilder {
	
	public V8ExtendedCommandBuilder(File thickClient, ThickClientMode mode) {
		super(thickClient, mode);
	}
	
	public V8ExtendedCommandBuilder forInfobase(InfobaseReference infobase, boolean split) {
		super.forInfobase(infobase, split);
		return this;
	}
	
	public V8ExtendedCommandBuilder dumpConfigurationToCfDistr(String destinationFile) {
		this.appendOption("CreateDistributionFiles");
		this.appendOptionParameter("-cffile");
		this.appendOptionParameter(destinationFile);
		return this;
	}
	
	public V8ExtendedCommandBuilder loadExtensionFromCfe(String sourceFile, String extensionName) {
		this.appendOption("LoadCfg");
		this.appendOptionParameter(sourceFile);
		this.appendOptionParameter("-Extension", extensionName);
		return this;
	}
	
	public V8ExtendedCommandBuilder loadExtensionFromXml(Path sourceFolder, String extensionName) {
		
		this.appendOption("LoadConfigFromFiles");
		this.appendOptionParameter(sourceFolder.toString());
		this.appendOptionParameter("-Extension", extensionName);
		return this;
	}
	
	public V8ExtendedCommandBuilder dumpExtensionToCfe(String destinationFile, String extensionName) {
		this.appendOption("DumpCfg");
		this.appendOptionParameter(destinationFile);
		this.appendOptionParameter("-Extension", extensionName);
		return this;
	}
	
	public V8ExtendedCommandBuilder dumpExtensionToXml(Path destinationFolder, String extensionName) {
		this.appendOption("DumpConfigToFiles");
		this.appendOptionParameter(destinationFolder.toString());
		this.appendOptionParameter("-Extension", extensionName);
		return this;
	}
	
}
