package org.jruby.compiler;

public class DefinedCallback extends TwoBranchCallback {
    private BodyCompiler context = null;
    
    DefinedCallback(BodyCompiler context) {
        this.context = context;
    }
    
    @Override
    public void invalid() {
        context.pushNull();
    }
}
