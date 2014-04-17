package org.jruby.ir.passes;

import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.representations.BasicBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnsureTempsAssigned extends CompilerPass {
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(CFGBuilder.class);

    @Override
    public String getLabel() {
        return "Ensure Temporary Variables Assigned";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        Set<TemporaryVariable> names = new HashSet<TemporaryVariable>();

        for (BasicBlock b : scope.getCFG().getBasicBlocks()) {
            for (Instr i : b.getInstrs()) {
                for (Operand o : i.getOperands()) {
                    if (o instanceof TemporaryVariable) {
                        names.add((TemporaryVariable)o);
                    }
                }
            }
        }

        BasicBlock bb = scope.getCFG().getEntryBB();
        for (TemporaryVariable name : names) {
            bb.getInstrs().add(0, new CopyInstr(name, new Nil()));
        }

        return null;
    }

    @Override
    public void invalidate(IRScope scope) {
    }
}
