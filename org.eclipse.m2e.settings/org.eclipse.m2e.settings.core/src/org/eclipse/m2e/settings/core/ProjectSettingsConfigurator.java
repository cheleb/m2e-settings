package org.eclipse.m2e.settings.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.settings.core.model.SettingFile;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectSettingsConfigurator extends ProjectConfigurator {

	
	private final static Logger LOGGER = LoggerFactory.getLogger(ProjectSettingsConfigurator.class);
	
	private static final String ORG_APACHE_MAVEN_PLUGINS_MAVEN_ECLIPSE_PLUGIN = "org.apache.maven.plugins:maven-eclipse-plugin";
	private static final Pattern PREF_PATTERN = Pattern
			.compile("^\\.settings/(.*)\\.prefs$");

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
			System.out.println("Could not set eclipse settings, consider org.apache.maven.plugins:maven-eclipse-plugin!");
			LOGGER.info("Could not set eclipse settings, consider org.apache.maven.plugins:maven-eclipse-plugin!");
		} else {
			LOGGER.debug("Configuring from org.apache.maven.plugins:maven-eclipse-plugin!");
			configureEclipseMeta(project, eclipsePlugin, monitor);
		}

		if ("jar".equals(mavenProject.getPackaging())) {

			IVirtualComponent component = ComponentCore
					.createComponent(project);
			if (component == null) {
				System.out.println("Not a WST component!");
			} else {
				IVirtualFolder rootFolder = component.getRootFolder();
				addClassesAndResourcesToWTPDeployment(project, mavenProject,
						rootFolder, monitor);
			}
		}
	}

	public static MavenRuntime getMavenRuntime(String location)
			throws CoreException {
		MavenRuntimeManager runtimeManager = MavenPlugin.getDefault()
				.getMavenRuntimeManager();

		MavenRuntime runtime = runtimeManager.getRuntime(location);

		return runtime;
	}

	private void setJavaOptions(InputStream inputStream,
			IProgressMonitor monitor, IProject project) throws IOException {

		IJavaProject javaProject = JavaCore.create(project);

		Map<String, String> javaOptions = extractJavaOption(javaProject);

		if (inputStream != null) {
			Reader inStreamReader = new InputStreamReader(inputStream,
					Charset.forName("utf8"));
			try {
				Properties properties = new Properties();
				properties.load(inStreamReader);
				for (Enumeration<Object> e = properties.keys(); e
						.hasMoreElements(); /* NO-OP */) {
					String key = (String) e.nextElement();
					javaOptions.put(key, properties.getProperty(key));
				}

			} finally {
				try {
					inStreamReader.close();
					inputStream.close();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}

			}
		}
		javaProject.setOptions(javaOptions);
	}

	/**
	 * Use the org.apache.maven.plugins:maven-eclipse-plugin to force the
	 * eclipse settngs.
	 * 
	 * @param project
	 * @param buildPluginMap
	 * @param monitor
	 * @return
	 */
	private boolean configureEclipseMeta(IProject project,
			Plugin eclipsePlugin, IProgressMonitor monitor) {

		URLClassLoader classLoader = configureClassLoader(
				eclipsePlugin.getDependencies(), monitor);

		List<SettingFile> settingFiles = extractSettingFile((Xpp3Dom) eclipsePlugin
				.getConfiguration());

		for (SettingFile settingFile : settingFiles) {
			InputStream contentStream = openStream(settingFile, classLoader);
			if (contentStream == null) {
				System.err.println("Could not find content for: " + settingFile);
			} else {
				try {
					if (".settings/org.eclipse.jdt.core.prefs"
							.equals(settingFile.getPath())) {
						setJavaOptions(contentStream, monitor, project);
					} else {
						Matcher matcher = PREF_PATTERN.matcher(settingFile
								.getPath());
						if (matcher.matches()) {
							String pref = matcher.group(1);
							setOtherPreferences(project, contentStream, pref);
						}
					}
				} catch (IOException e) {
					System.err.println(e.getMessage());
				} catch (BackingStoreException e) {
					System.err.println(e.getMessage());
				} finally {
					if (contentStream != null) {
						try {
							contentStream.close();
						} catch (IOException e) {
							System.err.println(e.getMessage());
						}
					}
				}
			}
		}

		deleteFolder(project, new Path("bin"), monitor);
		deleteFolder(project, new Path("build"), monitor);
		deleteFolder(project, new Path("src/META-INF"), monitor);
		return true;
	}

	private void insureClassPathEntry(IProgressMonitor monitor,
			IProject project, String srcPath) throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry prevClasspathEntries[] = javaProject.getRawClasspath();
		IPath path = project.getFolder(srcPath).getFullPath();

		//
		// FIXME This is never working ... the custom src folder are never in
		// this list ...
		//
		for (int i = 0; i < prevClasspathEntries.length; i++) {
			IClasspathEntry iClasspathEntry = prevClasspathEntries[i];
			if (iClasspathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath ePath = iClasspathEntry.getPath();
				if (ePath.equals(path))
					return;
			}
		}
		IClasspathEntry newClasspathEntries[] = new IClasspathEntry[prevClasspathEntries.length + 1];
		System.arraycopy(prevClasspathEntries, 0, newClasspathEntries, 0,
				prevClasspathEntries.length);
		IClasspathEntry updated = JavaCore.newSourceEntry(path);

		newClasspathEntries[prevClasspathEntries.length] = updated;
		javaProject.setRawClasspath(newClasspathEntries, monitor);
	}

	/**
	 * Delete a folder, in this project.
	 * 
	 * @param project
	 * @param path
	 * @param monitor
	 */
	private void deleteFolder(IProject project, IPath path,
			IProgressMonitor monitor) {
		IFolder bin = project.getFolder(path);
		if (bin.exists()) {
			System.err.println(path + " was existing ...");
			try {
				bin.delete(true, monitor);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private InputStream openStream(SettingFile settingFile,
			URLClassLoader classLoader) {
		if (settingFile.getContents() != null) {
			return new ByteArrayInputStream(settingFile.getContents()
					.getBytes());
		}
		if (settingFile.getUrl() != null) {
			try {
				return settingFile.getUrl().openStream();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				return null;
			}
		}
		if (settingFile.getLocation() != null) {

			if (classLoader == null) {
				System.out.println("No classloader to provide configuration file!");
				return null;
			}
			return classLoader.getResourceAsStream(settingFile.getLocation());
		}
		System.out.println("Could not find the stream to provide configuration file: "
				+ settingFile);
		return null;
	}

	private List<SettingFile> extractSettingFile(Xpp3Dom configuration) {
		Xpp3Dom additionalConfig = configuration.getChild("additionalConfig");
		if (additionalConfig == null) {
			return new ArrayList<SettingFile>();
		}
		Xpp3Dom[] files = additionalConfig.getChildren("file");

		return SettingFile.fromXpp3doms(files);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> extractJavaOption(IJavaProject javaProject) {
		return javaProject.getOptions(false);
	}

	/**
	 * List directory contents for a resource folder. Not recursive. This is
	 * basically a brute-force implementation. Works for regular files and also
	 * JARs.
	 * 
	 * @author Greg Briggs
	 * @param clazz
	 *            Any java class that lives in the same place as the resources
	 *            you want.
	 * @param path
	 *            Should end with "/", but not start with one.
	 * @return Just the name of each member item, not the full paths.
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	String[] getResourceListing(URL jarURL, String pathToConfiguationFiles) {

		String jarPath;
		/* A JAR path */
		if (jarURL.getProtocol().equals("jar")) {
			jarPath = jarURL.getPath().substring(5,
					jarURL.getPath().indexOf("!")); // strip out only the JAR
													// file
		} else if (jarURL.getProtocol().equals("file")) {
			jarPath = jarURL.getPath();
		} else {
			throw new UnsupportedOperationException(
					"Cannot list files for URL " + jarURL);
		}

		JarFile jar;
		try {
			jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new String[] {};
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new String[] {};
		}
		Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in
														// jar
		Set<String> result = new HashSet<String>(); // avoid duplicates in case
													// it is a subdirectory
		while (entries.hasMoreElements()) {
			String name = entries.nextElement().getName();
			if (name.startsWith(pathToConfiguationFiles)) { // filter according
															// to the path
				String entry = name.substring(pathToConfiguationFiles.length());
				int checkSubdir = entry.indexOf("/");
				if (checkSubdir >= 0) {
					// if it is a subdirectory, we just return the directory
					// name
					entry = entry.substring(0, checkSubdir).trim();
				}
				if (!"".equals(entry))
					result.add(entry);
			}
		}
		return result.toArray(new String[result.size()]);

	}

}
