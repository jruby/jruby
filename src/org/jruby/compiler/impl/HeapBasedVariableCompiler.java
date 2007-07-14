/*
 * HeapBasedVariableCompiler.java
 * 
 * Created on Jul 13, 2007, 11:23:05 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import org.jruby.compiler.MethodCompiler;
import org.jruby.compiler.VariableCompiler;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;

/**
 *
 * @author headius
 */
public class HeapBasedVariableCompiler implements VariableCompiler {
    private static final CodegenUtils cg = CodegenUtils.cg;
    private SkinnyMethodAdapter method;
    private StandardASMCompiler.AbstractMethodCompiler methodCompiler;
    private int scopeIndex; // the index of the DynamicScope in the local Java scope to use for depth > 0 variable accesses
    private int varsIndex; // the index of the IRubyObject[] in the local Java scope to use for depth 0 variable accesses

    public HeapBasedVariableCompiler(StandardASMCompiler.AbstractMethodCompiler methodCompiler, SkinnyMethodAdapter method, int scopeIndex, int varsIndex) {
        this.methodCompiler = methodCompiler;
        this.method = method;
        
        this.scopeIndex = scopeIndex;
        this.varsIndex = varsIndex;
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
        methodCompiler.nullToNil();
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
        methodCompiler.nullToNil();
    }

    public void assignLastLine() {
        method.dup();

        method.aload(scopeIndex);
        method.swap();
        method.invokevirtual(cg.p(DynamicScope.class), "setLastLine", cg.sig(Void.TYPE, cg.params(IRubyObject.class)));
    }

    public void retrieveLastLine() {
        method.aload(scopeIndex);
        method.invokevirtual(cg.p(DynamicScope.class), "getLastLine", cg.sig(IRubyObject.class));
        methodCompiler.nullToNil();
    }

    public void retrieveBackRef() {
        method.aload(scopeIndex);
        method.invokevirtual(cg.p(DynamicScope.class), "getBackRef", cg.sig(IRubyObject.class));
        methodCompiler.nullToNil();
    }
}
