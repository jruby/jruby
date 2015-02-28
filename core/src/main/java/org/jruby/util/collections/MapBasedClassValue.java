package org.jruby.util.collections;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple Map-based cache of proxies.
 */
public class MapBasedClassValue<T> extends ClassValue<T> {
    
    public MapBasedClassValue(ClassValueCalculator<T> calculator) {
        super(calculator);
    }

    @Override
    public T get(Class cls) {
        T obj = cache.get(cls);

        if (obj != null) return obj;

        synchronized (this) {
            obj = cache.get(cls);

            if (obj != null) return obj;

            obj = calculator.computeValue(cls);
            cache.put(cls, obj);
        }

        return obj;
    }

    // There's not a compelling reason to keep JavaClass instances in a weak map
    // (any proxies created are [were] kept in a non-weak map, so in most cases they will
    // stick around anyway), and some good reasons not to (JavaClass creation is
    // expensive, for one; many lookups are performed when passing parameters to/from
    // methods; etc.).
    // TODO: faster custom concurrent map
    private final ConcurrentHashMap<Class,T> cache =
        new ConcurrentHashMap<Class, T>(128);
}
