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

import org.jruby.Ruby;
import org.jruby.parser.EvalStaticScope;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * An implementation of BiVariable for a persistent local variable. This type of
 * a local variable survives over multiple evaluation.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class PersistentLocalVariable extends AbstractVariable {
    private static String pattern = "([a-z]|_)([a-zA-Z]|_|\\d)*";

    /**
     * Returns an instance of this class. This factory method is used when a
     * persistent local variable is put in {@link BiVariableMap}.
     *
     * @param runtime Ruby runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of PersistentLocalVariable
     */
    public static BiVariable getInstance(Ruby runtime, String name, Object... javaObject) {
        if (name.matches(pattern)) {
            return new PersistentLocalVariable(runtime, name, javaObject);
        }
        return null;
    }

    private PersistentLocalVariable(Ruby runtime, String name, Object... javaObject) {
        super(runtime, name, javaObject);
    }

    /**
     * A constructor used when persistent local variables are retrieved from Ruby.
     *
     * @param name the persistent local variable name
     * @param irubyObject Ruby local object
     */
    PersistentLocalVariable(String name, IRubyObject irubyObject) {
        super(name, irubyObject);
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.LocalVariable.
     */
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
        return isValidName(pattern, name);
    }

    /**
     * Retrieves local variables from Ruby after the evaluation.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved local variables.
     */
    public static void retrieve(Ruby runtime, IRubyObject receiver, BiVariableMap vars) {
        ManyVarsDynamicScope scope =
            (ManyVarsDynamicScope) runtime.getCurrentContext().getCurrentScope();
        if (scope == null) {
            return;
        }
        String[] names = scope.getAllNamesInScope();
        IRubyObject[] values = scope.getValues();
        if (names == null || values == null || names.length == 0 || values.length == 0) {
            return;
        }
        for (int i=0; i<names.length; i++) {
            BiVariable v;
            if ((v = vars.getVariable(names[i])) != null) {
                v.setRubyObject(values[i]);
            } else {
                v = new PersistentLocalVariable(names[i], values[i]);
                vars.update(names[i], v);
            }
        }
    }

    /**
     * Injects a local variable value to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    public void inject(Ruby runtime, IRubyObject receiver) {
        //done in JRubyVariableMap.inject()
    }

    /**
     * Removes this object from {@link BiVariableMap}.
     *
     * @param runtime enviroment where a variabe is removed.
     */
    public void remove(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        DynamicScope currentScope = context.getCurrentScope();
        ManyVarsDynamicScope scope = (ManyVarsDynamicScope) context.getCurrentScope();
        scope = new ManyVarsDynamicScope(new EvalStaticScope(currentScope.getStaticScope()), currentScope);
    }
}
