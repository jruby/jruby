package org.jruby.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.jruby.Ruby;

public class ClasspathLauncher {
    
    public static String jrubyCommand(ClassLoader classLoader) {
        String javaHome = SafePropertyAccessor.getProperty("java.home", "");
        // java.home is a JRE
        String javaCmd = javaHome + "/bin/java";
        if (!new File(javaCmd).exists()) {
            // java.home is a JDK
            javaCmd = javaHome + "/jre/bin/java";
        }
        if (!new File(javaCmd).exists()) {
            // can't find it, hope it's in path
            javaCmd = "java";
        }
        StringBuilder command = new StringBuilder().append(javaCmd).append(" -cp ");
        if (classLoader instanceof URLClassLoader) {
            for(URL url : ((URLClassLoader) classLoader).getURLs()) {
                String path = URLUtil.getPlatformPath(url);
                if (path != null) command.append(File.pathSeparatorChar).append(path);
            }
        } else {
            command.append(File.pathSeparatorChar).append(SafePropertyAccessor.getProperty("java.class.path"));
        }
        command.append(" org.jruby.Main");
        return command.toString();
    }

    public static String jrubyCommand(Ruby runtime) {
        return jrubyCommand(runtime.getJRubyClassLoader().getParent());
    }
}