package org.jruby.ir.passes;

import java.util.Arrays;
import java.util.List;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.UnboxableOpsAnalysisProblem;

public class UnboxingPass extends CompilerPass {
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(CFGBuilder.class, LiveVariableAnalysis.class);

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

        return true;
    }

    public void invalidate(IRScope scope) {
        // FIXME: Can we reset this?
    }
}
