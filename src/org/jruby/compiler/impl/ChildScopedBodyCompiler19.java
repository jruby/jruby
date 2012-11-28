package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;

public class ChildScopedBodyCompiler19 extends ChildScopedBodyCompiler {

    public ChildScopedBodyCompiler19(StandardASMCompiler scriptCompiler, String closureMethodName, String rubyName, ASTInspector inspector, StaticScope scope, int scopeIndex) {
        super(scriptCompiler, closureMethodName, rubyName, inspector, scope, scopeIndex);
        // we force argParamCount to 1 since we always know we'll have [] args
        argParamCount = 1;
    }

    protected String getSignature() {
        return StandardASMCompiler.getStaticClosure19Signature(script.getClassname());
    }

    public ChainedChildBodyCompiler outline(String methodName) {
        // chain to the next segment of this giant method
        method.aload(StandardASMCompiler.THIS);

        // load all arguments straight through
        for (int i = 1; i <= 4; i++) {
            method.aload(i);
        }
        // we append an index to ensure two identical method names will not conflict
        // TODO: make this match general method name structure with SYNTHETIC in place
        methodName = "chained_" + script.getAndIncrementMethodIndex() + "_" + methodName;
        method.invokestatic(script.getClassname(), methodName, getSignature());

        ChainedChildBodyCompiler19 methodCompiler = new ChainedChildBodyCompiler19(script, methodName, rubyName, inspector, scope, this);

        methodCompiler.beginChainedMethod();

        return methodCompiler;
    }
}
