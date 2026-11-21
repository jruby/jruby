package org.jruby.util;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClasspathLauncherTest {
    @Test
    public void usesLocalCodeSourceWhenClassResourceIsUnavailable() throws Exception {
        // Equinox uses a local bundle file without a .jar suffix, while its class
        // resources have bundleresource: URLs that a standalone JVM cannot use.
        File bundle = new File(System.getProperty("java.io.tmpdir"), "bundle cache/bundleFile");
        URL resource = new URL(null, "bundleresource://303/org/jruby/main/Main.class", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                throw new UnsupportedOperationException();
            }
        });
        Class<?> embedded = loadWithCodeSource(bundle.toURI().toURL(), resource);

        assertEquals(bundle.getPath(), ClasspathLauncher.getJRubyJar(embedded));
    }

    @Test
    public void supportsExplodedCodeSource() throws Exception {
        File directory = new File(System.getProperty("java.io.tmpdir"));
        Class<?> embedded = loadWithCodeSource(directory.toURI().toURL(), null);

        assertEquals(directory.getPath(), ClasspathLauncher.getJRubyJar(embedded));
    }

    @Test
    public void fallsBackToJarResourceWithoutCodeSource() throws Exception {
        File jar = new File(System.getProperty("java.io.tmpdir"), "jruby complete.jar");
        URL resource = new URL("jar:" + jar.toURI() + "!/org/jruby/main/Main.class");

        assertEquals(jar.getPath(), ClasspathLauncher.getJRubyJar(loadWithCodeSource(null, resource)));
    }

    private Class<?> loadWithCodeSource(URL location, URL resource) throws Exception {
        String name = ClasspathLauncherFixture.class.getName();
        byte[] bytes;
        try (var stream = ClasspathLauncherFixture.class.getResourceAsStream("ClasspathLauncherFixture.class")) {
            bytes = stream.readAllBytes();
        }
        return new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                return resource;
            }

            Class<?> define() {
                return defineClass(name, bytes, 0, bytes.length,
                        new ProtectionDomain(location == null ? null : new CodeSource(location, (Certificate[]) null), null));
            }
        }.define();
    }
}

class ClasspathLauncherFixture {}
