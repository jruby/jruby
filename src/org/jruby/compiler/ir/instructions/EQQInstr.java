package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQInstr extends Instr implements ResultInstr {
    private Operand arg1;
    private Operand arg2;
    private final Variable result;

    public EQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.EQQ);
        
        assert result != null: "EQQInstr result is null";
        
        this.arg1 = v1;
        this.arg2 = v2;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{arg1, arg2};
    }
    
    public Variable getResult() {
        return result;
    }
    
    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        arg1 = arg1.getSimplifiedOperand(valueMap);
        arg2 = arg2.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + arg1 + ", " + arg2 + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new EQQInstr(ii.getRenamedVariable(result), 
                arg1.cloneForInlining(ii), arg2.cloneForInlining(ii));
    }

    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block, Object exception, Object[] temp) {
        IRubyObject receiver = (IRubyObject) arg1.retrieve(interp, context, self, temp);
        IRubyObject value = (IRubyObject) arg2.retrieve(interp, context, self, temp);

        if (value == UndefinedValue.UNDEFINED) {
            result.store(interp, context, self, receiver, temp);
        } else if (receiver instanceof RubyArray) {
            RubyArray testVals = (RubyArray)receiver;
            for (int i = 0, n = testVals.getLength(); i < n; i++) {
                IRubyObject eqqVal = testVals.eltInternal(i).callMethod(context, "===", value);
                if (eqqVal.isTrue()) {
                    result.store(interp, context, self, eqqVal, temp);
                    return null;
                }
            }
            result.store(interp, context, self, context.getRuntime().newBoolean(false), temp);
        } else {
            result.store(interp, context, self, receiver.callMethod(context, "===", value), temp);
        }
        
        return null;
    }
}
