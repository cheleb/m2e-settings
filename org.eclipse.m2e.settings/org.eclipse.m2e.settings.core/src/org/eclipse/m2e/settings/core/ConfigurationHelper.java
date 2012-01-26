package org.eclipse.m2e.settings.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.m2e.settings.core.model.EclipsePreference;
import org.eclipse.m2e.settings.core.model.Formatter;
import org.eclipse.m2e.settings.core.model.SettingFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigurationHelper {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ConfigurationHelper.class);

	private ConfigurationHelper() {
	}

	public static SettingFiles extractSettingsFile(Plugin eclipsePlugin) {

		Xpp3Dom configurationXpp3Dom = (Xpp3Dom) eclipsePlugin
				.getConfiguration();

		if (configurationXpp3Dom == null) {
			LOGGER.error("Configuration should be provided.");
			return null;
		}

		SettingFiles settingFiles = new SettingFiles();

		Formatter formatter = extractFormatter(configurationXpp3Dom);

		settingFiles.setFormatter(formatter);

		List<EclipsePreference> eclipsePreferences = extractEclipsePreferences(configurationXpp3Dom);

		settingFiles.setEclipsePreferences(eclipsePreferences);

		return settingFiles;
	}

	private static List<EclipsePreference> extractEclipsePreferences(
			Xpp3Dom configurationXpp3Dom) {
		List<EclipsePreference> eclipsePreferences = new ArrayList<EclipsePreference>();
		Xpp3Dom preferencesDom = configurationXpp3Dom.getChild("preferences");
		if (preferencesDom != null) {

			Xpp3Dom[] preferenceDoms = preferencesDom.getChildren("preference");
			for (Xpp3Dom preferenceDom : preferenceDoms) {
				Xpp3Dom fileNameXpp3Dom = preferenceDom.getChild("filename");
				Xpp3Dom nameXpp3Dom = preferenceDom.getChild("name");

				if (fileNameXpp3Dom != null && nameXpp3Dom != null) {
					String filename = fileNameXpp3Dom.getValue();
					String name = nameXpp3Dom.getValue();

					EclipsePreference eclipsePreference = new EclipsePreference();
					eclipsePreference.setFilename(filename);
					eclipsePreference.setPref(name);
					eclipsePreferences.add(eclipsePreference);

				}
			}

		}
		return eclipsePreferences;
	}

	private static Formatter extractFormatter(Xpp3Dom configurationXpp3Dom) {
		Xpp3Dom formatterXpp3Dom = configurationXpp3Dom.getChild("formatter");
		if (formatterXpp3Dom != null) {
			Formatter formatter = new Formatter();
			Xpp3Dom formatterFileName = formatterXpp3Dom.getChild("filename");
			if (formatterFileName != null) {
				formatter.setFileName(formatterFileName.getValue());
			}
			Xpp3Dom formatterProfileName = formatterXpp3Dom.getChild("profile");
			if (formatterProfileName != null) {
				formatter.setProfile(formatterProfileName.getValue());
			}
			return formatter;
		}
		return null;
	}

}
