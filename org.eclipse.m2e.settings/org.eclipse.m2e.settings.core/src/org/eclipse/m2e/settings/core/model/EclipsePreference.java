package org.eclipse.m2e.settings.core.model;

public class EclipsePreference extends PropertiesHolder {

	private String pref;
	
	public void setPref(String pref) {
		this.pref = pref;
	}

	public String getPref() {
		return pref;
	}
}
