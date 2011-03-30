package org.jruby.maven;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import org.jruby.embed.ScriptingContainer;

/**
 * @goal classpath
 */
public class ClasspathMojo extends AbstractJRubyMojo {
    /**
     * @parameter expression="${basedir}"
     */
    protected String baseDirectory = null;

    /**
     * @parameter expression="${project.build.directory}"
     */
    protected String targetDirectory = "target";

    /**
     * @parameter expression="${jruby.classpath.rb}"
     */
    protected String classpathRb = null;

    /**
     * @parameter expression="${jruby.classpath.scope}"
     */
    protected String scope = null;

    /**
     * @parameter expression="${plugin.version}"
     */
    protected String pluginVersion = null;

    public void execute() throws MojoExecutionException {
        String options = "";
        if (classpathRb == null) {
            classpathRb = targetDirectory + System.getProperty("file.separator") + "classpath.rb";
        } else {
            options += " -Djruby.classpath.rb='" + classpathRb + "'";
        }
        if (scope == null) {
            scope = "runtime";
        } else {
            options += " -Djruby.classpath.scope=" + scope;
        }

        Project project = null;
        try {
            project = getProject();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("error resolving dependencies", e);
        }
        Map maven = new HashMap();
        maven.put("basedir", baseDirectory);
        maven.put("classpath_rb", classpathRb);
        maven.put("options", options);
        maven.put("scope", scope);
        maven.put("version", pluginVersion);
        Path p = (Path) project.getReference("maven." + scope + ".classpath");
        if (p == null) {
            throw new MojoExecutionException("error: could not find path maven." + scope + ".classpath from scope " + scope);
        }
        maven.put("classpath", p.toString());

        getLog().info("Creating classpath script: " + classpathRb);
        ScriptingContainer container = new ScriptingContainer();
        container.put("maven", maven);
        container.runScriptlet(getClass().getResourceAsStream("/dump_classpath.rb"), "dump_classpath.rb");
    }
}