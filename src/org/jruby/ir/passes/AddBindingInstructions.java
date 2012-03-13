package org.jruby.ir.passes;

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
        if (!(s instanceof IRMethod)) return null;

        IRMethod m = (IRMethod) s;
        //        if (m.requiresBinding()) {
        BindingStorePlacementProblem fsp = new BindingStorePlacementProblem();
        fsp.setup(m);
        fsp.compute_MOP_Solution();
        fsp.addStoreAndBindingAllocInstructions();

        BindingLoadPlacementProblem frp = new BindingLoadPlacementProblem();
        frp.setup(m);
        frp.compute_MOP_Solution();
        frp.addLoads();
        //       }
        
        return null;
    }
}
