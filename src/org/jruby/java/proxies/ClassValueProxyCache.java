package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaClass;

/**
 * A proxy cache that uses Java 7's ClassValue.
 */
public class ClassValueProxyCache extends ProxyCache {
    public ClassValueProxyCache(Ruby runtime) {
        super(runtime);
    }
    
    public JavaClass get(Class cls) {
        return (JavaClass)proxy.get(cls);
    }
    
    private final ClassValue proxy = new ClassValue() {
        @Override
        protected Object computeValue(Class type) {
            return new JavaClass(runtime, type);
        }
    };
}
