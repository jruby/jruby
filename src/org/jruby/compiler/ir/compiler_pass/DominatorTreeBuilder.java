package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

public class DominatorTreeBuilder implements CompilerPass
{
    public DominatorTreeBuilder() { }

    public boolean isPreOrder() { return false; }

    public void run(IR_Scope s)
    {
        if (s instanceof IR_ExecutionScope) {
            ((IR_ExecutionScope)s).getCFG().buildDominatorTree();
        }
    }
}
