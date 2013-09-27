package org.jruby.ir.passes;

import java.util.Arrays;
import java.util.List;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;

public class DeadCodeElimination extends CompilerPass {
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(LiveVariableAnalysis.class);

    public String getLabel() {
        return "Dead Code Elimination";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        ((LiveVariablesProblem) data[0]).markDeadInstructions();

        for (IRClosure cl: scope.getClosures()) {
            run(cl, true);
        }

        return true;
    }

    public void invalidate(IRScope scope) {
        // FIXME: Can we reset this?
    }
}
