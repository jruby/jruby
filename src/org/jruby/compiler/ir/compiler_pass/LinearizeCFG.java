package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRExecutionScope;

public class LinearizeCFG implements CompilerPass {
    public boolean isPreOrder()  {
        return true;
    }

    public void run(IRExecutionScope scope) {
        scope.buildLinearization();
    }
}
