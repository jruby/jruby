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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.StackElement;

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
    private static final int LASTLINE_INDEX = 0;
    private static final int BACKREF_INDEX = 1;
    private static final String[] SPECIAL_VARIABLE_NAMES =
            new String[] { "_", "~" };

    private IRubyObject rubyNil;

    private IRubyObject superObject = null;

    private List localNames = null;
	private List localValues = null;

    private Visibility visibility = Visibility.PUBLIC; // Constants.SCOPE_PRIVATE; ? // Same as default for top level...just in case

    private Scope next = null;

    public Scope(Ruby ruby) {
        this.rubyNil = ruby.getNil();
    }

    public StackElement getNext() {
        return next;
    }

    public void setNext(StackElement newNext) {
        next = (Scope)newNext;
    }

    /** Getter for property superObject.
     * @return Value of property superObject.
     */
    public IRubyObject getSuperObject() {
        return superObject;
    }

    /** Setter for property superObject.
     * @param superObject New value of property superObject.
     */
    public void setSuperObject(IRubyObject superObject) {
        this.superObject = superObject;
    }
    /**
     * Gets the localNames.
     * @return Returns a NameList
     */
    List getLocalNames() {
        return localNames;
    }

    /**
     * Sets the localNames.
     * @param localNames The localNames to set
     */
    public void resetLocalVariables(List localNames) {
        if (localNames == null || localNames.isEmpty()) {
            this.localNames = null;
            this.localValues = null;
        } else {
            this.localNames = localNames;
            this.localValues = new ArrayList(Collections.nCopies(localNames.size(), rubyNil));
        }
    }

    public void addLocalVariables(List localNames) {
        if (this.localNames == null || this.localNames.isEmpty()) {
            this.localNames = new ArrayList(localNames.size());
            this.localValues = new ArrayList(localNames.size());
        }
        this.localNames.addAll(localNames);
        this.localValues.addAll(Collections.nCopies(localNames.size(), rubyNil));
    }

    public List getLocalValues() {
        return localValues;
    }

    public boolean hasLocalVariables() {
        if (localNames == null) {
            return false;
        }
        return ! localNames.isEmpty();
    }

	public IRubyObject getValue(int count) {
	    return (IRubyObject)localValues.get(count);
	}

	public void setValue(int count, IRubyObject value) {
	    localValues.set(count, value);
	}

    /**
     * Gets the methodScope.
     * @return Returns a int
     */
    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public IRubyObject getLastLine() {
        if (hasLocalVariables()) {
            return getValue(LASTLINE_INDEX);
        }
        return rubyNil;
    }

    public void setLastLine(IRubyObject value) {
        if (! hasLocalVariables()) {
            resetLocalVariables(new ArrayList(Arrays.asList(SPECIAL_VARIABLE_NAMES)));
        }
        setValue(LASTLINE_INDEX, value);
    }

    public IRubyObject getBackref() {
        if (hasLocalVariables()) {
            return getValue(BACKREF_INDEX);
        }
        return rubyNil;
    }

    public void setBackref(IRubyObject match) {
        if (! hasLocalVariables()) {
            resetLocalVariables(new ArrayList(Arrays.asList(SPECIAL_VARIABLE_NAMES)));
        }
        setValue(BACKREF_INDEX, match);
    }
}
