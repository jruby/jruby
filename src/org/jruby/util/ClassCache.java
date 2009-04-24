package org.jruby.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Simple cache which maintains a collection of classes that can potentially be shared among
 * multiple runtimes (or whole JVM).
 */
public class ClassCache<T> {
    private final AtomicInteger classLoadCount = new AtomicInteger(0);
    private final AtomicInteger classReuseCount = new AtomicInteger(0);
    private final ReferenceQueue referenceQueue = new ReferenceQueue();
    private final Map<Object, KeyedClassReference> cache =
        new ConcurrentHashMap<Object, KeyedClassReference>();
    private final ClassLoader classLoader;
    private final int max;
    
    /**
     * The ClassLoader this class cache will use for any classes generated through it.  It is 
     * assumed that the classloader provided will be a parent loader of any runtime using it.
     * @param classLoader to use to generate shared classes
     */
    public ClassCache(ClassLoader classLoader, int max) {
        assert classLoader != null : "Null classloader provided for ClassCache";
        this.classLoader = classLoader;
        this.max = max;
    }
    
    public ClassCache(ClassLoader classLoader) {
        this(classLoader, -1);
    }
    
    public interface ClassGenerator {
        byte[] bytecode();
        String name();
    }
    
    private static class KeyedClassReference<T> extends WeakReference<Class<T>> {
        private final Object key;
        
        public KeyedClassReference(Object key, Class<T> referent, ReferenceQueue<Class<T>> referenceQueue) {
            super(referent, referenceQueue);
            
            this.key = key;
        }
        
        public Object getKey() {
            return key;
        }
    }
    
    public static class OneShotClassLoader extends ClassLoader {
        private final static ProtectionDomain DEFAULT_DOMAIN = 
            JRubyClassLoader.class.getProtectionDomain();
        
        public OneShotClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length, DEFAULT_DOMAIN);
         }
    }
    
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public int getMax() {
        return max;
    }
    
    public Class<T> cacheClassByKey(Object key, ClassGenerator classGenerator) 
        throws ClassNotFoundException {
        WeakReference<Class<T>> weakRef = cache.get(key);
        Class<T> contents = null;
        if (weakRef != null) contents = weakRef.get();
        
        if (weakRef == null || contents == null) {
            if (isFull()) return null;
            
            OneShotClassLoader oneShotCL = new OneShotClassLoader(getClassLoader());
            contents = (Class<T>)oneShotCL.defineClass(classGenerator.name(), classGenerator.bytecode());
            classLoadCount.incrementAndGet();
            
            cleanup();
            cache.put(key, new KeyedClassReference(key, contents, referenceQueue));
        } else {
            classReuseCount.incrementAndGet();
        }
        
        return contents;
    }
    
    public void flush() {
        cache.clear();
    }
    
    public boolean isFull() {
        cleanup();
        return max > 0 && cache.size() >= max;
    }
    
    public int getClassLoadCount() {
        return classLoadCount.get();
    }
    
    public int getLiveClassCount() {
        cleanup();
        return cache.size();
    }
    
    public int getClassReuseCount() {
        return classReuseCount.get();
    }
    
    private void cleanup() {
        KeyedClassReference reference;
        while ((reference = (KeyedClassReference)referenceQueue.poll()) != null) {
            cache.remove(reference.getKey());
        }
    }
}
