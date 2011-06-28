package org.eclipse.m2e.settings.core.model;

public class Formatter {
	private String fileName;
	private String profile;

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
}
