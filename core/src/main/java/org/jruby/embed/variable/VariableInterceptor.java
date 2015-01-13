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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * This class is responsible to local variable behavior dependent processing.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class VariableInterceptor {
    //private LocalVariableBehavior behavior;

    /**
     * Constructs an instance with a given local variable behavior.
     *
     * @param behavior local variable behavior
     */
    //public VariableInterceptor(LocalVariableBehavior behavior) {
    //    this.behavior = behavior;
    //}

    //public LocalVariableBehavior getLocalVariableBehavior() {
    //    return behavior;
    //}

    /**
     * Returns an appropriate type of a variable instance to the specified local
     * variable behavior.
     *
     * @param runtime Ruby runtime
     * @param name variable name
     * @param value variable value
     * @return an appropriate type of the variable instance.
     */
    public static BiVariable getVariableInstance(LocalVariableBehavior behavior, RubyObject receiver, String name, Object... value) {
        if (value == null || value.length < 1) {
            return null;
        }
        if ("ARGV".equals(name)) {
            return Argv.getInstance(receiver, name, value);
        }
        switch (behavior) {
            case GLOBAL:
                return LocalGlobalVariable.getInstance(receiver, name, value);
            case BSF:
                BiVariable[] bEntries = {
                    PersistentLocalVariable.getInstance(receiver, name, value),
                    GlobalVariable.getInstance(receiver, name, value)
                };
                return resolve(bEntries);
            case PERSISTENT:
                BiVariable[] pEntries = {
                    GlobalVariable.getInstance(receiver, name, value),
                    InstanceVariable.getInstance(receiver, name, value),
                    ClassVariable.getInstance(receiver, name, value),
                    Constant.getInstance(receiver, name, value),
                    PersistentLocalVariable.getInstance(receiver, name, value)
                };
                return resolve(pEntries);
            default:
                BiVariable[] tEntries = {
                    GlobalVariable.getInstance(receiver, name, value),
                    InstanceVariable.getInstance(receiver, name, value),
                    ClassVariable.getInstance(receiver, name, value),
                    Constant.getInstance(receiver, name, value),
                    TransientLocalVariable.getInstance(receiver, name, value)
                };
                return resolve(tEntries);
        }
    }

    private static BiVariable resolve(BiVariable[] entries) {
        for (BiVariable e : entries) {
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /**
     * Injects variable values from Java to Ruby just before an evaluation or
     * method invocation.
     *
     * @param map a variable map that has name-value pairs to be injected
     * @param runtime Ruby runtime
     * @param scope scope to inject local variable values
     * @param depth depth of a frame to inject local variable values
     * @param receiver a receiver when the script has been evaluated once
     */
    public static void inject(BiVariableMap map, Ruby runtime, ManyVarsDynamicScope scope, int depth, IRubyObject receiver) {
        // lvar might not be given while parsing but be given when evaluating.
        // to avoid ArrayIndexOutOfBoundsException, checks the length of scope.getValues()
        if (scope != null && scope.getValues().length > 0) {
            IRubyObject[] values4Injection = map.getLocalVarValues();
            if (values4Injection != null && values4Injection.length > 0) {
                for (int i = 0; i < values4Injection.length; i++) {
                    scope.setValue(i, values4Injection[i], depth);
                }
            }
        }
        Collection<BiVariable> variables = map.getVariables();
        if ( variables == null ) return;
        for ( final BiVariable var : variables ) var.inject();
    }

    /**
     * Retrieves variable/constant names and values after the evaluation or method
     * invocation.
     *
     * @param map variable map that holds retrieved name-value pairs.
     * @param runtime Ruby runtime
     * @param receiver a receiver when the script has been evaluated once
     */
    public static void retrieve(LocalVariableBehavior behavior, BiVariableMap map, RubyObject receiver) {
        Argv.retrieve(receiver, map);
        switch (behavior) {
            case GLOBAL:
                LocalGlobalVariable.retrieve(receiver, map);
                break;
            case BSF:
                PersistentLocalVariable.retrieve(receiver, map);
                break;
            case PERSISTENT:
                PersistentLocalVariable.retrieve(receiver, map);
            // continues to the default case
            default:
                InstanceVariable.retrieve(receiver, map);
                GlobalVariable.retrieve(receiver, map);
                ClassVariable.retrieve(receiver, map);
                Constant.retrieve(receiver, map);
        }
    }

    /**
     * Retrieves specified variable/constant name and value after the evaluation
     * or method invocation only when it is requested.
     *
     * @param map variable map that holds retrieved name-value pairs.
     * @param runtime Ruby runtime
     * @param receiver a receiver when the script has been evaluated once
     * @
     */
    public static void tryLazyRetrieval(LocalVariableBehavior behavior, BiVariableMap map, IRubyObject receiver, Object key) {
        if (Argv.isValidName(key)) {
            Argv.retrieveByKey((RubyObject)receiver, map, (String)key);
            return;
        }
        switch (behavior) {
            case GLOBAL:
                if (LocalGlobalVariable.isValidName(key)) {
                    LocalGlobalVariable.retrieveByKey(receiver.getRuntime(), map, (String)key);
                }
                break;
            case BSF:
                break;
            case PERSISTENT:
            default:
                if (GlobalVariable.isValidName(key)) {
                    GlobalVariable.retrieveByKey(receiver.getRuntime(), map, (String)key);
                } else if (InstanceVariable.isValidName(key)) {
                    InstanceVariable.retrieveByKey((RubyObject) receiver,map, (String)key);
                } else if (ClassVariable.isValidName(key)) {
                    ClassVariable.retrieveByKey((RubyObject)receiver, map, (String)key);
                } else if (Constant.isValidName(key)) {
                    Constant.retrieveByKey((RubyObject)receiver, map, (String)key);
                }
        }
    }

    /**
     * Clears global variable values from Ruby runtime to behave the same as
     * JSR 223 reference implementation.
     *
     * @param variables a variable list to be cleared from Ruby runtime
     * @param runtime Ruby runtime
     */
    public static void terminateGlobalVariables(LocalVariableBehavior behavior, Collection<BiVariable> variables, Ruby runtime) {
        if (variables == null) return;
        if (LocalVariableBehavior.GLOBAL == behavior) {
            for ( final BiVariable var : variables ) {
                if (BiVariable.Type.LocalGlobalVariable == var.getType()) {
                    String name = var.getName();
                    name = name.startsWith("$") ? name : "$" + name;
                    runtime.getGlobalVariables().set(name, runtime.getNil());
                }
            }
        }
    }

    /**
     * Clears local variables form the variable map so that old local variable
     * name-value pairs are not to be used in successive evaluations.
     *
     * @param varNames variable name list to be cleared
     * @param variables variable value list to be cleared
     */
    public static void terminateLocalVariables(LocalVariableBehavior behavior, List<String> varNames, List<BiVariable> variables) {
        if (variables == null) return;
        if (LocalVariableBehavior.TRANSIENT == behavior) {
            for (int i = 0; i < variables.size(); i++) {
                if (BiVariable.Type.LocalVariable == variables.get(i).getType()) {
                    varNames.remove(i);
                    variables.remove(i);
                }
            }
        }
    }

    /**
     * Checks the given name is whether a legal Ruby variable/constant name or not.
     *
     * @param name a given name to be checked
     * @return true when the name is a legal Ruby variable/constant name, otherwise false.
     */
    public static boolean isKindOfRubyVariable(LocalVariableBehavior behavior, String name) {
        if ("ARGV".equals(name)) return true;
        switch (behavior) {
            case GLOBAL:
                return LocalGlobalVariable.isValidName(name);
            case BSF:
                if (PersistentLocalVariable.isValidName(name)) return true;
                if (GlobalVariable.isValidName(name)) return true;
                return false;
            case PERSISTENT:
                if (GlobalVariable.isValidName(name)) return true;
                if (PersistentLocalVariable.isValidName(name)) return true;
                if (InstanceVariable.isValidName(name)) return true;
                if (Constant.isValidName(name)) return true;
                if (ClassVariable.isValidName(name)) return true;
                return false;
            default:
                if (GlobalVariable.isValidName(name)) return true;
                if (TransientLocalVariable.isValidName(name)) return true;
                if (InstanceVariable.isValidName(name)) return true;
                if (Constant.isValidName(name)) return true;
                if (ClassVariable.isValidName(name)) return true;
                return false;
        }
    }
}