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
import org.objectweb.asm.Label;
import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class StackBasedVariableCompiler extends AbstractVariableCompiler {
    private int baseVariableIndex;

    public StackBasedVariableCompiler(
            BaseBodyCompiler methodCompiler,
            SkinnyMethodAdapter method,
            StaticScope scope,
            boolean specificArity,
            int argsIndex,
            int firstTempIndex) {
        super(methodCompiler, method, scope, specificArity, argsIndex, firstTempIndex);
        this.baseVariableIndex = firstTempIndex;
    }

    public void beginMethod(CompilerCallback argsCallback, StaticScope scope) {
        // fill in all vars with nil so compiler is happy about future accesses
        if (scope.getNumberOfVariables() > 0) {
            // if we don't have opt args, start after args (they will be assigned later)
            // this is for crap like def foo(a = (b = true; 1)) which numbers b before a
            // FIXME: only starting after required args, since opt args may access others
            // and rest args conflicts with compileRoot using "0" to indicate [] signature.
            int start = scope.getRequiredArgs();
            for (int i = start; i < scope.getNumberOfVariables(); i++) {
                methodCompiler.loadNil();
                assignLocalVariable(i, false);
            }

            // temp locals must start after last real local
            tempVariableIndex += scope.getNumberOfVariables();
        }

        if (argsCallback != null) {
            argsCallback.call(methodCompiler);
        }
    }
    
    public void declareLocals(StaticScope scope, Label start, Label end) {
        // declare locals for Java debugging purposes
        String[] variables = scope.getVariables();
        for (int i = 0; i < variables.length; i++) {
            method.visitLocalVariable(variables[i], ci(IRubyObject.class), null, start, end, baseVariableIndex + i);
        }
    }

    public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
        // class bodies prepare their own dynamic scope, so let it do that
        bodyPrep.call(methodCompiler);
        
        // fill in all vars with nil so compiler is happy about future accesses
        if (scope.getNumberOfVariables() > 0) {
            // if we don't have opt args, start after args (they will be assigned later)
            // this is for crap like def foo(a = (b = true; 1)) which numbers b before a
            // FIXME: only starting after required args, since opt args may access others
            // and rest args conflicts with compileRoot using "0" to indicate [] signature.
            int start = scope.getRequiredArgs();
            for (int i = start; i < scope.getNumberOfVariables(); i++) {
                methodCompiler.loadNil();
                assignLocalVariable(i, false);
            }

            // temp locals must start after last real local
            tempVariableIndex += scope.getNumberOfVariables();
        }
    }

    public void beginClosure(CompilerCallback argsCallback, StaticScope scope) {
        // store the local vars in a local variable
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
        method.astore(methodCompiler.getDynamicScopeIndex());
        
        if (scope != null) {
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                methodCompiler.loadNil();
                assignLocalVariable(i, false);
            }
            
            // temp locals must start after last real local
            tempVariableIndex += scope.getNumberOfVariables();
        }
        
        if (argsCallback != null) {
            // load args[0] which will be the IRubyObject representing block args
            method.aload(argsIndex);
            argsCallback.call(methodCompiler);
        }
    }

    public void beginFlatClosure(CompilerCallback argsCallback, StaticScope scope) {
        throw new NotCompilableException("Can't have flat closure with stack-based scope");
    }

    public void assignLocalVariable(int index, boolean expr) {
        if (expr) {
            method.dup();
        }

        method.astore(baseVariableIndex + index);
    }

    private void assignLocalVariable(int index, CompilerCallback value, boolean expr) {
        value.call(methodCompiler);
        assignLocalVariable(index, expr);
    }

    public void assignLocalVariable(int index, int depth,boolean expr) {
        if (depth == 0) {
            assignLocalVariable(index, expr);
        } else {
            assignHeapLocal(depth, index, expr);
        }
    }

    public void assignLocalVariable(int index, int depth, CompilerCallback value, boolean expr) {
        if (depth == 0) {
            assignLocalVariable(index, value, expr);
        } else {
            assignHeapLocal(value, depth, index, expr);
        }
    }

    public void retrieveLocalVariable(int index) {
        method.aload(baseVariableIndex + index);
    }

    public void retrieveLocalVariable(int index, int depth) {
        if (depth == 0) {
            retrieveLocalVariable(index);
        } else {
            retrieveHeapLocal(depth, index);
        }
    }
}
