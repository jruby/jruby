package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;

/*
 * args field: [self, receiver, *args]
 */
public class CallInstr extends CallBase implements ResultInstr {
    protected Variable result;

    public static CallInstr create(Variable result, String name, Operand receiver, Operand[] args, Operand closure) {
        return new CallInstr(CallType.NORMAL, result, name, receiver, args, closure);
    }

    public static CallInstr create(CallType callType, Variable result, String name, Operand receiver, Operand[] args, Operand closure) {
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

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public CallBase specializeForInterpretation() {
        Operand[] callArgs = getCallArgs();
        if (containsArgSplat(callArgs)) return this;

        switch (callArgs.length) {
            case 0:
                return hasClosure() ? this : new ZeroOperandArgNoBlockCallInstr(this);
            case 1:
                if (isAllFixnums() && !hasClosure()) return new OneFixnumArgNoBlockCallInstr(this);

                return hasClosure() ? new OneOperandArgBlockCallInstr(this) : new OneOperandArgNoBlockCallInstr(this);
        }
        return this;
    }

    public Instr discardResult() {
        return new NoResultCallInstr(Operation.NORESULT_CALL, getCallType(), getName(), getReceiver(), getCallArgs(), closure);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new CallInstr(getCallType(), ii.getRenamedVariable(result), getName(), receiver.cloneForInlining(ii),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public String toString() {
        return (hasUnusedResult() ? "[DEAD-RESULT]" : "") + result + " = " + super.toString();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CallInstr(this);
    }
}
