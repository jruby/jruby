package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.ArgIndex;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// Assign the 'index' argument to 'dest'.
// If it is not null, you are all done
// If null, you jump to 'nullLabel' and execute that code.
public class ReceiveOptionalArgumentInstr extends TwoOperandInstr {
    public ReceiveOptionalArgumentInstr(Variable dest, int index, Label nullLabel) {
        super(Operation.RECV_OPT_ARG, dest, new ArgIndex(index), nullLabel);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        Operand callArg = ii.getCallArg(((ArgIndex)_arg1)._index);
        if (callArg == null) {// FIXME: Or should we also have a check for Nil.NIL?
            return new JumpInstr(ii.getRenamedLabel((Label)_arg2));
        }
        
        return new CopyInstr(ii.getRenamedVariable(result), callArg);
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        System.out.println("TODO: Optional Argument");
    }
}
