package org.jruby.util;

/**
 * Represents a class loader designed to load exactly one class.
*/
public class OneShotClassLoader extends ClassLoader implements ClassDefiningClassLoader {
    static {
        registerAsParallelCapable();
    }

    public OneShotClassLoader(JRubyClassLoader parent) {
        super(parent);
    }

    public OneShotClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        Class<?> cls = super.defineClass(name, bytes, 0, bytes.length, ClassDefiningJRubyClassLoader.DEFAULT_DOMAIN);
        resolveClass(cls);
        return cls;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    @Override
    public boolean hasDefinedClass(String name) {
        return super.findLoadedClass(name) != null;
    }
}
