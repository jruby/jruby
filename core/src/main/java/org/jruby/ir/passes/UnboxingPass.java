package org.jruby.ir.passes;

import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.UnboxableOpsAnalysisProblem;

import java.util.Arrays;
import java.util.List;

public class UnboxingPass extends CompilerPass {
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(LiveVariableAnalysis.class);

    public String getLabel() {
        return "Unboxing Pass";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        UnboxableOpsAnalysisProblem problem = new UnboxableOpsAnalysisProblem();
        problem.setup(scope);
        problem.compute_MOP_Solution();
        problem.unbox();

        // LVA information is no longer valid after the pass
        // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
        (new LiveVariableAnalysis()).invalidate(scope);

        return true;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getUnboxableOpsAnalysisProblem();
    }

    public boolean invalidate(IRScope scope) {
        // Cannot run unboxing more than once on a scope in its current form
        return false;
    }
}
