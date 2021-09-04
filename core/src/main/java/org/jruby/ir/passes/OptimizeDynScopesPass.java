package org.jruby.ir.passes;

import org.jruby.ir.IRFlags;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.LoadLocalVarInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

public class OptimizeDynScopesPass extends CompilerPass {
    @Override
    public String getLabel() {
        return "Optimize Dynamic Scopes";
    }

    @Override
    public String getShortLabel() {
        return "Opt Dyn Scopes";
    }

    private void setupLocalVarReplacement(LocalVariable v, FullInterpreterContext fic, Map<Operand, Operand> varRenameMap) {
         if (varRenameMap.get(v) == null) varRenameMap.put(v, fic.getNewTemporaryVariableFor(v));
    }

    private void decrementScopeDepth(LocalVariable v, Map<Operand, Operand> varRenameMap) {
         if (varRenameMap.get(v) == null) varRenameMap.put(v, v.cloneForDepth(v.getScopeDepth() - 1));
    }

    public void eliminateLocalVars(FullInterpreterContext fic) {
        assert fic.getScope().getClosures().isEmpty() : "We assume that if a scope has nested closures, it uses a dynamic scoope.";

        Map<Operand, Operand> varRenameMap = new HashMap<>();

        fic.setDynamicScopeEliminated(true); // Record the fact that we eliminated the scope

        // Since the scope does not require a binding, no need to do
        // any analysis. It is sufficient to rename all local var uses
        // with a temporary variable.
        boolean parentScopeNeeded = false;
        for (BasicBlock b: fic.getCFG().getBasicBlocks()) {
            ListIterator<Instr> instrs = b.getInstrs().listIterator();
            while (instrs.hasNext()) {
                Instr i = instrs.next();
                if (i instanceof ResultInstr) {
                    Variable v = ((ResultInstr) i).getResult();
                    // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
                    if (v instanceof LocalVariable && !v.isSelf()) {
                        LocalVariable lv = (LocalVariable)v;
                        if (lv.getScopeDepth() == 0) {
                            // Make sure there is a replacement tmp-var allocated for lv
                            setupLocalVarReplacement(lv, fic, varRenameMap);
                        } else {
                            parentScopeNeeded = true;
                            decrementScopeDepth(lv, varRenameMap);
                        }
                    }
                }

                for (Variable v : i.getUsedVariables()) {
                    if (v instanceof LocalVariable && !v.isSelf()) {
                        LocalVariable lv = (LocalVariable)v;
                        if (lv.getScopeDepth() == 0) {
                            // SSS FIXME: Ugly/Dirty! Some abstraction is broken.
                            // If we hit a load/store instr for a local-var and we
                            // eliminated the dynscope for it, we no longer need the
                            // load/store instr for it.
                            if (i instanceof LoadLocalVarInstr) {
                                LoadLocalVarInstr llvi = (LoadLocalVarInstr)i;
                                if (llvi.getLocalVar() == lv) {
                                    instrs.remove();
                                }
                            } else if (i instanceof StoreLocalVarInstr) {
                                StoreLocalVarInstr slvi = (StoreLocalVarInstr)i;
                                if (slvi.getLocalVar() == lv) {
                                    instrs.remove();
                                }
                            }

                            // Make sure there is a replacement tmp-var allocated for lv
                            setupLocalVarReplacement(lv, fic, varRenameMap);
                        } else {
                            // SSS FIXME: Ugly/Dirty! Some abstraction is broken.
                            if (i instanceof LoadLocalVarInstr) {
                                LoadLocalVarInstr llvi = (LoadLocalVarInstr)i;
                                if (llvi.getLocalVar() == lv) {
                                    llvi.decrementLVarScopeDepth();
                                }
                            } else if (i instanceof StoreLocalVarInstr) {
                                StoreLocalVarInstr slvi = (StoreLocalVarInstr)i;
                                if (slvi.getLocalVar() == lv) {
                                    slvi.decrementLVarScopeDepth();
                                }
                            }

                            parentScopeNeeded = true;
                            decrementScopeDepth(lv, varRenameMap);
                        }
                    }
                }
            }
        }

        if (parentScopeNeeded) fic.setReuseParentDynScope(true);

        // Rename all local var uses with their tmp-var stand-ins
        for (BasicBlock b: fic.getCFG().getBasicBlocks()) {
            for (Instr i: b.getInstrs()) i.renameVars(varRenameMap);
        }

        // LVA information is no longer valid after this pass
        // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
        (new LiveVariableAnalysis()).invalidate(fic);
    }

    @Override
    public Object execute(FullInterpreterContext fic, Object... data) {
        // Cannot run this on scopes that require dynamic scopes
        if (fic.getFlags().contains(IRFlags.REQUIRES_DYNSCOPE)) return null;
        if (fic.getFlags().contains(IRFlags.HAS_END_BLOCKS)) return null;


        eliminateLocalVars(fic);

        // SSS FIXME: Why null? Return a non-null value so that we don't
        // run this repeatedly on the same scope.
        return null;
    }

    @Override
    public boolean invalidate(FullInterpreterContext fic) {
        // No invalidation for this right now.
        // But, if necessary, we can reverse this operation.
        return false;
    }
}
