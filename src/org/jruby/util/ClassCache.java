/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.ast.executable.Script;

/**
 *
 * @author headius
 */
public class ClassCache {
    public interface ClassGenerator {
        Class generate() throws ClassNotFoundException;
    }
    
    public interface ScriptGenerator {
        Script generate() throws ClassNotFoundException;
    }
    
    private Map cache = new ConcurrentHashMap();
    
    public Class cacheClassByBytes(byte[] bytecode, ClassGenerator classGenerator) throws ClassNotFoundException {
        int hashcode = Arrays.hashCode(bytecode);
        
        WeakReference weakRef = (WeakReference)cache.get(hashcode);
        Class contents = null;
        if (weakRef != null) {
            contents = (Class)weakRef.get();
        }
        
        if (weakRef == null || contents == null) {
            contents = classGenerator.generate();
            cache.put(hashcode, new WeakReference(contents));
        }
        
        return contents;
    }
    
    public Script cacheScriptByBytes(byte[] bytecode, ScriptGenerator scriptGenerator) throws ClassNotFoundException {
        int hashcode = Arrays.hashCode(bytecode);
        
        WeakReference weakRef = (WeakReference)cache.get(hashcode);
        Script contents = null;
        if (weakRef != null) {
            contents = (Script)weakRef.get();
        }
        
        if (weakRef == null || contents == null) {
            contents = scriptGenerator.generate();
            cache.put(hashcode, new WeakReference(contents));
        }
        
        return contents;
    }
    
    public Class cacheClassByKey(Object key, ClassGenerator classGenerator) throws ClassNotFoundException {
        WeakReference weakRef = (WeakReference)cache.get(key);
        Class contents = null;
        if (weakRef != null) {
            contents = (Class)weakRef.get();
        }
        
        if (weakRef == null || contents == null) {
            contents = classGenerator.generate();
            cache.put(key, new WeakReference(contents));
        }
        
        return contents;
        
    }
    
    public Script cacheScriptByKey(Object key, ScriptGenerator scriptGenerator) throws ClassNotFoundException {
        WeakReference weakRef = (WeakReference)cache.get(key);
        Script contents = null;
        if (weakRef != null) {
            contents = (Script)weakRef.get();
        }
        
        if (weakRef == null || contents == null) {
            contents = scriptGenerator.generate();
            cache.put(key, new WeakReference(contents));
        }
        
        return contents;
    }
}
