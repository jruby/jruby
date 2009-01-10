package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;
import org.objectweb.asm.Label;

public class ChainedChildBodyCompiler extends ChildScopedBodyCompiler {
    ChildScopedBodyCompiler parent;

    @Override
    public void endBody() {
        // return last value from execution
        method.areturn();
        Label end = new Label();
        method.label(end);

        method.end();
    }

    public ChainedChildBodyCompiler(StandardASMCompiler scriptCompiler, String methodName, ASTInspector inspector, StaticScope scope, ChildScopedBodyCompiler parent) {
        super(scriptCompiler, methodName, inspector, scope);
        this.parent = parent;
        this.inNestedMethod = true;
    }
}
