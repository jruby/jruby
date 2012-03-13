package org.jruby.ir.passes;

import org.jruby.ir.IRScope;

public class CFGBuilder extends CompilerPass {
    public static String[] NAMES = new String[] { "cfg", "cfg_builder" };
    
    public String getLabel() {
        return "CFG Builder";
    }
    public boolean isPreOrder() {
        return true;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getCFG();
    }
    
    public Object execute(IRScope scope, Object... data) {
        return scope.buildCFG();
    }
}
