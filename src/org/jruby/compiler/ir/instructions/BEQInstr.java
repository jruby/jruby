package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;

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

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BEQInstr(getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public void compile(JVM jvm) {
        Operand[] args = getOperands();
        jvm.method().loadLocal(0);
        jvm.emit(args[0]);
        jvm.emit(args[1]);
        jvm.method().invokeOtherBoolean("==", 1);
    }
}
