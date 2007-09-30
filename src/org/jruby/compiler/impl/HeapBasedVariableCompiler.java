/*
 * HeapBasedVariableCompiler.java
 * 
 * Created on Jul 13, 2007, 11:23:05 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.ClosureCallback;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.VariableCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public class HeapBasedVariableCompiler extends AbstractVariableCompiler {
    private int scopeIndex; // the index of the DynamicScope in the local Java scope to use for depth > 0 variable accesses
    private int varsIndex; // the index of the IRubyObject[] in the local Java scope to use for depth 0 variable accesses

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

    public void beginMethod(ClosureCallback argsCallback, StaticScope scope) {
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

    public void beginClass(ClosureCallback bodyPrep, StaticScope scope) {
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

    public void beginClosure(ClosureCallback argsCallback, StaticScope scope) {
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
        method.invokevirtual(cg.p(DynamicScope.class), "getValue", cg.sig(IRubyObject.class, cg.params(Integer.TYPE, Integer.TYPE)));
        
        // FIXME: This is a pretty unpleasant perf hit, and it's not required for most local var accesses. We need a better way
        methodCompiler.nullToNil();
    }
}
