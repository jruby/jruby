package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;

public class ChildScopedBodyCompiler19 extends ChildScopedBodyCompiler {

    public ChildScopedBodyCompiler19(StandardASMCompiler scriptCompiler, String closureMethodName, ASTInspector inspector, StaticScope scope) {
        super(scriptCompiler, closureMethodName, inspector, scope);
        // we force argParamCount to 1 since we always know we'll have [] args
        argParamCount = 1;
    }

    protected String getSignature() {
        return StandardASMCompiler.CLOSURE_SIGNATURE19;
    }
}
