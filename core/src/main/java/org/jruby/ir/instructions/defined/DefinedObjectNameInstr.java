/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions.defined;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;

import java.util.Map;

/**
 * This is a base class for define instructions which have a receiver (object)
 * and a name to query on that object.
 */
public abstract class DefinedObjectNameInstr extends DefinedInstr {
    public DefinedObjectNameInstr(Operation operation, Variable result, Operand[] operands) {
        super(operation, result, operands);

        assert operands.length >= 2 : "Too few operands to " + getClass().getName();
        assert operands[1] instanceof StringLiteral : "Operand 1 must be a string literal.  Was '" + operands[1].getClass() + "'";
    }

    public Operand getObject() {
        return operands[0];
    }

    public StringLiteral getName() {
        return (StringLiteral) operands[1];
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        // ENEBO: can variables ever simplify?  (CallBase does it?)
        //result = (Variable) result.getSimplifiedOperand(valueMap, force);

        for (int i = 0; i < operands.length; i++) {
            operands[i] = operands[i].getSimplifiedOperand(valueMap, force);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getObject() + ", " + getName() + ")";
    }
}
