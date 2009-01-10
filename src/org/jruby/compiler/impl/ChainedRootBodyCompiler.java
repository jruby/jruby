package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;
import org.objectweb.asm.Label;

public class ChainedRootBodyCompiler extends RootScopedBodyCompiler {
    RootScopedBodyCompiler parent;

    @Override
    public void endBody() {
        // return last value from execution
        method.areturn();
        Label end = new Label();
        method.label(end);

        method.end();
    }

    public ChainedRootBodyCompiler(StandardASMCompiler scriptCompiler, String methodName, ASTInspector inspector, StaticScope scope, RootScopedBodyCompiler parent) {
        super(scriptCompiler, methodName, inspector, scope);
        this.parent = parent;
        this.inNestedMethod = true;
    }
}
