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

import org.jruby.RubyModule;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;

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

    private ICallable method;
    private Visibility visibility;

    private CacheEntry(String name, String originalName, RubyModule recvClass, RubyModule origin, ICallable method, Visibility visibility) {
        this.name = name;
        this.originalName = originalName;
        this.recvClass = recvClass;
        this.origin = origin;
        this.method = method;
        this.visibility = visibility;
    }

    public CacheEntry(String name, RubyModule recvClass) {
        this(name, name, recvClass, null, null, null);
    }

    public static CacheEntry createUndefined(String name, RubyModule recvClass) {
        return new CacheEntry(name, name, recvClass, recvClass, UndefinedMethod.getInstance(), Visibility.PUBLIC);
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
    public ICallable getMethod() {
        return method;
    }

    /** Setter for property method.
     * @param method New value of property method.
     */
    public void setMethod(ICallable method) {
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
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

    public boolean isDefined() {
        return ! getMethod().isUndefined();
    }
}

