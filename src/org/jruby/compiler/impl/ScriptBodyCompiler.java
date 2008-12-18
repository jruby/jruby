package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;

/**
 * MethodBodyCompiler is the base compiler for all method bodies.
 */
public class ScriptBodyCompiler extends MethodBodyCompiler {
    public ScriptBodyCompiler(StandardASMCompiler scriptCompiler, String rubyName, String javaName, ASTInspector inspector, StaticScope scope) {
        super(scriptCompiler, rubyName, javaName, inspector, scope);
    }

    @Override
    protected String getSignature() {
        specificArity = false;
        return StandardASMCompiler.METHOD_SIGNATURES[4];
    }

    @Override
    public void endBody() {
        // return last value from execution
        method.areturn();

        // end of variable scope
        method.label(scopeEnd);

        // method is done, declare all variables
        variableCompiler.declareLocals(scope, scopeStart, scopeEnd);

        method.end();
    }
}
