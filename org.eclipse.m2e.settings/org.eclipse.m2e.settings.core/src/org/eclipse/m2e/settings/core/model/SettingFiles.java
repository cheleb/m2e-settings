package org.eclipse.m2e.settings.core.model;

import java.util.Map;

public class SettingFiles {

	private Formatter formatter;
	
	private Map<String, String> extraFiles;

	public Formatter getFormatter() {
		return formatter;
	}

	public void setFormatter(Formatter formatter) {
		this.formatter = formatter;
	}

	public Map<String, String> getExtraFiles() {
		return extraFiles;
	}

	public void setExtraFiles(Map<String, String> extraFiles) {
		this.extraFiles = extraFiles;
	}

	public boolean hasFormatter() {
		return formatter != null;
	}
	
	 
	
}
