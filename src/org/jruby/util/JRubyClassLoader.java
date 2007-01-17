package org.jruby.util;

public class JRubyClassLoader extends ClassLoader {
    public JRubyClassLoader(ClassLoader parent) {
        super(parent);
    }
    
    public Class defineClass(String name, byte[] bytes) {
       return super.defineClass(name, bytes, 0, bytes.length);
    }
}
