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

    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder()  { return true; }

    public void run(IR_Scope s)
    {
        if (s instanceof IR_Method) {
            IR_Method m = (IR_Method)s;
            System.out.println("Building CFG for " + m._name);
            CFG c = new CFG(m);
            System.out.println("CFG for " + m._name + "is ...");
            System.out.println(c.getCFG().toString());
            org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem lvp = new org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem(c);
            lvp.compute_MOP_Solution();
            System.out.println("LVP Output:" + lvp.toString());
        }
    }
}
