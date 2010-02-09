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
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            return super.findClass(className);
        } catch (ClassNotFoundException ex) {
            String resourceName = className.replace('.', '/').concat(".class");

            for (URL jarUrl : getURLs()) {
                try {
                    InputStream baseInputStream = jarUrl.openStream();

                    try {
                        JarInputStream baseJar = new JarInputStream(baseInputStream);

                        InputStream input = performDeepSearch(baseJar, resourceName, 0);

                        if (input != null) {
                            byte[] buffer = new byte[1024];
                            ByteArrayOutputStream output = new ByteArrayOutputStream();

                            for (int count = input.read(buffer); count > 0; count = input.read(buffer)) {
                                output.write(buffer, 0, count);
                            }

                            byte[] data = output.toByteArray();

                            return defineClass(className, data, 0, data.length);
                        }
                    } finally {
                        close(baseInputStream);
                    }
                } catch (IOException innerEx) {
                    // can't read the stream, keep going
                }
            }

            throw new ClassNotFoundException(className, ex);
        }
    }

    @Override
    public URL findResource(String resourceName) {
        URL result = super.findResource(resourceName);

        if (result == null) {
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

                @Override
                public URL nextElement() {
                    if (extendedResult == null) {
                        return originalResult.nextElement();
                    } else {
                        return extendedResult.next();
                    }
                }

                @Override
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

    private InputStream performDeepSearch(JarInputStream currentJar, String resourceName, int level) throws IOException {

        for (JarEntry entry = currentJar.getNextJarEntry(); entry != null; entry = currentJar.getNextJarEntry()) {

            String entryName = entry.getName();

            if (level > 0 && entryName.equals(resourceName)) {
                return currentJar;
            } else if (isJarFile(entry)) {
                JarInputStream embeddedJar = new JarInputStream(currentJar);

                InputStream result = performDeepSearch(embeddedJar, resourceName, level + 1);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private List<String> findEmbeddedResource(JarInputStream currentJar, String resourceName, List<String> currentPath,
            int level) throws IOException {

        for (JarEntry entry = currentJar.getNextJarEntry(); entry != null; entry = currentJar.getNextJarEntry()) {

            String entryName = entry.getName();

            List<String> result = null;

            if (level > 0 && entryName.equals(resourceName)) {
                result = new ArrayList<String>(currentPath);
                result.add(resourceName);
            } else if (isJarFile(entry)) {
                String embeddedResourceName = resourceName;
                if (resourceName.startsWith(entryName + "!")) {
                    embeddedResourceName = resourceName.substring(entryName.length() + 1);
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

            if (level > 0 && entryName.equals(resourceName)) {
                List<String> path = new ArrayList<String>(currentPath);
                path.add(resourceName);

                result.add(path);
            }

            if (isJarFile(entry)) {
                String embeddedResourceName = resourceName;
                if (resourceName.startsWith(entryName + "!")) {
                    embeddedResourceName = resourceName.substring(entryName.length() + 1);
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
