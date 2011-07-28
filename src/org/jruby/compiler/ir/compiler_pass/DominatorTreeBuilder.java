package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class DominatorTreeBuilder implements CompilerPass
{

    private static final Logger LOG = LoggerFactory.getLogger("DominatorTreeBuilder");

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
                LOG.debug("Caught exception building dom tree for {}", c.getGraph());
                LOG.debug("\nInstructions:\n {}", c.toStringInstrs());
            }
        }
    }
}
