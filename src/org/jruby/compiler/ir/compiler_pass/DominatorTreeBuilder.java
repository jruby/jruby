package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;

public class DominatorTreeBuilder implements CompilerPass
{
    public DominatorTreeBuilder() { }

    public boolean isPreOrder() { return false; }

    public void run(IRScope s)
    {
        if (s instanceof IRExecutionScope) {
//            System.out.println("Starting build of dom tree for " + s);
            CFG c = ((IRExecutionScope)s).getCFG();
            try {
                c.buildDominatorTree();
            } catch (Exception e) {
                System.out.println("Caught exception building dom tree for " + c.getGraph());
                System.out.println("\nInstructions:\n" + c.toStringInstrs());
            }
        }
    }
}
