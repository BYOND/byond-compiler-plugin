package com.byond.maven.plugin.compiler;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class CompileMojo extends AbstractMojo {
	/**
	 * The final name of the archive.
	 */
	@Parameter(property = "finalName", defaultValue = "${project.build.finalName}", required = true)
	private String finalName;
	
	@Component
	private MavenProject project;

	/**
	 * The output directory to write the archive to.
	 */
	@Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}", required = true)
	private File outputDirectory;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
	}
}
