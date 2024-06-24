/**
 * **** BEGIN LICENSE BLOCK *****
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
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.runtime.ivars.VariableAccessor;

/**
 * An implementation of BiVariable for a Ruby instance variable.
 *
 * @author Yoko Harada &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
public class InstanceVariable extends AbstractVariable {

    private static final String VALID_NAME = "@([a-zA-Z]|_)([a-zA-Z]|_|\\d)*";

    /**
     * Returns an instance of this class. This factory method is used when an instance
     * variable is put in {@link BiVariableMap}.
     *
     * @param receiver Receiver object returned when a script is evaluated.
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of InstanceVariable
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if (name.matches(VALID_NAME)) {
            return new InstanceVariable(receiver, name, javaObject);
        }
        return null;
    }

    private InstanceVariable(RubyObject receiver, String name, Object... javaObjects) {
        super(receiver, name, false);
        updateByJavaObject(receiver.getRuntime(), javaObjects);
    }

    /**
     * A constructor used when instance variables are retrieved from Ruby.
     *
     * @param receiver a receiver object that this variable/constant is originally in. When
     *        the variable/constant is originated from Ruby, receiver may not be null.
     * @param name the instance variable name
     * @param irubyObject Ruby instance object
     */
    public InstanceVariable(IRubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, true, irubyObject);
    }

    InstanceVariable(RubyObject receiver, String name, IRubyObject irubyObject) {
        this((IRubyObject) receiver, name, irubyObject);
    }

    /**
     * Retrieves instance variables from Ruby after the evaluation.
     *
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved instance variables.
     */
    public static void retrieve(RubyObject receiver, BiVariableMap vars) {
        if (vars.isLazy()) return;
        updateInstanceVar(receiver, vars);
        updateInstanceVar(getTopSelf(receiver), vars);
    }

    static void updateInstanceVar(final RubyObject receiver, final BiVariableMap vars) {
        receiver.forEachInstanceVariable((name, value) -> updateVariable(receiver, vars, name, value));
    }

    private static void updateVariable(RubyObject receiver, BiVariableMap vars, String key, IRubyObject value) {
        BiVariable var = vars.getVariable(receiver, key);
        if (var != null) {
            var.setRubyObject(value);
        } else {
            var = new InstanceVariable(receiver, key, value);
            vars.update(key, var);
        }
    }

    /**
     * Retrieves a instance variable by key from Ruby runtime after the evaluation.
     * This method is used when eager retrieval is off.
     *
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved instance variables.
     * @param key instace varible name
     */
    public static void retrieveByKey(RubyObject receiver, BiVariableMap vars, String key) {
        // if the specified key doesn't exist, this method is called before the
        // evaluation. Don't update value in this case.
        VariableAccessor accessor = receiver.getMetaClass().getVariableTableManager().getVariableAccessorForRead(key);
        if (accessor == VariableAccessor.DUMMY_ACCESSOR) return;

        // the specified key is found, so let's update
        // TODO: this would break if the variable is not IRubyObject, because this is not filtering non-ivars
        IRubyObject value = (IRubyObject) accessor.get(receiver);
        updateVariable(receiver, vars, key, value);
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.InstanceVariable.
     */
    @Override
    public Type getType() {
        return Type.InstanceVariable;
    }

    /**
     * Returns true if the given name is a decent Ruby instance variable. Unless
     * returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is of a Ruby instance variable.
     */
    public static boolean isValidName(Object name) {
        return isValidName(VALID_NAME, name);
    }

    /**
     * Injects an instance variable value to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     */
    @Override
    public void inject() {
        ((RubyObject) getReceiver()).setInstanceVariable(name, getRubyObject());
    }

    /**
     * Attempts to remove this variable from top self or receiver.
     *
     */
    @Override
    public void remove() {
        ((RubyObject) getReceiver()).removeInstanceVariable(name);
    }
}
