package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneFloatArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
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
    protected Variable result;

    public static CallInstr create(IRScope scope, Variable result, String name, Operand receiver, Operand[] args, Operand closure) {
        return create(scope, CallType.NORMAL, result, name, receiver, args, closure);
    }

    public static CallInstr create(IRScope scope, CallType callType, Variable result, String name, Operand receiver, Operand[] args, Operand closure) {
        if (scope.maybeUsingRefinements()) {
            // FIXME: Make same instr with refined callSite here or push though all path below
        }

        if (!containsArgSplat(args)) {
            boolean hasClosure = closure != null;

            if (args.length == 0 && !hasClosure) {
                return new ZeroOperandArgNoBlockCallInstr(callType, result, name, receiver, args);
            } else if (args.length == 1) {
                if (hasClosure) return new OneOperandArgBlockCallInstr(callType, result, name, receiver, args, closure);
                if (isAllFixnums(args)) return new OneFixnumArgNoBlockCallInstr(callType, result, name, receiver, args);
                if (isAllFloats(args)) return new OneFloatArgNoBlockCallInstr(callType, result, name, receiver, args);

                return new OneOperandArgNoBlockCallInstr(callType, result, name, receiver, args);
            }
        }

        return new CallInstr(callType, result, name, receiver, args, closure);
    }


    public CallInstr(CallType callType, Variable result, String name, Operand receiver, Operand[] args, Operand closure) {
        this(Operation.CALL, callType, result, name, receiver, args, closure);
    }

    protected CallInstr(Operation op, CallType callType, Variable result, String name, Operand receiver, Operand[] args, Operand closure) {
        super(op, callType, name, receiver, args, closure);

        assert result != null;

        this.result = result;
    }

    public CallInstr(Operation op, CallInstr ordinary) {
        this(op, ordinary.getCallType(), ordinary.getResult(),
                ordinary.getName(), ordinary.getReceiver(), ordinary.getCallArgs(),
                ordinary.getClosureArg(null));
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
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - methaddr:  " + methAddr);
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

        return create(d.getCurrentScope(), callType, result, methAddr, receiver, args, closure);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Instr discardResult() {
        return NoResultCallInstr.create(getCallType(), getName(), getReceiver(), getCallArgs(), getClosureArg());
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new CallInstr(getCallType(), ii.getRenamedVariable(result), getName(), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CallInstr(this);
    }
}
