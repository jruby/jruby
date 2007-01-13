
package org.jruby.maven;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal install-gems
 */
public class InstallGemsMojo extends AbstractJRubyMojo {
    /**
     * @parameter
     */
    private String gems = null;

    public void execute() throws MojoExecutionException {
        ensureGems(gems.split("[, ]+"));
    }    
}
