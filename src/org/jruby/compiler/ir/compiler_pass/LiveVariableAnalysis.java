package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class LiveVariableAnalysis extends CompilerPass {
    public static String[] NAMES = new String[] { "lva", "LVA", "live_variable_analysis" };
    
    public String getLabel() {
        return "Live Variable Analysis";
    }
    
    public boolean isPreOrder() {
        return false;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getDataFlowSolution(LiveVariablesProblem.NAME);
    }

    public Object execute(IRScope scope, Object... data) {
        LiveVariablesProblem lvp = new LiveVariablesProblem(scope);
        lvp.compute_MOP_Solution();
        
        scope.setDataFlowSolution(lvp.getName(), lvp);
        
        return lvp;
    }
}
