package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;

public class CallSplitter implements CompilerPass {
    public boolean isPreOrder() {
        return true;
    }

    public void run(IRScope s) {
        if (s instanceof IRExecutionScope) ((IRExecutionScope)s).splitCalls();
    }
}
