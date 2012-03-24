package org.jruby.ir.passes;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.BindingLoadPlacementProblem;
import org.jruby.ir.dataflow.analyses.BindingStorePlacementProblem;

public class AddBindingInstructions extends CompilerPass {
    public static String[] NAMES = new String[] { "add_binding", "add_binding_instructions" };
    
    public String getLabel() {
        return "Add Binding Instructions";
    }
    
    public boolean isPreOrder() {
        return false;
    }

    public Object execute(IRScope s, Object... data) {
        //        if (s.requiresBinding()) {
        BindingStorePlacementProblem fsp = new BindingStorePlacementProblem();
        fsp.setup(s);
        fsp.compute_MOP_Solution();
        fsp.addStoreAndBindingAllocInstructions();

        BindingLoadPlacementProblem frp = new BindingLoadPlacementProblem();
        frp.setup(s);
        frp.compute_MOP_Solution();
        frp.addLoads();
        //       }

        // Run on all nested closures.
        // In the current implementation, nested scopes are processed independently (unlike Live Variable Analysis)
        for (IRClosure c: s.getClosures()) execute(c);

        return null;
    }
}
