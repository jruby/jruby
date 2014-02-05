package org.jruby.ir.passes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;
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

    private void setupLocalVarReplacement(LocalVariable v, IRScope s, Map<Operand, Operand> varRenameMap) {
         if (varRenameMap.get(v) == null) varRenameMap.put(v, s.getNewTemporaryVariableFor(v));
    }

    @Override
    public Object execute(IRScope s, Object... data) {
        StoreLocalVarPlacementProblem slvp = new StoreLocalVarPlacementProblem();
        Map<Operand, Operand> varRenameMap = new HashMap<Operand, Operand>();
        if (s.bindingHasEscaped() || s instanceof IRClosure) {
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

            // Add loads,
            llvp.addLoads(varRenameMap);
        } else {
            // Since the scope does not require a binding, no need to do
            // any analysis. It is sufficient to rename all local var uses
            // with a temporary variable.
            for (BasicBlock b: s.getCFG().getBasicBlocks()) {
                for (Instr i: b.getInstrs()) {
                    if (i instanceof ResultInstr) {
                        Variable v = ((ResultInstr) i).getResult();
                        // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
                        if (v instanceof LocalVariable && !v.isSelf()) {
                            // Make sure there is a replacement tmp-var allocated for lv
                            setupLocalVarReplacement((LocalVariable)v, s, varRenameMap);
                        }
                    }
                    for (Variable v : i.getUsedVariables()) {
                        if (v instanceof LocalVariable && !v.isSelf()) {
                            setupLocalVarReplacement((LocalVariable)v, s, varRenameMap);
                        }
                    }
                }
            }
        }

        // Rename all local var uses with their tmp-var stand-ins
        for (BasicBlock b: s.getCFG().getBasicBlocks()) {
            for (Instr i: b.getInstrs()) i.renameVars(varRenameMap);
        }

        // Run on all nested closures.
        // In the current implementation, nested scopes are processed independently (unlike Live Variable Analysis)
        for (IRClosure c: s.getClosures()) execute(c);

        s.setDataFlowSolution(StoreLocalVarPlacementProblem.NAME, slvp);

        // LVA information is no longer valid after the pass
        s.setDataFlowSolution(LiveVariablesProblem.NAME, null);

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
