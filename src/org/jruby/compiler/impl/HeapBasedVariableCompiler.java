/*
 ***** BEGIN LICENSE BLOCK *****
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

import org.jruby.Ruby;
import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;
import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class HeapBasedVariableCompiler extends AbstractVariableCompiler {
    public HeapBasedVariableCompiler(
            BaseBodyCompiler methodCompiler,
            SkinnyMethodAdapter method,
            StaticScope scope,
            boolean specificArity,
            int argsIndex,
            int firstTempIndex) {
        super(methodCompiler, method, scope, specificArity, argsIndex, firstTempIndex);
    }

    public void beginMethod(CompilerCallback argsCallback, StaticScope scope) {
        // store the local vars in a local variable if there are any
        if (scope.getNumberOfVariables() > 0) {
            methodCompiler.loadThreadContext();
            methodCompiler.invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
            method.astore(methodCompiler.getDynamicScopeIndex());

            // if more than 4 locals, get and populate the locals array too
            if (scope.getNumberOfVariables() > 4) {
                method.aload(methodCompiler.getDynamicScopeIndex());
                method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
                method.astore(methodCompiler.getVarsArrayIndex());

                // fill local vars with nil, to avoid checking every access.
                method.aload(methodCompiler.getVarsArrayIndex());
                methodCompiler.loadRuntime();
                methodCompiler.invokeUtilityMethod("fillNil", sig(void.class, IRubyObject[].class, Ruby.class));
            }
        }

        if (argsCallback != null) {
            argsCallback.call(methodCompiler);
        }
        
        // default for starting tempVariableIndex is ok
    }
    
    public void declareLocals(StaticScope scope, Label start, Label end) {
        // declare locals for Java debugging purposes
        method.visitLocalVariable("locals", ci(DynamicScope.class), null, start, end, methodCompiler.getDynamicScopeIndex());
    }

    public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
        // store the local vars in a local variable for preparing the class (using previous scope)
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
        method.dup();
        method.astore(methodCompiler.getDynamicScopeIndex());
        method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
        method.astore(methodCompiler.getVarsArrayIndex());
        
        // class bodies prepare their own dynamic scope, so let it do that
        bodyPrep.call(methodCompiler);
        
        // store the new local vars in a local variable
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
        method.astore(methodCompiler.getDynamicScopeIndex());

        // if more than 4 locals, get the locals array too
        if (scope.getNumberOfVariables() > 4) {
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
            method.astore(methodCompiler.getVarsArrayIndex());
        }

        if (scope != null && scope.getNumberOfVariables() >= 1) {
            switch (scope.getNumberOfVariables()) {
            case 1:
                methodCompiler.loadNil();
                assignLocalVariable(0, false);
                break;
            case 2:
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,false);
                break;
            case 3:
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,true);
                assignLocalVariable(2,false);
                break;
            case 4:
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,true);
                assignLocalVariable(2,true);
                assignLocalVariable(3,false);
                break;
            default:
                method.aload(methodCompiler.getVarsArrayIndex());
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,true);
                assignLocalVariable(2,true);
                assignLocalVariable(3,false);
                methodCompiler.loadRuntime();
                methodCompiler.invokeUtilityMethod("fillNil", sig(void.class, IRubyObject[].class, Ruby.class));
            }
        }
    }

    public void beginClosure(CompilerCallback argsCallback, StaticScope scope) {
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
        method.astore(methodCompiler.getDynamicScopeIndex());

        // if more than 4 locals, get the locals array too
        if (scope.getNumberOfVariables() > 4) {
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
            method.astore(methodCompiler.getVarsArrayIndex());
        }
        
        if (scope != null && scope.getNumberOfVariables() >= 1) {
            switch (scope.getNumberOfVariables()) {
            case 1:
                methodCompiler.loadNil();
                assignLocalVariable(0, false);
                break;
            case 2:
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,false);
                break;
            case 3:
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,true);
                assignLocalVariable(2,false);
                break;
            case 4:
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,true);
                assignLocalVariable(2,true);
                assignLocalVariable(3,false);
                break;
            default:
                method.aload(methodCompiler.getVarsArrayIndex());
                methodCompiler.loadNil();
                assignLocalVariable(0,true);
                assignLocalVariable(1,true);
                assignLocalVariable(2,true);
                assignLocalVariable(3,false);
                methodCompiler.loadRuntime();
                methodCompiler.invokeUtilityMethod("fillNil", sig(void.class, IRubyObject[].class, Ruby.class));
            }
        }
        
        if (argsCallback != null) {
            // in 1.8 mode, this will be a RubyArray containing the arguments
            // in 1.9 mode, this will be an IRubyObject[]
            method.aload(argsIndex);
            argsCallback.call(methodCompiler);
        }
    }

    public void beginFlatClosure(CompilerCallback argsCallback, StaticScope scope) {
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
        method.astore(methodCompiler.getDynamicScopeIndex());

        // if more than 4 locals, get the locals array too
        if (scope.getNumberOfVariables() > 4) {
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
            method.astore(methodCompiler.getVarsArrayIndex());
        }

        // no variable initialization, because we're reusing parent's scope (flat)

        if (argsCallback != null) {
            // load args[0] which will be the IRubyObject representing block args
            method.aload(argsIndex);
            argsCallback.call(methodCompiler);
        }
    }

    public void assignLocalVariable(int index,boolean expr) {
        switch (index) {
        case 0:
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueZeroDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        case 1:
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueOneDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        case 2:
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueTwoDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        case 3:
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueThreeDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        default:
            if (expr) method.dup();
            method.aload(methodCompiler.getVarsArrayIndex());
            method.swap();
            method.pushInt(index);
            method.swap();
            method.arraystore();
            break;
        }

        // for specific-index cases, clean up non-expression values
    }

    public void assignLocalVariable(int index, CompilerCallback value, boolean expr) {
        switch (index) {
        case 0:
            method.aload(methodCompiler.getDynamicScopeIndex());
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueZeroDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        case 1:
            method.aload(methodCompiler.getDynamicScopeIndex());
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueOneDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        case 2:
            method.aload(methodCompiler.getDynamicScopeIndex());
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueTwoDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        case 3:
            method.aload(methodCompiler.getDynamicScopeIndex());
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueThreeDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            if (!expr) method.pop();
            break;
        default:
            method.aload(methodCompiler.getVarsArrayIndex());
            method.pushInt(index);
            value.call(methodCompiler);
            if (expr) method.dup_x2();
            method.arraystore();
            break;
        }
    }

    public void assignLocalVariable(int index, int depth,boolean expr) {
        if (depth == 0) {
            assignLocalVariable(index, expr);
            return;
        }

        assignHeapLocal(depth, index, expr);
    }

    public void assignLocalVariable(int index, int depth, CompilerCallback value,boolean expr) {
        if (depth == 0) {
            assignLocalVariable(index, value, expr);
            return;
        }

        assignHeapLocal(value, depth, index, expr);
    }

    public void retrieveLocalVariable(int index) {
        switch (index) {
        case 0:
            method.aload(methodCompiler.getDynamicScopeIndex());
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueZeroDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        case 1:
            method.aload(methodCompiler.getDynamicScopeIndex());
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueOneDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        case 2:
            method.aload(methodCompiler.getDynamicScopeIndex());
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueTwoDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        case 3:
            method.aload(methodCompiler.getDynamicScopeIndex());
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueThreeDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        default:
            method.aload(methodCompiler.getVarsArrayIndex());
            method.pushInt(index);
            method.arrayload();
        }
    }

    public void retrieveLocalVariable(int index, int depth) {
        if (depth == 0) {
            retrieveLocalVariable(index);
            return;
        }
        
        retrieveHeapLocal(depth, index);
    }
}
