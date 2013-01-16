package org.eclipse.m2e.settings.core;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

@SuppressWarnings("restriction")
public final class ProfileHelper {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ProfileHelper.class);

	private ProfileHelper() {

	}

	public static Profile findProfile(List<Profile> profiles, String profileName) {
		if (profiles == null || profiles.size() == 0) {
			LOGGER.warn("No profiles: " + profiles);
			return null;
		}

		if (profiles.size() == 1 && profileName != null) {
			Profile profile = profiles.get(0);
			if (profileName.equals(profile.getName())) {
				return profile;
			} else {
				LOGGER.warn("Profile name: " + profileName
						+ " does not match with the only profile found: "
						+ profile.getName());
				return profile;
			}
		}
		if (profiles.size() > 0) {

			if (profileName == null) {
				Profile profile = profiles.get(0);
				LOGGER.debug("Profile not specified, taking the first found: "
						+ profile.getName());
				return profile;
			}
			for (Profile profile : profiles) {
				if (profileName.equals(profile.getName())) {
					return profile;
				}

			}
			Profile profile = profiles.get(0);
			LOGGER.warn("Profile: " + profileName
					+ " not found, taking the first one: " + profile.getName());
			return profile;

		}
		// This should never happen ...
		LOGGER.warn("Could not find profile: " + profileName + " in "
				+ profiles);
		return null;
	}

	public static void doProfile(IProject project, InputStream inputStream,
			String profileName) throws CoreException {
		List<Profile> profiles = FormatterProfileStore
				.readProfilesFromStream(new InputSource(inputStream));
		Set<String> skipedOptions = new HashSet<String>();
		skipedOptions.add("org.eclipse.jdt.core.compiler.codegen.targetPlatform");
		skipedOptions.add("org.eclipse.jdt.core.compiler.compliance");
		skipedOptions.add("org.eclipse.jdt.core.compiler.source");
		Profile profile = findProfile(profiles, profileName);
		if (profile != null) {
			IJavaProject javaProject = JavaCore.create(project);

            for (Entry<String, String> entry : profile.getSettings().entrySet()) {
            	if(skipedOptions.contains(entry.getKey()))
            		continue;
				javaProject.setOption(entry.getKey(), entry.getValue());
			}
		}

	}


}
