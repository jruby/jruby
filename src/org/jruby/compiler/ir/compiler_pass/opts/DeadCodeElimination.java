package org.jruby.compiler.ir.compiler_pass.opts;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class DeadCodeElimination implements CompilerPass
{
    public DeadCodeElimination() { }

    public boolean isPreOrder() { return false; }

    public void run(IR_Scope s)
    {
        if (!(s instanceof IR_Method))
            return;

        CFG c = ((IR_Method)s).getCFG();
        LiveVariablesProblem lvp = (LiveVariablesProblem)c.getDataFlowSolution(DataFlowConstants.LVP_NAME);
        if (lvp == null) {
            lvp = new LiveVariablesProblem();
            lvp.setup(c);
            lvp.compute_MOP_Solution();
            c.setDataFlowSolution(lvp.getName(), lvp);
        }
        lvp.markDeadInstructions();
    }
}
