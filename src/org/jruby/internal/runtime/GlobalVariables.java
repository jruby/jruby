/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.SecurityError;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class GlobalVariables {
    private Ruby runtime;
    private Map globalVariables = new HashMap();

    public GlobalVariables(Ruby runtime) {
        this.runtime = runtime;
    }

    public void define(String name, IAccessor accessor) {
        Asserts.notNull(name);
        Asserts.notNull(accessor);
        Asserts.isTrue(name.startsWith("$"));

        globalVariables.put(name, new GlobalVariable(accessor));
    }
    
    public void defineReadonly(String name, IAccessor accessor) {
        Asserts.notNull(name);
        Asserts.notNull(accessor);
        Asserts.isTrue(name.startsWith("$"));

        globalVariables.put(name, new GlobalVariable(new ReadonlyAccessor(name, accessor)));
    }

    public boolean isDefined(String name) {
        Asserts.notNull(name);
        Asserts.isTrue(name.startsWith("$"));
        
        GlobalVariable variable = (GlobalVariable)globalVariables.get(name);
        return variable != null && !(variable.getAccessor() instanceof UndefinedAccessor);
    }

    /** Creates a new global variable which links to
     * the oldName global variable.
     * 
     * <b>WANRING</b> we are already using the 1.7.1 behaviour.
     */
    public void alias(String name, String oldName) {
        Asserts.notNull(name);
        Asserts.notNull(oldName);
        Asserts.isTrue(name.startsWith("$"));
        Asserts.isTrue(oldName.startsWith("$"));

        if (runtime.getSafeLevel() >= 4) {
            throw new SecurityError(runtime, "Insecure: can't alias global variable");
        }

        GlobalVariable oldVariable = createIfNotDefined(oldName);
        GlobalVariable variable = (GlobalVariable)globalVariables.get(name);

        if (variable != null && oldVariable != variable && variable.isTracing()) {
            throw new RaiseException(runtime, "RuntimeError", "can't alias in tracer");
        }

        globalVariables.put(name, oldVariable);
    }

    public IRubyObject get(String name) {
	    Asserts.notNull(name);
	    Asserts.isTrue(name.startsWith("$"));
	
	    GlobalVariable variable = (GlobalVariable)globalVariables.get(name);
	    if (variable != null) {
	        return variable.getAccessor().getValue();
	    } else {
	        runtime.getErrorHandler().warning("global variable `" + name + "' not initialized");
	        return runtime.getNil();
	    }
	}

    public IRubyObject set(String name, IRubyObject value) {
        Asserts.notNull(name);
        Asserts.isTrue(name.startsWith("$"));

        if (runtime.getSafeLevel() >= 4) {
            throw new SecurityError(runtime, "Insecure: can't change global variable value");
        }

        GlobalVariable variable = createIfNotDefined(name);
        IRubyObject result = variable.getAccessor().setValue(value);
        // variable.trace();
        return result;
    }

    public Iterator getNames() {
        return globalVariables.keySet().iterator();
    }

    private GlobalVariable createIfNotDefined(String name) {
        GlobalVariable variable = (GlobalVariable)globalVariables.get(name);
        if (variable == null) {
            variable = GlobalVariable.newUndefined(runtime, name);
            globalVariables.put(name, variable);
        }
        return variable;
    }
}