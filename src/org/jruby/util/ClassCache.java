package org.jruby.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Simple cache which maintains a collection of classes that can potentially be shared among
 * multiple runtimes (or whole JVM).
 */
public class ClassCache<T> {
    /**
     * The ClassLoader this class cache will use for any classes generated through it.  It is 
     * assumed that the classloader provided will be a parent loader of any runtime using it.
     * @param classLoader to use to generate shared classes
     */
    public ClassCache(ClassLoader classLoader, int max) {
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
        private Object key;
        
        public KeyedClassReference(Object key, Class<T> referent, ReferenceQueue<Class<T>> referenceQueue) {
            super(referent, referenceQueue);
            
            this.key = key;
        }
        
        public Object getKey() {
            return key;
        }
    }
    
    private static class OneShotClassLoader extends ClassLoader {
        public OneShotClassLoader(ClassLoader parent) {
            super(parent);
        }
        
        public Class<?> defineClass(String name, byte[] bytecode) {
            return super.defineClass(name, bytecode, 0, bytecode.length);
        }
    }
    
    private ReferenceQueue referenceQueue = new ReferenceQueue();
    private Map<Object, KeyedClassReference> cache = 
        new ConcurrentHashMap<Object, KeyedClassReference>();
    private ClassLoader classLoader;
    private int max;
    
    public ClassLoader getClassLoader() {
        return classLoader;
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
            
            cache.put(key, new KeyedClassReference(key, contents, referenceQueue));
        }
        
        return contents;
    }
    
    public boolean isFull() {
        cleanup();
        return max > 0 && cache.size() >= max;
    }
    
    private void cleanup() {
        KeyedClassReference reference;
        while ((reference = (KeyedClassReference)referenceQueue.poll()) != null) {
            cache.remove(reference.getKey());
        }
    }
}
