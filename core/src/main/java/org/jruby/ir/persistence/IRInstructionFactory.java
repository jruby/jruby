package org.jruby.ir.persistence;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

public enum IRInstructionFactory {
    INSTANCE;

    public AliasInstr createAlias(Variable receiver, Operand newName, Operand oldName) {
        return new AliasInstr(receiver, newName, oldName);
    }

    public BlockGivenInstr createBlockGiven(Variable result) {
        // FIXME: second argument should be closure operand but we have no way to make one...
        return new BlockGivenInstr(result, null);
    }

    public BackrefIsMatchDataInstr createBackrefIsMatchData(Variable result) {
        return new BackrefIsMatchDataInstr(result);
    }

    public BranchInstr createBranch(String instrName, Operand arg1, Operand arg2, Label target)
            throws IRPersistenceException {
        Operation operation = Operation.valueOf(instrName.toUpperCase());
        
        switch (operation) {
        case BEQ:
        case B_FALSE:
        case B_TRUE:
        case B_NIL:
        case B_UNDEF:
            return BEQInstr.create(arg1, arg2, target);
        case BNE:
            return BNEInstr.create(arg1, arg2, target);

        default:
            throw new IRPersistenceException();
        }
    }

}
