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
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.util.KeyValuePair;

import java.util.List;

/*
 * args field: [self, receiver, *args]
 */
public class CallInstr extends CallBase implements ResultInstr {
    protected transient Variable result;

    public static CallInstr createWithKwargs(IRScope scope, CallType callType, Variable result, RubySymbol name,
                                             Operand receiver, Operand[] args, Operand closure,
                                             List<KeyValuePair<Operand, Operand>> kwargs) {
        // FIXME: This is obviously total nonsense but this will be on an optimized path and we will not be constructing
        // a new hash like this unless the eventual caller needs an ordinary hash.
        Operand[] newArgs = new Operand[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = new Hash(kwargs, true);

        return create(scope, callType, result, name, receiver, newArgs, closure);
    }

    public static CallInstr create(IRScope scope, Variable result, RubySymbol name, Operand receiver, Operand[] args, Operand closure) {
        return create(scope, CallType.NORMAL, result, name, receiver, args, closure);
    }

    public static CallInstr create(IRScope scope, CallType callType, Variable result, RubySymbol name, Operand receiver, Operand[] args, Operand closure) {
        boolean isPotentiallyRefined = scope.maybeUsingRefinements();

        if (!containsArgSplat(args)) {
            boolean hasClosure = closure != null;

            if (args.length == 0 && !hasClosure) {
                return new ZeroOperandArgNoBlockCallInstr(scope, callType, result, name, receiver, args, isPotentiallyRefined);
            } else if (args.length == 1) {
                if (hasClosure) return new OneOperandArgBlockCallInstr(scope, callType, result, name, receiver, args, closure, isPotentiallyRefined);
                if (!isPotentiallyRefined) {
                    // These instructions use primitive call paths that are not implemented for RefinedCallSite.
                    if (isAllFixnums(args)) {
                        return new OneFixnumArgNoBlockCallInstr(scope, callType, result, name, receiver, args, isPotentiallyRefined);
                    }
                    if (isAllFloats(args)) {
                        return new OneFloatArgNoBlockCallInstr(scope, callType, result, name, receiver, args, isPotentiallyRefined);
                    }
                }

                return new OneOperandArgNoBlockCallInstr(scope, callType, result, name, receiver, args, isPotentiallyRefined);
            } else if (args.length == 2 && !hasClosure) {
                return new TwoOperandArgNoBlockCallInstr(scope, callType, result, name, receiver, args, isPotentiallyRefined);
            }
        }

        return new CallInstr(scope, callType, result, name, receiver, args, closure, isPotentiallyRefined);
    }


    public CallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name, Operand receiver, Operand[] args, Operand closure,
                     boolean potentiallyRefined) {
        this(scope, Operation.CALL, callType, result, name, receiver, args, closure, potentiallyRefined);
    }

    // clone constructor
    protected CallInstr(IRScope scope, Operation op, CallType callType, Variable result, RubySymbol name, Operand receiver,
                                Operand[] args, Operand closure, boolean potentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, op, callType, name, receiver, args, closure, potentiallyRefined, callSite, callSiteId);

        assert result != null;

        this.result = result;
    }

    // normal constructor
    protected CallInstr(IRScope scope, Operation op, CallType callType, Variable result, RubySymbol name, Operand receiver,
                        Operand[] args, Operand closure, boolean potentiallyRefined) {
        super(scope, op, callType, name, receiver, args, closure, potentiallyRefined);

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

        Operand closure = hasClosureArg ? d.decodeOperand() : null;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("before result");
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, result:  "+ result);

        return create(d.getCurrentScope(), callType, result, name, receiver, args, closure);
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
                getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii), isPotentiallyRefined(),
                getCallSite(), getCallSiteId());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CallInstr(this);
    }
}
