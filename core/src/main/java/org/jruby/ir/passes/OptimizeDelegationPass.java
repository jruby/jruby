package org.jruby.ir.passes;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRFlags;
import org.jruby.ir.instructions.*;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class OptimizeDelegationPass extends CompilerPass {
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(CFGBuilder.class);

    @Override
    public String getLabel() {
        return "Delegated Variable Removal";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope s, Object... data) {
        for (IRClosure c: s.getClosures()) {
            run(c, false, true);
        }

        s.computeScopeFlags();

        if (s.getFlags().contains(IRFlags.BINDING_HAS_ESCAPED))
            return null;

        if (!s.getFlags().contains(IRFlags.RECEIVES_CLOSURE_ARG))
            return null;

        optimizeDelegatedVars(s);

        return true;
    }

    @Override
    public boolean invalidate(IRScope s) {
        // Not reversible right now
        return false;
    }

    private static void optimizeDelegatedVars(IRScope s) {
        for (BasicBlock bb: s.cfg().getBasicBlocks()) {
            DelegatedImplicitClosureTracker tracker = new DelegatedImplicitClosureTracker(s);

            for (Instr i: bb.getInstrs()) {
                if (i instanceof ReifyClosureInstr) {
                    tracker.addUnusedExplicitBlock((ReifyClosureInstr) i);
                } else {
                    Iterator<Variable> it = tracker.iterator();
                    while (it.hasNext()) {
                        Variable explicitBlock = it.next();
                        if (usesVariableAsNonClosureArg(i, explicitBlock)) {
                            it.remove();
                        }
                    }
                }
            }

            for (Instr i: bb.getInstrs()) {
                tracker.renameVarsOnInstr(i);
            }
        }
    }

    private static boolean usesVariableAsNonClosureArg(Instr i, Variable v) {
        List<Variable> usedVariables = i.getUsedVariables();
        if (usedVariables.contains(v)) {
            if (i instanceof ClosureAcceptingInstr) {
                return usedVariables.indexOf(v) != usedVariables.lastIndexOf(v) ||
                    v != ((ClosureAcceptingInstr) i).getClosureArg();
            } else
                return true;
        }
        return false;
    }

    private static class DelegatedImplicitClosureTracker {
        private final IRScope scope;
        private final Map<Operand, Operand> unusedExplicitBlocks;
        private final Map<Operand, Operand> procToNewTempRename;
        private final Map<Operand, Operand> blockToNewTempRename;

        public DelegatedImplicitClosureTracker(IRScope scope) {
            this.scope = scope;
            unusedExplicitBlocks = new HashMap<Operand, Operand>();
            procToNewTempRename = new HashMap<Operand, Operand>();
            blockToNewTempRename = new HashMap<Operand, Operand>();
        }

        public void addUnusedExplicitBlock(ReifyClosureInstr i) {
            Operand proc = i.getResult();
            Operand block = i.getSource();
            Operand newTemp = scope.createTemporaryVariable();

            unusedExplicitBlocks.put(proc, block);
            procToNewTempRename.put(proc, newTemp);
            blockToNewTempRename.put(block, newTemp);
        }

        public Iterator<Variable> iterator() {
            final Iterator<Map.Entry<Operand, Operand>> it = unusedExplicitBlocks.entrySet().iterator();

            return new Iterator<Variable>() {
                private Variable currentProc;
                private Operand currentBlock;

                public boolean hasNext() {
                    return it.hasNext();
                }

                public Variable next() {
                    Map.Entry<Operand, Operand> e = it.next();
                    currentProc = (Variable) e.getKey();
                    currentBlock = e.getValue();
                    return currentProc;
                }

                public void remove() {
                    procToNewTempRename.remove(currentProc);
                    blockToNewTempRename.remove(currentBlock);
                    it.remove();
                }
            };
        }

        public void renameVarsOnInstr(Instr i) {
            if (i instanceof ClosureAcceptingInstr) {
                i.renameVars(procToNewTempRename);
            } else if (i instanceof LoadImplicitClosureInstr) {
                i.renameVars(blockToNewTempRename);
            }
        }
    }
}
