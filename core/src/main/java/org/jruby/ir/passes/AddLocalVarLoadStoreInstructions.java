package org.jruby.ir.passes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.representations.BasicBlock;

public class AddLocalVarLoadStoreInstructions extends CompilerPass {
    @Override
    public String getLabel() {
        return "Add Local Variable Load/Store Instructions";
    }

    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(LiveVariableAnalysis.class);

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope s, Object... data) {
        // SSS FIXME: In ancient times, I had this check that wrapped the code below.
        // Still useful?
        //
        // if (s.requiresBinding()) {
        // }

        // 1. Figure out required stores
        // 2. Add stores
        // 3. Figure out required loads
        // 4. Add loads
        //
        // Order is important since loads in 3. depend on stores in 2.
        StoreLocalVarPlacementProblem slvp = new StoreLocalVarPlacementProblem();
        slvp.setup(s);
        slvp.compute_MOP_Solution();

        // Add stores, assigning an equivalent tmp-var for each local var
        Map<Operand, Operand> varRenameMap = new HashMap<Operand, Operand>();
        slvp.addStores(varRenameMap);

        // Once stores have been added, figure out required loads
        LoadLocalVarPlacementProblem llvp = new LoadLocalVarPlacementProblem();
        llvp.setup(s);
        llvp.compute_MOP_Solution();

        // Add loads,
        llvp.addLoads(varRenameMap);

        // Rename all local var uses with their tmp-var stand-ins
        for (BasicBlock b: s.getCFG().getBasicBlocks()) {
            for (Instr i: b.getInstrs()) i.renameVars(varRenameMap);
        }

        // Run on all nested closures.
        // In the current implementation, nested scopes are processed independently (unlike Live Variable Analysis)
        for (IRClosure c: s.getClosures()) execute(c);

        s.setDataFlowSolution(StoreLocalVarPlacementProblem.NAME, slvp);

        return slvp;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getDataFlowSolution(StoreLocalVarPlacementProblem.NAME);
    }

    @Override
    public void invalidate(IRScope scope) {
        scope.setDataFlowSolution(StoreLocalVarPlacementProblem.NAME, null);
    }
}
