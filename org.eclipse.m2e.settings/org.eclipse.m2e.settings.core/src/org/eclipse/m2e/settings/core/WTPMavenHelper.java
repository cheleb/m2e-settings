package org.eclipse.m2e.settings.core;


import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;

public class WTPMavenHelper {

	private static final String ORG_CODEHAUS_MOJO_WEBSTART_WEBSTART_MAVEN_PLUGIN = "org.codehaus.mojo.webstart:webstart-maven-plugin";

	/**
	 * 
	 * 
	 * 
	 * &lt;webResources><br />
	 * &nbsp; &lt;resource><br />
	 * &nbsp;&nbsp; &lt;directory>target/elnweb&lt;/directory><br />
	 * &nbsp;&nbsp; &lt;filtering>false&lt;/filtering><br />
	 * &nbsp; &lt;/resource><br />
	 * &lt;/webResources>
	 * 
	 * @param buildDir
	 * 
	 * 
	 * @param buildPluginMap
	 * @param monitor
	 * @param rootFolder
	 * @throws CoreException
	 */
	public static void deployExtraWebResources(MavenProject mavenProject,
			IProgressMonitor monitor, IVirtualFolder rootFolder, IPath src)
			throws CoreException {

		Plugin warPlugin = mavenProject
				.getPlugin("org.apache.maven.plugins:maven-war-plugin");

		if (warPlugin != null) {
			Xpp3Dom configuration = (Xpp3Dom) warPlugin.getConfiguration();

			if (configuration == null) {
				return;
			}

			Xpp3Dom webResources[] = configuration.getChildren("webResources");

			for (int i = 0; i < webResources.length; i++) {
				Xpp3Dom webResource = webResources[i];
				Xpp3Dom resource = webResource.getChild("resource");
				if (resource == null) {
					continue;
				}
				Xpp3Dom directory = resource.getChild("directory");
				if (directory == null) {
					continue;
				}
				String path = directory.getValue();
				if (path == null) {
					continue;
				}

				publishResources(rootFolder, new Path(path), src, monitor);
			
			}
		}
	}

	public static String getProjectRelativeRelativePath(String path,
			String buildDir) {
		path = path.replace('\\', '/');
		buildDir = buildDir.replace('\\', '/');
		int indexOfBuildDir = path.indexOf(buildDir);
		if (indexOfBuildDir == 0) {
			path = path.substring(buildDir.length());
		}
		if (path.startsWith("/")) {
			return path.substring(1);
		}
		return path;
	}

	public static void deployTargetJNLP(MavenProject mavenProject,
			IProgressMonitor monitor, IVirtualFolder rootFolder,
			IPath src) throws CoreException {

		String buildDir = mavenProject.getBasedir().getAbsolutePath();

		Plugin plugin = mavenProject
				.getPlugin(ORG_CODEHAUS_MOJO_WEBSTART_WEBSTART_MAVEN_PLUGIN);
		if (plugin != null) {
			IVirtualFolder webstart = rootFolder.getFolder("webstart");

			Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

			if (configuration == null) {
				return;
			}

			Xpp3Dom workDirectory = configuration.getChild("workDirectory");

			if (workDirectory == null) {
				throw new CoreException(new Status(Status.ERROR,
						SettingsActivator.PLUGIN_ID, "Could not find JNLP directory!"));
			}
			String jnlp = getProjectRelativeRelativePath(
					workDirectory.getValue(), buildDir);
			Path jnlpPath = new Path(jnlp);
			jnlpPath = new Path(jnlpPath.toOSString());

			publishResources(webstart, jnlpPath, src, monitor);

		}
	}

	private static void publishResources(IVirtualFolder vfolder, Path newPath,
			IPath src, IProgressMonitor monitor) throws CoreException {
		
		System.out.println("o Adding " + newPath + " as " + vfolder.getName());
		boolean shouldIAddEntry = true;
		if (vfolder.exists()) {
			System.out.println(" \t- Virtual folder exist: " + vfolder.getName());
			IContainer[] deployedFolders = vfolder.getUnderlyingFolders();
			for (int i = 0; i < deployedFolders.length; i++) {
				IContainer deployedFolder = deployedFolders[i];

				IPath deployedFolderPath = deployedFolder
						.getProjectRelativePath();
				if (src.equals(deployedFolderPath)) {
					// Ignore
					continue;
				}
				System.out.println(" \t\t- " + deployedFolderPath);

				if (deployedFolderPath.equals(newPath)) {
					System.out.println(" \t\t\t- Targeted path " + newPath
							+ " alreaddy added");
					shouldIAddEntry = false;
				} else {
					if (src.isPrefixOf(deployedFolderPath)) {
						System.out.println(" \t\t\t- Overlap in " + vfolder.getName() + " with " + src);
					} else {
						System.out.println(" \t\t\t- Found an old entry for targeted path: "
								+ deployedFolderPath);
						vfolder.removeLink(deployedFolderPath,
								IVirtualResource.FOLDER, monitor);
					}
				}
			}
		}
		if (shouldIAddEntry) {
			System.out.println("Create new entry for path: " + newPath);
			vfolder.createLink(newPath, IVirtualResource.FOLDER, monitor);
		}

	}
}
