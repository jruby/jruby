package org.jruby.compiler.ir.compiler_pass.opts;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class DeadCodeElimination implements CompilerPass {
    public boolean isPreOrder() {
        return false;
    }

    public void run(IRScope s) {
        if (!(s instanceof IRMethod)) return;

        CFG c = ((IRMethod) s).getCFG();
        LiveVariablesProblem lvp = (LiveVariablesProblem) c.getDataFlowSolution(DataFlowConstants.LVP_NAME);
        
        if (lvp == null) {
            lvp = new LiveVariablesProblem();
            lvp.setup(c);
            lvp.compute_MOP_Solution();
            c.setDataFlowSolution(lvp.getName(), lvp);
        }
        
        lvp.markDeadInstructions();
    }
}
