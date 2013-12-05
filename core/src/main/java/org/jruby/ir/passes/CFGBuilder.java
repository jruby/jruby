package org.jruby.ir.passes;

import org.jruby.ir.IRScope;

public class CFGBuilder extends CompilerPass {
    @Override
    public String getLabel() {
        return "CFG Builder";
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getCFG();
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        return scope.buildCFG();
    }

    @Override
    public void invalidate(IRScope scope) {
        scope.resetCFG();
    }
}
