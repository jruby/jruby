package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;

public class CallSplitter implements CompilerPass {
    public static String[] NAMES = new String[] {"split_calls"};
    
    public String getLabel() {
        return "Call Splitting";
    }
    
    public boolean isPreOrder() {
        return true;
    }

    public void run(IRScope scope) {
        scope.splitCalls();
    }
}
