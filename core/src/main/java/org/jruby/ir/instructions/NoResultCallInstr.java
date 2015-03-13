package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;

public class NoResultCallInstr extends CallBase {
    // FIXME: Removed results undoes specialized callinstrs.  Audit how often and what and make equalivalent versions here.
    public static NoResultCallInstr create(CallType callType, String name, Operand receiver, Operand[] args, Operand closure) {
        if (closure == null && !containsArgSplat(args) && args.length == 1) {
            return new OneOperandArgNoBlockNoResultCallInstr(callType, name, receiver, args, null);
        }

        return new NoResultCallInstr(Operation.NORESULT_CALL, callType, name, receiver, args, closure);
    }

    public NoResultCallInstr(Operation op, CallType callType, String name, Operand receiver, Operand[] args, Operand closure) {
        super(op, callType, name, receiver, args, closure);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new NoResultCallInstr(getOperation(), getCallType(), getName(), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReceiver());
        e.encode(getName());
        e.encode(getCallArgs());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NoResultCallInstr(this);
    }
}
