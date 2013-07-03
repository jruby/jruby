/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.variable;

import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of BiVariable for JSR223 style global variable. The assigned
 * name is like a local variables in Java, but a global in Ruby.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class LocalGlobalVariable extends GlobalVariable {
    private static String pattern = "([a-zA-Z]|(_([a-zA-Z]|_|\\d)))([a-zA-Z]|_|\\d)*";

    /**
     * Returns an instance of this class. This factory method is used when a local
     * global type variable is put into {@link BiVariableMap}.
     *
     * @param runtime Ruby runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of LocalGlobalVariable
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if (name.matches(pattern)) {
            return new LocalGlobalVariable(receiver, name, javaObject);
        }
        return null;
    }

    private LocalGlobalVariable(RubyObject receiver, String name, Object... javaObject) {
        super(receiver, name, javaObject);
    }

    /**
     * A constructor used when local global type variables are retrieved from Ruby.
     *
     * @param name the local global type variable name
     * @param irubyObject Ruby global object
     */
    LocalGlobalVariable(IRubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, irubyObject);
    }

    /**
     * Retrieves global variables eagerly from Ruby right after the evaluation. The
     * variable names to be retrieved must be in a variable map.
     *
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved global variables.
     */
    public static void retrieve(RubyObject receiver, BiVariableMap vars) {
        if (vars.isLazy()) return;
        
        GlobalVariables gvars = receiver.getRuntime().getGlobalVariables();
        Set<String> names = gvars.getNames();
        for (String name : names) {
            if (isPredefined(name)) {
                continue;
            }
            IRubyObject value = gvars.get(name);
            String javaName = name.substring(1); // eliminates a preceding character, "$"
            updateLocalGlobal((RubyObject)receiver.getRuntime().getTopSelf(), vars, javaName, value);
        }
    }

    private static void updateLocalGlobal(RubyObject receiver, BiVariableMap vars, String name, IRubyObject value) {
        BiVariable var;
        if (vars.containsKey((Object) name)) {
            var = vars.getVariable(receiver, name);
            var.setRubyObject(value);
        } else {
            var = new LocalGlobalVariable(receiver, name, value);
            vars.update(name, var);
        }
    }

    /**
     * Retrieves a global variable by key from Ruby runtime after the evaluation.
     * This method is used when eager retrieval is off.
     *
     * @param runtime Ruby runtime
     * @param vars map to save a retrieved global variable.
     * @param key name of the global variable
     */
    public static void retrieveByKey(Ruby runtime, BiVariableMap vars, String key) {
        GlobalVariables gvars = runtime.getGlobalVariables();
        // if the specified key doesn't exist, this method is called before the
        // evaluation. Don't update value in this case.
        String rubyKey = ("$" + key).intern();
        if (!gvars.getNames().contains(rubyKey)) return;

        // the specified key is found, so let's update
        IRubyObject value = gvars.get(rubyKey);
        updateLocalGlobal((RubyObject)runtime.getTopSelf(), vars, key, value);
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.GlobalVariable.
     */
    @Override
    public Type getType() {
        return Type.LocalGlobalVariable;
    }

    /**
     * Returns true if the given name is a local global type variable. Unless
     * returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is of a local global type variable.
     */
    public static boolean isValidName(Object name) {
        return isValidName(pattern, name);
    }

    /**
     * Injects a global value to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     */
    @Override
    public void inject() {
        synchronized (receiver.getRuntime()) {
            String varname = (name.startsWith("$") ? name : "$" + name);
            receiver.getRuntime().getGlobalVariables().set(varname.intern(), irubyObject);
        }
    }

    /**
     * Removes this object from {@link BiVariableMap}.
     *
     * @param runtime environment where a variable is removed.
     */
    @Override
    public void remove() {
        synchronized (receiver.getRuntime()) {
            String varname = (name.startsWith("$") ? name : "$" + name);
            receiver.getRuntime().getGlobalVariables().clear(varname.intern());
        }
    }
}