package org.jruby.ir.passes;

import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class LocalOptimizationPass extends CompilerPass {
    @Override
    public String getLabel() {
        return "Local Optimizations";
    }

    @Override
    public Object execute(IRScope s, Object... data) {
        for (BasicBlock b: s.getCFG().getBasicBlocks()) {
            runLocalOptsOnBasicBlock(s, b);
        }

        // SSS FIXME: What is this about? 
        // Why 'Only after running local opts'? Figure out and document.
        //
        // Only after running local opts, compute various execution scope flags.
        s.computeScopeFlags();

        // LVA information is no longer valid after this pass
        // Currently, we don't run this after LVA, but just in case ...
        //
        // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
        (new LiveVariableAnalysis()).invalidate(s);

        return null;
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

    public static Instr optInstr(IRScope s, Instr instr, Map<Operand,Operand> valueMap, Map<Variable,List<Variable>> simplificationMap) {
        // System.out.println("BEFORE: " + instr);

        // Simplify instruction and record mapping between target variable and simplified value
        Operand val = instr.simplifyAndGetResult(s, valueMap);

        // Variable dst = (instr instanceof ResultInstr) ? ((ResultInstr) instr).getResult() : null;
        // System.out.println("AFTER: " + instr + "; dst = " + dst + "; val = " + val);

        if (!(instr instanceof ResultInstr)) {
            return instr;
        }

        Instr newInstr = instr;
        Variable res = ((ResultInstr) instr).getResult();
        if (val == null) {
            // If we didn't get a simplified value, remove existing simplifications
            // for the result to get rid of RAW hazards!
            valueMap.remove(res);
        } else {
            if (!res.equals(val)) {
                recordSimplification(res, val, valueMap, simplificationMap);
            }

            if (!instr.hasSideEffects()) {
                if (instr instanceof CopyInstr) {
                    if (res.equals(val) && instr.canBeDeleted(s)) {
                        System.out.println("DEAD: marking instr dead!!");
                        instr.markDead();
                    }
                } else {
                    newInstr = new CopyInstr(res, val);
                }
            }
        }

        // Purge all entries in valueMap that have 'res' as their simplified value
        // to take care of RAW scenarios (because we aren't in SSA form yet!)
        if (!res.equals(val)) {
            List<Variable> simplifiedVars = simplificationMap.get(res);
            if (simplifiedVars != null) {
                for (Variable v: simplifiedVars) {
                    valueMap.remove(v);
                }
                simplificationMap.remove(res);
            }
        }

        return newInstr;
    }

    public static void runLocalOptsOnInstrArray(IRScope s, Instr[] instrs) {
        // Reset value map if this instruction is the start/end of a basic block
        Map<Operand,Operand> valueMap = new HashMap<>();
        Map<Variable,List<Variable>> simplificationMap = new HashMap<>();
        for (int i = 0; i < instrs.length; i++) {
            Instr instr = instrs[i];
            Instr newInstr = optInstr(s, instr, valueMap, simplificationMap);
            if (newInstr != instr) {
                instrs[i] = newInstr;
            }

            // If the call has been optimized away in the previous step, it is no longer a hard boundary for opts!
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
            // This information is present in instruction flags on CallBase. Use it!
            Operation iop = instr.getOperation();
            if (iop.startsBasicBlock() || iop.endsBasicBlock() || (iop.isCall() && !instr.isDead())) {
                valueMap = new HashMap<>();
                simplificationMap = new HashMap<>();
            }
        }
    }

    public static void runLocalOptsOnBasicBlock(IRScope s, BasicBlock b) {
        ListIterator<Instr> instrs = b.getInstrs().listIterator();
        // Reset value map if this instruction is the start/end of a basic block
        Map<Operand,Operand> valueMap = new HashMap<>();
        Map<Variable,List<Variable>> simplificationMap = new HashMap<>();
        while (instrs.hasNext()) {
            Instr instr = instrs.next();
            Instr newInstr = optInstr(s, instr, valueMap, simplificationMap);
            if (newInstr.isDead()) {
                instrs.remove();
            } else if (newInstr != instr) {
                instrs.set(newInstr);
            }

            // If the call has been optimized away in the previous step, it is no longer a hard boundary for opts!
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
            // This information is present in instruction flags on CallBase. Use it!
            Operation iop = instr.getOperation();
            if (iop.isCall() && !instr.isDead()) {
                valueMap = new HashMap<>();
                simplificationMap = new HashMap<>();
            }
        }
    }
}
