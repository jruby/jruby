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
 * @version 
 */
public class RubyScope {
    public static final int SCOPE_ALLOCA = 0;
    public static final int SCOPE_MALLOC = 1;
    public static final int SCOPE_NOSTACK = 2;
    public static final int SCOPE_DONT_RECYCLE = 4;

    private RubyObject superObject = null;
    
    private int flags = 0;
    
    private ExtendedList localNames = null;
	private ExtendedList localValues = null;
    
    private int oldActMethodScope = Constants.NOEX_PRIVATE; // Same as default for top level...just in case
    private Ruby ruby = null;

    private RubyScope old = null;

    public RubyScope(Ruby ruby) {
        this.ruby = ruby;
    }

    public RubyScope(RubyScope scope, Ruby ruby) {
        this.superObject = scope.superObject;
        this.localNames = scope.localNames;
        this.localValues = scope.localValues;
        this.flags = scope.flags;
        this.old = scope.old;
        this.ruby = ruby;
    }

    public void push() {
        oldActMethodScope = ruby.getActMethodScope();
        ruby.setActMethodScope(Constants.NOEX_PUBLIC);
        old = new RubyScope(this, ruby);

		localNames = null;
        localValues = null;
        flags = 0;
    }

    public void pop() {
        ruby.setActMethodScope(oldActMethodScope);
        this.superObject = old.superObject;
        
        this.localNames = old.localNames;
        this.localValues = old.localValues;
        
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
    public ExtendedList getLocalNames() {
        return localNames;
    }

    /**
     * Sets the localNames.
     * @param localNames The localNames to set
     */
    public void setLocalNames(ExtendedList localName) {
        this.localNames = localName;
    }

    /**
     * Gets the localValues.
     * @return Returns a ArrayList
     */
    public ExtendedList getLocalValues() {
        return localValues;
    }

    /**
     * Sets the localValues.
     * @param localValues The localValues to set
     */
    public void setLocalValues(ExtendedList localValue) {
        this.localValues = localValue;
    }

	public RubyObject getValue(int count) {
	    return (RubyObject)localValues.get(count);
	}
	
	public void setValue(int count, RubyObject value) {
	    localValues.set(count, value);
	}
}
