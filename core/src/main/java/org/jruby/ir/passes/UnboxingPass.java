package org.jruby.ir.passes;

import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.UnboxableOpsAnalysisProblem;
import org.jruby.ir.interpreter.FullInterpreterContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UnboxingPass extends CompilerPass {
    public static final List<Class<? extends CompilerPass>> DEPENDENCIES = Collections.singletonList(LiveVariableAnalysis.class);

    public String getLabel() {
        return "Unboxing Pass";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(FullInterpreterContext fic, Object... data) {
        UnboxableOpsAnalysisProblem problem = new UnboxableOpsAnalysisProblem();
        problem.setup(fic);
        problem.compute_MOP_Solution();
        problem.unbox();

        // LVA information is no longer valid after the pass
        // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
        (new LiveVariableAnalysis()).invalidate(fic);

        return true;
    }

    @Override
    public Object previouslyRun(FullInterpreterContext fic) {
        return fic.getDataFlowProblems().get(UnboxableOpsAnalysisProblem.NAME);
    }

    public boolean invalidate(IRScope scope) {
        // Cannot run unboxing more than once on a scope in its current form
        return false;
    }
}
