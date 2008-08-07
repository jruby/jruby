package org.jruby.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;

public class JRubyClassLoader extends URLClassLoader {
    private final static ProtectionDomain DEFAULT_DOMAIN
            = JRubyClassLoader.class.getProtectionDomain();

    public JRubyClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    // Change visibility so others can see it
    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return super.defineClass(name, bytes, 0, bytes.length, DEFAULT_DOMAIN);
     }

    public Class<?> defineClass(String name, byte[] bytes, ProtectionDomain domain) {
       return super.defineClass(name, bytes, 0, bytes.length, domain);
    }
}
