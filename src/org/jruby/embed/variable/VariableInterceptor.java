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
import org.jruby.embed.BiVariable;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class VariableInterceptor {
    private LocalVariableBehavior behavior;
    
    public VariableInterceptor(LocalVariableBehavior behavior) {
        this.behavior = behavior;
    }

    public BiVariable getVariableInstance(Ruby runtime, String name, Object... value) {
        if (value == null || value.length < 1) {
            return null;
        }
        switch (behavior) {
            case GLOBAL:
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
                GlobalVariable.retrieve(runtime, receiver, map);
                ClassVariable.retrieve(runtime, receiver, map);
                Constant.retrieve(runtime, receiver, map);
        }
        /*
        if (LocalVariableBehavior.GLOBAL == behavior) {
            LocalGlobalVariable.retrieve(runtime, receiver, map);
        } else {
            InstanceVariable.retrieve(runtime, receiver, map);
            GlobalVariable.retrieve(runtime, receiver, map);
            ClassVariable.retrieve(runtime, receiver, map);
            Constant.retrieve(runtime, receiver, map);

            if (LocalVariableBehavior.PERSISTENT == behavior) {
                PersistentLocalVariable.retrieve(runtime, receiver, map);
            } else if (LocalVariableBehavior.TRANSIENT == behavior) {
                // no operation
            }
        }
        */
    }

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
}