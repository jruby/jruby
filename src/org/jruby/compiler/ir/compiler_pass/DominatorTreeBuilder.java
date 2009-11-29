package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;

public class DominatorTreeBuilder implements CompilerPass
{
    public DominatorTreeBuilder() { }

    public boolean isPreOrder() { return false; }

    public void run(IR_Scope s)
    {
        if (s instanceof IR_ExecutionScope) {
//            System.out.println("Starting build of dom tree for " + s);
            CFG c = ((IR_ExecutionScope)s).getCFG();
            try {
                c.buildDominatorTree();
            } catch (Exception e) {
                System.out.println("Caught exception building dom tree for " + c.getGraph());
                System.out.println("\nInstructions:\n" + c.toStringInstrs());
            }
        }
    }
}
