/*
 * CacheEntry.java - description
 * Created on 02.03.2002, 23:43:12
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.internal.runtime.methods;

import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.runtime.*;
import org.jruby.runtime.methods.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class CacheEntry {
    private String name;            /* method's id */
    private String originalName;    /* method's original id */
    private RubyModule recvClass;   /* receiver's class */
    private RubyModule origin;      /* where method defined  */

    private IMethod method;
    private int noex;

    public CacheEntry(String name, String originalName, RubyModule recvClass, RubyModule origin, IMethod method, int noex) {
        this.name = name;
        this.originalName = originalName;
        
        this.recvClass = recvClass;
        this.origin = origin;

        this.method = method;
        this.noex = noex;
    }
    
    public CacheEntry(String name, RubyModule recvClass) {
        this(name, name, recvClass, recvClass, null, 0);
    }
    
    public CacheEntry(RubyModule recvClass, int noex) {
        this(null, null, recvClass, null, null, noex);
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
    public IMethod getMethod() {
        return method;
    }
    
    /** Setter for property method.
     * @param method New value of property method.
     */
    public void setMethod(IMethod method) {
        this.method = method;
    }
    
    /** Getter for property mid.
     * @return Value of property mid.
     */
    public String getName() {
        return name;
    }
    
    /** Setter for property mid.
     * @param mid New value of property mid.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /** Getter for property mid0.
     * @return Value of property mid0.
     */
    public String getOriginalName() {
        return originalName;
    }
    
    /** Setter for property mid0.
     * @param mid0 New value of property mid0.
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
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
}