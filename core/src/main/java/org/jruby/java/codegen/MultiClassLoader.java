package org.jruby.java.codegen;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class MultiClassLoader
    extends ClassLoader
{

    private final List<ClassLoader> classLoaders = new LinkedList<>();

    public MultiClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void addClassLoader(ClassLoader loader) {
        classLoaders.add(loader);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        for (ClassLoader classLoader : classLoaders) {
            URL resource = classLoader.getResource(path);
            if (resource != null) {
                return classLoader.loadClass(name);
            }
        }
        return super.findClass(name);
    }

    protected URL findResource(String name) {
        for (ClassLoader classLoader : classLoaders) {
            URL resource = classLoader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return super.findResource(name);
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        Vector<URL> vector = new Vector<URL>();
        for (ClassLoader classLoader : classLoaders) {
            Enumeration<URL> enumeration = classLoader.getResources(name);
            while (enumeration.hasMoreElements()) {
                vector.add(enumeration.nextElement());
            }
        }
        Enumeration<URL> enumeration = super.findResources(name);
        while (enumeration.hasMoreElements()) {
            vector.add(enumeration.nextElement());
        }
        return vector.elements();
    }

}
