package org.jruby.maven;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * @goal rake
 */
public class RakeMojo extends AbstractJRubyMojo {
    /**
     * @parameter expression="Rakefile"
     */
    private String rakefile;
    
    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter
     */
    private String script = null;

    /**
     * @parameter
     */
    private String args = null;

    public void execute() throws MojoExecutionException {
        ensureGem("rake");
        List allArgs = new ArrayList();
        allArgs.add("--command");
        allArgs.add("rake");
        if (script != null) {
            File scriptFile = new File(outputDirectory, "rake_script.rb");
            try {
                FileWriter writer = new FileWriter(scriptFile);
                writer.write(script);
                writer.close();
            } catch (IOException io) {
                throw new MojoExecutionException("error writing temporary script");
            }
            allArgs.add("-f");
            allArgs.add(scriptFile.getPath());
        }
        allArgs.addAll(Arrays.asList(args.split("\\s+")));
        Java jruby = jruby((String[]) allArgs.toArray(new String[allArgs.size()]));
        jruby.execute();
    }
}