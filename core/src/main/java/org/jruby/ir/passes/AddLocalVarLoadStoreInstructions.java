package org.jruby.ir.passes;

import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.representations.BasicBlock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddLocalVarLoadStoreInstructions extends CompilerPass {
    @Override
    public String getLabel() {
        return "Add Local Variable Load/Store Instructions";
    }

    @Override
    public String getShortLabel() {
        return "Add LVar L/S";
    }

    public static final List<Class<? extends CompilerPass>> DEPENDENCIES = Collections.singletonList(LiveVariableAnalysis.class);

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(FullInterpreterContext fic, Object... data) {
        StoreLocalVarPlacementProblem slvp = new StoreLocalVarPlacementProblem();

        // Only run if we are pushing a scope or we are reusing the parents scope.
        if (!fic.isDynamicScopeEliminated() || fic.reuseParentDynScope()) {
            Map<Operand, Operand> varRenameMap = new HashMap<>();
            // 1. Figure out required stores
            // 2. Add stores
            // 3. Figure out required loads
            // 4. Add loads
            //
            // Order is important since loads in 3. depend on stores in 2.
            slvp.setup(fic);
            slvp.compute_MOP_Solution();

            // Add stores, assigning an equivalent tmp-var for each local var
            slvp.addStores(varRenameMap);

            // Once stores have been added, figure out required loads
            LoadLocalVarPlacementProblem llvp = new LoadLocalVarPlacementProblem();
            llvp.setup(fic);
            llvp.compute_MOP_Solution();

            // Add loads
            llvp.addLoads(varRenameMap);

            // Rename all local var uses with their tmp-var stand-ins
            for (BasicBlock b: fic.getCFG().getBasicBlocks()) {
                for (Instr i: b.getInstrs()) i.renameVars(varRenameMap);
            }

            // LVA information is no longer valid after this pass
            // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
            (new LiveVariableAnalysis()).invalidate(fic);
        }

        fic.getDataFlowProblems().put(StoreLocalVarPlacementProblem.NAME, slvp);

        return slvp;
    }

    @Override
    public Object previouslyRun(FullInterpreterContext fic) {
        return fic.getDataFlowProblems().get(StoreLocalVarPlacementProblem.NAME);
    }

    @Override
    public boolean invalidate(FullInterpreterContext fic) {
        super.invalidate(fic);
        fic.getDataFlowProblems().put(StoreLocalVarPlacementProblem.NAME, null);
        return true;
    }
}
