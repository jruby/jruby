/*
 * RubyMethodCacheEntry.java - No description
 * Created on 18. Oktober 2001, 17:19
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby.runtime;

import org.jruby.*;
import org.jruby.nodes.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyMethodCacheEntry {
    private String mid;             /* method's id */
    private String mid0;            /* method's original id */
    private RubyModule recvClass;   /* receiver's class */
    private RubyModule origin;      /* where method defined  */

    private Node method;
    private int noex;

    public RubyMethodCacheEntry(String mid, String mid0, RubyModule recvClass, 
                                    RubyModule origin, Node method, int noex) {
        this.mid = mid;
        this.mid0 = mid0;
        this.recvClass = recvClass;
        this.origin = origin;
        this.method = method;
        this.noex = noex;
    }
    
    public RubyMethodCacheEntry(RubyModule recvClass, String mid) {
        this.mid = mid;
        this.mid0 = mid;
        this.recvClass = recvClass;
        this.origin = recvClass;
        this.method = null;
        this.noex = 0;
    }
    
    public RubyMethodCacheEntry(RubyModule recvClass, int noex) {
        this.recvClass = recvClass;
        this.noex = noex;
    }
    
    public static void saveEmptyEntry(Ruby ruby, RubyModule recvClass, String id) {
        ruby.getMethodCache().put(getCacheHash(recvClass, id), new RubyMethodCacheEntry(recvClass, id));
    }

    public static void saveEntry(Ruby ruby, RubyModule recvClass, String id, RubyMethodCacheEntry entry) {
        ruby.getMethodCache().put(getCacheHash(recvClass, id), entry);
    }
    
    public static RubyMethodCacheEntry getEntry(Ruby ruby, RubyModule recvClass, String id) {
        RubyMethodCacheEntry entry = (RubyMethodCacheEntry)ruby.getMethodCache().get(getCacheHash(recvClass, id));
        if (entry != null && entry.mid.equals(id) && entry.recvClass == recvClass) {
            return entry;
        } else {
            return null;
        }
    }

    /** Getter for property recvClass.
     * @return Value of property recvClass.
     */
    public RubyModule getRecvClass() {
        return recvClass;
    }    
    
    /** Setter for property recvClass.
     * @param recvClass New value of property recvClass.
     */
    public void setRecvClass(RubyModule recvClass) {
        this.recvClass = recvClass;
    }
    
    /** Getter for property method.
     * @return Value of property method.
     */
    public Node getMethod() {
        return method;
    }
    
    /** Setter for property method.
     * @param method New value of property method.
     */
    public void setMethod(Node method) {
        this.method = method;
    }
    
    /** Getter for property mid.
     * @return Value of property mid.
     */
    public String getMid() {
        return mid;
    }
    
    /** Setter for property mid.
     * @param mid New value of property mid.
     */
    public void setMid(String mid) {
        this.mid = mid;
    }
    
    /** Getter for property mid0.
     * @return Value of property mid0.
     */
    public String getMid0() {
        return mid0;
    }
    
    /** Setter for property mid0.
     * @param mid0 New value of property mid0.
     */
    public void setMid0(String mid0) {
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
    
    public static Integer getCacheHash(RubyModule recvClass, String id) {
        int c = System.identityHashCode(recvClass);
        return new Integer((((c) >> 3) ^ (id.hashCode())) & CACHE_MASK);
    }
}
