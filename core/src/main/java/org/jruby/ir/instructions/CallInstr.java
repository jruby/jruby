package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.CallType;

/*
 * args field: [self, receiver, *args]
 */
public class CallInstr extends CallBase implements ResultInstr {
    protected Variable result;

    public static CallInstr create(Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        return new CallInstr(CallType.NORMAL, result, methAddr, receiver, args, closure);
    }

    public static CallInstr create(CallType callType, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        return new CallInstr(callType, result, methAddr, receiver, args, closure);
    }


    public CallInstr(CallType callType, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        this(Operation.CALL, callType, result, methAddr, receiver, args, closure);
    }

    protected CallInstr(Operation op, CallType callType, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(op, callType, methAddr, receiver, args, closure);

        assert result != null;

        this.result = result;
    }

    public CallInstr(Operation op, CallInstr ordinary) {
        this(op, ordinary.getCallType(), ordinary.getResult(),
                ordinary.getMethodAddr(), ordinary.getReceiver(), ordinary.getCallArgs(),
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
        if (hasClosure() || containsSplat(callArgs)) return this;

        switch (callArgs.length) {
            case 0:
                return new ZeroOperandArgNoBlockCallInstr(this);
            case 1:
                if (isAllFixnums()) return new OneFixnumArgNoBlockCallInstr(this);

                return new OneOperandArgNoBlockCallInstr(this);
        }
        return this;
    }

    public Instr discardResult() {
        return new NoResultCallInstr(Operation.NORESULT_CALL, getCallType(), getMethodAddr(), getReceiver(), getCallArgs(), closure);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CallInstr(getCallType(), ii.getRenamedVariable(result),
                (MethAddr) getMethodAddr().cloneForInlining(ii),
                receiver.cloneForInlining(ii), cloneCallArgs(ii),
                closure == null ? null : closure.cloneForInlining(ii));
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
