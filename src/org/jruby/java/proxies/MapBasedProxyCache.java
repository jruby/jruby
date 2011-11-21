package org.jruby.java.proxies;

import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaClass;

/**
 * A simple Map-based cache of proxies.
 */
public class MapBasedProxyCache extends ProxyCache {
    public MapBasedProxyCache(Ruby runtime) {
        super(runtime);
    }

    public JavaClass get(Class cls) {
        JavaClass javaClass = javaClassCache.get(cls);
        
        if (javaClass == null) {
            JavaClass newJavaClass = new JavaClass(runtime, cls);
            javaClass = javaClassCache.putIfAbsent(cls, newJavaClass);
            if (javaClass == null) {
                javaClass = newJavaClass;
            }
        }
        
        return javaClass;
    }
    
    // There's not a compelling reason to keep JavaClass instances in a weak map
    // (any proxies created are [were] kept in a non-weak map, so in most cases they will
    // stick around anyway), and some good reasons not to (JavaClass creation is
    // expensive, for one; many lookups are performed when passing parameters to/from
    // methods; etc.).
    // TODO: faster custom concurrent map
    private final ConcurrentHashMap<Class,JavaClass> javaClassCache =
        new ConcurrentHashMap<Class, JavaClass>(128);
}
