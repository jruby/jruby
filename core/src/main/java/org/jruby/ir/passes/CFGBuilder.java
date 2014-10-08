package org.jruby.ir.passes;

import org.jruby.ir.IRScope;

/**
 * CFGBuilder is mainly a pass to be lazy.  We do not want to build CFG for scopes which are never called.
 *
 * Once we have a CFG that is the base data structure where we interact with instructions.  The original
 * list of instructions from IRBuilder is no longer important.  This is also why this pass is incapable
 * of invalidating the CFG.
  */


public class CFGBuilder extends CompilerPass {
    @Override
    public String getLabel() {
        return "CFG Builder";
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getCFG();
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        return scope.buildCFG();
    }

    @Override
    public boolean invalidate(IRScope scope) {
        // CFG is primal information to a scope and cannot be recreated once generated.
        return false;
    }
}
