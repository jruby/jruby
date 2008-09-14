package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;
import org.objectweb.asm.Label;

public class ASMMethodContinuationCompiler extends ASMMethodCompiler {
    AbstractMethodCompiler parent;

    @Override
    public void endMethod() {
        // return last value from execution
        method.areturn();
        Label end = new Label();
        method.label(end);

        method.end();
    }

    public ASMMethodContinuationCompiler(String methodName, ASTInspector inspector, StaticScope scope, StandardASMCompiler scriptCompiler, AbstractMethodCompiler parent) {
        super(methodName, inspector, scope, scriptCompiler);
        this.parent = parent;
    }
}
