package org.jruby.ir.passes;

import java.util.ArrayList;
import java.util.List;
import org.jruby.ir.IRScope;

public class LinearizeCFG extends CompilerPass {
    public static String[] NAMES = new String[] { "linearize", "linearize_cfg" };
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = new ArrayList<Class<? extends CompilerPass>>() {{
       add(CFGBuilder.class);
    }};
        
    public String getLabel() {
        return "Linearize CFG";
    }
    
    public boolean isPreOrder()  {
        return true;
    }
    
    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    public Object execute(IRScope scope, Object... data) {
        scope.buildLinearization();
        
        return null;
    }
}
