package org.jruby.maven;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal irb
 */
public class IRBMojo extends JRubyMojo {
    public IRBMojo() {
        shouldFork = false;
    }

    public void execute() throws MojoExecutionException {
        String commandString = "-S irb";
        if (args != null) {
            commandString += " " + args;
        }
        executeCmd(commandString);
    }
}
