package org.jruby.ir.passes;

import org.jruby.ir.IRFlags;
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReifyClosureInstr;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class OptimizeDelegationPass extends CompilerPass {
    @Override
    public String getLabel() {
        return "Delegated Variable Removal";
    }

    @Override
    public String getShortLabel() {
        return "Opt Delegation";
    }

    @Override
    public Object execute(FullInterpreterContext fic, Object... data) {
        EnumSet<IRFlags> flags = fic.getFlags();

        if (flags.contains(IRFlags.BINDING_HAS_ESCAPED)) return null;
        if (!fic.getScope().receivesClosureArg()) return null;

        optimizeDelegatedVars(fic);

        return true;
    }

    @Override
    public boolean invalidate(FullInterpreterContext fic) {
        // Not reversible right now
        return false;
    }

    private static void optimizeDelegatedVars(FullInterpreterContext fic) {
        Map<Operand, Operand> unusedExplicitBlocks = new HashMap<>();

        for (BasicBlock bb: fic.getCFG().getBasicBlocks()) {
            for (Instr i: bb.getInstrs()) {
                if (i instanceof ReifyClosureInstr) {
                    ReifyClosureInstr ri = (ReifyClosureInstr) i;

                    // can't store un-reified block in DynamicScope (only accepts IRubyObject)
                    // FIXME: (con) it would be nice to not have this limitation
                    if (ri.getResult() instanceof LocalVariable) continue;

                    unusedExplicitBlocks.put(ri.getResult(), ri.getSource());
                } else { 
                    Iterator<Operand> it = unusedExplicitBlocks.keySet().iterator();
                    while (it.hasNext()) {
                        Variable explicitBlock = (Variable) it.next();
                        if (usesVariableAsNonClosureArg(i, explicitBlock)) {
                            it.remove();
                        }
                    }
                }
            }
        }

        for (BasicBlock bb: fic.getCFG().getBasicBlocks()) {
            ListIterator<Instr> instrs = bb.getInstrs().listIterator();
            while (instrs.hasNext()) {
                Instr i = instrs.next();
                if (i instanceof ReifyClosureInstr) {
                    ReifyClosureInstr ri = (ReifyClosureInstr) i;
                    Variable procVar = ri.getResult();
                    Operand blockVar = unusedExplicitBlocks.get(procVar);

                    if (blockVar != null) {
                        ri.markDead();
                        instrs.set(new CopyInstr(procVar, blockVar));
                    }
                }
            }
        }
    }

    private static boolean usesVariableAsNonClosureArg(Instr i, Variable v) {
        List<Variable> usedVariables = i.getUsedVariables();
        return usedVariables.contains(v) &&
                (!(i instanceof ClosureAcceptingInstr) ||
                 usedVariables.indexOf(v) != usedVariables.lastIndexOf(v) ||
                 v != ((ClosureAcceptingInstr) i).getClosureArg());
    }
}
