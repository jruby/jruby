package org.jruby.ir.instructions.boxing;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.IRVisitor;

import java.util.Map;

public class AluInstr extends Instr implements ResultInstr {
    protected Variable result;
    protected Operand a1;
    protected Operand a2;

    public AluInstr(Operation op, Variable result, Operand a1, Operand a2) {
        super(op);
        this.result = result;
        this.a1 = a1;
        this.a2 = a2;
    }

    public Operand[] getOperands() {
        return new Operand[]{a1, a2};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand getArg1() {
        return a1;
    }

    public Operand getArg2() {
        return a2;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        a1 = a1.getSimplifiedOperand(valueMap, force);
        a2 = a2.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new AluInstr(getOperation(), ii.getRenamedVariable(result), a1.cloneForInlining(ii), a2.cloneForInlining(ii));
    }

    @Override
    public String toString() {
        return result + " = " + getOperation() + "(" + a1 + ", " + a2 + ")";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AluInstr(this);
    }
}
