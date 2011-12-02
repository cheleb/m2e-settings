package org.eclipse.m2e.settings.core.model;

public class EclipsePreference {

	private String filename;
	private String pref;

	public void setFilename(String filename) {
		this.filename = filename;

	}

	public String getFilename() {
		return filename;
	}

	public void setPref(String pref) {
		this.pref = pref;

	}

	public String getPref() {
		return pref;
	}
}
