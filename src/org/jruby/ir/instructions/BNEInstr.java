package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BNEInstr extends BranchInstr {
    public static BranchInstr create(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof BooleanLiteral) {
            return ((BooleanLiteral) v2).isFalse() ? new BTrueInstr(v1, jmpTarget) : new BFalseInstr(v1, jmpTarget);
        }        
        return new BNEInstr(v1, v2, jmpTarget);
    }

    public BNEInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BNE, v1, v2, jmpTarget);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        return new BNEInstr(getArg1().cloneForInlining(ii), 
                getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new BNEInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), getJumpTarget());
    }

    @Override
    public void compile(JVM jvm) {
        Operand[] args = getOperands();
        jvm.method().loadLocal(0);
        jvm.emit(args[0]);
        jvm.emit(args[1]);
        jvm.method().invokeHelper("BNE", boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        jvm.method().adapter.iftrue(jvm.methodData().getLabel(getJumpTarget()));
    }
}
