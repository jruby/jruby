package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRExecutionScope;

public class CFGBuilder implements CompilerPass {
    public boolean isPreOrder() {
        return true;
    }

    public void run(IRExecutionScope scope) {
        scope.buildCFG();
    }
}
