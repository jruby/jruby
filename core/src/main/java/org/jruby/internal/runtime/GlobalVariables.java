/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.internal.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.GlobalSite;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author jpetersen
 */
public class GlobalVariables {
    private final Ruby runtime;
    private final Map<String, GlobalVariable> globalVariables = new ConcurrentHashMap<String, GlobalVariable>();

    public GlobalVariables(Ruby runtime) {
        this.runtime = runtime;
    }

    public void define(GlobalSite accessor, GlobalVariable.Scope scope) {
        assert accessor != null;

        globalVariables.put(accessor.name(), new GlobalVariable(accessor, scope));
    }

    public void define(String name, GlobalSite accessor, GlobalVariable.Scope scope) {
        assert accessor != null;

        globalVariables.put(name, new GlobalVariable(accessor, scope));
    }

    public void define(String name, IRubyObject value, GlobalVariable.Scope scope) {
        assert name != null;
        assert value != null;
        assert name.startsWith("$");

        globalVariables.put(name, new GlobalVariable(new GlobalSite(runtime, name, value), scope));
    }

    public void defineReadonly(GlobalSite accessor, GlobalVariable.Scope scope) {
        assert accessor != null;

        String name = accessor.name();

        globalVariables.put(name, new GlobalVariable(new ReadonlyAccessor(runtime, name, accessor), scope));
    }

    public void defineReadonly(String name, Supplier<IRubyObject> accessor, GlobalVariable.Scope scope) {
        assert name != null;
        assert accessor != null;
        assert name.startsWith("$");

        globalVariables.put(name, new GlobalVariable(new ReadonlyAccessor(runtime, name, accessor), scope));
    }

    public void defineReadonly(String name, IRubyObject value, GlobalVariable.Scope scope) {
        assert name != null;
        assert value != null;
        assert name.startsWith("$");

        globalVariables.put(name, new GlobalVariable(new ReadonlyAccessor(runtime, name, value), scope));
    }

    public boolean isDefined(String name) {
        assert name != null;
        assert name.startsWith("$");

        GlobalVariable variable = globalVariables.get(name);

        if (variable == null) return false;

        if (variable.getAccessor() instanceof UndefinedAccessor) {
            return variable.getAccessor().isDefined();
        }

        return true;
    }

    /**
     * Creates a new global variable which links to the oldName global variable.
     * @param name
     * @param  oldName
     */
    public void alias(String name, String oldName) {
        assert name != null;
        assert oldName != null;
        assert name.startsWith("$");
        assert oldName.startsWith("$");

        GlobalVariable oldVariable = createIfNotDefined(oldName);
        GlobalVariable variable = globalVariables.get(name);

        if (variable != null && oldVariable != variable && variable.isTracing()) {
            throw RaiseException.from(runtime, runtime.getRuntimeError(), "can't alias in tracer");
        }

        globalVariables.put(name, oldVariable);
    }

    public IRubyObject get(String name) {
	    assert name != null;
	    assert name.startsWith("$");

	    GlobalVariable variable = getVariable(name);

	    return variable.getAccessor().get();
	}

    public GlobalVariable getVariable(String name) {
	    assert name != null;
	    assert name.startsWith("$");

	    GlobalVariable variable = globalVariables.get(name);
        if (variable != null) return variable;

        return createIfNotDefined(name);
    }

    public MethodHandle getVariableInvoker(String name) {
        return getVariable(name).getAccessor().dynamicInvoker();
    }

    public IRubyObject set(String name, IRubyObject value) {
        assert name != null;
        assert name.startsWith("$");

        GlobalVariable variable = createIfNotDefined(name);
        IRubyObject result = variable.getAccessor().set(value);
        variable.trace(value);
        return result;
    }

    public IRubyObject clear(String name) {
        return set(name, runtime.getNil());
    }

    public void setTraceVar(String name, RubyProc proc) {
        assert name != null;
        assert name.startsWith("$");

        GlobalVariable variable = createIfNotDefined(name);
        variable.addTrace(proc);
    }

    public boolean untraceVar(String name, IRubyObject command) {
        assert name != null;
        assert name.startsWith("$");

        if (isDefined(name)) {
            GlobalVariable variable = globalVariables.get(name);
            return variable.removeTrace(command);
        }
        return false;
    }

    public void untraceVar(String name) {
        assert name != null;
        assert name.startsWith("$");

        if (isDefined(name)) {
            GlobalVariable variable = globalVariables.get(name);
            variable.removeTraces();
        }
    }

    public Set<String> getNames() {
        return globalVariables.keySet();
    }

    private GlobalVariable createIfNotDefined(String name) {
        GlobalVariable variable = globalVariables.get(name);
        if (variable == null) {
            variable = GlobalVariable.newUndefined(runtime, name);
            globalVariables.put(name, variable);
        }
        return variable;
    }

    private IRubyObject defaultSeparator;

    public IRubyObject getDefaultSeparator() {
        return defaultSeparator;
    }

    public void setDefaultSeparator(IRubyObject defaultSeparator) {
        this.defaultSeparator = defaultSeparator;
    }
}
