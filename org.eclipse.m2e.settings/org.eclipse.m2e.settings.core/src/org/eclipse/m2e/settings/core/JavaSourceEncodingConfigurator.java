package org.eclipse.m2e.settings.core;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Eclipse configurator to set source encoding. <br />
 * Relies on
 * {@link JavaSourceEncodingConfigurator#ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN}
 * mojo's configuration.
 * 
 * @author olivier.nouguier@gmail.com
 * 
 */
public class JavaSourceEncodingConfigurator extends AbstractProjectConfigurator {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(JavaSourceEncodingConfigurator.class);

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

		MavenProject mavenProject = request.getMavenProject();

		Plugin plugin = mavenProject
				.getPlugin(ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN);

		if (plugin == null) {
			LOGGER.warn("Could not force the encoding, consider "
					+ ORG_APACHE_MAVEN_PLUGINS_MAVEN_COMPILER_PLUGIN
					+ " <encoding>");
		} else {
			configureEncoding(request, plugin);
		}

	}

	private boolean configureEncoding(ProjectConfigurationRequest request,
			Plugin plugin) {

		IProject project = request.getProject();
		String encoding = extractEncoding((Xpp3Dom) plugin.getConfiguration());
		if (encoding == null) {
			IResource pomFile = request.getPom();
			addMarker(
					pomFile,
					IMarker.PROBLEM,
					"No encoding found, org.apache.maven.plugins:maven-compiler-plugin found but without <encoding>",
					1, IMarker.PRIORITY_NORMAL, false);
			LOGGER.info("Could not force the encoding, no encoding found, org.apache.maven.plugins:maven-compiler-plugin found but without <encoding>");
			LOGGER.warn("Could not force the encoding, no encoding found, org.apache.maven.plugins:maven-compiler-plugin found but without <encoding>");
			return false;
		}

		Preferences preferences = Platform
				.getPreferencesService()
				.getRootNode()
				.node("project/" + project.getName()
						+ "/org.eclipse.core.resources/encoding");

		String orig = preferences.get("<project>", null);
		try {
			IMarker iMarker = findMarker(
					request.getPom(),
					IMarker.PROBLEM,
					"No encoding found, org.apache.maven.plugins:maven-compiler-plugin found but without <encoding>",
					1, IMarker.PRIORITY_NORMAL, false);
			if (iMarker != null) {
				iMarker.delete();
			}
		} catch (CoreException e1) {
			LOGGER.error(request.getMavenProject().toString(), e1);
		}

		if (encoding.equals(orig)) {
			LOGGER.debug("Encoding unchanged (" + encoding + ").");
			return false;
		}

		preferences.put("<project>", encoding);

		try {
			preferences.flush();
			LOGGER.debug("Encoding changed from " + orig + " to " + encoding
					+ ".");
			return true;
		} catch (BackingStoreException e) {
			LOGGER.error("Configuring encoding error", e);
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

	private IMarker addMarker(IResource resource, String type, String message,
			int lineNumber, int severity, boolean isTransient) {
		IMarker marker = null;
		try {
			if (resource.isAccessible()) {
				// mkleint: this strongly smells like some sort of workaround
				// for a problem with bad marker cleanup.
				// adding is adding and as such shall always be performed.
				marker = findMarker(resource, type, message, lineNumber,
						severity, isTransient);
				if (marker != null) {
					// This marker already exists
					return marker;
				}
				marker = resource.createMarker(type);
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.SEVERITY, severity);
				marker.setAttribute(IMarker.TRANSIENT, isTransient);

				if (lineNumber == -1) {
					lineNumber = 1;
				}
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				LOGGER.debug("Created marker '{}' on resource '{}'.", message,
						resource.getFullPath());
			}
		} catch (CoreException ex) {
			LOGGER.error("Unable to add marker; " + ex.toString(), ex); //$NON-NLS-1$
		}
		return marker;
	}

	private IMarker findMarker(IResource resource, String type, String message,
			int lineNumber, int severity, boolean isTransient)
			throws CoreException {
		IMarker[] markers = resource.findMarkers(type,
				false /* includeSubtypes */, IResource.DEPTH_ZERO);
		if (markers == null || markers.length == 0) {
			return null;
		}
		for (IMarker marker : markers) {
			if (eq(message, marker.getAttribute(IMarker.MESSAGE))
					&& eq(lineNumber, marker.getAttribute(IMarker.LINE_NUMBER))
					&& eq(severity, marker.getAttribute(IMarker.SEVERITY))
					&& eq(isTransient, marker.getAttribute(IMarker.TRANSIENT))) {
				return marker;
			}
		}
		return null;
	}

	private static <T> boolean eq(T a, T b) {
		if (a == null) {
			if (b == null) {
				return true;
			}
			return false;
		}
		return a.equals(b);
	}
}
