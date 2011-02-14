package org.eclipse.m2e.settings.core;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Eclipse configurator to set source encoding.
 * <br />
 * Relies on {@link JavaSourceEncodingConfigurator#ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN} mojo's configuration.
 * @author olivier.nouguier@gmail.com
 *
 */
public class JavaSourceEncodingConfigurator extends AbstractProjectConfigurator {

	/**
	 * Maven compiler Mojo.
	 */
	private static final String ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN = "org.apache.maven.plugins:maven-compiler-plugin";

	/**
	 * 
	 */
	@Override
	public void configure(ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {

		IProject project = request.getProject();

		MavenProject mavenProject = request.getMavenProject();

		Plugin plugin = mavenProject
				.getPlugin(ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN);

		if (plugin == null) {
			System.out.println("Could not force the encoding, consider " + ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN + " <encoding>");
		} else {
			configureEncoding(project, plugin); 
		}

	}

	private boolean configureEncoding(IProject project, Plugin plugin) {

		String encoding = extractEncoding((Xpp3Dom) plugin.getConfiguration());
		if (encoding == null) {
			System.out.println("Could not force the encoding, org.apache.maven.plugins:maven-compiler-plugin found but without <encoding>");
			return false;
		}

		Preferences preferences = Platform
				.getPreferencesService()
				.getRootNode()
				.node("project/" + project.getName()
						+ "/org.eclipse.core.resources/encoding");
		
		String orig = preferences.get("<project>", null);
		
		if(encoding.equals(orig)) {
			System.out.println("Encoding unchanged (" + encoding + ").");
			return false;
		}
		
		preferences.put("<project>", encoding);

		try {
			preferences.flush();
			System.out.println("Encoding changed from " + orig + " to " + encoding + ".");
			return true;
		} catch (BackingStoreException e) {
			System.err.println(e.getMessage());
		}

		return false;
	}

	private String extractEncoding(Xpp3Dom xpp3Dom) {

		if (xpp3Dom == null) {
			return null;
		}
		Xpp3Dom encodingDom = xpp3Dom.getChild("encoding");
		if (encodingDom != null)
			return encodingDom.getValue();
		return null;
	}
}
