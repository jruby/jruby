/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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
 ***** END LICENSE BLOCK *****/

package org.jruby.compiler.impl;

import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.NotCompilableException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class StackBasedVariableCompiler extends AbstractVariableCompiler {
    private int scopeIndex; // the index of the dynamic scope for higher scopes
    private int baseVariableIndex;

    public StackBasedVariableCompiler(
            StandardASMCompiler.AbstractMethodCompiler methodCompiler,
            SkinnyMethodAdapter method,
            int scopeIndex,
            int argsIndex,
            int closureIndex,
            int firstTempIndex) {
        super(methodCompiler, method, argsIndex, closureIndex, firstTempIndex);
        this.baseVariableIndex = firstTempIndex;
        this.scopeIndex = scopeIndex;
    }

    public void beginMethod(CompilerCallback argsCallback, StaticScope scope) {
        // fill in all vars with nil so compiler is happy about future accesses
        if (scope.getNumberOfVariables() > 0) {
            methodCompiler.loadNil();
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                assignLocalVariable(i);
            }
            method.pop();

            // temp locals must start after last real local
            tempVariableIndex += scope.getNumberOfVariables();
        }

        if (argsCallback != null) {
            argsCallback.call(methodCompiler);
        }
    }

    public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
        throw new NotCompilableException("ERROR: stack-based variables should not be compiling class bodies");
    }

    public void beginClosure(CompilerCallback argsCallback, StaticScope scope) {
        // store the local vars in a local variable
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
        method.astore(scopeIndex);
        
        if (scope != null) {
            methodCompiler.loadNil();
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                assignLocalVariable(i);
            }
            method.pop();
            
            // temp locals must start after last real local
            tempVariableIndex += scope.getNumberOfVariables();
        }
        
        if (argsCallback != null) {
            // load args[0] which will be the IRubyObject representing block args
            method.aload(argsIndex);
            method.pushInt(0);
            method.arrayload();
            argsCallback.call(methodCompiler);
            method.pop(); // clear remaining value on the stack
        }
    }

    public void assignLocalVariable(int index) {
        method.dup();

        method.astore(baseVariableIndex + index);
    }

    public void assignLocalVariable(int index, CompilerCallback value) {
        value.call(methodCompiler);
        assignLocalVariable(index);
    }

    public void assignLocalVariable(int index, int depth) {
        if (depth == 0) {
            assignLocalVariable(index);
        } else {
            method.aload(scopeIndex);
            method.swap();
            method.pushInt(index);
            method.swap();
            method.pushInt(depth);
            method.invokevirtual(p(DynamicScope.class), "setValue", sig(IRubyObject.class, params(Integer.TYPE, IRubyObject.class, Integer.TYPE)));
        }
    }

    public void assignLocalVariable(int index, int depth, CompilerCallback value) {
        if (depth == 0) {
            assignLocalVariable(index, value);
        } else {
            method.aload(scopeIndex);
            method.pushInt(index);
            value.call(methodCompiler);
            method.pushInt(depth);
            method.invokevirtual(p(DynamicScope.class), "setValue", sig(IRubyObject.class, params(Integer.TYPE, IRubyObject.class, Integer.TYPE)));
        }
    }

    public void retrieveLocalVariable(int index) {
        method.aload(baseVariableIndex + index);
    }

    public void retrieveLocalVariable(int index, int depth) {
        if (depth == 0) {
            retrieveLocalVariable(index);
        } else {
            method.aload(scopeIndex);
            method.pushInt(index);
            method.pushInt(depth);
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueOrNil", sig(IRubyObject.class, params(Integer.TYPE, Integer.TYPE, IRubyObject.class)));
        }
    }
}
