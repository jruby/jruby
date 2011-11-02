package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyArray;
import org.jruby.RubyModule;

// This instruction is similar to EQQInstr, except it also verifies that
// the type to EQQ with is actually a class or a module since rescue clauses
// have this requirement unlike case statements.
//
// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class RescueEQQInstr extends TwoOperandInstr {
    public RescueEQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.RESCUE_EQQ, result, v1, v2);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new RescueEQQInstr(ii.getRenamedVariable(getResult()), 
                getOperand1().cloneForInlining(ii), getOperand2().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject receiver = (IRubyObject) getOperand1().retrieve(interp, context, self);
        IRubyObject value = (IRubyObject) getOperand2().retrieve(interp, context, self);

        if (value == UndefinedValue.UNDEFINED) {
            getResult().store(interp, context, self, receiver);
        } else if (receiver instanceof RubyArray) {
            RubyArray testVals = (RubyArray)receiver;
            for (int i = 0, n = testVals.getLength(); i < n; i++) {
                IRubyObject excType = (IRubyObject)testVals.eltInternal(i);
                if (!(excType instanceof RubyModule)) {
                   throw context.getRuntime().newTypeError("class or module required for rescue clause. Found: " + excType);
                }
                IRubyObject eqqVal = excType.callMethod(context, "===", value);
                if (eqqVal.isTrue()) {
                    getResult().store(interp, context, self, eqqVal);
                    return null;
                }
            }
            getResult().store(interp, context, self, context.getRuntime().newBoolean(false));
        } else {
            if (!(receiver instanceof RubyModule)) {
               throw context.getRuntime().newTypeError("class or module required for rescue clause. Found: " + receiver);
            }
            getResult().store(interp, context, self, receiver.callMethod(context, "===", value));
        }
        
        return null;
    }
}
