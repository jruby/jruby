package org.jruby.ir.passes;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.representations.BasicBlock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        StoreLocalVarPlacementProblem slvp = new StoreLocalVarPlacementProblem();

        // Only run if we are pushing a scope or we are reusing the parents scope.
        if (!s.getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED) || s.getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE)) {
            Map<Operand, Operand> varRenameMap = new HashMap<>();
            // 1. Figure out required stores
            // 2. Add stores
            // 3. Figure out required loads
            // 4. Add loads
            //
            // Order is important since loads in 3. depend on stores in 2.
            slvp.setup(s);
            slvp.compute_MOP_Solution();

            // Add stores, assigning an equivalent tmp-var for each local var
            slvp.addStores(varRenameMap);

            // Once stores have been added, figure out required loads
            LoadLocalVarPlacementProblem llvp = new LoadLocalVarPlacementProblem();
            llvp.setup(s);
            llvp.compute_MOP_Solution();

            // Add loads
            llvp.addLoads(varRenameMap);

            // Rename all local var uses with their tmp-var stand-ins
            for (BasicBlock b: s.getCFG().getBasicBlocks()) {
                for (Instr i: b.getInstrs()) i.renameVars(varRenameMap);
            }

            // LVA information is no longer valid after this pass
            // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
            (new LiveVariableAnalysis()).invalidate(s);
        }

        s.putStoreLocalVarPlacementProblem(slvp);

        return slvp;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return scope.getStoreLocalVarPlacementProblem();
    }

    @Override
    public boolean invalidate(IRScope scope) {
        super.invalidate(scope);
        scope.putStoreLocalVarPlacementProblem(null);
        return true;
    }
}
