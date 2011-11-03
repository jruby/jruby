package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class IRPrinter implements CompilerPass {
    private static final Logger LOG = LoggerFactory.getLogger("IR_Printer");

    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder() {
        return true;
    }

    public void run(IRScope s) {
        LOG.info("----------------------------------------");
        LOG.info(s.toString());

        // If the cfg of the method is around, print the CFG!
        if (s instanceof IRExecutionScope) {
            IRExecutionScope scope = (IRExecutionScope) s;

            if (scope.getCFG() != null) LOG.info("\nGraph:\n" + scope.cfg());
        } else if (s instanceof IRMethod) {
            IRMethod m = (IRMethod)s;
            LOG.info("\n  instrs:\n" + m.toStringInstrs());
            LOG.info("\n  live variables:\n" + m.toStringVariables());
        }
    }
}
