package org.jruby.util;

public interface ClassDefiningClassLoader {

    Class<?> defineClass(String name, byte[] bytes);

    Class<?> loadClass(String name) throws ClassNotFoundException;

    boolean hasDefinedClass(String name);

    default ClassLoader asClassLoader() { return (ClassLoader) this; }

}
