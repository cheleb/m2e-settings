package org.eclipse.m2e.settings.core.model;


public class Formatter extends PropertiesHolder {
	private String profile;

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profileName) {
		this.profile = profileName;
	}
}
