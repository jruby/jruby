package org.jruby.ir.passes;

import java.util.Arrays;
import java.util.List;

import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;

public class LiveVariableAnalysis extends CompilerPass {
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(CFGBuilder.class);

    @Override
    public String getLabel() {
        return "Live Variable Analysis";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getDataFlowSolution(LiveVariablesProblem.NAME);
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        LiveVariablesProblem lvp = new LiveVariablesProblem(scope);
        lvp.compute_MOP_Solution();

        scope.setDataFlowSolution(LiveVariablesProblem.NAME, lvp);

        return lvp;
    }

    @Override
    public void invalidate(IRScope scope) {
        scope.setDataFlowSolution(LiveVariablesProblem.NAME, null);
    }
}
