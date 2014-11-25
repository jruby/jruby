package org.jruby.util;

import java.security.ProtectionDomain;

/**
 * Represents a class loader designed to load exactly one class.
*/
public class OneShotClassLoader extends ClassLoader implements ClassDefiningClassLoader {

    public OneShotClassLoader(JRubyClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return super.defineClass(name, bytes, 0, bytes.length, JRubyClassLoader.DEFAULT_DOMAIN);
     }
}
