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

import java.util.Arrays;
import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class HeapBasedVariableCompiler extends AbstractVariableCompiler {
    protected int scopeIndex; // the index of the DynamicScope in the local Java scope to use for depth > 0 variable accesses
    protected int varsIndex; // the index of the IRubyObject[] in the local Java scope to use for depth 0 variable accesses

    public HeapBasedVariableCompiler(
            StandardASMCompiler.AbstractMethodCompiler methodCompiler,
            SkinnyMethodAdapter method,
            int scopeIndex,
            int varsIndex,
            int argsIndex,
            int closureIndex) {
        super(methodCompiler, method, argsIndex, closureIndex);
        
        this.scopeIndex = scopeIndex;
        this.varsIndex = varsIndex;
    }

    public void beginMethod(CompilerCallback argsCallback, StaticScope scope) {
        // store the local vars in a local variable
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", cg.sig(DynamicScope.class));
        method.dup();
        method.astore(scopeIndex);
        method.invokevirtual(cg.p(DynamicScope.class), "getValues", cg.sig(IRubyObject[].class));
        method.astore(varsIndex);

        // fill local vars with nil, to avoid checking every access.
        method.aload(varsIndex);
        methodCompiler.loadNil();
        method.invokestatic(cg.p(Arrays.class), "fill", cg.sig(Void.TYPE, cg.params(Object[].class, Object.class)));
        
        if (argsCallback != null) {
            argsCallback.compile(methodCompiler);
        }
    }

    public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
        // store the local vars in a local variable for preparing the class (using previous scope)
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", cg.sig(DynamicScope.class));
        method.dup();
        method.astore(scopeIndex);
        method.invokevirtual(cg.p(DynamicScope.class), "getValues", cg.sig(IRubyObject[].class));
        method.astore(varsIndex);
        
        // class bodies prepare their own dynamic scope, so let it do that
        bodyPrep.compile(methodCompiler);
        
        // store the new local vars in a local variable
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", cg.sig(DynamicScope.class));
        method.dup();
        method.astore(scopeIndex);
        method.invokevirtual(cg.p(DynamicScope.class), "getValues", cg.sig(IRubyObject[].class));
        method.astore(varsIndex);

        // fill local vars with nil, to avoid checking every access.
        method.aload(varsIndex);
        methodCompiler.loadNil();
        method.invokestatic(cg.p(Arrays.class), "fill", cg.sig(Void.TYPE, cg.params(Object[].class, Object.class)));
    }

    public void beginClosure(CompilerCallback argsCallback, StaticScope scope) {
        // store the local vars in a local variable
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentScope", cg.sig(DynamicScope.class));
        method.dup();
        method.astore(scopeIndex);
        method.invokevirtual(cg.p(DynamicScope.class), "getValues", cg.sig(IRubyObject[].class));
        method.astore(varsIndex);

        if (scope != null) {
            methodCompiler.loadNil();
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                assignLocalVariable(i);
            }
            method.pop();
        }
        
        if (argsCallback != null) {
            // load args[0] which will be the IRubyObject representing block args
            method.aload(argsIndex);
            method.ldc(new Integer(0));
            method.arrayload();
            argsCallback.compile(methodCompiler);
            method.pop(); // clear remaining value on the stack
        }
    }

    public void assignLocalVariable(int index) {
        method.dup();

        method.aload(varsIndex);
        method.swap();
        method.ldc(new Integer(index));
        method.swap();
        method.arraystore();
    }

    public void assignLocalVariable(int index, int depth) {
        if (depth == 0) {
            assignLocalVariable(index);
            return;
        }

        method.dup();

        method.aload(scopeIndex);
        method.swap();
        method.ldc(new Integer(index));
        method.swap();
        method.ldc(new Integer(depth));
        method.invokevirtual(cg.p(DynamicScope.class), "setValue", cg.sig(Void.TYPE, cg.params(Integer.TYPE, IRubyObject.class, Integer.TYPE)));
    }

    public void retrieveLocalVariable(int index) {
        method.aload(varsIndex);
        method.ldc(new Integer(index));
        method.arrayload();
    }

    public void retrieveLocalVariable(int index, int depth) {
        if (depth == 0) {
            retrieveLocalVariable(index);
            return;
        }

        method.aload(scopeIndex);
        method.ldc(new Integer(index));
        method.ldc(new Integer(depth));
        methodCompiler.loadNil();
        method.invokevirtual(cg.p(DynamicScope.class), "getValueOrNil", cg.sig(IRubyObject.class, cg.params(Integer.TYPE, Integer.TYPE, IRubyObject.class)));
    }
}
