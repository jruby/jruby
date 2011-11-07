package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class NotInstr extends Instr {
    private Operand arg;

    public NotInstr(Variable dst, Operand arg) {
        super(Operation.NOT, dst);
        this.arg = arg;
    }

    public Operand[] getOperands() {
        return new Operand[]{arg};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        arg = arg.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + arg + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new NotInstr(ii.getRenamedVariable(getResult()), arg.cloneForInlining(ii));
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        return (arg instanceof BooleanLiteral) ? ((BooleanLiteral) arg).logicalNot() : null;
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        boolean not = !((IRubyObject) arg.retrieve(interp, context, self)).isTrue();

        getResult().store(interp, context, self, context.getRuntime().newBoolean(not));
        return null;
    }
}
