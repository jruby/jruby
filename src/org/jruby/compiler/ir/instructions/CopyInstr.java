package org.jruby.compiler.ir.instructions;

// This is of the form:
//   d = s

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CopyInstr extends OneOperandInstr {
    public CopyInstr(Variable d, Operand s) {
        super(Operation.COPY, d, s);
        if (s == null) new Exception().printStackTrace();
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        
        return argument;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(getResult()), argument.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        getResult().store(interp, context, self, getArg().retrieve(interp, context, self));
        return null;
    }

    @Override
    public String toString() { 
        return (argument instanceof Variable) ? super.toString() : (getResult() + " = " + getArg());
    }

    // Can this instruction raise exceptions?
    @Override
    public boolean canRaiseException() {
        return false;
    }
}
