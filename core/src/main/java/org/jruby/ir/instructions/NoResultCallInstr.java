package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;

public class NoResultCallInstr extends CallBase {
    // FIXME: Removed results undoes specialized callinstrs.  Audit how often and what and make equalivalent versions here.
    public static NoResultCallInstr create(IRScope scope, CallType callType, RubySymbol name, Operand receiver,
                                           Operand[] args, Operand closure, int flags, boolean isPotentiallyRefined) {
        if (closure == null && !containsArgSplat(args) && args.length == 1) {
            return new OneOperandArgNoBlockNoResultCallInstr(scope, callType, name, receiver, args, NullBlock.INSTANCE, flags, isPotentiallyRefined);
        }

        return new NoResultCallInstr(scope, Operation.NORESULT_CALL, callType, name, receiver, args, closure, flags, isPotentiallyRefined);
    }

    // normal constructor
    public NoResultCallInstr(IRScope scope, Operation op, CallType callType, RubySymbol name, Operand receiver,
                             Operand[] args, Operand closure, int flags, boolean isPotentiallyRefined) {
        super(scope, op, callType, name, receiver, args, closure, flags, isPotentiallyRefined);
    }

    /**
     * result and non-result call instructions are processed with the same code so we provide
     * this method to make that same code simpler.
     */
    @Override
    public Variable getResult() {
        return null;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new NoResultCallInstr(ii.getScope(), getOperation(), getCallType(), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii),
                getClosureArg().cloneForInlining(ii), getFlags(),
                isPotentiallyRefined());
    }

    public static NoResultCallInstr decode(IRReaderDecoder d) {
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, ordinal:  "+ callTypeOrdinal);
        RubySymbol name = d.decodeSymbol();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, methaddr:  "+ name);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : NullBlock.INSTANCE;
        int flags = d.decodeInt();

        return NoResultCallInstr.create(d.getCurrentScope(), CallType.fromOrdinal(callTypeOrdinal), name, receiver,
                args, closure, flags, d.getCurrentScope().maybeUsingRefinements());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NoResultCallInstr(this);
    }
}
