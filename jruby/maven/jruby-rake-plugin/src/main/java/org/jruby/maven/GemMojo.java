package org.jruby.maven;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal gem
 */
public class GemMojo extends JRubyMojo {
    public void execute() throws MojoExecutionException {
        String commandString = "--command gem";
        if (args != null) {
            commandString += " " + args;
        }
        executeCmd(commandString);
    }
}
