package org.jruby.util;

import java.security.ProtectionDomain;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JRubyClassLoader extends URLClassLoader {
    private final static ProtectionDomain DEFAULT_DOMAIN
            = JRubyClassLoader.class.getProtectionDomain();

    public JRubyClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    // Change visibility so others can see it
    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return super.defineClass(name, bytes, 0, bytes.length, DEFAULT_DOMAIN);
     }

    public Class<?> defineClass(String name, byte[] bytes, ProtectionDomain domain) {
       return super.defineClass(name, bytes, 0, bytes.length, domain);
    }

    // From Stas Garifulin's CompundJarClassLoader. http://jira.codehaus.org/browse/JRUBY-3299
    @Override
    public URL findResource(String resourceName) {
        URL result = super.findResource(resourceName);

        if (result == null && embeddedResourcesEnabled()) {
            for (URL jarUrl : getURLs()) {
                try {
                    InputStream baseInputStream = jarUrl.openStream();

                    try {
                        JarInputStream baseJar = new JarInputStream(baseInputStream);

                        List<String> path = findEmbeddedResource(baseJar, resourceName, new ArrayList<String>(), 0);

                        if (path != null) {
                            result = CompoundJarURLStreamHandler.createUrl(jarUrl, path);
                        }

                    } finally {
                        close(baseInputStream);
                    }
                } catch (IOException ex) {
                    // can't read the stream, keep going
                }
            }
        }

        return result;
    }

    @Override
    public Enumeration<URL> findResources(String resourceName) throws IOException {
        if (!embeddedResourcesEnabled()) { // Quick bailout
            return super.findResources(resourceName);
        }

        final List<URL> embeddedUrls = new ArrayList<URL>();

        for (URL jarUrl : getURLs()) {
            try {
                InputStream baseInputStream = jarUrl.openStream();

                try {
                    JarInputStream baseJar = new JarInputStream(baseInputStream);
                    List<List<String>> result = new ArrayList<List<String>>();

                    collectEmbeddedResources(result, baseJar, resourceName, new ArrayList<String>(), 0);

                    for (List<String> path : result) {
                        embeddedUrls.add(CompoundJarURLStreamHandler.createUrl(jarUrl, path));
                    }
                } finally {
                    close(baseInputStream);
                }
            } catch (IOException ex) {
                // can't read the stream, keep going
            }
        }

        if (embeddedUrls.isEmpty()) {
            return super.findResources(resourceName);
        } else {
            final Enumeration<URL> originalResult = super.findResources(resourceName);

            return new Enumeration<URL>() {
                private Iterator<URL> extendedResult;

                public URL nextElement() {
                    if (extendedResult == null) {
                        return originalResult.nextElement();
                    } else {
                        return extendedResult.next();
                    }
                }

                public boolean hasMoreElements() {
                    if (extendedResult == null) {
                        boolean result = originalResult.hasMoreElements();

                        if (!result) {
                            // original result is consumed, switching to result
                            // from embedded jars processing.
                            extendedResult = embeddedUrls.iterator();
                            result = extendedResult.hasNext();
                        }
                        return result;
                    } else {
                        return extendedResult.hasNext();
                    }
                }
            };
        }
    }

    private List<String> findEmbeddedResource(JarInputStream currentJar, String resourceName, List<String> currentPath,
            int level) throws IOException {

        for (JarEntry entry = currentJar.getNextJarEntry(); entry != null; entry = currentJar.getNextJarEntry()) {

            String entryName = entry.getName();

            List<String> result = null;

            if (entryName.equals(resourceName)) {
                result = new ArrayList<String>(currentPath);
                result.add(resourceName);
            } else if (isJarFile(entry)) {
                String embeddedResourceName = resourceName;
                if (resourceName.startsWith(entryName + "!")) {
                    embeddedResourceName = resourceName.substring(entryName.length() + 1);
                } else {
                    continue;
                }


                JarInputStream embeddedJar = new JarInputStream(currentJar);

                currentPath.add(entryName);
                try {
                    result = findEmbeddedResource(embeddedJar, embeddedResourceName, currentPath, level + 1);
                } finally {
                    currentPath.remove(entryName);
                }
            }

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private void collectEmbeddedResources(List<List<String>> result, JarInputStream currentJar, String resourceName,
            List<String> currentPath, int level) throws IOException {

        for (JarEntry entry = currentJar.getNextJarEntry(); entry != null; entry = currentJar.getNextJarEntry()) {

            String entryName = entry.getName();

            if (entryName.equals(resourceName)) {
                List<String> path = new ArrayList<String>(currentPath);
                path.add(resourceName);

                result.add(path);
            }

            if (isJarFile(entry)) {
                String embeddedResourceName = resourceName;
                if (resourceName.startsWith(entryName + "!")) {
                    embeddedResourceName = resourceName.substring(entryName.length() + 1);
                } else {
                    continue;
                }

                JarInputStream embeddedJar = new JarInputStream(currentJar);

                currentPath.add(entryName);

                try {
                    collectEmbeddedResources(result, embeddedJar, embeddedResourceName, currentPath, level + 1);
                } finally {
                    currentPath.remove(entryName);
                }
            }
        }
    }

    private static boolean embeddedResourcesEnabled() {
        return SafePropertyAccessor.getBoolean("jruby.embedded.resources", false);
    }

    private static boolean isJarFile(JarEntry entry) {
        return !entry.isDirectory() && entry.getName().endsWith(".jar");
    }

    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignore) {
            }
        }
    }
}
