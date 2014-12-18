package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;

public class NoResultCallInstr extends CallBase {
    public NoResultCallInstr(Operation op, CallType callType, String name, Operand receiver, Operand[] args, Operand closure) {
        super(op, callType, name, receiver, args, closure);
    }

    public NoResultCallInstr(Operation op, NoResultCallInstr instr) {
        this(op, instr.getCallType(), instr.getName(), instr.receiver, instr.arguments, instr.closure);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new NoResultCallInstr(getOperation(), getCallType(), getName(), receiver.cloneForInlining(ii),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public CallBase specializeForInterpretation() {
        Operand[] callArgs = getCallArgs();
        if (hasClosure() || containsArgSplat(callArgs)) return this;

        switch (callArgs.length) {
//            case 0:
//                return new ZeroOperandArgNoBlockNoResultCallInstr(this);
            case 1:
//                if (isAllFixnums()) return new OneFixnumArgNoBlockNoResultCallInstr(this);

                return new OneOperandArgNoBlockNoResultCallInstr(this);
        }
        return this;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NoResultCallInstr(this);
    }
}
