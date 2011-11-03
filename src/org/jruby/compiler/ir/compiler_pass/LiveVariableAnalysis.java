package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class LiveVariableAnalysis implements CompilerPass {
    public boolean isPreOrder() {
        return false;
    }

    public void run(IRScope s) {
        if (!(s instanceof IRMethod)) return;

        IRMethod method = ((IRMethod) s);
        LiveVariablesProblem lvp = new LiveVariablesProblem();
        String lvpName = lvp.getName();
        
        lvp.setup(method);
        lvp.compute_MOP_Solution();
        method.setDataFlowSolution(lvp.getName(), lvp);
//        System.out.println("LVP for " + s + " is: " + lvp);
        for (IRClosure x: method.getClosures()) {
            lvp = (LiveVariablesProblem) x.getDataFlowSolution(lvpName);
        }
    }
}
