/*
 * MethodCacheEntry.java
 *
 * Created on 5. Oktober 2001, 10:01
 */

package org.jruby.interpreter;

import org.jruby.*;
import org.jruby.original.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class MethodCacheEntry {
    private RubyId mid;         /* method's id */
    private RubyId mid0;        /* method's original id */
    private RubyModule klass;   /* receiver's class */
    private RubyModule origin;  /* where method defined  */
    private NODE method;
    private int noex;

    public MethodCacheEntry(RubyId mid, RubyId mid0, RubyModule klass,
                            RubyModule origin, NODE method, int noex) {
        this.mid = mid;
        this.mid0 = mid0;
        this.klass = klass;
        this.origin = origin;
        this.method = method;
        this.noex = noex;
    }
    
    public MethodCacheEntry(RubyModule klass, RubyId mid) {
        this.mid = mid;
        this.mid0 = mid;
        this.klass = klass;
        this.origin = klass;
        this.method = null;
        this.noex = 0;
    }
    
    public MethodCacheEntry(RubyModule klass, int noex) {
        this.klass = klass;
        this.noex = noex;
    }
    
    public static void saveEmptyEntry(Ruby ruby, RubyModule klass, RubyId id) {
        ruby.getMethodCache().put(getCacheHash(klass, id), new MethodCacheEntry(klass, id));
    }
    
    public static MethodCacheEntry getEntry(Ruby ruby, RubyModule klass, RubyId id) {
        MethodCacheEntry entry = (MethodCacheEntry)ruby.getMethodCache().get(getCacheHash(klass, id));
        if (entry != null && entry.mid.intValue() == id.intValue() && entry.klass == klass) {
            return entry;
        } else {
            return null;
        }
    }

    /** Getter for property klass.
     * @return Value of property klass.
     */
    public RubyModule getKlass() {
        return klass;
    }    
    
    /** Setter for property klass.
     * @param klass New value of property klass.
     */
    public void setKlass(RubyModule klass) {
        this.klass = klass;
    }
    
    /** Getter for property method.
     * @return Value of property method.
     */
    public NODE getMethod() {
        return method;
    }
    
    /** Setter for property method.
     * @param method New value of property method.
     */
    public void setMethod(NODE method) {
        this.method = method;
    }
    
    /** Getter for property mid.
     * @return Value of property mid.
     */
    public RubyId getMid() {
        return mid;
    }
    
    /** Setter for property mid.
     * @param mid New value of property mid.
     */
    public void setMid(RubyId mid) {
        this.mid = mid;
    }
    
    /** Getter for property mid0.
     * @return Value of property mid0.
     */
    public RubyId getMid0() {
        return mid0;
    }
    
    /** Setter for property mid0.
     * @param mid0 New value of property mid0.
     */
    public void setMid0(RubyId mid0) {
        this.mid0 = mid0;
    }
    
    /** Getter for property noex.
     * @return Value of property noex.
     */
    public int getNoex() {
        return noex;
    }
    
    /** Setter for property noex.
     * @param noex New value of property noex.
     */
    public void setNoex(int noex) {
        this.noex = noex;
    }
    
    /** Getter for property origin.
     * @return Value of property origin.
     */
    public RubyModule getOrigin() {
        return origin;
    }
    
    /** Setter for property origin.
     * @param origin New value of property origin.
     */
    public void setOrigin(RubyModule origin) {
        this.origin = origin;
    }
    
    private static final int CACHE_MASK = 0x7ff;
    
    public static Integer getCacheHash(RubyModule klass, RubyId id) {
        int c = System.identityHashCode(klass);
        return new Integer((((c) >> 3) ^ (id.intValue())) & CACHE_MASK);
    }
}