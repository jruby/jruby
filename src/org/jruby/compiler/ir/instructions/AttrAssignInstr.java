package org.jruby.compiler.ir.instructions;

import org.jruby.RubyString;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AttrAssignInstr extends MultiOperandInstr {
    public AttrAssignInstr(Operand obj, Operand attr, Operand value) {
        super(Operation.ATTR_ASSIGN, null, new Operand[]{obj, attr, value});
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new AttrAssignInstr(_args[0].cloneForInlining(ii), _args[1].cloneForInlining(ii),
                _args[2].cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Operand[] operands = getOperands();
        IRubyObject receiver = (IRubyObject) operands[0].retrieve(interp);
        String attr = ((RubyString) operands[1].retrieve(interp)).asJavaString();
        IRubyObject value = (IRubyObject) operands[2].retrieve(interp);

        receiver.callMethod(interp.getContext(), attr, value);
        return null;
    }
}
