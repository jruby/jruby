package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class NotInstr extends OneOperandInstr {
    public NotInstr(Variable dst, Operand arg) {
        super(Operation.NOT, dst, arg);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new NotInstr(ii.getRenamedVariable(result), argument.cloneForInlining(ii));
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        return (argument instanceof BooleanLiteral) ? ((BooleanLiteral) argument).logicalNot() : null;
    }

    // Can this instruction raise exceptions?
    @Override
    public boolean canRaiseException() { return false; }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        boolean not = !((IRubyObject) getArg().retrieve(interp)).isTrue();

        getResult().store(interp, interp.getRuntime().newBoolean(not));
        return null;
    }
}
