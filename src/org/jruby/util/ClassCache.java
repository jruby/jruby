package org.jruby.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.ast.executable.Script;

/**
 * A Simple cache which maintains a collection of classes that can potentially be shared among
 * multiple runtimes (or whole JVM).
 */
public class ClassCache {
    public interface ClassGenerator {
        Class<Script> generate(ClassLoader classLoader) throws ClassNotFoundException;
    }
    
    /**
     * The ClassLoader this class cache will use for any classes generated through it.  It is 
     * assumed that the classloader provided will be a parent loader of any runtime using it.
     * @param classLoader to use to generate shared classes
     */
    public ClassCache(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    private Map<Object, WeakReference<Class<Script>>> cache = 
        new ConcurrentHashMap<Object, WeakReference<Class<Script>>>();
    private ClassLoader classLoader;
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public Class<Script> cacheClassByKey(Object key, ClassGenerator classGenerator) 
        throws ClassNotFoundException {
        WeakReference<Class<Script>> weakRef = cache.get(key);
        Class<Script> contents = null;
        if (weakRef != null) contents = weakRef.get();
        
        if (weakRef == null || contents == null) {
            contents = classGenerator.generate(getClassLoader());
            cache.put(key, new WeakReference<Class<Script>>(contents));
        }
        
        return contents;
    }
}
