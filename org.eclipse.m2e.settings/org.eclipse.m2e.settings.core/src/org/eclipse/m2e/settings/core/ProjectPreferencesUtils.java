package org.eclipse.m2e.settings.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.settings.core.model.EclipsePreference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectPreferencesUtils {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ProjectPreferencesUtils.class);

	private ProjectPreferencesUtils() {
	}

	static Preferences getPrefences(IProject project, String pref) {
		return Platform.getPreferencesService().getRootNode()
				.node("project/" + project.getName() + "/" + pref);
	}

	static void setOtherPreferences(IProject project, InputStream inputStream,
			EclipsePreference eclipsePreference) throws IOException,
			BackingStoreException {

		String pref = eclipsePreference.getPref();
		if (inputStream == null) {
			LOGGER.warn("No settings for: " + pref);
			return;
		}
		Preferences preferences = getPrefences(project, pref);

		Reader inStreamReader = new InputStreamReader(inputStream,
				Charset.forName("utf8"));
		Properties properties = new Properties();
		properties.load(inStreamReader);
		Map<String, String> overridenProperties = eclipsePreference
				.getProperties();
		for (Enumeration<Object> e = properties.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			if (overridenProperties.containsKey(key)) {
				preferences.put(key, overridenProperties.get(key));
			} else {
				preferences.put(key, properties.getProperty(key));
			}
		}

		preferences.flush();

		LOGGER.info("Updated preferences: " + pref);
	}

}
