package org.jruby.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * @goal jruby
 */
public class JRubyMojo extends AbstractJRubyMojo {
    /**
     * @parameter expression="${jruby.args}"
     */
    protected String args = null;

    public void execute() throws MojoExecutionException {
        executeCmd(args);
    }

    protected void executeCmd(String commandline) throws MojoExecutionException {
        Java jruby = jruby(commandline.split("\\s+"));
        jruby.execute();
    }

}