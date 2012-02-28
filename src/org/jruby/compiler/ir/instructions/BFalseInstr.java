package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;

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
