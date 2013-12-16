package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ReceiveExceptionInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;

    /** If true, the implementation (compiler/interpreter) may have to check the type
     *  of the received value and unwrap it to yield a Ruby-level exception.
     *  RaiseException values would have to be unwrapped, but other Throwables are
     *  passed right through. This flag is just an optimization to let the compiler
     *  not emit type-check and unwrapping code where it wont be necessary. */
    public final boolean checkType;

    public ReceiveExceptionInstr(Variable result, boolean checkType) {
        super(Operation.RECV_EXCEPTION);

        assert result != null : "ResultExceptionInstr result is null";

        this.result = result;
        this.checkType = checkType;
    }

    public ReceiveExceptionInstr(Variable result) {
        this(result, true);
    }
    
    public boolean isCheckType() {
        return checkType;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new BooleanLiteral(checkType) };
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + (!checkType ? " [no-typecheck]" : "");
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveExceptionInstr(ii.getRenamedVariable(result));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveExceptionInstr(this);
    }
}
