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
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>
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

import org.jruby.embed.internal.BiVariableMap;
import java.util.List;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of BiVariable for a Ruby class variable.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class ClassVariable extends AbstractVariable {
    private static String pattern = "@@([a-zA-Z]|_)([a-zA-Z]|_|\\d)*";

    /**
     * Returns an instance of this class. This factory method is used when a class
     * variables is put in {@link BiVariableMap}. This variable is originated from Java.
     *
     * @param runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of ClassVariable
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if (name.matches(pattern)) {
            return new ClassVariable(receiver, name, javaObject);
        }
        return null;
    }

    /**
     * Constructor when the variable is originated from Java
     * 
     * @param receiver
     * @param name
     * @param javaObject
     */
    private ClassVariable(RubyObject receiver, String name, Object... javaObjects) {
        super(receiver, name, false);
        updateByJavaObject(receiver.getRuntime(), javaObjects);
    }

    /**
     * A constructor used when this variable is retrieved from Ruby.
     *
     * @param receiver a receiver object that this variable/constant is originally in. When
     *        the variable/constant is originated from Ruby, receiver may not be null.
     * @param name the class variable name
     * @param irubyObject Ruby class variable object
     */
    ClassVariable(IRubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, true, irubyObject);
    }

    /**
     * Retrieves class variables from Ruby after the evaluation.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved class variables.
     */
    public static void retrieve(RubyObject receiver, BiVariableMap vars) {
        if (vars.isLazy()) return;
        // trying to get variables from receiver;
        updateClassVar(receiver, vars);
        // trying to get variables from topself.
        RubyObject topSelf = (RubyObject) receiver.getRuntime().getTopSelf();
        updateClassVar(topSelf, vars);
    }

    private static void updateClassVar(RubyObject receiver, BiVariableMap vars) {
        List<String> keys = receiver.getMetaClass().getClassVariableNameList();
        for (String key : keys) {
            IRubyObject value = receiver.getMetaClass().getClassVar(key);
            BiVariable var = vars.getVariable(receiver, key);
            if (var != null) {
                var.setRubyObject(value);
            } else {
                var = new ClassVariable(receiver, key, value);
                vars.update(key, var);
            }
        }
    }

    /**
     * Retrieves a class variable by key from Ruby runtime after the evaluation.
     * This method is used when eager retrieval is off.
     *
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved instance variables.
     * @param key instace varible name
     */
    public static void retrieveByKey(RubyObject receiver, BiVariableMap vars, String key) {
        IRubyObject value = null;
        if (receiver == receiver.getRuntime().getTopSelf()
                && receiver.getMetaClass().getClassVariableNameList().contains(key)) {
            value = receiver.getMetaClass().getClassVar(key);
        } else {
            RubyClass klazz = receiver.getMetaClass();
            if (klazz.hasClassVariable(key.intern())) {
                value = klazz.getClassVar(key.intern());
            }
        }
        if (value == null) return;
        
        BiVariable var = vars.getVariable(receiver, key);
        if (var != null) {
            var.setRubyObject(value);
        } else {
            var = new ClassVariable(receiver, key, value);
            vars.update(key, var);
        }
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.ClassVariable.
     */
    public Type getType() {
        return Type.ClassVariable;
    }

    /**
     * Returns true if the given name is a decent Ruby class variable. Unless
     * returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is of a Ruby class variable.
     */
    public static boolean isValidName(Object name) {
        return isValidName(pattern, name);
    }

    /**
     * Injects a class variable value to a parsed Ruby script. This method is 
     * invoked during EvalUnit#run() is executed.
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    public void inject() {
        RubyModule rubyClass = getRubyClass(receiver.getRuntime());
        rubyClass.setClassVar(name, irubyObject);
    }

    /**
     * Attempts to remove this variable from top self or receiver.
     * 
     */
    public void remove() {
        RubyModule rubyClass = getRubyClass(receiver.getRuntime());
        rubyClass.removeClassVariable(name);
    }
}
