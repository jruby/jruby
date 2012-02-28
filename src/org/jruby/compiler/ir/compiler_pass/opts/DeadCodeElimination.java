package org.jruby.compiler.ir.compiler_pass.opts;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class DeadCodeElimination implements CompilerPass {
    public boolean isPreOrder() {
        return false;
    }

    public void run(IRScope scope) {
        LiveVariablesProblem lvp = (LiveVariablesProblem) scope.getDataFlowSolution(DataFlowConstants.LVP_NAME);

        if (lvp == null) {
            lvp = new LiveVariablesProblem();
            lvp.setup(scope);
            lvp.compute_MOP_Solution();
            scope.setDataFlowSolution(lvp.getName(), lvp);
        }
        
        lvp.markDeadInstructions();

        // Run on nested closures!
        for (IRClosure cl: scope.getClosures()) {
            run(cl);
        }
    }
}
