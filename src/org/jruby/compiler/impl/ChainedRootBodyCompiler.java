package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;
import org.objectweb.asm.Label;

public class ChainedRootBodyCompiler extends RootScopedBodyCompiler {
    @Override
    public void endBody() {
        // return last value from execution
        method.areturn();
        Label end = new Label();
        method.label(end);

        method.end();
    }

    public ChainedRootBodyCompiler(StandardASMCompiler scriptCompiler, String methodName, String rubyName, ASTInspector inspector, StaticScope scope, RootScopedBodyCompiler parent) {
        super(scriptCompiler, methodName, rubyName, inspector, scope, parent.scopeIndex);
        this.inNestedMethod = true;
    }

    public boolean isSimpleRoot() {
        return false;
    }
}
