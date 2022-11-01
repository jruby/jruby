package org.jruby.ir.passes;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

import java.util.*;

public class LocalOptimizationPass extends CompilerPass {
    @Override
    public String getLabel() {
        return "Local Optimizations";
    }

    @Override
    public String getShortLabel() {
        return "Local Opts";
    }

    @Override
    public Object execute(FullInterpreterContext fic, Object... data) {
        boolean reexamineCFG = false;
        CFG cfg = fic.getCFG();

        for (BasicBlock bb: cfg.getBasicBlocks()) {
            if (runLocalOptsOnBasicBlock(fic, bb)) {
                reexamineCFG = true;
                cfg.fixupEdges(bb);
            }
        }

        // If we changed any edges we can potentially remove some whole BBs!
        if (reexamineCFG) fic.getCFG().optimize();

        // LVA information is no longer valid after this pass
        // Currently, we don't run this after LVA, but just in case ...
        //
        // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
        (new LiveVariableAnalysis()).invalidate(fic);

        return null;
    }

    private static void recordSimplification(Variable res, Operand val, Map<Operand, Operand> valueMap, Map<Variable, List<Variable>> simplificationMap) {
        valueMap.put(res, val);

        // For all variables used by val, record a reverse mapping to let us track
        // Read-After-Write scenarios when any of these variables are modified.
        List<Variable> valVars = new ArrayList<>();
        val.addUsedVariables(valVars);
        for (Variable v: valVars) {
           List<Variable> x = simplificationMap.get(v);
           if (x == null) {
              x = new ArrayList<>();
              simplificationMap.put(v, x);
           }
           x.add(res);
        }
    }

    public static Instr optInstr(FullInterpreterContext fic, Instr instr, Map<Operand,Operand> valueMap, Map<Variable,List<Variable>> simplificationMap) {
        // System.out.println("BEFORE: " + instr);

        // Simplify instruction and record mapping between target variable and simplified value
        Operand val = instr.simplifyAndGetResult(fic.getScope(), valueMap);

        // A branch may no longer need to be a branch (e.g. b_true(true, label) -> jump(label)).
        if (instr instanceof BranchInstr) {
            instr = ((BranchInstr) instr).simplifyBranch(fic);
        }

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
                    if (res.equals(val) && instr.canBeDeletedFromScope(fic)) {
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

    // FIXME: Currently dead but why was this made dead?
    public static void runLocalOptsOnInstrArray(FullInterpreterContext fic, Instr[] instrs) {
        // Reset value map if this instruction is the start/end of a basic block
        Map<Operand,Operand> valueMap = new HashMap<>();
        Map<Variable,List<Variable>> simplificationMap = new HashMap<>();
        for (int i = 0; i < instrs.length; i++) {
            Instr instr = instrs[i];
            Instr newInstr = optInstr(fic, instr, valueMap, simplificationMap);
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

    public static boolean runLocalOptsOnBasicBlock(FullInterpreterContext fic, BasicBlock b) {
        boolean reexamineCFG = false;  // If we changed something which changes flow we need to update CFG.
        ListIterator<Instr> instrs = b.getInstrs().listIterator();
        // Reset value map if this instruction is the start/end of a basic block
        Map<Operand,Operand> valueMap = new HashMap<>();
        Map<Variable,List<Variable>> simplificationMap = new HashMap<>();
        while (instrs.hasNext()) {
            Instr instr = instrs.next();
            Instr newInstr = optInstr(fic, instr, valueMap, simplificationMap);
            if (newInstr.isDead()) {
                if (newInstr != instr && instr.getOperation().endsBasicBlock()) reexamineCFG = true;
                instrs.remove();
            } else if (newInstr != instr) {
                if (instr.getOperation().endsBasicBlock()) reexamineCFG = true;
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

        return reexamineCFG;
    }
}
