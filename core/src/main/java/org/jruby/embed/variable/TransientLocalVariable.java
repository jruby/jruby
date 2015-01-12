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
import org.jruby.RubyObject;

/**
 * An implementation of BiVariable for a transient local variable. This type of
 * a local variable is available during only one evaluation. After the evaluation,
 * the variable vanishes.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class TransientLocalVariable extends AbstractVariable {

    private static final String VALID_NAME = "([a-z]|_)([a-zA-Z]|_|\\d)*";

    /**
     * Returns an instance of this class. This factory method is used when a
     * transient local variable is put in {@link BiVariableMap}.
     *
     * @param runtime Ruby runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of TransientLocalVariable
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if (name.matches(VALID_NAME)) {
            return new TransientLocalVariable(receiver, name, javaObject);
        }
        return null;
    }

    private TransientLocalVariable(RubyObject receiver, String name, Object... javaObjects) {
        super(receiver, name, false);
        updateByJavaObject(receiver.getRuntime(), javaObjects);
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.LocalVariable.
     */
    @Override
    public Type getType() {
        return Type.LocalVariable;
    }

    /**
     * Returns true if the given name is a decent Ruby local variable. Unless
     * returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is of a Ruby local variable.
     */
    public static boolean isValidName(Object name) {
        return isValidName(VALID_NAME, name);
    }

    /**
     * Doesn't do anything since a transient local variable should not be retrieved
     * from Ruby.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved local variables.
     */
    public static void retrieve(RubyObject receiver, BiVariableMap vars) {
        // Does nothing. This type of variavles never survive over evaluations.
    }

    /**
     * Injects a local variable value to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     */
    @Override
    public void inject() {
        //done in BiVariableMap.inject()
    }

    /**
     * Attempts to remove this variable from top self or receiver.
     *
     */
    @Override
    public void remove() {
        // local variables won't survice over the eval on runime.
        // no need to remove lvar from runtime
        /*
        ThreadContext context = receiver.getRuntime().getCurrentContext();
        try {
            DynamicScope currentScope = context.getCurrentScope();
            ManyVarsDynamicScope scope = (ManyVarsDynamicScope) context.getCurrentScope();
            scope = new ManyVarsDynamicScope(StaticScope.newEvalScope(currentScope.getStaticScope()), currentScope);
        } catch (ArrayIndexOutOfBoundsException e) {
            //no context is left.
            //no operation is needed.
        }*/
    }
}
