package org.jruby.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Created by cmeier on 7/30/15.
 */
public interface Loader
{

    URL getResource(String path);// throws IOException;

    Enumeration<URL> getResources(String path) throws IOException;

    Class<?> loadClass(String name) throws ClassNotFoundException;

    ClassLoader getClassLoader();
}
