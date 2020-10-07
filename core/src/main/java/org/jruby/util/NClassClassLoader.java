package org.jruby.util;

public class NClassClassLoader extends ClassLoader implements ClassDefiningClassLoader {
    private final int size;
    private int i;

    public NClassClassLoader(ClassLoader parent, int n) {
        super(parent);
        size = n;
    }
    @Override
    public Class<?> defineClass(String name, byte[] bytes) {
        i++;
        return super.defineClass(name, bytes, 0, bytes.length, ClassDefiningJRubyClassLoader.DEFAULT_DOMAIN);
    }

    public boolean isFull() {
        return i >= size;
    }
}
