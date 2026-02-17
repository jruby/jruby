package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneFloatArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.TwoOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;

/*
 * args field: [self, receiver, *args]
 */
public class CallInstr extends CallBase implements ResultInstr {
    protected transient Variable result;

    public static CallInstr create(IRScope scope, CallType callType, Variable result, RubySymbol name, Operand receiver,
                                   Operand[] args, Operand closure, int flags) {
        boolean isPotentiallyRefined = scope.maybeUsingRefinements();

        if (!containsArgSplat(args)) {
            boolean hasClosure = closure != NullBlock.INSTANCE;

            if (args.length == 0 && !hasClosure) {
                return new ZeroOperandArgNoBlockCallInstr(scope, callType, result, name, receiver, args, flags, isPotentiallyRefined);
            } else if (args.length == 1) {
                if (hasClosure) return new OneOperandArgBlockCallInstr(scope, callType, result, name, receiver, args, closure, flags, isPotentiallyRefined);
                if (!isPotentiallyRefined) {
                    // These instructions use primitive call paths that are not implemented for RefinedCallSite.
                    if (isAllFixnums(args)) {
                        return new OneFixnumArgNoBlockCallInstr(scope, callType, result, name, receiver, args, flags, isPotentiallyRefined);
                    }
                    if (isAllFloats(args)) {
                        return new OneFloatArgNoBlockCallInstr(scope, callType, result, name, receiver, args, flags, isPotentiallyRefined);
                    }
                }

                return new OneOperandArgNoBlockCallInstr(scope, callType, result, name, receiver, args, flags, isPotentiallyRefined);
            } else if (args.length == 2 && !hasClosure) {
                return new TwoOperandArgNoBlockCallInstr(scope, callType, result, name, receiver, args, flags, isPotentiallyRefined);
            }
        }

        return new CallInstr(scope, callType, result, name, receiver, args, closure, flags, isPotentiallyRefined);
    }


    public CallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name, Operand receiver, Operand[] args,
                     Operand closure, int flags, boolean potentiallyRefined) {
        this(scope, Operation.CALL, callType, result, name, receiver, args, closure, flags, potentiallyRefined);
    }

    // normal constructor
    protected CallInstr(IRScope scope, Operation op, CallType callType, Variable result, RubySymbol name, Operand receiver,
                        Operand[] args, Operand closure, int flags, boolean potentiallyRefined) {
        super(scope, op, callType, name, receiver, args, closure, flags, potentiallyRefined);

        assert result != null;

        this.result = result;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);

        e.encode(getResult());
    }

    public static CallInstr decode(IRReaderDecoder d) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall");
        int callTypeOrdinal = d.decodeInt();
        CallType callType = CallType.fromOrdinal(callTypeOrdinal);
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - calltype:  " + callType);
        RubySymbol name = d.decodeSymbol();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - methaddr:  " + name);
        Operand receiver = d.decodeOperand();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - receiver:  " + receiver);
        int argsCount = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - # of args:  " + argsCount);
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - # of args(2): " + argsLength);
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - hasClosure: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : NullBlock.INSTANCE;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("before result");
        int flags = d.decodeInt();
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, result:  "+ result);

        return create(d.getCurrentScope(), callType, result, name, receiver, args, closure, flags);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    /* // FIXME: removing results is currently unsupported
    public Instr discardResult() {
        return NoResultCallInstr.create(getCallType(), getName(), getReceiver(), getCallArgs(), getClosureArg(), isPotentiallyRefined());
    }*/

    @Override
    public Instr clone(CloneInfo ii) {
        return new CallInstr(ii.getScope(), getOperation(), getCallType(), ii.getRenamedVariable(result), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii),
                getClosureArg().cloneForInlining(ii),
                getFlags(), isPotentiallyRefined()
        );
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CallInstr(this);
    }
}
