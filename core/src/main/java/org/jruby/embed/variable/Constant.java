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
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
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

    private static final String VALID_NAME = "[A-Z]([a-zA-Z]|_)([a-zA-Z]|_|\\d)*";

    //private boolean initialized = false;

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
        if (name.matches(VALID_NAME)) {
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
    Constant(RubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, true, irubyObject);
    }

    //Constant markInitialized() { this.initialized = true; return this; }

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
        updateConstants(getTopSelf(receiver), vars);
    }

    private static void updateConstantsOfSuperClass(RubyObject receiver, BiVariableMap vars) {
        // Super class has many many constants, so this method updates only
        // constans in BiVariableMap.
        final Map<String, RubyModule.ConstantEntry> constantMap =
            getTopSelf(receiver).getMetaClass().getSuperClass().getConstantMap();
        @SuppressWarnings("deprecation")
        final Collection<BiVariable> variables = vars.getVariables();
        // Need to check that this constant has been stored in BiVariableMap.
        for ( final BiVariable variable : variables ) {
            if ( variable.getType() == Type.Constant ) {
                if ( constantMap.containsKey( variable.getName() ) ) {
                    IRubyObject value = constantMap.get( variable.getName() ).value;
                    variable.setRubyObject(value);
                }
            }
        }
    }

    private static void updateConstants(final RubyObject receiver, final BiVariableMap vars) {
        final RubyClass klazz = receiver.getMetaClass();
        final Collection<String> constantNames = klazz.getConstantNames();
        for ( final String name : constantNames ) {
            final IRubyObject value = klazz.getConstant(name);

            final BiVariable var = vars.getVariable(receiver, name);
            if (var == null) {
                vars.update(name, new Constant(receiver, name, value));
            }
            else {
                var.setRubyObject(value);
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
    public static void retrieveByKey(final RubyObject receiver,
        final BiVariableMap vars, final String key) {
        // if the specified key doesn't exist, this method is called before the
        // evaluation. Don't update value in this case.
        IRubyObject value = null;

        final RubyClass klazz = receiver.getMetaClass();
        if ( klazz.getConstantNames().contains(key) ) {
            value = klazz.getConstant(key);
        }
        else if (getTopSelf(receiver).getMetaClass().getConstantNames().contains(key)) {
            value = getTopSelf(receiver).getMetaClass().getConstant(key);
        }
        else if (getTopSelf(receiver).getMetaClass().getSuperClass().getConstantNames().contains(key)) {
            value = getTopSelf(receiver).getMetaClass().getSuperClass().getConstant(key);
        }

        if ( value == null ) return;

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
    @Override
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
        return isValidName(VALID_NAME, name);
    }

    /**
     * Injects a constant value to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     */
    @Override
    public void inject() {
        final Ruby runtime = getRuntime();
        if (receiver == runtime.getTopSelf()) {
            RubyModule rubyModule = getRubyClass(runtime);
            // SSS FIXME: With rubyclass stack gone, this needs a replacement
            if (rubyModule == null) rubyModule = null; // receiver.getRuntime().getCurrentContext().getRubyClass();
            if (rubyModule == null) return;
            rubyModule.storeConstant(name, irubyObject);
        }
        else {
            receiver.getMetaClass().storeConstant(name, irubyObject);
        }
        runtime.getConstantInvalidator(name).invalidate();
        //initialized = true;
    }

    /**
     * Attempts to remove this constant from top self or receiver.
     *
     */
    @Override
    public void remove() {
        final Ruby runtime = getRuntime();
        final IRubyObject rubyName = JavaUtil.convertJavaToRuby(runtime, name);
        final RubyClass metaClass = receiver.getMetaClass();
        if (metaClass.getConstantNames().contains(name)) {
            metaClass.remove_const(runtime.getCurrentContext(), rubyName);
        }
        else if (getTopSelf().getMetaClass().getConstantNames().contains(name)) {
            getTopSelf().getMetaClass().remove_const(runtime.getCurrentContext(), rubyName);
        }
        else if (getTopSelf().getMetaClass().getSuperClass().getConstantNames().contains(name)) {
            getTopSelf().getMetaClass().getSuperClass().remove_const(runtime.getCurrentContext(), rubyName);
        }
    }
}
