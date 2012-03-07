package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;

public class CFGBuilder implements CompilerPass {
    public static String[] NAMES = new String[] { "cfg", "cfg_builder" };
    
    public String getLabel() {
        return "CFG Builder";
    }
    public boolean isPreOrder() {
        return true;
    }

    public void run(IRScope scope) {
        scope.buildCFG();
    }
}
