package org.jruby.compiler.ir.compiler_pass;

import java.util.List;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

public class CallSplitter implements CompilerPass
{
    public CallSplitter() { }

    public boolean isPreOrder()  { return true; }

    public void run(IRScope s) {
        if (s instanceof IRExecutionScope) {
            ((IRExecutionScope)s).getCFG().splitCalls();
        }
    }
}
