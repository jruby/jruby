package org.jruby.compiler.ir.compiler_pass;

import java.util.ArrayList;
import java.util.List;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Tuple;

public class LinearizeCFG extends CompilerPass {
    public static String[] NAMES = new String[] { "linearize", "linearize_cfg" };
    public static List<Tuple<Class<CompilerPass>, DependencyType>> DEPENDENCIES = new ArrayList<Tuple<Class<CompilerPass>, DependencyType>>() {{
       add(new Tuple(CFGBuilder.class, CompilerPass.DependencyType.RETRIEVE)); 
    }};
        
    public String getLabel() {
        return "Linearize CFG";
    }
    
    public boolean isPreOrder()  {
        return true;
    }
    
    @Override
    public List<Tuple<Class<CompilerPass>, DependencyType>> getDependencies() {
        return DEPENDENCIES;
    }

    public Object execute(IRScope scope, Object... data) {
        scope.buildLinearization();
        
        return null;
    }
}
