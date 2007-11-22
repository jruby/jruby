/*
 * HeapBasedVariableCompiler.java
 * 
 * Created on Jul 13, 2007, 11:23:05 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import org.jruby.compiler.ClosureCallback;
import org.jruby.parser.StaticScope;

/**
 *
 * @author headius
 */
public class BoxedVariableCompiler extends HeapBasedVariableCompiler {
    private StaticScope scope; // the static scope for the currently compiling method

    public BoxedVariableCompiler(
            StandardASMCompiler.AbstractMethodCompiler methodCompiler,
            SkinnyMethodAdapter method,
            int scopeIndex,
            int varsIndex,
            int argsIndex,
            int closureIndex) {
        super(methodCompiler, method,scopeIndex, varsIndex, argsIndex, closureIndex);
        
        this.scopeIndex = scopeIndex;
        this.varsIndex = varsIndex;
    }

    public void beginMethod(ClosureCallback argsCallback, StaticScope scope) {
        this.scope = scope; 
        
        // fill non-captured Java local vars with nil as well
        if (scope != null) {
            methodCompiler.loadNil();
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                if (scope.isCaptured(i)) continue;
                
                assignLocalVariable(i);
            }
            method.pop();
        }
        
        super.beginMethod(argsCallback, scope);
    }

    public void beginClass(ClosureCallback bodyPrep, StaticScope scope) {
        assert false : "Do not use boxed var compiler for class bodies";
    }

    public void beginClosure(ClosureCallback argsCallback, StaticScope scope) {
        this.scope = scope;

        // fill non-captured Java local vars with nil as well
        if (scope != null) {
            methodCompiler.loadNil();
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                if (scope.isCaptured(i)) continue;
                
                assignLocalVariable(i);
            }
            method.pop();
        }
        
        super.beginClosure(argsCallback, scope);
    }

    public void assignLocalVariable(int index) {
        method.dup();

        if (scope.isCaptured(index)) {
            super.assignLocalVariable(index);
        } else {
            // non-captured var, just use locals
            method.astore(10 + index);
        }
    }

    public void retrieveLocalVariable(int index) {
        if (scope.isCaptured(index)) {
            super.retrieveLocalVariable(index);
        } else {
            // non-captured, use java local vars
            method.aload(10 + index);
        }
    }
}
