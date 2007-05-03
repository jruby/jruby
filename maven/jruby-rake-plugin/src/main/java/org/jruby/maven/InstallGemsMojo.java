
package org.jruby.maven;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal install-gems
 */
public class InstallGemsMojo extends AbstractJRubyMojo {
    /**
     * @parameter expression="${jruby.gems}"
     */
    private String gems = null;

    public void execute() throws MojoExecutionException {
        ensureGems(gems.split("[, ]+"));
    }    
}
