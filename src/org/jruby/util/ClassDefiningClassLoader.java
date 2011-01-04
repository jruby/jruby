package org.jruby.util;

public interface ClassDefiningClassLoader {

    public Class<?> defineClass(String name, byte[] bytes);

    public Class<?> loadClass(String name) throws ClassNotFoundException;

}
