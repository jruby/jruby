package org.jruby.ir.passes;

import java.util.HashMap;
import java.util.Map;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.representations.BasicBlock;

public class AddLocalVarLoadStoreInstructions extends CompilerPass {
    public static String[] NAMES = new String[] { "add_lvar_load_store", "add_local_var_load_store_instructions" };
    
    public String getLabel() {
        return "Add Local Variable Load/Store Instructions";
    }

    public Object execute(IRScope s, Object... data) {
        //        if (s.requiresBinding()) {

        // 1. Figure out required stores
        // 2. Add stores
        // 3. Figure out required loads
        // 4. Add loads
        //
        // Order is important since loads in 3. depend on stores in 2.
        StoreLocalVarPlacementProblem fsp = new StoreLocalVarPlacementProblem();
        fsp.setup(s);
        fsp.compute_MOP_Solution();

        // Add stores, assigning an equivalent tmp-var for each local var
        Map<Operand, Operand> varRenameMap = new HashMap<Operand, Operand>();
        fsp.addStoreAndBindingAllocInstructions(varRenameMap);

        // Once stores have been added, figure out required loads 
        LoadLocalVarPlacementProblem frp = new LoadLocalVarPlacementProblem();
        frp.setup(s);
        frp.compute_MOP_Solution();

        // Add loads, 
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
    
    public void invalidate(IRScope scope) {
        // FIXME: ...
    }
}
