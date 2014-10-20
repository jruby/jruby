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

    private StaticScope staticScope;
    private final Instr[] instructions;

    // Cached computed fields
    private final boolean hasExplicitCallProtocol;
    private final boolean pushNewDynScope;
    private final boolean reuseParentDynScope;
    private final boolean popDynScope;
    private final boolean receivesKeywordArguments;
    private final boolean metaClassBodyScope;

    public InterpreterContext(StaticScope staticScope, boolean metaClassBodyScope,
                              int temporaryVariablecount, int temporaryBooleanVariablecount,
                              int temporaryFixnumVariablecount, int temporaryFloatVariablecount,
                              EnumSet<IRFlags> flags, Instr[] instructions) {
        super(null);

        this.staticScope = staticScope;
        this.metaClassBodyScope = metaClassBodyScope; // IRMetaClassBody
        this.temporaryVariablecount = temporaryVariablecount;
        this.temporaryBooleanVariablecount = temporaryBooleanVariablecount;
        this.temporaryFixnumVariablecount = temporaryFixnumVariablecount;
        this.temporaryFloatVariablecount = temporaryFloatVariablecount;
        this.instructions = instructions;
        this.hasExplicitCallProtocol = flags.contains(IRFlags.HAS_EXPLICIT_CALL_PROTOCOL);
        this.reuseParentDynScope = flags.contains(IRFlags.REUSE_PARENT_DYNSCOPE);
        this.pushNewDynScope = !flags.contains(IRFlags.DYNSCOPE_ELIMINATED) && !this.reuseParentDynScope;
        this.popDynScope = this.pushNewDynScope || this.reuseParentDynScope;
        this.receivesKeywordArguments = flags.contains(IRFlags.RECEIVES_KEYWORD_ARGS);
    }

    @Override
    public void addUsedVariables(List<Variable> l) {}

    @Override
    public Operand cloneForInlining(CloneInfo info) {
        throw new IllegalStateException("Should not clone interp context");
    }

    public StaticScope getStaticScope() {
        return staticScope;
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

    /**
     * Get a new dynamic scope.  Note: This only works for method scopes (ClosureIC will throw).
     */
    public DynamicScope newDynamicScope(ThreadContext context) {
        // Add a parent-link to current dynscope to support non-local returns cheaply. This doesn't
        // affect variable scoping since local variables will all have the right scope depth.
        if (metaClassBodyScope) return DynamicScope.newDynamicScope(staticScope, context.getCurrentScope());

        return DynamicScope.newDynamicScope(staticScope);
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

    public boolean receivesKeywordArguments() {
        return receivesKeywordArguments;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return super.retrieve(context, self, currScope, currDynScope, temp);
    }
}
