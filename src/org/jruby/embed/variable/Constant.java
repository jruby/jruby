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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.javasupport.JavaUtil;
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
     * @param runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of Constant
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if (name.matches(pattern)) {
            return new Constant(receiver, name, javaObject);
        }
        return null;
    }
    
    private Constant(RubyObject receiver, String name, Object... javaObjects) {
        super(receiver, name, false);
        updateByJavaObject(receiver.getRuntime(), javaObjects);
    }

    /**
     * A constructor used when constants are retrieved from Ruby.
     *
     * @param receiver a receiver object that this variable/constant is originally in. When
     *        the variable/constant is originated from Ruby, receiver may not be null.
     * @param name the constant name
     * @param irubyObject Ruby constant object
     */
    Constant(IRubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, true, irubyObject);
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
    public static void retrieve(RubyObject receiver, BiVariableMap vars) {
        if (vars.isLazy()) return;
        // user defined constants of top level go to a super class
        updateConstantsOfSuperClass(receiver, vars);
        // Constants might have the same names but different receivers.
        updateConstants(receiver, vars);
        RubyObject topSelf = (RubyObject)receiver.getRuntime().getTopSelf();
        updateConstants(topSelf, vars);
    }

    /*
    private static void updateARGV(IRubyObject receiver, BiVariableMap vars) {
        String name = "ARGV".intern();
        IRubyObject argv = receiver.getRuntime().getTopSelf().getMetaClass().fastGetConstant(name);
        if (argv == null || (argv instanceof RubyNil)) return;
        BiVariable var;  // This var is for ARGV.
        // ARGV constant should be only one
        if (vars.containsKey((Object)name)) {
            var = vars.getVariable((RubyObject)receiver.getRuntime().getTopSelf(), name);
            var.setRubyObject(argv);
        } else {
            var = new Constant(receiver.getRuntime().getTopSelf(), name, argv);
            ((Constant) var).markInitialized();
            vars.update(name, var);
        }
    }*/

    private static void updateConstantsOfSuperClass(RubyObject receiver, BiVariableMap vars) {
        // Super class has many many constants, so this method updates only
        // constans in BiVariableMap.
        Map<String, RubyModule.ConstantEntry> map =
            receiver.getRuntime().getTopSelf().getMetaClass().getSuperClass().getConstantMap();
        List<BiVariable> variables = vars.getVariables();
            // Need to check that this constant has been stored in BiVariableMap.
        for (BiVariable variable : variables) {
            if (variable.getType() == Type.Constant) {
                if (map.containsKey(variable.getName())) {
                    IRubyObject value = map.get(variable.getName()).value;
                    variable.setRubyObject(value);
                }
            }
        }
    }

    private static void updateConstants(RubyObject receiver, BiVariableMap vars) {
        Collection<String> names = receiver.getMetaClass().getConstantNames();
        for (String name : names) {
            IRubyObject value = receiver.getMetaClass().getConstant(name);
            BiVariable var = null;
            List<String> savedNames = vars.getNames();
            // Need to check that this constant has been stored in BiVariableMap.
            for (int i=0; i<savedNames.size(); i++) {
                if (name.equals(savedNames.get(i))) {
                    var = (BiVariable) vars.getVariables().get(i);
                    if (receiver == var.getReceiver()) {
                        var.setRubyObject(value);
                    } else {
                        var = null;
                    }
                }
            }
            if (var == null) {
                var = new Constant(receiver, name, value);
                ((Constant) var).markInitialized();
                vars.update(name, var);
            }
        }
    }

    /**
     * Retrieves a constant by key from Ruby runtime after the evaluation.
     * This method is used when eager retrieval is off.
     *
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved instance variables.
     * @param key instace varible name
     */
    public static void retrieveByKey(RubyObject receiver, BiVariableMap vars, String key) {
        // if the specified key doesn't exist, this method is called before the
        // evaluation. Don't update value in this case.
        IRubyObject value = null;
        if (receiver.getMetaClass().getConstantNames().contains(key)) {
            value = receiver.getMetaClass().getConstant(key);
        } else if (receiver.getRuntime().getTopSelf().getMetaClass().getConstantNames().contains(key)) {
            value = receiver.getRuntime().getTopSelf().getMetaClass().getConstant(key);
        } else if (receiver.getRuntime().getTopSelf().getMetaClass().getSuperClass().getConstantNames().contains(key)) {
            value = receiver.getRuntime().getTopSelf().getMetaClass().getSuperClass().getConstant(key);
        }
        if (value == null) return;

        // the specified key is found, so let's update
        BiVariable var = vars.getVariable(receiver, key);
        if (var != null) {
            var.setRubyObject(value);
        } else {
            var = new Constant(receiver, key, value);
            vars.update(key, var);
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
     */
    public void inject() {
        if (receiver == receiver.getRuntime().getTopSelf()) {
            RubyModule rubyModule = getRubyClass(receiver.getRuntime());
            if (rubyModule == null) rubyModule = receiver.getRuntime().getCurrentContext().getRubyClass();
            if (rubyModule == null) return;

            rubyModule.storeConstant(name, irubyObject);
        } else {
            receiver.getMetaClass().storeConstant(name, irubyObject);
        }
        receiver.getRuntime().getConstantInvalidator(name).invalidate();
        initialized = true;
    }

    /**
     * Attempts to remove this constant from top self or receiver.
     *
     */
    public void remove() {
        IRubyObject rubyName = JavaUtil.convertJavaToRuby(receiver.getRuntime(), name);
        if (receiver.getMetaClass().getConstantNames().contains(name)) {
            receiver.getMetaClass().remove_const(receiver.getRuntime().getCurrentContext(), rubyName);
        } else if (receiver.getRuntime().getTopSelf().getMetaClass().getConstantNames().contains(name)) {
            receiver.getRuntime().getTopSelf().getMetaClass().remove_const(receiver.getRuntime().getCurrentContext(), rubyName);
        } else if (receiver.getRuntime().getTopSelf().getMetaClass().getSuperClass().getConstantNames().contains(name)) {
            receiver.getRuntime().getTopSelf().getMetaClass().getSuperClass().remove_const(receiver.getRuntime().getCurrentContext(), rubyName);
        }
    }
}
