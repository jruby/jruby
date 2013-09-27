/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;

/**
 *
 * @author enebo
 */
public class OptimizeTempVarsPass extends CompilerPass {
    boolean optimizedTempVars = false;

    @Override
    public String getLabel() {
        return "Temporary Variable Reduction";
    }

    @Override
    public Object execute(IRScope s, Object... data) {
        for (IRClosure c: s.getClosures()) {
            run(c, true);
        }

        optimizeTmpVars(s);

        optimizedTempVars = true;

        return null;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return optimizedTempVars ? new Object() : null;
    }

    @Override
    public void invalidate(IRScope s) {
        // FIXME: How do we un-optmize?
    }

    private static void allocVar(Operand oldVar, IRScope s, List<TemporaryVariable> freeVarsList, Map<Operand, Operand> newVarMap) {
        // If we dont have a var mapping, get a new var -- try the free list first
        // and if none available, allocate a fresh one
        if (newVarMap.get(oldVar) == null) {
            newVarMap.put(oldVar, freeVarsList.isEmpty() ? s.getNewTemporaryVariable() : freeVarsList.remove(0));
        }
    }

    private static void freeVar(TemporaryVariable newVar, List<TemporaryVariable> freeVarsList) {
        // Put the new var onto the free list (but only if it is not already there).
        if (!freeVarsList.contains(newVar)) freeVarsList.add(0, newVar);
    }

    private static void optimizeTmpVars(IRScope s) {
        // Cannot run after CFG has been built in the form it has been written here.
        if (s.getCFG() != null) return;

        // Pass 1: Analyze instructions and find use and def count of temporary variables
        Map<TemporaryVariable, List<Instr>> tmpVarUses = new HashMap<TemporaryVariable, List<Instr>>();
        Map<TemporaryVariable, List<Instr>> tmpVarDefs = new HashMap<TemporaryVariable, List<Instr>>();
        for (Instr i: s.getInstrs()) {
            for (Variable v: i.getUsedVariables()) {
                 if (v instanceof TemporaryVariable) {
                     TemporaryVariable tv = (TemporaryVariable)v;
                     List<Instr> uses = tmpVarUses.get(tv);
                     if (uses == null) {
                         uses = new ArrayList<Instr>();
                         tmpVarUses.put(tv, uses);
                     }
                     uses.add(i);
                 }
            }
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr)i).getResult();
                if (v instanceof TemporaryVariable) {
                     TemporaryVariable tv = (TemporaryVariable)v;
                     List<Instr> defs = tmpVarDefs.get(tv);
                     if (defs == null) {
                         defs = new ArrayList<Instr>();
                         tmpVarDefs.put(tv, defs);
                     }
                     defs.add(i);
                }
            }
        }

        // Pass 2: Transform code and do additional analysis:
        // * If the result of this instr. has not been used, mark it dead
        // * Find copies where constant values are set
        Map<TemporaryVariable, Variable> removableCopies = new HashMap<TemporaryVariable, Variable>();
        ListIterator<Instr> instrs = s.getInstrs().listIterator();
        while (instrs.hasNext()) {
            Instr i = instrs.next();

            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr)i).getResult();
                if (v instanceof TemporaryVariable) {
                    // Deal with this code pattern:
                    //    %v = ...
                    // %v not used anywhere
                    List<Instr> uses = tmpVarUses.get((TemporaryVariable)v);
                    List<Instr> defs = tmpVarDefs.get((TemporaryVariable)v);
                    if (uses == null) {
                        if (i instanceof CopyInstr) {
                            i.markDead();
                            instrs.remove();
                        } else if (i instanceof CallInstr) {
                            instrs.set(((CallInstr)i).discardResult());
                        } else {
                            i.markUnusedResult();
                        }
                    }
                    // Deal with this code pattern:
                    //    %v = <some-operand>
                    //    .... %v ...
                    // %v not used or defined anywhere else
                    // So, %v can be replaced by the operand
                    else if ((uses.size() == 1) && (defs != null) && (defs.size() == 1) && (i instanceof CopyInstr)) {
                        CopyInstr ci = (CopyInstr)i;
                        Operand src = ci.getSource();
                        i.markDead();
                        instrs.remove();

                        // Fix up use
                        Map<Operand, Operand> copyMap = new HashMap<Operand, Operand>();
                        copyMap.put(v, src);
                        Instr soleUse = uses.get(0);
                        soleUse.simplifyOperands(copyMap, true);
                    }
                }
                // Deal with this code pattern:
                //    1: %v = ... (not a copy)
                //    2: x = %v
                // If %v is not used anywhere else, the result of 1. can be updated to use x and 2. can be removed
                //
                // NOTE: consider this pattern:
                //    %v = <operand> (copy instr)
                //    x = %v
                // This code will have been captured in the previous if branch which would have deleted %v = 5
                // Hence the check for whether the src def instr is dead
                else if (i instanceof CopyInstr) {
                    CopyInstr ci = (CopyInstr)i;
                    Operand src = ci.getSource();
                    if (src instanceof TemporaryVariable) {
                        TemporaryVariable vsrc = (TemporaryVariable)src;
                        List<Instr> uses = tmpVarUses.get(vsrc);
                        List<Instr> defs = tmpVarDefs.get(vsrc);
                        if ((uses.size() == 1) && (defs.size() == 1)) {
                            Instr soleDef = defs.get(0);
                            if (!soleDef.isDead()) {
                                // Fix up def
                                ((ResultInstr)soleDef).updateResult(ci.getResult());
                                ci.markDead();
                                instrs.remove();
                            }
                        }
                    }
                }
            }
        }

        // Pass 3: Replace all single use operands with constants they were assigned to.
        // Using operand -> operand signature because simplifyOperands works on operands
        //
        // In parallel, compute last use of temporary variables -- this effectively is the
        // end of the live range that started with its first definition.  This implicitly
        // encodes the live range of the temporary variable.
        //
        // These live ranges are valid because these instructions are generated from an AST
        // and they haven't been rearranged yet.  In addition, since temporaries are used to
        // communicate results from lower levels to higher levels in the tree, a temporary
        // defined outside a loop cannot be used within the loop.  So, the first definition
        // of a temporary and the last use of the temporary delimit its live range.
        //
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
        Map<TemporaryVariable, Integer> lastVarUseOrDef = new HashMap<TemporaryVariable, Integer>();
        int iCount = -1;
        for (Instr i: s.getInstrs()) {
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

        // If the scope has loops, extend live range of %current-module and %current-scope
        // to end of scope (see note earlier).
        if (s.hasLoops()) {
            lastVarUseOrDef.put((TemporaryVariable)s.getCurrentScopeVariable(), iCount);
            lastVarUseOrDef.put((TemporaryVariable)s.getCurrentModuleVariable(), iCount);
        }

        // Pass 4: Reallocate temporaries based on last uses to minimize # of unique vars.
        Map<Operand, Operand>   newVarMap    = new HashMap<Operand, Operand>();
        List<TemporaryVariable> freeVarsList = new ArrayList<TemporaryVariable>();
        iCount = -1;
        s.resetTemporaryVariables();

        for (Instr i: s.getInstrs()) {
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
            if ((result instanceof TemporaryVariable) && lastVarUseOrDef.get((TemporaryVariable)result) == iCount) {
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
    }
}
