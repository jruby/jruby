/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    public Scope(Ruby runtime) {
        this.rubyNil = runtime.getNil();
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
