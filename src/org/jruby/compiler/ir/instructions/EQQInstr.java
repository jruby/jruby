package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyArray;

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQInstr extends TwoOperandInstr {
    public EQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.EQQ, result, v1, v2);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new EQQInstr(ii.getRenamedVariable(result), operand1.cloneForInlining(ii), operand2.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject receiver = (IRubyObject) getOperand1().retrieve(interp, context, self);
        IRubyObject value = (IRubyObject) getOperand2().retrieve(interp, context, self);

        if (receiver instanceof RubyArray) {
            RubyArray testVals = (RubyArray)receiver;
            for (int i = 0, n = testVals.getLength(); i < n; i++) {
                IRubyObject eqqVal = testVals.eltInternal(i).callMethod(context, "===", value);
                if (eqqVal.isTrue()) {
                    getResult().store(interp, context, self, eqqVal);
                    return null;
                }
            }
            getResult().store(interp, context, self, context.getRuntime().newBoolean(false));
        } else if (value.equals(context.getRuntime().getTrue())) { // SSS FIXME: Can I use value == RubyBoolean.True?
            getResult().store(interp, context, self, receiver);
        } else {
            getResult().store(interp, context, self, receiver.callMethod(context, "===", value));
        }
        
        return null;
    }
}
