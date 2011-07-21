package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReturnInstr extends OneOperandInstr {
    public final IRMethod methodToReturnFrom;

    public ReturnInstr(Operand returnValue) {
        super(Operation.RETURN, null, returnValue);
        this.methodToReturnFrom = null;
        
        assert returnValue != null : "RETURN must have argument operand";
    }

    @Override
    public String toString() { 
        return operation + "(" + argument + (methodToReturnFrom == null ? "" : ", <" + methodToReturnFrom.getName() + ">") + ")";
    }

    public ReturnInstr(Operand returnValue, IRMethod m) {
        super(Operation.RETURN, null, returnValue);
        this.methodToReturnFrom = m;

        assert returnValue != null : "RETURN must have argument operand";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReturnInstr(getArg().cloneForInlining(ii), methodToReturnFrom);
    }

    @Override
    public Label interpret(InterpreterContext interp) {
        interp.setReturnValue(getArg().retrieve(interp));
        return interp.getMethodExitLabel();
    }
}
