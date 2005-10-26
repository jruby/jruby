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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby.internal.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.IRuby;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public class GlobalVariables {
    private IRuby runtime;
    private Map globalVariables = new HashMap();

    public GlobalVariables(IRuby runtime) {
        this.runtime = runtime;
    }

    public void define(String name, IAccessor accessor) {
        assert name != null;
        assert accessor != null;
        assert name.startsWith("$");

        globalVariables.put(name, new GlobalVariable(accessor));
    }
    
    public void defineReadonly(String name, IAccessor accessor) {
        assert name != null;
        assert accessor != null;
        assert name.startsWith("$");

        globalVariables.put(name, new GlobalVariable(new ReadonlyAccessor(name, accessor)));
    }

    public boolean isDefined(String name) {
        assert name != null;
        assert name.startsWith("$");
        
        GlobalVariable variable = (GlobalVariable)globalVariables.get(name);
        return variable != null && !(variable.getAccessor() instanceof UndefinedAccessor);
    }

    /** Creates a new global variable which links to
     * the oldName global variable.
     * 
     * <b>WANRING</b> we are already using the 1.7.1 behaviour.
     */
    public void alias(String name, String oldName) {
        assert name != null;
        assert oldName != null;
        assert name.startsWith("$");
        assert oldName.startsWith("$");

        if (runtime.getSafeLevel() >= 4) {
            throw runtime.newSecurityError("Insecure: can't alias global variable");
        }

        GlobalVariable oldVariable = createIfNotDefined(oldName);
        GlobalVariable variable = (GlobalVariable)globalVariables.get(name);

        if (variable != null && oldVariable != variable && variable.isTracing()) {
            throw new RaiseException(runtime, runtime.getClass("RuntimeError"), "can't alias in tracer", false);
        }

        globalVariables.put(name, oldVariable);
    }

    public IRubyObject get(String name) {
	    assert name != null;
	    assert name.startsWith("$");
	
	    GlobalVariable variable = (GlobalVariable)globalVariables.get(name);
	    if (variable != null) {
	        return variable.getAccessor().getValue();
	    }
		runtime.getWarnings().warning("global variable `" + name + "' not initialized");
		return runtime.getNil();
	}

    public IRubyObject set(String name, IRubyObject value) {
        assert name != null;
        assert name.startsWith("$");

        if (runtime.getSafeLevel() >= 4) {
            throw runtime.newSecurityError("Insecure: can't change global variable value");
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
