package org.eclipse.m2e.settings.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public abstract class ProjectConfigurator extends org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator {

	
	/*
	 * Create a classloader based on the PMD plugin dependencies, if any
	 */

	protected URLClassLoader configureClassLoader(
			List<Dependency> dependencies, IProgressMonitor monitor) {
		// Let's default to the current context classloader
		URLClassLoader classLoader = null;

		if (dependencies != null && dependencies.size() > 0) {
			List<URL> jars = new LinkedList<URL>();
			for (int i = 0; i < dependencies.size(); i++) {
				Dependency dependency = dependencies.get(i);

				// create artifact based on dependency
				Artifact artifact = null;
				try {
					artifact = maven.resolve(dependency.getGroupId(),
							dependency.getArtifactId(),
							dependency.getVersion(), dependency.getType(),
							dependency.getClassifier(),
							maven.getArtifactRepositories(), monitor);
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					System.err.println(e1.getMessage());
				}

				// add artifact and its dependencies to list of jars
				if (artifact != null && artifact.isResolved()) {
					try {
						jars.add(artifact.getFile().toURI().toURL());
					} catch (MalformedURLException e) {
						System.err.println("Could not create URL for artifact: "
								+ artifact.getFile());
					}
				}
			}
			classLoader = new URLClassLoader(jars.toArray(new URL[0]),
					classLoader);
		}

		return classLoader;
	}

	protected Preferences getPrefences(IProject project, String pref) {
		return Platform.getPreferencesService().getRootNode()
				.node("project/" + project.getName() + "/" + pref);
	}

	protected void setOtherPreferences(IProject project,
			InputStream inputStream, String pref) throws IOException,
			BackingStoreException {

		if (inputStream == null) {
			System.out.println("No settings for: " + pref);
			return;
		}
		Preferences preferences = getPrefences(project, pref);

		Reader inStreamReader = new InputStreamReader(inputStream,
				Charset.forName("utf8"));
		Properties properties = new Properties();
		properties.load(inStreamReader);
		for (Enumeration<Object> e = properties.keys(); e.hasMoreElements(); /*
																			 * NO-
																			 * OP
																			 */) {
			String key = (String) e.nextElement();
			preferences.put(key, properties.getProperty(key));
		}

		preferences.flush();

		System.out.println("Updated preferences: " + pref);
	}

	protected void addClassesAndResourcesToWTPDeployment(IProject project,
			MavenProject mavenProject, IVirtualFolder rootFolder,
			IProgressMonitor monitor) throws CoreException {
		
		IContainer srcFolder = rootFolder.getUnderlyingFolder();
		if (srcFolder.exists()) {
			if ("src".equals(srcFolder.getName())) {
				System.err.println("Removed " + srcFolder.getName()
						+ " from wtp deployment!");
				rootFolder.removeLink(new Path("src"), IVirtualResource.FOLDER,
						monitor);
			}

		}

		if (project.getFolder("src/main/java").exists()) {
			rootFolder.createLink(new Path("src/main/java"),
					IVirtualResource.FOLDER, monitor);
		}

		List<Resource> resources = mavenProject.getResources();

		if (resources.isEmpty()) {
			if (project.getFolder("src/main/resources").exists()) {
				rootFolder.createLink(new Path("src/main/resources"),
						IVirtualResource.FOLDER, monitor);
				System.out.println("Linked src/main/resources to "
						+ rootFolder.getName() + "  from wtp deployment!");
			}
		} else {
			String basedir = project.getLocation().toString();
			for (Resource resource : resources) {
				String pathAsString = resource.getDirectory();

				pathAsString = WTPMavenHelper.getProjectRelativeRelativePath(
						pathAsString, basedir);
				rootFolder.createLink(new Path(pathAsString),
						IVirtualResource.FOLDER, monitor);
				System.out.println("Linked " + pathAsString + "  to "
						+ rootFolder.getName() + "  from wtp deployment!");
			}
		}

	}

	public static void addNature(IProject project, String natureId,
			IProgressMonitor monitor) throws CoreException {
		if (!project.hasNature(natureId)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = natureId;
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		}
	}

	public static void addBuildCommand(IProject project, String builderName,
			IProgressMonitor monitor) throws CoreException {

		IProjectDescription description = project.getDescription();
		ICommand[] prevCommands = description.getBuildSpec();
		for (ICommand command : prevCommands) {
			if (builderName.equals(command.getBuilderName())) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[prevCommands.length + 1];
		System.arraycopy(prevCommands, 0, newCommands, 0, prevCommands.length);
		newCommands[prevCommands.length] = description.newCommand();
		newCommands[prevCommands.length].setBuilderName(builderName);
		description.setBuildSpec(newCommands);
		project.setDescription(description, monitor);

	}

}
