/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;

/**
 *
 * @author enebo
 */
public abstract class TwoOperandBranchInstr extends BranchInstr {
    private Operand arg1;
    private Operand arg2;

    public TwoOperandBranchInstr(Operation op, Operand arg1, Operand arg2, Label jumpTarget) {
        super(op, jumpTarget);

        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public Operand getArg1() {
        return arg1;
    }

    public Operand getArg2() {
        return arg2;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { getArg1(), getArg2(), getJumpTarget() };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        arg1 = arg1.getSimplifiedOperand(valueMap, force);
        arg2 = arg2.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return "" + getOperation() + "(" + arg1 + ", " +  arg2 + ", "  + getJumpTarget() + ")";
    }
}
