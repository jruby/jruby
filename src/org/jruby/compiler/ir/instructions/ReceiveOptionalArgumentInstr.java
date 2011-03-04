package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.ArgIndex;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// Assign the 'index' argument to 'dest'.
public class ReceiveOptionalArgumentInstr extends NoOperandInstr {
    int argIndex;
    public ReceiveOptionalArgumentInstr(Variable dest, int index) {
        super(Operation.RECV_OPT_ARG, dest);
        this.argIndex = index;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveOptionalArgumentInstr(ii.getRenamedVariable(result), argIndex);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Object v = interp.getParameterCount() > argIndex ? interp.getParameter(argIndex) : Nil.NIL.retrieve(interp);
        getResult().store(interp, v);
        return null;
    }
}
