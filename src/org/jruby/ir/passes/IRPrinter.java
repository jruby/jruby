package org.jruby.ir.passes;

import java.util.ArrayList;
import java.util.List;
import org.jruby.ir.IRScope;
import org.jruby.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class IRPrinter extends CompilerPass {
    public static String[] NAMES = new String[] { "printer", "p" };
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = new ArrayList<Class<? extends CompilerPass>>() {{
       add(CFGBuilder.class);
    }};
    
    private static final Logger LOG = LoggerFactory.getLogger("IR_Printer");
    
    public String getLabel() {
        return "Printer";
    }
    
    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }
    
    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder() {
        return true;
    }
    
    public Object execute(IRScope scope, Object... data) {
        LOG.info("----------------------------------------");
        LOG.info(scope.toString());

        // If the cfg of the method is around, print the CFG!
        CFG c = (CFG) data[0];
        if (c != null) {
            LOG.info("\nGraph:\n" + c.toStringGraph());
            LOG.info("\nInstructions:\n" + c.toStringInstrs());
        } else {
            LOG.info("\n  instrs:\n" + scope.toStringInstrs());
            LOG.info("\n  live variables:\n" + scope.toStringVariables());
        }
        
        return null;
    }
    
    public void reset(IRScope scope) {
        // No state...IRPrint will be going away
    }
}
