package org.jruby.util;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

// From Stas Garifulin's CompoundJarURLStreamHandler. http://jira.codehaus.org/browse/JRUBY-3299
public class CompoundJarURLStreamHandler extends URLStreamHandler {

    public static final String PROTOCOL = "compoundjar";

    private static final CompoundJarURLStreamHandler instance = new CompoundJarURLStreamHandler();

    public static URL createUrl(URL base, List<String> path) throws MalformedURLException {
        return createUrl(base, path.toArray(new String[0]));
    }

    public static URL createUrl(URL base, String... path) throws MalformedURLException {
        StringBuilder pathBuilder = new StringBuilder();

        if (base.getProtocol().equals("jar")) {
            pathBuilder.append(base.getFile());
        } else {
            pathBuilder.append(base.toExternalForm());
        }

        for (String entry : path) {
            pathBuilder.append("!");

            if (!entry.startsWith("/")) {
                pathBuilder.append("/");
            }

            pathBuilder.append(entry);
        }

        return new URL(PROTOCOL, null, -1, pathBuilder.toString(), instance);
    }

    static class CompoundJarURLConnection extends URLConnection {

        private final URL baseJarUrl;

        private final String[] path;

        CompoundJarURLConnection(URL url) throws MalformedURLException {
            super(url);

            String spec = url.getPath();

            path = spec.split("\\!\\/");
            baseJarUrl = new URL(path[0]);
        }

        @Override
        public void connect() throws IOException {
            connected = true;
        }

        private InputStream openEntry(String[] path, JarInputStream currentJar, int currentDepth) throws IOException {

            final String localPath = path[currentDepth];

            for (JarEntry entry = currentJar.getNextJarEntry(); entry != null; entry = currentJar.getNextJarEntry()) {

                if (entry.getName().equals(localPath)) {
                    if (currentDepth + 1 < path.length) {
                        JarInputStream embeddedJar = new JarInputStream(currentJar);

                        return openEntry(path, embeddedJar, currentDepth + 1);
                    } else {
                        return currentJar;
                    }
                }
            }

            return null;
        }

        private static void close(Closeable resource) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (IOException ignore) {
                }
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {

            InputStream result;

            InputStream baseInputStream = baseJarUrl.openStream();

            if (path.length > 1) {
                try {
                    JarInputStream baseJar = new JarInputStream(baseInputStream);

                    result = openEntry(path, baseJar, 1);
                } catch (IOException ex) {
                    close(baseInputStream);

                    throw ex;
                } catch (RuntimeException ex) {
                    close(baseInputStream);

                    throw ex;
                }
            } else {
                result = baseInputStream;
            }

            if (result == null) {
                throw new FileNotFoundException(url.toExternalForm());
            }

            return result;
        }
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new CompoundJarURLConnection(url);
    }
}