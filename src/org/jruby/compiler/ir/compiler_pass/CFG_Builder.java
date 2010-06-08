package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;

public class CFG_Builder implements CompilerPass
{
    public CFG_Builder() { }

    public boolean isPreOrder()  { return true; }

    public void run(IRScope s) { if (s instanceof IRExecutionScope) ((IRExecutionScope)s).buildCFG(); }
}
