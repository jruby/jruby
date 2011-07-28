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
        return new CopyInstr(ii.getRenamedVariable(result), argument.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp) {
        getResult().store(interp, getArg().retrieve(interp));
        return null;
    }

    // Can this instruction raise exceptions?
    @Override
    public boolean canRaiseException() { return false; }
}
