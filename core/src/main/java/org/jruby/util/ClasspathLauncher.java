package org.jruby.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jruby.main.Main;
import org.jruby.Ruby;

public class ClasspathLauncher {

    // Try and return reasonable java command which can be launched as a process.
    private static String getJavaCommand() {
        String home = SafePropertyAccessor.getProperty("java.home", "");
        String command = home + "/bin/java";                                       // java.home is JRE

        if (!new File(command).exists()) command = home + "/jre/bin/java";         // java.home is JDK
        if (!new File(command).exists()) command = "java";                         // hope java is in path

        return command;
    }

    // This only retrieves the name of JRuby jar used (which seemingly only ever gets hit for
    // jruby-complete.jar.
    // Approach from: https://www.baeldung.com/java-full-path-of-jar-from-class
    private static String getJRubyJar() {
        Class<Main> clazz = Main.class;
        URL classResource = clazz.getResource(clazz.getSimpleName() + ".class");

        if (classResource == null) return null;

        String jarPath = classResource.toString().replaceAll("^jar:(file:.*[.]jar)!/.*", "$1");

        try {
            return Paths.get(new URL(jarPath).toURI()).toString();
        } catch (URISyntaxException | MalformedURLException | FileSystemNotFoundException e) {
            return null;
        }
    }

    public static String jrubyCommand(ClassLoader classLoader) {
        StringBuilder command = new StringBuilder().append(getJavaCommand()).append(" -cp ");
        String mainJar = getJRubyJar();

        if (classLoader instanceof URLClassLoader) {
            getClassPathEntriesJava8AndBelow((URLClassLoader) classLoader, command, mainJar);
        } else {
            // We fail back to just using what is set by the system property.  This might be missing entries
            // otherwise added during runtime.  This might be fine since if we loaded them once from previous
            // execution we probably will again in the new execution.
            String path = SafePropertyAccessor.getProperty("java.class.path");
            if (mainJar != null) {
                command.append(mainJar);
                if (path != null) command.append(File.pathSeparatorChar).append(path);
            } else {
                command.append(path);
            }
        }

        return command.append(" org.jruby.main.Main").toString();
    }

    // FIXME: Java 8 is capable of building up complete list of jars from classloaders but Java 9+ is not without
    // the use of the instrumentation APIs.
    private static void getClassPathEntriesJava8AndBelow(URLClassLoader classLoader, StringBuilder command, String mainJar) {
        List<String> entries = new ArrayList<>();
        if (mainJar != null) entries.add(mainJar);

        for(URL url : classLoader.getURLs()) {
            String path = URLUtil.getPlatformPath(url);
            if (path != null) entries.add(path);
        }

        command.append(String.join(File.pathSeparator, entries));
    }

    public static String jrubyCommand(Ruby runtime) {
        return jrubyCommand(runtime.getJRubyClassLoader().getParent());
    }
}