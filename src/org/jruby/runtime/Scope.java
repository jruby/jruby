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

import java.util.Arrays;
import org.jruby.IRuby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Scope in the Ruby Stack of scopes.
 * This is used to maintain a stack of scopes through a linked list.
 * Each scope holds a list of local values and a list of local names
 * Each scope also hold a pointer to the previous scope, a new empty scope
 * can be pushed on top of the stack using the push method, the top scope
 * can be popped of the top of the stack using the pop method.
 *
 * @author  jpetersen
 */
public class Scope {
    private static final int LASTLINE_INDEX = 0;
    private static final int BACKREF_INDEX = 1;
    private static final String[] SPECIAL_VARIABLE_NAMES =
            new String[] { "_", "~" };

    private IRubyObject rubyNil;

    //private List localNames = null;
	//private List localValues = null;
    private String[] localNames;
    private IRubyObject[] localValues;

    private Visibility visibility = Visibility.PUBLIC; // Constants.SCOPE_PRIVATE; ? // Same as default for top level...just in case

    public Scope(IRuby runtime) {
        this.rubyNil = runtime.getNil();
    }
	
	public Scope(IRuby runtime, String[] names) {
		this(runtime);
		
		resetLocalVariables(names);
	}

    /**
     * Gets the localNames.
     * @return Returns a NameList
     */
    public String[] getLocalNames() {
        return localNames;
    }

    /**
     * Sets the localNames.
     * @param someLocalNames The localNames to set
     */
    public void resetLocalVariables(String[] someLocalNames) {
        if (someLocalNames == null || someLocalNames.length == 0) {
            this.localNames = null;
            this.localValues = null;
        } else {
            this.localNames = someLocalNames;
            this.localValues = new IRubyObject[someLocalNames.length];
            Arrays.fill(localValues, rubyNil);
        }
    }

    public void addLocalVariables(String[] someLocalNames) {
        if (this.localNames == null || this.localNames.length == 0) {
            this.localNames = someLocalNames;
            this.localValues = new IRubyObject[someLocalNames.length];
            Arrays.fill(localValues, rubyNil);
            
            System.arraycopy(someLocalNames, 0, localNames, 0, someLocalNames.length);
        } else {
            String[] newLocalNames = new String[localNames.length + someLocalNames.length];
            
            System.arraycopy(localNames, 0, newLocalNames, 0, localNames.length);
            System.arraycopy(someLocalNames, 0, newLocalNames, localNames.length, someLocalNames.length);
            
            IRubyObject[] newLocalValues = new IRubyObject[newLocalNames.length];
            
            System.arraycopy(localValues, 0, newLocalValues, 0, localValues.length);
            Arrays.fill(newLocalValues, localValues.length, newLocalValues.length, rubyNil);
            
            this.localNames = newLocalNames;
            this.localValues = newLocalValues;
        }
    }

    public boolean hasLocalVariables() {
        if (localNames == null) {
            return false;
        }
        return localNames.length != 0;
    }

	public IRubyObject getValue(int count) {
	    return localValues[count];
	}

	public void setValue(int count, IRubyObject value) {
	    localValues[count] = value;
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
            resetLocalVariables(SPECIAL_VARIABLE_NAMES);
        }
        setValue(LASTLINE_INDEX, value);
    }

    IRubyObject getBackref() {
        if (hasLocalVariables()) {
            return getValue(BACKREF_INDEX);
        }
        return rubyNil;
    }

    void setBackref(IRubyObject match) {
        if (! hasLocalVariables()) {
            resetLocalVariables(SPECIAL_VARIABLE_NAMES);
        }
        setValue(BACKREF_INDEX, match);
    }
}
