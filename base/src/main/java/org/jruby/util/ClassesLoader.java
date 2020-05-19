package org.jruby.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class ClassesLoader implements Loader {

    private final ClassLoader loader;

    public ClassesLoader(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public URL getResource(String path) {
        return loader.getResource(path);
    }

    @Override
    public Enumeration<URL> getResources(String path) throws IOException {
        return loader.getResources(path);
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return this.loader.loadClass(name);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.loader;
    }
}
