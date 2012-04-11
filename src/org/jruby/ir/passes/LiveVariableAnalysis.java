package org.jruby.ir.passes;

import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;

public class LiveVariableAnalysis extends CompilerPass {
    public String getLabel() {
        return "Live Variable Analysis";
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getDataFlowSolution(LiveVariablesProblem.NAME);
    }

    public Object execute(IRScope scope, Object... data) {
        LiveVariablesProblem lvp = new LiveVariablesProblem(scope);
        lvp.compute_MOP_Solution();
        
        scope.setDataFlowSolution(LiveVariablesProblem.NAME, lvp);
        
        return lvp;
    }
    
    public void invalidate(IRScope scope) {
        scope.setDataFlowSolution(LiveVariablesProblem.NAME, null);
    }
}
