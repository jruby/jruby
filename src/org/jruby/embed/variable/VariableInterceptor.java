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

import java.util.List;
import org.jruby.Ruby;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * This class is responsible to local variable behavior dependent processings.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class VariableInterceptor {
    private LocalVariableBehavior behavior;

    /**
     * Contructs an instance with a given local variable behavior.
     *
     * @param behavior local variable behavior
     */
    public VariableInterceptor(LocalVariableBehavior behavior) {
        this.behavior = behavior;
    }

    /**
     * Returns an appropriate type of a variable instance to the specified local
     * variable behavior.
     * 
     * @param runtime Ruby runtime
     * @param name variable name
     * @param value variable value
     * @return an appropriate type of the variable instance.
     */
    public BiVariable getVariableInstance(Ruby runtime, String name, Object... value) {
        if (value == null || value.length < 1) {
            return null;
        }
        switch (behavior) {
            case GLOBAL:
                if ("ARGV".equals(name)) {
                    return Constant.getInstance(runtime, name, value);
                }
                return LocalGlobalVariable.getInstance(runtime, name, value);
            case BSF:
                BiVariable[] bEntries = {
                    PersistentLocalVariable.getInstance(runtime, name, value),
                    GlobalVariable.getInstance(runtime, name, value)
                };
                return resolve(bEntries);
            case PERSISTENT:
                BiVariable[] pEntries = {
                    GlobalVariable.getInstance(runtime, name, value),
                    InstanceVariable.getInstance(runtime, name, value),
                    ClassVariable.getInstance(runtime, name, value),
                    Constant.getInstance(runtime, name, value),
                    PersistentLocalVariable.getInstance(runtime, name, value)
                };
                return resolve(pEntries);
            default:
                BiVariable[] tEntries = {
                    GlobalVariable.getInstance(runtime, name, value),
                    InstanceVariable.getInstance(runtime, name, value),
                    ClassVariable.getInstance(runtime, name, value),
                    Constant.getInstance(runtime, name, value),
                    TransientLocalVariable.getInstance(runtime, name, value)
                };
                return resolve(tEntries);
        }
    }

    private BiVariable resolve(BiVariable[] entries) {
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
    public void inject(BiVariableMap map, Ruby runtime, ManyVarsDynamicScope scope, int depth, IRubyObject receiver) {
        if (scope != null) {
            IRubyObject[] values4Injection = map.getLocalVarValues();
            if (values4Injection != null && values4Injection.length > 0) {
                for (int i = 0; i < values4Injection.length; i++) {
                    scope.setValue(i, values4Injection[i], depth);
                }
            }
        }
        List<BiVariable> variables = map.getVariables();
        for (int i=0; i<variables.size(); i++) {
            variables.get(i).inject(runtime, receiver);
        }
    }

    /**
     * Retrieves variable/constant names and values after the evaluation or method
     * invocation.
     *
     * @param map varible map that holds retrieved name-value pairs.
     * @param runtime Ruby runtime
     * @param receiver a receiver when the script has been evaluated once
     */
    public void retrieve(BiVariableMap map, Ruby runtime, IRubyObject receiver) {
        switch (behavior) {
            case GLOBAL:
                LocalGlobalVariable.retrieve(runtime, receiver, map);
                break;
            case BSF:
                PersistentLocalVariable.retrieve(runtime, receiver, map);
                break;
            case PERSISTENT:
                PersistentLocalVariable.retrieve(runtime, receiver, map);
            // continues to the default case
            default:
                InstanceVariable.retrieve(runtime, receiver, map);
                //GlobalVariable.retrieve(runtime, receiver, map);//tryLazyRetrieval
                ClassVariable.retrieve(runtime, receiver, map);
                Constant.retrieve(runtime, receiver, map);
        }
    }

    /**
     * Retrieves specified variable/constant name and value after the evaluation
     * or method invocation only when it is requested.
     *
     * @param map varible map that holds retrieved name-value pairs.
     * @param runtime Ruby runtime
     * @param receiver a receiver when the script has been evaluated once
     * @
     */
    public void tryLazyRetrieval(BiVariableMap map, Ruby runtime, IRubyObject receiver, Object key) {
        switch (behavior) {
            case GLOBAL:
                break;
            case BSF:
                break;
            case PERSISTENT:
            default:
                if (GlobalVariable.isValidName(key)) {
                    GlobalVariable.retrieveByKey(runtime, map, (String)key);
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
    public void terminateGlobalVariables(List<BiVariable> variables, Ruby runtime) {
        if (LocalVariableBehavior.GLOBAL == behavior) {
            for (int i = 0; i < variables.size(); i++) {
                if (BiVariable.Type.GlobalVariable == variables.get(i).getType()) {
                    IRubyObject irobj = JavaEmbedUtils.javaToRuby(runtime, null);
                    runtime.getGlobalVariables().set("$" + variables.get(i).getName(), irobj);
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
    public void terminateLocalVariables(List<String> varNames, List<BiVariable> variables) {
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
     * @return true when the name is a lega Ruby variable/constant name, otherwise false.
     */
    public boolean isKindOfRubyVariable(String name) {
        switch (behavior) {
            case GLOBAL:
                if ("ARGV".equals(name)) {
                    return true;
                }
                return LocalGlobalVariable.isValidName(name);
            case BSF:
                if (PersistentLocalVariable.isValidName(name)) {
                    return true;
                } else if (GlobalVariable.isValidName(name)) {
                    return true;
                }
                return false;
            case PERSISTENT:
                if (GlobalVariable.isValidName(name)) {
                    return true;
                } else if (PersistentLocalVariable.isValidName(name)) {
                    return true;
                } else if (InstanceVariable.isValidName(name)) {
                    return true;
                } else if (Constant.isValidName(name)) {
                    return true;
                } else if (ClassVariable.isValidName(name)) {
                    return true;
                }
                return false;
            default:
                if (GlobalVariable.isValidName(name)) {
                    return true;
                } else if (TransientLocalVariable.isValidName(name)) {
                    return true;
                } else if (InstanceVariable.isValidName(name)) {
                    return true;
                } else if (Constant.isValidName(name)) {
                    return true;
                } else if (ClassVariable.isValidName(name)) {
                    return true;
                }
                return false;
        }
    }
}