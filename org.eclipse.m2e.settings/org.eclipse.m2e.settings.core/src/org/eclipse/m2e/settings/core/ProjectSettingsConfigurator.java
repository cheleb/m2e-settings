package org.eclipse.m2e.settings.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.settings.core.model.EclipsePreference;
import org.eclipse.m2e.settings.core.model.Formatter;
import org.eclipse.m2e.settings.core.model.SettingFiles;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


public class ProjectSettingsConfigurator extends AbstractProjectConfigurator {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ProjectSettingsConfigurator.class);

	private static final String ORG_ECLIPSE_M2E_SETTINGS_MAVEN_ECLIPSE_PLUGIN = "org.eclipse.m2e.settings:maven-eclipse-plugin";

	@Override
	public void configure(
			ProjectConfigurationRequest projectConfigurationRequest,
			IProgressMonitor monitor) throws CoreException {

		IProject project = projectConfigurationRequest.getProject();

		MavenProject mavenProject = projectConfigurationRequest
				.getMavenProject();

		Plugin eclipsePlugin = mavenProject
				.getPlugin(ORG_ECLIPSE_M2E_SETTINGS_MAVEN_ECLIPSE_PLUGIN);
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

	
	private void setJavaOptions(InputStream inputStream, String profileName,
			IProject project) throws IOException, CoreException {

		if (inputStream != null) {

			ProfileHelper.doProfile(project, inputStream, profileName);
			

		}
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

		if(settingFiles==null) {
			LOGGER.warn("No settings specified.");
			return false;
		}
		
		List<JarFile> jarFiles = JarFileUtil.resolveJar(maven,
				eclipsePlugin.getDependencies(), monitor);

		applyFormatter(project, settingFiles, jarFiles);

		applyEclipsePreferencesPref(project, settingFiles, jarFiles);

		return true;
	}

	private void applyEclipsePreferencesPref(IProject project,
			SettingFiles settingFiles, List<JarFile> jarFiles)
			throws IOException {
		List<EclipsePreference> eclipsePreferences = settingFiles
				.getEclipsePreferences();

		for (EclipsePreference eclipsePreference : eclipsePreferences) {

			InputStream contentStream = null;
			try {
				contentStream = openStream(eclipsePreference.getFilename(),
						jarFiles);
				if (contentStream == null) {
					LOGGER.error("Could not find content for: "
							+ eclipsePreference.getFilename());
				} else {
					ProjectPreferencesUtils.setOtherPreferences(project,
							contentStream, eclipsePreference.getPref());
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

	private void applyFormatter(IProject project, SettingFiles settingFiles,
			List<JarFile> jarFiles) throws IOException, CoreException {
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
							project);
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

	

}
