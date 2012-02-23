package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.util.NoSuchVertexException;

public class CFGBuilder implements CompilerPass {
    public boolean isPreOrder() {
        return true;
    }

    public void run(IRScope scope) throws NoSuchVertexException {
        scope.buildCFG();
    }
}
