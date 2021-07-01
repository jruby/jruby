package org.jruby.ir.passes;

import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.interpreter.FullInterpreterContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeadCodeElimination extends CompilerPass {
    public static final List<Class<? extends CompilerPass>> DEPENDENCIES = Collections.singletonList(LiveVariableAnalysis.class);

    public String getLabel() {
        return "Dead Code Elimination";
    }

    @Override
    public String getShortLabel() {
        return "DCE";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(FullInterpreterContext fic, Object... data) {
        ((LiveVariablesProblem) data[0]).markDeadInstructions();

        return true;
    }
}
