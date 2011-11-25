package org.eclipse.m2e.settings.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.settings.core.model.Formatter;
import org.eclipse.m2e.settings.core.model.JDTUIPref;
import org.eclipse.m2e.settings.core.model.SettingFiles;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ProjectSettingsConfigurator extends AbstractProjectConfigurator {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ProjectSettingsConfigurator.class);

	private static final String ORG_APACHE_MAVEN_PLUGINS_MAVEN_ECLIPSE_PLUGIN = "org.eclipse.maven.plugins:maven-eclipse-plugin";


	@Override
	public void configure(
			ProjectConfigurationRequest projectConfigurationRequest,
			IProgressMonitor monitor) throws CoreException {

		IProject project = projectConfigurationRequest.getProject();

		MavenProject mavenProject = projectConfigurationRequest
				.getMavenProject();

		Plugin eclipsePlugin = mavenProject
				.getPlugin(ORG_APACHE_MAVEN_PLUGINS_MAVEN_ECLIPSE_PLUGIN);
		if (eclipsePlugin == null) {
			LOGGER.info("Could not set eclipse settings, consider org.apache.maven.plugins:maven-eclipse-plugin!");
		} else {
			LOGGER.info("Using org.apache.maven.plugins:maven-eclipse-plugin configuration");
			try {
				if (configureEclipseMeta(project, eclipsePlugin, monitor)) {
					LOGGER.info("Project configured.");
				} else {
					LOGGER.error("Project not configured.");
				}
			} catch (IOException e) {
				LOGGER.error("Failure during settings configuration", e);
			}
		}

	}

	public static MavenRuntime getMavenRuntime(String location)
			throws CoreException {
		MavenRuntimeManager runtimeManager = MavenPlugin
				.getMavenRuntimeManager();

		MavenRuntime runtime = runtimeManager.getRuntime(location);

		return runtime;
	}

	@SuppressWarnings("restriction")
	private void setJavaOptions(InputStream inputStream, String profileName,
			IProgressMonitor monitor, IProject project) throws IOException,
			CoreException {

		if (inputStream != null) {

			List<Profile> profiles = FormatterProfileStore
					.readProfilesFromStream(new InputSource(inputStream));

			Profile profile = findProfile(profiles, profileName);
			if (profile != null) {
				IJavaProject javaProject = JavaCore.create(project);

				Map<String, String> javaOptions = extractJavaOption(javaProject);
				javaOptions.putAll(profile.getSettings());
				javaProject.setOptions(javaOptions);

			}

		}
	}

	@SuppressWarnings("restriction")
	private Profile findProfile(List<Profile> profiles, String profileName) {
		if (profiles == null || profiles.size() == 0) {
			LOGGER.warn("No profiles: " + profiles);
			return null;
		}

		if (profiles.size() == 1) {
			if (profileName != null) {
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
		}
		if (profiles.size() > 0) {

			if (profileName == null) {
				Profile profile = profiles.get(0);
				LOGGER.warn("Profile not specified, taking the first found: "
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

	/**
	 * Use the org.apache.maven.plugins:maven-eclipse-plugin to force the
	 * eclipse settngs.
	 * 
	 * @param project
	 * @param buildPluginMap
	 * @param monitor
	 * @return
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws CoreException
	 */
	private boolean configureEclipseMeta(IProject project,
			Plugin eclipsePlugin, IProgressMonitor monitor) throws IOException,
			CoreException {

		SettingFiles settingFiles = ConfigurationHelper
				.extractSettingsFile(eclipsePlugin);

		List<JarFile> jarFiles = JarFileUtil.resolveJar(maven,
				eclipsePlugin.getDependencies(), monitor);

		applyFormatter(project, monitor, settingFiles, jarFiles);

		applyJdtUIPref(project, monitor, settingFiles, jarFiles);

		return true;
	}

	private void applyJdtUIPref(IProject project, IProgressMonitor monitor,
			SettingFiles settingFiles, List<JarFile> jarFiles)
			throws IOException {
		if (settingFiles.hasJdtUIPref()) {
			JDTUIPref jdtuiPref = settingFiles.getJdtUIPref();
			InputStream contentStream = null;
			try {
				contentStream = openStream(jdtuiPref.getFilename(), jarFiles);
				if (contentStream == null) {
					LOGGER.error("Could not find content for: "
							+ jdtuiPref.getFilename());
				} else {
					ProjectPreferencesUtils.setOtherPreferences(project,
							contentStream, JavaUI.ID_PLUGIN);
				}

			} catch (BackingStoreException e) {
				throw new IOException(e);
			} finally {
				if (contentStream != null) {
					contentStream.close();
				}
			}
		}

	}

	private void applyFormatter(IProject project, IProgressMonitor monitor,
			SettingFiles settingFiles, List<JarFile> jarFiles)
			throws IOException, CoreException {
		if (settingFiles.hasFormatter()) {
			Formatter formatter = settingFiles.getFormatter();
			InputStream contentStream = null;
			try {
				contentStream = openStream(formatter.getFileName(), jarFiles);
				if (contentStream == null) {
					LOGGER.error("Could not find content for: "
							+ formatter.getFileName());
				} else {
					setJavaOptions(contentStream, formatter.getProfile(),
							monitor, project);
				}
			} finally {
				if (contentStream != null) {
					contentStream.close();
				}
			}

		}
	}

	private InputStream openStream(String formatterPath, List<JarFile> jarFiles)
			throws IOException {

		for (JarFile jarFile : jarFiles) {
			ZipEntry entry = jarFile.getEntry(formatterPath);
			if (entry != null) {
				return jarFile.getInputStream(entry);
			}
		}
		LOGGER.warn("Entry " + formatterPath + " not found in " + jarFiles);
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> extractJavaOption(IJavaProject javaProject) {
		return javaProject.getOptions(false);
	}

}
