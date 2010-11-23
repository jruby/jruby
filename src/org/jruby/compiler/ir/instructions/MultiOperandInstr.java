package org.jruby.compiler.ir.instructions;

import java.util.Arrays;
import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// This is of the form:
//   v = OP(args, attribute_array); Ex: v = CALL(args, v2)

public abstract class MultiOperandInstr extends Instr {
    public Operand[] _args;

    public MultiOperandInstr(Operation opType, Variable result, Operand[] args) {
        super(opType, result);

        _args = args;
    }

    @Override
    public String toString() {
        return super.toString() + Arrays.toString(_args);
    }

    public Operand[] getOperands() {
        return _args;
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        for (int i = 0; i < _args.length; i++) {
            _args[i] = _args[i].getSimplifiedOperand(valueMap);
        }
    }

    public Operand[] cloneOperandsForInlining(InlinerInfo ii) {
        Operand[] newArgs = new Operand[_args.length];
        for (int i = 0; i < _args.length; i++) {
            newArgs[i] = _args[i].cloneForInlining(ii);
        }

        return newArgs;
    }
}
