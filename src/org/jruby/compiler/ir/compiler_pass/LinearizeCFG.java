package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;

public class LinearizeCFG implements CompilerPass {
    public static String[] NAMES = new String[] { "linearize", "linearize_cfg" };
    
    public String getLabel() {
        return "Linearize CFG";
    }
    
    public boolean isPreOrder()  {
        return true;
    }

    public void run(IRScope scope) {
        scope.buildLinearization();
    }
}
