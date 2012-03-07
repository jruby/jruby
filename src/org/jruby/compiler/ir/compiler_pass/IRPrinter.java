package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class IRPrinter extends CompilerPass {
    public static String[] NAMES = new String[] { "printer", "p" };

    private static final Logger LOG = LoggerFactory.getLogger("IR_Printer");
    
    public String getLabel() {
        return "Printer";
    }

    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder() {
        return true;
    }
    
    public Object execute(IRScope scope, Object... data) {
        LOG.info("----------------------------------------");
        LOG.info(scope.toString());

        // If the cfg of the method is around, print the CFG!
        CFG c = scope.getCFG();
        if (c != null) {
            LOG.info("\nGraph:\n" + c.toStringGraph());
            LOG.info("\nInstructions:\n" + c.toStringInstrs());
        } else {
            LOG.info("\n  instrs:\n" + scope.toStringInstrs());
            LOG.info("\n  live variables:\n" + scope.toStringVariables());
        }
        
        return null;
    }
}
