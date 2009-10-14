package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;

public class CFG_Builder implements CompilerPass
{
    public CFG_Builder() { }

    public boolean isPreOrder()  { return true; }

    public void run(IR_Scope s) { if (s instanceof IR_ExecutionScope) ((IR_ExecutionScope)s).buildCFG(); }
}
