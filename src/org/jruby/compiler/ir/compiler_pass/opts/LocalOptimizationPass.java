package org.jruby.compiler.ir.compiler_pass.opts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.CallBase;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.CodeVersion;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.TemporaryVariable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.instructions.ResultInstr;

public class LocalOptimizationPass implements CompilerPass {
    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder() {
        return false;
    }

    public void run(IRScope s) {
        if (s instanceof IRExecutionScope) {
            IRExecutionScope es = (IRExecutionScope)s;

            // Run this pass on nested closures first!
            // This let us compute execute scope flags for a method based on what all nested closures do
            for (IRClosure c: es.getClosures()) {
                run(c);
            }

            // Now, run on current scope
            runLocalOpts(es);

            // Only after running local opts, compute various execution scope flags
            es.computeExecutionScopeFlags();
        }
    }

    private static void allocVar(Operand oldVar, IRExecutionScope s, List<TemporaryVariable> freeVarsList, Map<Operand, Operand> newVarMap) {
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

    private static void optimizeTmpVars(IRExecutionScope s) {
        // Pass 1: Analyze instructions and find use and def count of temporary variables
        Map<TemporaryVariable, Integer> tmpVarUseCounts = new HashMap<TemporaryVariable, Integer>();
        Map<TemporaryVariable, Integer> tmpVarDefCounts = new HashMap<TemporaryVariable, Integer>();
        for (Instr i: s.getInstrs()) {
            for (Variable v: i.getUsedVariables()) {
                 if (v instanceof TemporaryVariable) {
                     TemporaryVariable tv = (TemporaryVariable)v;
                     Integer n = tmpVarUseCounts.get(tv);
                     if (n == null) n = new Integer(0);
                     tmpVarUseCounts.put(tv, new Integer(n+1));
                 }
            }
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr)i).getResult();
                if (v instanceof TemporaryVariable) {
                     TemporaryVariable tv = (TemporaryVariable)v;
                     Integer n = tmpVarDefCounts.get(tv);
                     if (n == null) n = new Integer(0);
                     tmpVarDefCounts.put(tv, new Integer(n+1));
                }
            }
        }

        // Pass 2: Transform code and do additional analysis:
        // * If the result of this instr. has not been used, mark it dead
        // * Find copies where constant values are set
        Map<Operand, Operand> constValMap = new HashMap<Operand, Operand>();
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
                    Integer useCount = tmpVarUseCounts.get((TemporaryVariable)v);
                    Integer defCount = tmpVarDefCounts.get((TemporaryVariable)v);
                    if (useCount == null) {
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
                    //    %v = 5
                    //    .... %v ...
                    // %v not used or defined anywhere else
                    // So, %v can be replaced by 5 (or whichever constant it is)
                    else if ((useCount == 1) && (defCount == 1) && (i instanceof CopyInstr)) {
                        CopyInstr ci = (CopyInstr)i;
                        Operand src = ci.getSource();
                        if (src.isConstant()) {
                            i.markDead();
                            instrs.remove();
                            constValMap.put(v, src);
                        }
                    }
                }
                // Deal with this code pattern:
                //    1: %v = ...
                //    2: x = %v
                // If %v is not used anywhere else, the result of 1. can be updated to use x and 2. can be removed
                //
                // NOTE: consider this pattern:
                //    %v = 5
                //    x = %v
                // This code will have been captured in the previous if branch which would have deleted %v = 5
                // Hence the check for constValMap.get(src) == null
                else if (i instanceof CopyInstr) {
                    CopyInstr ci = (CopyInstr)i;
                    Operand src = ci.getSource();
                    if (src instanceof TemporaryVariable) {
                        TemporaryVariable vsrc = (TemporaryVariable)src;
                        Integer useCount = tmpVarUseCounts.get(vsrc);
                        Integer defCount = tmpVarDefCounts.get(vsrc);
                        if ((useCount == 1) && (defCount == 1) && (constValMap.get(vsrc) == null)) {
                            ci.markDead();
                            instrs.remove();
                            removableCopies.put(vsrc, ci.getResult());
                        }
                    }
                }
            }
        }

/*
        Set<TemporaryVariable> usedVars = tmpVarUseCounts.keySet();
        usedVars.removeAll(constValMap.keySet());     // these var defs have been removed
        usedVars.removeAll(removableCopies.keySet()); // these var defs have been removed
        System.out.println("For scope: " + s + ", we had " + tmpVarDefCounts.size() + " tmp vars and are now left with " + usedVars.size());
*/

        // Pass 3: Replace all single use operands with constants they were assigned to.
        // Using operand -> operand signature because simplifyOperands works on operands
        //
        // In parallel, compute last use of temporary variables -- this effectively is the
        // end of the live range that started with its first definition.  This implicitly
        // encodes the live range of the temporary variable.  These live ranges are valid
        // because the instructions that we are processing have come out an AST which means
        // instruction uses are properly nested and haven't been rearranged yet.
        //
        // If anything, the live ranges are conservative -- but given that most temporaries
        // are very short-lived (2 instructions), this quick analysis is good enough for most cases.
        Map<TemporaryVariable, Integer> lastVarUseOrDef = new HashMap<TemporaryVariable, Integer>();
        int iCount = -1;
        for (Instr i: s.getInstrs()) {
            iCount++;

            // rename dest
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr)i).getResult();
                if (v instanceof TemporaryVariable) {
                    Variable ci = removableCopies.get((TemporaryVariable)v);
                    if (ci != null) {
                        ((ResultInstr)i).updateResult(ci);
                        if (ci instanceof TemporaryVariable) lastVarUseOrDef.put((TemporaryVariable)ci, iCount);
                    } else {
                        lastVarUseOrDef.put((TemporaryVariable)v, iCount);
                    }
                }
            }

            // rename uses
            i.simplifyOperands(constValMap, true);

            // compute last use
            for (Variable v: i.getUsedVariables()) {
                if (v instanceof TemporaryVariable) lastVarUseOrDef.put((TemporaryVariable)v, iCount);
            }
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

    private static void recordSimplification(Variable res, Operand val, Map<Operand, Operand> valueMap, Map<Variable, List<Variable>> simplificationMap) {
        valueMap.put(res, val);

        // For all variables used by val, record a reverse mapping to let us track
        // Read-After-Write scenarios when any of these variables are modified.
        List<Variable> valVars = new ArrayList<Variable>(); 
        val.addUsedVariables(valVars);
        for (Variable v: valVars) {
           List<Variable> x = simplificationMap.get(v);
           if (x == null) {
              x = new ArrayList<Variable>();
              simplificationMap.put(v, x);
           }
           x.add(res);
        }
    }

    private static void runLocalOpts(IRExecutionScope s) {
        optimizeTmpVars(s);

        // Reset value map if this instruction is the start/end of a basic block
        //
        // Right now, calls are considered hard boundaries for optimization and
        // information cannot be propagated across them!
        //
        // SSS FIXME: Rather than treat all calls with a broad brush, what we need
        // is to capture different attributes about a call :
        //   - uses closures
        //   - known call target
        //   - can modify scope,
        //   - etc.
        //
        // This information is probably already present in the AST Inspector
        Label deoptLabel = s.getNewLabel();
        Map<Operand,Operand> valueMap = new HashMap<Operand,Operand>();
        Map<Variable,List<Variable>> simplificationMap = new HashMap<Variable,List<Variable>>();
        Map<String,CodeVersion> versionMap = new HashMap<String,CodeVersion>();
        ListIterator<Instr> instrs = s.getInstrs().listIterator();
        while (instrs.hasNext()) {
            Instr i = instrs.next();
            Operation iop = i.getOperation();
            if (iop.startsBasicBlock()) {
                valueMap = new HashMap<Operand,Operand>();
                simplificationMap = new HashMap<Variable,List<Variable>>();
                versionMap = new HashMap<String, CodeVersion>();
            }

            // Simplify instruction and record mapping between target variable and simplified value
            // System.out.println("BEFORE: " + i);
            Operand  val = i.simplifyAndGetResult(valueMap);
            // FIXME: This logic can be simplified based on the number of res != null checks only done if doesn't
            Variable res = i instanceof ResultInstr ? ((ResultInstr) i).getResult() : null;

            // System.out.println("For " + i + "; dst = " + res + "; val = " + val);
            // System.out.println("AFTER: " + i);

            if (res != null && val != null) {
                if (!res.equals(val)) recordSimplification(res, val, valueMap, simplificationMap);
                if (!i.hasSideEffects() && !(i instanceof CopyInstr)) {
                    if (!res.equals(val)) {
                        instrs.set(new CopyInstr(res, val));
                    } else {
                        i.markDead();
                        instrs.remove();
                    }
                }
            } else if (res != null && val == null) {
                // If we didn't get a simplified value, remove any existing simplifications for the result
                // to get rid of RAW hazards!
                valueMap.remove(res);
            } else if (iop.isCall()) { // Optimize some core class method calls for constant values
                val = null;
                CallBase call = (CallBase) i;
                Operand   r    = call.getReceiver(); 
                // SSS FIXME: r can be null for ruby/jruby internal call instructions!
                // Cannot optimize them as of now.
                if (r != null) {
                    // If 'r' is not a constant, it could actually be a compound value!
                    // Look in our value map to see if we have a simplified value for the receiver.
                    if (!r.isConstant()) {
                        Operand v = valueMap.get(r);
                        if (v != null) r = v;
                    }
                }
            }

            // Purge all entries in valueMap that have 'res' as their simplified value to take care of RAW scenarios (because we aren't in SSA form yet!)
            if ((res != null) && !res.equals(val)) {
                List<Variable> simplifiedVars = simplificationMap.get(res);
                if (simplifiedVars != null) {
                    for (Variable v: simplifiedVars) {
                        valueMap.remove(v);
                    }
                    simplificationMap.remove(res);
                }
            }

            // If the call has been optimized away in the previous step, it is no longer a hard boundary for opts!
            if (iop.endsBasicBlock() || (iop.isCall() && !i.isDead())) {
                valueMap = new HashMap<Operand,Operand>();
                simplificationMap = new HashMap<Variable,List<Variable>>();
                versionMap = new HashMap<String, CodeVersion>();
            }
        }
    }
}
