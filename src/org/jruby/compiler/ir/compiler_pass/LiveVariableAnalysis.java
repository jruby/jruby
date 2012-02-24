package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class LiveVariableAnalysis implements CompilerPass {
    public boolean isPreOrder() {
        return false;
    }

    public void run(IRScope scope) {
        LiveVariablesProblem lvp = new LiveVariablesProblem();
        String lvpName = lvp.getName();
        
        lvp.setup(scope);
        lvp.compute_MOP_Solution();
        scope.setDataFlowSolution(lvp.getName(), lvp);
//        System.out.println("LVP for " + s + " is: " + lvp);
        for (IRClosure x: scope.getClosures()) {
            lvp = (LiveVariablesProblem) x.getDataFlowSolution(lvpName);
        }
    }
}
