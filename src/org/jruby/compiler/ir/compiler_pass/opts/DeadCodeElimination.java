package org.jruby.compiler.ir.compiler_pass.opts;

import java.util.ArrayList;
import java.util.List;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Tuple;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.compiler_pass.LiveVariableAnalysis;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

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
