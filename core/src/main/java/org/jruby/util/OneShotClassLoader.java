package org.jruby.util;

import java.security.ProtectionDomain;

/**
 * Represents a class loader designed to load exactly one class.
*/
public class OneShotClassLoader extends ClassLoader implements ClassDefiningClassLoader {
    private final static ProtectionDomain DEFAULT_DOMAIN =
        JRubyClassLoader.class.getProtectionDomain();

    public OneShotClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return super.defineClass(name, bytes, 0, bytes.length, DEFAULT_DOMAIN);
     }
}
