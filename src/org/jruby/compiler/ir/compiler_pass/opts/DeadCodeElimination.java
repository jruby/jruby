package org.jruby.compiler.ir.compiler_pass.opts;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class DeadCodeElimination implements CompilerPass
{
    public DeadCodeElimination() { }

    public boolean isPreOrder() { return false; }

    public void run(IR_Scope s)
    {
        if (!(s instanceof IR_Method))
            return;

        LiveVariablesProblem lvp = new LiveVariablesProblem();
//        System.out.println("------- Live variable analysis output for scope -------");
        lvp.compute_MOP_Solution(s.getCFG());
//        System.out.println(s.toString());
//        System.out.println(lvp.toString());
        lvp.markDeadInstructions();
    }
}
