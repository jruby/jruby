/*
 * RubyScope.java - No description
 * Created on 10. September 2001, 17:54
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

import java.util.*;

import org.jruby.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyScope {
    public static final int SCOPE_ALLOCA = 0;
    public static final int SCOPE_MALLOC = 1;
    public static final int SCOPE_NOSTACK = 2;
    public static final int SCOPE_DONT_RECYCLE = 4;

    private RubyObject superObject = null;
    private RubyIdPointer localTbl = null;
    private RubyPointer localVars = null;
    private int flags = 0;

    private RubyScope old = null;

    public RubyScope() {
    }

    public RubyScope(RubyScope scope) {
        this.superObject = scope.superObject;
        this.localTbl = scope.localTbl;
        this.localVars = scope.localVars;
        this.flags = scope.flags;
        this.old = scope.old;
    }

    public void push() {
        old = new RubyScope(this);

        localTbl = null;
        localVars = null;
        flags = 0;
    }

    public void pop() {
        this.superObject = old.superObject;
        this.localTbl = old.localTbl;
        this.localVars = old.localVars;
        this.flags = old.flags;
        this.old = old.old;
    }

    /** Getter for property flags.
     * @return Value of property flags.
     */
    public int getFlags() {
        return flags;
    }

    /** Setter for property flags.
     * @param flags New value of property flags.
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /** Indexed getter for property localTbl.
     * @param index Index of the property.
     * @return Value of the property at <CODE>index</CODE>.
     */
    public RubyId getLocalTbl(int index) {
        return localTbl.getId(index);
    }

    /** Getter for property localTbl.
     * @return Value of property localTbl.
     */
    public RubyIdPointer getLocalTbl() {
        return localTbl;
    }

    /** Indexed setter for property localTbl.
     * @param index Index of the property.
     * @param localTbl New value of the property at <CODE>index</CODE>.
     */
    public void setLocalTbl(int index, RubyId newId) {
        localTbl.set(index, newId);
    }

    /** Setter for property localTbl.
     * @param localTbl New value of property localTbl.
     */
    public void setLocalTbl(RubyIdPointer localTbl) {
        this.localTbl = localTbl;
    }

    /** Indexed getter for property localVars.
     * @param index Index of the property.
     * @return Value of the property at <CODE>index</CODE>.
     */
    public RubyObject getLocalVars(int index) {
        return localVars.getRuby(index);
    }

    /** Getter for property localVars.
     * @return Value of property localVars.
     */
    public RubyPointer getLocalVars() {
        return localVars;
    }

    /** Indexed setter for property localVars.
     * @param index Index of the property.
     * @param localVars New value of the property at <CODE>index</CODE>.
     */
    public void setLocalVars(int index, RubyObject newValue) {
        // HACK +++
        /*if (localVars == null) {
            localVars = new ShiftableList(new ArrayList());
            localVars.shiftLeft(index + 1);
        }*/
        if (localVars == null) {
            localVars = new RubyPointer(new ArrayList());
            localVars.inc(index + 1);
        }
        // HACK ---
        localVars.set(index, newValue);
    }

    /** Setter for property localVars.
     * @param localVars New value of property localVars.
     */
    public void setLocalVars(RubyPointer localVars) {
        this.localVars = localVars;
    }

    /** Getter for property superObject.
     * @return Value of property superObject.
     */
    public RubyObject getSuperObject() {
        return superObject;
    }

    /** Setter for property superObject.
     * @param superObject New value of property superObject.
     */
    public void setSuperObject(RubyObject superObject) {
        this.superObject = superObject;
    }
}