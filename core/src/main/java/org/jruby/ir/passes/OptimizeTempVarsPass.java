package org.jruby.ir.passes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.*;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;


/**
 * Takes multiple single def-use temporary variables and reduces them to share the same temp variable.
 * This ends up reducing the amount of allocation and most likely helps hotspot warm up in some way quicker.
 *
 * This traditionally was a compiler pass (extends CompilerPass) but it is special in that it is the only
 * pass which does not require any supplementary datastructures.  In fact, it cannot be run by the time
 * a CFG is created.  So it was de-CompilerPassed and called directly.
 */
public class OptimizeTempVarsPass {
    private static void allocVar(Operand oldVar, IRScope s, List<TemporaryVariable> freeVarsList, Map<Operand, Operand> newVarMap) {
        // If we dont have a var mapping, get a new var -- try the free list first
        // and if none available, allocate a fresh one
        if (newVarMap.get(oldVar) == null) {
            newVarMap.put(oldVar, freeVarsList.isEmpty() ? s.createTemporaryVariable() : freeVarsList.remove(0));
        }
    }

    private static void freeVar(TemporaryVariable newVar, List<TemporaryVariable> freeVarsList) {
        // Put the new var onto the free list (but only if it is not already there).
        if (!freeVarsList.contains(newVar)) freeVarsList.add(0, newVar);
    }

    public static Instr[] optimizeTmpVars(IRScope s, Instr[] initialInstrs) {
        List<Instr> instructions = new ArrayList<>(Arrays.asList(initialInstrs));

        // Pass 1: Analyze instructions and find use and def count of temporary variables
        Map<TemporaryVariable, Instr> tmpVarUses = new HashMap<>();
        Map<TemporaryVariable, Instr> tmpVarDefs = new HashMap<>();
        for (Instr i: instructions) {
            for (Variable v: i.getUsedVariables()) {
                 if (v instanceof TemporaryVariable) {
                     TemporaryVariable tv = (TemporaryVariable)v;
                     Instr use = tmpVarUses.get(tv);
                     if (use == null) {
                         tmpVarUses.put(tv, i);
                     } else if (use != NopInstr.NOP) {
                         tmpVarUses.put(tv, NopInstr.NOP);
                     }
                 }
            }
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr)i).getResult();
                if (v instanceof TemporaryVariable) {
                     TemporaryVariable tv = (TemporaryVariable)v;
                     Instr defs = tmpVarDefs.get(tv);
                     if (defs == null) {
                         tmpVarDefs.put(tv, i);
                     } else if (defs != NopInstr.NOP) {
                         tmpVarDefs.put(tv, NopInstr.NOP);
                     }
                }
            }
        }

        // Pass 2: Transform code and do additional analysis:
        // * If the result of this instr. has not been used, mark it dead
        // * Find copies where constant values are set

        ListIterator<Instr> instrs = instructions.listIterator();
        while (instrs.hasNext()) {
            Instr i = instrs.next();

            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr)i).getResult();
                if (v instanceof TemporaryVariable) {
                    // Deal with this code pattern:
                    //    %v = ...
                    // %v not used anywhere
                    Instr use = tmpVarUses.get(v);
                    Instr def = tmpVarDefs.get(v);
                    if (use == null) {
                        if (i instanceof CopyInstr) {
                            i.markDead();
                            instrs.remove();
                        } else if (i instanceof CallInstr) {
                            instrs.set(((CallInstr)i).discardResult());
                        //} else {
                        //  FIXME: This was not being used and is not for calls specifically so we were unsure how much it would help
                        //  but it is left here. For some instrs which assign the result to a tempvar but we notice it is not used
                        //  we can eliminate setting the result.  In pratice this seems to mostly happen in module bodies.
                        //  i.markUnusedResult();
                        }
                    }
                    // Replace <operand> in use from def if single-def and single-use:   %v = <operand>; ... %v ...
                    else if (use != NopInstr.NOP && def != null && def != NopInstr.NOP && i instanceof CopyInstr) {
                        // Conservatively avoid optimizing return values since
                        // intervening cloned ensure block code can modify the
                        // copy source (if it is a variable).
                        //
                        // Ex:
                        //    v = 1
                        //    %v_1 = v
                        // L1:
                        //    v = 2
                        //    return %v_1 <-- cannot be replaced with v
                        //    ....
                        if (!(use instanceof ReturnInstr)) {
                            CopyInstr ci = (CopyInstr)i;
                            Operand src = ci.getSource();
                            // Only tmp vars are in SSA form post IR-building and it is safe to
                            // replace uses with defs without examining intervening instrs. But,
                            // not true for local vars and other operands that use local vars.
                            //   a = 0
                            //   %v_1 = a
                            //   a = 1
                            //   x = %v_1
                            // In that snippet, it would be buggy to rewrite it to:
                            //   a = 0
                            //   a = 1
                            //   x = a
                            if (src instanceof TemporaryVariable || src instanceof ImmutableLiteral) {
                                i.markDead();
                                instrs.remove();

                                // Fix up use
                                Map<Operand, Operand> copyMap = new HashMap<>();
                                copyMap.put(v, src);
                                use.simplifyOperands(copyMap, true);
                            }
                        }
                    }
                }
                // Deal with this code pattern:
                //    1: %v = ... (not a copy)
                //    2: x = %v
                // If %v is not used anywhere else, the result of 1. can be updated to use x and 2. can be removed
                //
                // CAVEATS:
                // --------
                // 1. We only do this if 'x' is a temporary variable since only tmp vars are in SSA form.
                //      %v = ...(not a copy-1)
                //      x = .. (not a copy-2)
                //      x = %v
                //    In that snippet above, it would be buggy to replace it with:
                //      x = ...(not a copy-1)
                //      x = .. (not a copy-2)
                //
                // 2. Consider this pattern
                //      %v = <operand> (copy instr)
                //      x = %v
                //    This code will have been captured in the previous if branch which would have deleted %v = 5
                //    Hence the check for whether the src def instr is dead
                else if (i instanceof CopyInstr) {
                    CopyInstr ci = (CopyInstr)i;
                    Operand src = ci.getSource();
                    if (src instanceof TemporaryVariable) {
                        TemporaryVariable vsrc = (TemporaryVariable)src;
                        Instr use = tmpVarUses.get(vsrc);
                        Instr def = tmpVarDefs.get(vsrc);
                        if (use != null && use != NopInstr.NOP &&
                            def != null && def != NopInstr.NOP &&
                            !def.isDead() && ((ResultInstr)def).getResult() instanceof TemporaryVariable)
                        {
                            // Fix up def
                            ((ResultInstr) def).updateResult(ci.getResult());
                            ci.markDead();
                            instrs.remove();
                        }
                    }
                }
            }
        }

        // Pass 3: Compute last use of temporary variables -- this effectively is the
        // end of the live range that started with its first definition. This implicitly
        // encodes the live range of the temporary variable.
        //
        // These live ranges are valid because these instructions are generated from an AST
        // and they haven't been rearranged yet.  In addition, since temporaries are used to
        // communicate results from lower levels to higher levels in the tree, a temporary
        // defined outside a loop cannot be used within the loop.  So, the first definition
        // of a temporary and the last use of the temporary delimit its live range.
        //
        // Caveat
        // ------
        // %current-scope and %current-module are the two "temporary" variables that violate
        // this contract right now since they are used everywhere in the scope.
        // So, in the presence of loops, we:
        // - either assume that the live range of these  variables extends to
        //   the end of the outermost loop in which they are used
        // - or we do not rename %current-scope and %current-module in such scopes.
        //
        // SSS FIXME: For now, we just extend the live range of these vars all the
        // way to the end of the scope!
        //
        // NOTE: It is sufficient to just track last use for renaming purposes.
        // At the first definition, we allocate a variable which then starts the live range
        Map<TemporaryVariable, Integer> lastVarUseOrDef = new HashMap<>();
        int iCount = -1;
        for (Instr i: instructions) {
            iCount++;

            // update last use/def
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr)i).getResult();
                if (v instanceof TemporaryVariable) lastVarUseOrDef.put((TemporaryVariable)v, iCount);
            }

            // update last use/def
            for (Variable v: i.getUsedVariables()) {
                if (v instanceof TemporaryVariable) lastVarUseOrDef.put((TemporaryVariable)v, iCount);
            }
        }

        // Always make sure the yield closure variable is considered live, so we don't clobber it
        lastVarUseOrDef.put(s.getYieldClosureVariable(), iCount);

        // If the scope has loops, extend live range of %current-module and %current-scope
        // to end of scope (see note earlier).
        if (s.hasLoops()) {
            lastVarUseOrDef.put((TemporaryVariable)s.getCurrentScopeVariable(), iCount);
            lastVarUseOrDef.put((TemporaryVariable)s.getCurrentModuleVariable(), iCount);
        }

        // Pass 4: Reallocate temporaries based on last uses to minimize # of unique vars.
        // Replace all single use operands with constants they were assigned to.
        // Using operand -> operand signature because simplifyOperands works on operands
        Map<Operand, Operand>   newVarMap    = new HashMap<>();
        List<TemporaryVariable> freeVarsList = new ArrayList<>();
        iCount = -1;
        s.resetTemporaryVariables();

        for (Instr i: instructions) {
            iCount++;

            // Assign new vars
            Variable result = null;
            if (i instanceof ResultInstr) {
                result = ((ResultInstr)i).getResult();
                if (result instanceof TemporaryVariable) allocVar(result, s, freeVarsList, newVarMap);
            }
            for (Variable v: i.getUsedVariables()) {
                if (v instanceof TemporaryVariable) allocVar(v, s, freeVarsList, newVarMap);
            }

            // Free dead vars
            if ((result instanceof TemporaryVariable) && lastVarUseOrDef.get(result) == iCount) {
                freeVar((TemporaryVariable)newVarMap.get(result), freeVarsList);
            }
            for (Variable v: i.getUsedVariables()) {
                if (v instanceof TemporaryVariable) {
                    TemporaryVariable tv = (TemporaryVariable)v;
                    if (lastVarUseOrDef.get(tv) == iCount) freeVar((TemporaryVariable)newVarMap.get(tv), freeVarsList);
                }
            }

            // Rename
            i.renameVars(newVarMap);
        }

        Instr[] newInstrs = new Instr[instructions.size()];
        instructions.toArray(newInstrs);
        return newInstrs;
    }
}
