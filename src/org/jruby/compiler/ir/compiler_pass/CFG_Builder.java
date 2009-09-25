package org.jruby.compiler.ir.compiler_pass;

import java.util.Map;
import java.util.HashMap;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;

public class CFG_Builder implements CompilerPass
{
    public CFG_Builder() { }

    public boolean isPreOrder()  { return true; }

    public void run(IR_Scope s) { s.buildCFG(); }
}
