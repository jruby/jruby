package org.jruby.ir.passes;

import org.jruby.ir.IRScope;

public class CallSplitter extends CompilerPass {
    public String getLabel() {
        return "Call Splitting";
    }

    public Object execute(IRScope scope, Object... data) {
        scope.splitCalls();
        
        return null;
    }
    
    public void invalidate(IRScope scope) {
        // FIXME: ...
    }
}
