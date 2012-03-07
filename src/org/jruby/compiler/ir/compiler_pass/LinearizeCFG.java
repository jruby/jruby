package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;

public class LinearizeCFG extends CompilerPass {
    public static String[] NAMES = new String[] { "linearize", "linearize_cfg" };
    
    public String getLabel() {
        return "Linearize CFG";
    }
    
    public boolean isPreOrder()  {
        return true;
    }

    public Object execute(IRScope scope, Object... data) {
        scope.buildLinearization();
        
        return null;
    }
}
