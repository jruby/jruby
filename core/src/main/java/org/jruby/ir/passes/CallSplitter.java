package org.jruby.ir.passes;

import org.jruby.ir.IRScope;

public class CallSplitter extends CompilerPass {
    @Override
    public String getLabel() {
        return "Call Splitting";
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        scope.splitCalls();

        return null;
    }

    @Override
    public void invalidate(IRScope scope) {
        // FIXME: ...
    }
}
