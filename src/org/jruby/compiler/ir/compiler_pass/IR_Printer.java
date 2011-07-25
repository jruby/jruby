package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class IR_Printer implements CompilerPass {

    private static final Logger LOG = LoggerFactory.getLogger("IR_Printer");

    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder()  { return true; }

    public void run(IRScope s) {
        System.out.println("----------------------------------------");
        System.out.println(s.toString());

        // If the cfg of the method is around, print the CFG!
        CFG c = null;
        if (s instanceof IRExecutionScope)
            c = ((IRExecutionScope)s).getCFG();

        if (c != null) {
            LOG.debug("\nGraph:\n" + c.getGraph().toString());
            LOG.debug("\nInstructions:\n" + c.toStringInstrs());
        } else if (s instanceof IRMethod) {
            IRMethod m = (IRMethod)s;
            LOG.debug("\n  instrs:\n" + m.toStringInstrs());
            LOG.debug("\n  live variables:\n" + m.toStringVariables());
        }
    }
}
