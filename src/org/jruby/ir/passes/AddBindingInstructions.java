package org.jruby.ir.passes;

import java.util.HashMap;
import java.util.Map;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.BindingLoadPlacementProblem;
import org.jruby.ir.dataflow.analyses.BindingStorePlacementProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.representations.BasicBlock;

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

        BindingLoadPlacementProblem frp = new BindingLoadPlacementProblem();
        frp.setup(s);
        frp.compute_MOP_Solution();

        // Add stores and loads, assigning an equivalent tmp-var for each local var
        Map<Operand, Operand> varRenameMap = new HashMap<Operand, Operand>();
        fsp.addStoreAndBindingAllocInstructions(varRenameMap);
        frp.addLoads(varRenameMap);

        // Rename all local var uses with their tmp-var stand-ins
        for (BasicBlock b: s.getCFG().getBasicBlocks()) {
            for (Instr i: b.getInstrs()) i.renameVars(varRenameMap);
        }
        //       }

        // Run on all nested closures.
        // In the current implementation, nested scopes are processed independently (unlike Live Variable Analysis)
        for (IRClosure c: s.getClosures()) execute(c);

        return null;
    }
}
