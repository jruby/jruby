package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaClass;

/**
 * Represents a cache or other mechanism for getting the Ruby-level proxy classes
 * for a given Java class.
 */
public abstract class ProxyCache {
    public ProxyCache(Ruby runtime) {
        this.runtime = runtime;
    }
    
    public abstract JavaClass get(Class cls);
    
    protected final Ruby runtime;
}
