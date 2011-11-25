package org.eclipse.m2e.settings.core;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.m2e.settings.core.model.Formatter;
import org.eclipse.m2e.settings.core.model.JDTUIPref;
import org.eclipse.m2e.settings.core.model.SettingFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationHelper {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ConfigurationHelper.class);

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

		JDTUIPref jdtUIPref = extractJdtUIPref(configurationXpp3Dom);

		settingFiles.setJdtUIPref(jdtUIPref);

		return settingFiles;
	}

	private static JDTUIPref extractJdtUIPref(Xpp3Dom configurationXpp3Dom) {
		Xpp3Dom formatterXpp3Dom = configurationXpp3Dom.getChild("jdtui");
		if (formatterXpp3Dom != null) {
			JDTUIPref jdtuiPref = new JDTUIPref();
			Xpp3Dom fileName = formatterXpp3Dom.getChild("filename");
			if (fileName != null) {
				jdtuiPref.setFilename(fileName.getValue());
			}
			return jdtuiPref;
		}
		return null;
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
