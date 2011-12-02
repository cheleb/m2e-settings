package org.eclipse.m2e.settings.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingFiles {

	private Formatter formatter;
	
	private Map<String, String> extraFiles;

	private List<EclipsePreference> eclipsePreferences = new ArrayList<EclipsePreference>();

	

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



	public void setEclipsePreferences(List<EclipsePreference> eclipsePreferences) {
		this.eclipsePreferences = eclipsePreferences;
		
	}

	
	public List<EclipsePreference> getEclipsePreferences() {
		return eclipsePreferences;
	}
	

	
	
	 
	
}
