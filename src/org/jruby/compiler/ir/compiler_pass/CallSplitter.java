package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.util.NoSuchVertexException;

public class CallSplitter implements CompilerPass {
    public boolean isPreOrder() {
        return true;
    }

    public void run(IRScope scope) throws NoSuchVertexException {
        scope.splitCalls();
    }
}
