/*
 * Scope.java - No description
 * Created on 20.01.2002, 15:28:30
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
import org.jruby.util.collections.*;

/**
 * A Scope in the Ruby Stack of scopes.
 * This is used to maintain a stack of scopes through a linked list.
 * Each scope holds a list of local values and a list of local names 
 * Each scope also hold a pointer to the previous scope, a new empty scope
 * can be pushed on top of the stack using the push method, the top scope
 * can be popped of the top of the stack using the pop method.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Scope implements StackElement {
    public static final int SCOPE_ALLOCA = 0;
    public static final int SCOPE_MALLOC = 1;
    public static final int SCOPE_NOSTACK = 2;
    public static final int SCOPE_DONT_RECYCLE = 4;

    private RubyObject superObject = null;
    
    private int flags = 0;
    
    private List localNames = null;
	private List localValues = null;
    
    private int methodScope = Constants.NOEX_PRIVATE; // Same as default for top level...just in case

    private Scope next = null;

    public Scope() {
    }
    
    public StackElement getNext() {
        return next;
    }

    public void setNext(StackElement newNext) {
        next = (Scope)newNext;
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
    /**
     * Gets the localNames.
     * @return Returns a NameList
     */
    public List getLocalNames() {
        return localNames;
    }

    /**
     * Sets the localNames.
     * @param localNames The localNames to set
     */
    public void setLocalNames(List localName) {
        this.localNames = localName;
    }

    /**
     * Gets the localValues.
     * @return Returns a ArrayList
     */
    public List getLocalValues() {
        return localValues;
    }

    /**
     * Sets the localValues.
     * @param localValues The localValues to set
     */
    public void setLocalValues(List localValue) {
        this.localValues = localValue;
    }

	public RubyObject getValue(int count) {
	    return (RubyObject)localValues.get(count);
	}
	
	public void setValue(int count, RubyObject value) {
	    localValues.set(count, value);
	}

    /**
     * Gets the methodScope.
     * @return Returns a int
     */
    public int getMethodScope() {
        return methodScope;
    }

    /**
     * Sets the methodScope.
     * @param methodScope The methodScope to set
     */
    public void setMethodScope(int methodScope) {
        this.methodScope = methodScope;
    }
}