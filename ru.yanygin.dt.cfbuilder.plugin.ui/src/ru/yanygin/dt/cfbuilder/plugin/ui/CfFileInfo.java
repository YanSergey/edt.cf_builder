package ru.yanygin.dt.cfbuilder.plugin.ui;

public class CfFileInfo {

	public final String FULLNAME;
	public final String LOCATION;
	public final String NAME;
	public final String NAMEWITHOUTEXTENSION;
	
	public CfFileInfo(String fullName, String location, String name, String nameWithoutExtension) {
	
		this.FULLNAME = fullName;
		this.LOCATION = location;
		this.NAME = name;
		this.NAMEWITHOUTEXTENSION = nameWithoutExtension;
	}
}
