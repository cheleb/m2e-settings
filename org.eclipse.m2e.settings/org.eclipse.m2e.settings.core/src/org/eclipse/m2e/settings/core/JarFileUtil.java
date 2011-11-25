package org.eclipse.m2e.settings.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.embedder.IMaven;

public class JarFileUtil {
	public static List<JarFile> resolveJar(IMaven maven,
			List<Dependency> dependencies, IProgressMonitor monitor)
			throws IOException, CoreException {
		List<JarFile> jarFiles = new ArrayList<JarFile>();
		for (int i = 0; i < dependencies.size(); i++) {
			Dependency dependency = dependencies.get(i);

			// create artifact based on dependency
			Artifact artifact = maven.resolve(dependency.getGroupId(),
					dependency.getArtifactId(), dependency.getVersion(),
					dependency.getType(), dependency.getClassifier(),
					maven.getArtifactRepositories(), monitor);
			jarFiles.add(new JarFile(artifact.getFile()));
		}
		return jarFiles;

	}
}
