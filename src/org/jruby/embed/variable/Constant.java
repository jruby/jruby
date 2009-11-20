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
import java.util.Collection;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of BiVariable for a Ruby constant.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class Constant extends AbstractVariable {
    private static String pattern = "[A-Z]([a-zA-Z]|_)([a-zA-Z]|_|\\d)*";
    private boolean initialized = false;

    /**
     * Returns an instance of this class. This factory method is used when a constant
     * is put in {@link BiVariableMap}.
     *
     * @param runtime Ruby runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of Constant
     */
    public static BiVariable getInstance(Ruby runtime, String name, Object... javaObject) {
        if (name.matches(pattern)) {
            return new Constant(runtime, name, javaObject);
        }
        return null;
    }
    
    private Constant(Ruby runtime, String name, Object... javaObject) {
        super(runtime, name, javaObject);
    }

    /**
     * A constructor used when constants are retrieved from Ruby.
     *
     * @param name the constant name
     * @param irubyObject Ruby constant object
     */
    Constant(String name, IRubyObject irubyObject) {
        super(name, irubyObject);
    }

    void markInitialized() {
        this.initialized = true;
    }

    /**
     * Retrieves constants from Ruby after the evaluation or method invocation.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved constants.
     */
    public static void retrieve(Ruby runtime, IRubyObject receiver, BiVariableMap vars) {
        if (receiver == null) {
            receiver = runtime.getTopSelf();
        }
        Collection<String> names = receiver.getMetaClass().getConstantNames();
        for (String name : names) {
            BiVariable var;
            IRubyObject value = receiver.getMetaClass().getConstant(name);
            if (vars.containsKey((Object)name)) {
                var = vars.getVariable(name);
                var.setRubyObject(value);
            } else {
                var = new Constant(name, value);
                ((Constant)var).markInitialized();
                vars.update(name, var);
            }
        }
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.Constant.
     */
    public Type getType() {
        return Type.Constant;
    }

    /**
     * Returns true if the given name is a decent Ruby constant. Unless
     * returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is of a Ruby constant.
     */
    public static boolean isValidName(Object name) {
        return isValidName(pattern, name);
    }

    /**
     * Injects a constant value to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    public void inject(Ruby runtime, IRubyObject receiver) {
        if (initialized) {
            return;
        }
        RubyModule rubyClass = getRubyClass(runtime);
        if (rubyClass != null) {
          rubyClass.setConstant(name, irubyObject);
        } else {
          RubyModule module = runtime.getCurrentContext().getRubyClass();
          module.setConstant(name, irubyObject);
        }
        initialized = true;
    }

    /**
     * Removes this object from {@link BiVariableMap}.
     *
     * @param runtime enviroment where a variabe is removed.
     */
    public void remove(Ruby runtime) {
        /* Like this? - from RubyModule
         IRubyObject oldValue = fetchConstant(name);
         if (oldValue != null) {
            Ruby runtime = getRuntime();
            if (oldValue == UNDEF) {
                runtime.getLoadService().removeAutoLoadFor(getName() + "::" + name);
            } else {
                runtime.getWarnings().warn(ID.CONSTANT_ALREADY_INITIALIZED, "already initialized constant " + name, name);
            }
        }
         */
    }

}
