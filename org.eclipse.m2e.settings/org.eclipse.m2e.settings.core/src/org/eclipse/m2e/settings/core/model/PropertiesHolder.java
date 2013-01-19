package org.eclipse.m2e.settings.core.model;

import java.util.HashMap;
import java.util.Map;

public abstract class PropertiesHolder {

	private String filename;

	private Map<String, String> properties = new HashMap<String, String>();

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public void addProperty(String name, String value) {
		properties.put(name, value);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

}
