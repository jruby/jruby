package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;

public class ChainedChildBodyCompiler19 extends ChainedChildBodyCompiler {
    public ChainedChildBodyCompiler19(StandardASMCompiler scriptCompiler, String methodName, ASTInspector inspector, StaticScope scope, ChildScopedBodyCompiler parent) {
        super(scriptCompiler, methodName, inspector, scope, parent);
        // we force argParamCount to 1 since we always know we'll have [] args
        argParamCount = 1;
    }

    @Override
    protected String getSignature() {
        return StandardASMCompiler.getStaticClosure19Signature(script.getClassname());
    }

    @Override
    public ChainedChildBodyCompiler outline(String methodName) {
        // chain to the next segment of this giant method
        method.aload(StandardASMCompiler.THIS);

        // load all arguments straight through
        for (int i = 1; i <= 4; i++) {
            method.aload(i);
        }
        // we append an index to ensure two identical method names will not conflict
        methodName = methodName + "_" + script.getAndIncrementMethodIndex();
        method.invokestatic(script.getClassname(), methodName, getSignature());

        ChainedChildBodyCompiler19 methodCompiler = new ChainedChildBodyCompiler19(script, methodName, inspector, scope, this);

        methodCompiler.beginChainedMethod();

        return methodCompiler;
    }
}
