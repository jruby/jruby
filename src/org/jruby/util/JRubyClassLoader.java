package org.jruby.util;

import java.net.URL;
import java.net.URLClassLoader;

public class JRubyClassLoader extends URLClassLoader {
    public JRubyClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }
    
    public Class defineClass(String name, byte[] bytes) {
       return super.defineClass(name, bytes, 0, bytes.length);
    }
}
