package org.jruby.ir.passes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

public class LocalOptimizationPass extends CompilerPass {
    boolean locallyOptimized = false;

    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(CFGBuilder.class);

    @Override
    public String getLabel() {
        return "Local Optimizations";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope s, Object... data) {
        // This let us compute execute scope flags for a method based on what all nested closures do
        for (IRClosure c: s.getClosures()) {
            run(c, true);
        }

        for (BasicBlock b: ((CFG) data[0]).getBasicBlocks()) {
            runLocalOptsOnInstrList(s, b.getInstrs().listIterator(), false);
        }

        // Only after running local opts, compute various execution scope flags
        s.computeScopeFlags();

        // Mark done
        locallyOptimized = true;

        return null;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return locallyOptimized ? new Object() : null;
    }

    @Override
    public void invalidate(IRScope scope) {
        locallyOptimized = false;
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

    public static void runLocalOptsOnInstrList(IRScope s, ListIterator<Instr> instrs, boolean preCFG) {
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
        Map<Operand,Operand> valueMap = new HashMap<Operand,Operand>();
        Map<Variable,List<Variable>> simplificationMap = new HashMap<Variable,List<Variable>>();
        while (instrs.hasNext()) {
            Instr i = instrs.next();
            Operation iop = i.getOperation();
            if (preCFG && iop.startsBasicBlock()) {
                valueMap = new HashMap<Operand,Operand>();
                simplificationMap = new HashMap<Variable,List<Variable>>();
            }

            // Simplify instruction and record mapping between target variable and simplified value
            // System.out.println("BEFORE: " + i);
            Operand  val = i.simplifyAndGetResult(s, valueMap);
            // FIXME: This logic can be simplified based on the number of res != null checks only done if doesn't
            Variable res = i instanceof ResultInstr ? ((ResultInstr) i).getResult() : null;

            // System.out.println("For " + i + "; dst = " + res + "; val = " + val);
            // System.out.println("AFTER: " + i);

            if (res != null && val != null) {
                if (!res.equals(val)) {
                    recordSimplification(res, val, valueMap, simplificationMap);
                } else if (!i.hasSideEffects()) {
                    if (i instanceof CopyInstr) {
                        if (i.canBeDeleted(s)) {
                            i.markDead();
                            instrs.remove();
                        }
                    } else {
                        instrs.set(new CopyInstr(res, val));
                    }
                }
            } else if (res != null && val == null) {
                // If we didn't get a simplified value, remove any existing simplifications for the result
                // to get rid of RAW hazards!
                valueMap.remove(res);
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
            if ((preCFG && iop.endsBasicBlock()) || (iop.isCall() && !i.isDead())) {
                valueMap = new HashMap<Operand,Operand>();
                simplificationMap = new HashMap<Variable,List<Variable>>();
            }
        }
    }
}
