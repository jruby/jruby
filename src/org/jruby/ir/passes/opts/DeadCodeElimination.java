package org.jruby.ir.passes.opts;

import java.util.ArrayList;
import java.util.List;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.Tuple;
import org.jruby.ir.passes.CompilerPass;
import org.jruby.ir.passes.LiveVariableAnalysis;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;

public class DeadCodeElimination extends CompilerPass {
    public static String[] NAMES = new String[] {"dce", "DCE", "dead_code"};
    public static List<Tuple<Class<CompilerPass>, DependencyType>> DEPENDENCIES = new ArrayList<Tuple<Class<CompilerPass>, DependencyType>>() {{
       add(new Tuple(LiveVariableAnalysis.class, CompilerPass.DependencyType.RETRIEVE)); 
    }};
    
    public String getLabel() {
        return "Dead Code Elimination";
    }
    
    public boolean isPreOrder() {
        return false;
    }

    @Override
    public List<Tuple<Class<CompilerPass>, DependencyType>> getDependencies() {
        return DEPENDENCIES;
    }
    
    @Override
    public Object execute(IRScope scope, Object... data) {
        ((LiveVariablesProblem) data[0]).markDeadInstructions();

        for (IRClosure cl: scope.getClosures()) {
            run(cl);
        }
        
        return true;
    }
}
