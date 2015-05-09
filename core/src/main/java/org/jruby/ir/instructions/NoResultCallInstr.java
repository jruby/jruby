package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;

public class NoResultCallInstr extends CallBase {
    // FIXME: Removed results undoes specialized callinstrs.  Audit how often and what and make equalivalent versions here.
    public static NoResultCallInstr create(CallType callType, String name, Operand receiver, Operand[] args,
                                           Operand closure, boolean isPotentiallyRefined) {
        if (closure == null && !containsArgSplat(args) && args.length == 1) {
            return new OneOperandArgNoBlockNoResultCallInstr(callType, name, receiver, args, null, isPotentiallyRefined);
        }

        return new NoResultCallInstr(Operation.NORESULT_CALL, callType, name, receiver, args, closure, isPotentiallyRefined);
    }

    public NoResultCallInstr(Operation op, CallType callType, String name, Operand receiver, Operand[] args,
                             Operand closure, boolean isPotentiallyRefined) {
        super(op, callType, name, receiver, args, closure, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new NoResultCallInstr(getOperation(), getCallType(), getName(), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii), isPotentiallyRefined());
    }

    public static NoResultCallInstr decode(IRReaderDecoder d) {
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, ordinal:  "+ callTypeOrdinal);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, methaddr:  "+ methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        return NoResultCallInstr.create(CallType.fromOrdinal(callTypeOrdinal), methAddr, receiver, args, closure, d.getCurrentScope().maybeUsingRefinements());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NoResultCallInstr(this);
    }
}
