package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BEQInstr extends BranchInstr {
    public static BranchInstr create(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof BooleanLiteral) {
            return ((BooleanLiteral) v2).isTrue() ? new BTrueInstr(v1, jmpTarget) : new BFalseInstr(v1, jmpTarget);
        }
        if (v2 instanceof Nil) return new BNilInstr(v1, jmpTarget);
        if (v2 == UndefinedValue.UNDEFINED) return new BUndefInstr(v1, jmpTarget);
        return new BEQInstr(v1, v2, jmpTarget);
    }

    protected BEQInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BEQ, v1, v2, jmpTarget);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        return new BEQInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new BEQInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), getJumpTarget());
    }

    @Override
    public void compile(JVM jvm) {
        Operand[] args = getOperands();
        jvm.method().loadLocal(0);
        jvm.emit(args[0]);
        jvm.emit(args[1]);
        jvm.method().invokeHelper("BEQ", boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        jvm.method().adapter.iftrue(jvm.methodData().getLabel(getJumpTarget()));
    }
}
