
package org.jruby.runtime.load;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jruby.Ruby;
import org.jruby.exceptions.IOError;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Loading of Ruby scripts packaged in Jar files.
 *
 * Usually the Ruby scripts are accompanied by Java class files in the Jar.
 *
 */
public class JarredScript implements Library {
    private final URL file;

    public JarredScript(URL file) {
        this.file = file;
    }

    public void load(Ruby runtime) {
        URL jarFile = file;

        // Make Java class files in the jar reachable from Ruby
        runtime.getJavaSupport().addToClasspath(jarFile);

        try {
            JarInputStream in = new JarInputStream(new BufferedInputStream(jarFile.openStream()));

            Manifest mf = in.getManifest();
            String rubyInit = mf.getMainAttributes().getValue("Ruby-Init");
            if (rubyInit != null) {
                JarEntry entry = in.getNextJarEntry();
                while (entry != null && !entry.getName().equals(rubyInit)) {
                    entry = in.getNextJarEntry();
                }
                if (entry != null) {
                    IRubyObject old = runtime.getGlobalVariables().isDefined("$JAR_URL") ? runtime.getGlobalVariables().get("$JAR_URL") : runtime.getNil();
                    try {
                        runtime.getGlobalVariables().set("$JAR_URL", runtime.newString("jar:" + jarFile + "!/"));
                        runtime.loadScript("init", new InputStreamReader(in), false);
                    } finally {
                        runtime.getGlobalVariables().set("$JAR_URL", old);
                    }
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            throw IOError.fromException(runtime, e);
        } catch (IOException e) {
            throw IOError.fromException(runtime, e);
        }
    }
}
