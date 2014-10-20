package org.jruby.ir.operands;

import java.util.EnumSet;
import java.util.List;
import org.jruby.ir.IRFlags;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpreterContext extends Operand {
    private final int temporaryVariablecount;
    private final int temporaryBooleanVariablecount;
    private final int temporaryFixnumVariablecount;
    private final int temporaryFloatVariablecount;

    private final Instr[] instructions;

    // Cached computed fields
    private final boolean hasExplicitCallProtocol;
    private final boolean pushNewDynScope;
    private final boolean reuseParentDynScope;
    private final boolean popDynScope;

    public InterpreterContext(int temporaryVariablecount, int temporaryBooleanVariablecount,
                              int temporaryFixnumVariablecount, int temporaryFloatVariablecount,
                              EnumSet<IRFlags> flags, Instr[] instructions) {
        super(null);

        this.temporaryVariablecount = temporaryVariablecount;
        this.temporaryBooleanVariablecount = temporaryBooleanVariablecount;
        this.temporaryFixnumVariablecount = temporaryFixnumVariablecount;
        this.temporaryFloatVariablecount = temporaryFloatVariablecount;
        this.instructions = instructions;
        this.hasExplicitCallProtocol = flags.contains(IRFlags.HAS_EXPLICIT_CALL_PROTOCOL);
        this.reuseParentDynScope = flags.contains(IRFlags.REUSE_PARENT_DYNSCOPE);
        this.pushNewDynScope = !flags.contains(IRFlags.DYNSCOPE_ELIMINATED) && !this.reuseParentDynScope;
        this.popDynScope = this.pushNewDynScope || this.reuseParentDynScope;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {}

    @Override
    public Operand cloneForInlining(CloneInfo info) {
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

    public Instr[] getInstructions() {
        return instructions;
    }


    public boolean hasExplicitCallProtocol() {
        return hasExplicitCallProtocol;
    }

    public boolean pushNewDynScope() {
        return pushNewDynScope;
    }

    public boolean reuseParentDynScope() {
        return reuseParentDynScope;
    }

    public boolean popDynScope() {
        return popDynScope;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return super.retrieve(context, self, currScope, currDynScope, temp);
    }
}
