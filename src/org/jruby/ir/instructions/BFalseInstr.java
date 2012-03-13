package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.representations.InlinerInfo;
import org.jruby.ir.targets.JVM;

public class BFalseInstr extends BranchInstr {
    protected BFalseInstr(Operand v, Label jmpTarget) {
        super(Operation.B_FALSE, v, null, jmpTarget);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        return new BFalseInstr(getArg1().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new BFalseInstr(getArg1().cloneForInlining(ii), getJumpTarget());
    }

    public void compile(JVM jvm) {
        jvm.emit(getArg1());
        jvm.method().isTrue();
        jvm.method().bfalse(jvm.methodData().getLabel(getJumpTarget()));
    }
}
