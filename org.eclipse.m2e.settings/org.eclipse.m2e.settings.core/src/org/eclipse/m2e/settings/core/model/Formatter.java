package org.eclipse.m2e.settings.core.model;

import java.util.HashMap;
import java.util.Map;

public class Formatter {
	private String fileName;
	private String profile;
	private Map<String, String> properties = new HashMap<String, String>();

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profileName) {
		this.profile = profileName;
	}
	
	public void addProperty(String name, String value) {
		properties.put(name, value);
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
}
