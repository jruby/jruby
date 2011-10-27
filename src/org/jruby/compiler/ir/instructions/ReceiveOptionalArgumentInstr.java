package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Assign the 'index' argument to 'dest'.
public class ReceiveOptionalArgumentInstr extends NoOperandInstr {
    int argIndex;
    public ReceiveOptionalArgumentInstr(Variable dest, int index) {
        super(Operation.RECV_OPT_ARG, dest);
        this.argIndex = index;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveOptionalArgumentInstr(ii.getRenamedVariable(getResult()), argIndex);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object v = interp.getParameterCount() > argIndex ? 
                interp.getParameter(argIndex) : UndefinedValue.UNDEFINED;
        getResult().store(interp, context, self, v);
        return null;
    }
}
