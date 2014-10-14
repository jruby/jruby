package org.jruby.ir.operands;

import java.util.EnumSet;
import java.util.List;
import org.jruby.ir.IRFlags;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.transformations.inlining.CloneInfo;

/**
 *
 */
public class InterpreterContext extends Operand {
    private int temporaryVariablecount;
    private int temporaryBooleanVariablecount;
    private int temporaryFixnumVariablecount;
    private int temporaryFloatVariablecount;

    private EnumSet<IRFlags> flags;

    private Instr[] instructions;

    public InterpreterContext(int temporaryVariablecount, int temporaryBooleanVariablecount,
                              int temporaryFixnumVariablecount, int temporaryFloatVariablecount,
                              EnumSet<IRFlags> flags, Instr[] instructions) {
        super(null);

        this.temporaryVariablecount = temporaryVariablecount;
        this.temporaryBooleanVariablecount = temporaryBooleanVariablecount;
        this.temporaryFixnumVariablecount = temporaryFixnumVariablecount;
        this.temporaryFloatVariablecount = temporaryFloatVariablecount;
        this.flags = flags;
        this.instructions = instructions;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {}

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        throw new IllegalStateException("Should not clone interp context");
    }


    public int getTemporaryVariablecount() {
        return temporaryVariablecount;
    }

    public int getTemporaryBooleanVariablecount() {
        return temporaryBooleanVariablecount;
    }

    public int getTemporaryFixnumVariablecount() {
        return temporaryFixnumVariablecount;
    }

    public int getTemporaryFloatVariablecount() {
        return temporaryFloatVariablecount;
    }

    public EnumSet<IRFlags> getFlags() {
        return flags;
    }

    public Instr[] getInstructions() {
        return instructions;
    }
}
