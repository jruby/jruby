/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.variable;

import org.jruby.embed.internal.BiVariableMap;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of BiVariable for JSR223 style global variable. The assigend
 * name is like a local vars in Java, but a global in Ruby.
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
    public static BiVariable getInstance(Ruby runtime, String name, Object... javaObject) {
        if (name.matches(pattern)) {
            return new LocalGlobalVariable(runtime, name, javaObject);
        }
        return null;
    }

    private LocalGlobalVariable(Ruby runtime, String name, Object... javaObject) {
        super(runtime, name, javaObject);
    }

    /**
     * A constructor used when local global type variables are retrieved from Ruby.
     *
     * @param name the local global type variable name
     * @param irubyObject Ruby global object
     */
    LocalGlobalVariable(String name, IRubyObject irubyObject) {
        super(name, irubyObject);
    }

    /**
     * Retrieves global variables from Ruby after the evaluation as a local global type.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved global variables.
     */
    public static void retrieve(Ruby runtime, IRubyObject receiver, BiVariableMap vars) {
        GlobalVariables gvars = runtime.getGlobalVariables();
        Set<String> names = gvars.getNames();
        for (String name : names) {
            if (isPredefined(name)) {
                continue;
            }
            BiVariable var;
            IRubyObject value = gvars.get(name);
            String javaName = name.substring(1); // eliminates a preceding character, "$"
            if (vars.containsKey((Object)javaName)) {
                var = vars.getVariable(javaName);
                var.setRubyObject(value);
            } else {
                var = new LocalGlobalVariable(javaName, value);
                vars.update(javaName, var);
            }
        }
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
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    @Override
    public void inject(Ruby runtime, IRubyObject receiver) {
        runtime.getGlobalVariables().set("$"+name, irubyObject);
    }

    /**
     * Removes this object from {@link BiVariableMap}.
     *
     * @param runtime enviroment where a variabe is removed.
     */
    @Override
    public void remove(Ruby runtime) {
        setJavaObject(runtime, null);
        runtime.getGlobalVariables().set("$"+name, irubyObject);
    }
}