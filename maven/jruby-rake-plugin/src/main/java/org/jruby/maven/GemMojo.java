
package org.jruby.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * @goal gem
 */
public class GemMojo extends AbstractJRubyMojo {
    /**
     * @parameter
     */
    private String args = null;

    public void execute() throws MojoExecutionException {
        String commandString = "--command gem";
        if (args != null) {
            commandString += " " + args;
        }
        Java jruby = jruby(commandString.split("\\s+"));
        jruby.execute();        
    }
}
