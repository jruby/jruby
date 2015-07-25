package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.*;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ir.IRFlags.REQUIRES_FRAME;

public class RuntimeHelperCall extends ResultBaseInstr {
    public enum Methods {
        HANDLE_PROPAGATE_BREAK, HANDLE_NONLOCAL_RETURN, HANDLE_BREAK_AND_RETURNS_IN_LAMBDA,
        IS_DEFINED_BACKREF, IS_DEFINED_NTH_REF, IS_DEFINED_GLOBAL, IS_DEFINED_INSTANCE_VAR,
        IS_DEFINED_CLASS_VAR, IS_DEFINED_SUPER, IS_DEFINED_METHOD, IS_DEFINED_CALL,
        IS_DEFINED_CONSTANT_OR_METHOD, MERGE_KWARGS;

        public static Methods fromOrdinal(int value) {
            return value < 0 || value >= values().length ? null : values()[value];
        }
    }

    Methods    helperMethod;

    public RuntimeHelperCall(Variable result, Methods helperMethod, Operand[] args) {
        super(Operation.RUNTIME_HELPER, result, args);
        this.helperMethod = helperMethod;
    }

    public Operand[] getArgs() {
        return operands;
    }

    public Methods getHelperMethod() {
        return helperMethod;
    }

    /**
     * Does this instruction do anything the scope is interested in?
     *
     * @param scope to be updated
     * @return true if it modified the scope.
     */
    @Override
    public boolean computeScopeFlags(IRScope scope) {
        boolean modifiedScope = false;

        // FIXME: Impl of this helper uses frame class.  Determine if we can do this another way.
        if (helperMethod == Methods.IS_DEFINED_SUPER) {
            modifiedScope = true;
            scope.getFlags().add(REQUIRES_FRAME);
        }

        return modifiedScope;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        Variable var = getResult();
        return new RuntimeHelperCall(var == null ? null : ii.getRenamedVariable(var), helperMethod, cloneOperands(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getHelperMethod().ordinal());
        e.encode(getArgs());
    }

    public static RuntimeHelperCall decode(IRReaderDecoder d) {
        return new RuntimeHelperCall(d.decodeVariable(), Methods.fromOrdinal(d.decodeInt()), d.decodeOperandArray());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "method: " + helperMethod};
    }

    public IRubyObject callHelper(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block.Type blockType) {
        StaticScope scope = currDynScope.getStaticScope();

        if (helperMethod == Methods.IS_DEFINED_BACKREF) return IRRuntimeHelpers.isDefinedBackref(context);

        switch (helperMethod) {
            case IS_DEFINED_NTH_REF:
                return IRRuntimeHelpers.isDefinedNthRef(context, (int) ((Fixnum) operands[0]).getValue());
            case IS_DEFINED_GLOBAL:
                return IRRuntimeHelpers.isDefinedGlobal(context, ((StringLiteral) operands[0]).getString());
        }

        Object arg1 = operands[0].retrieve(context, self, currScope, currDynScope, temp);

        switch (helperMethod) {
            case HANDLE_PROPAGATE_BREAK:
                return IRRuntimeHelpers.handlePropagatedBreak(context, currDynScope, arg1, blockType);
            case HANDLE_NONLOCAL_RETURN:
                return IRRuntimeHelpers.handleNonlocalReturn(scope, currDynScope, arg1, blockType);
            case HANDLE_BREAK_AND_RETURNS_IN_LAMBDA:
                return IRRuntimeHelpers.handleBreakAndReturnsInLambdas(context, scope, currDynScope, arg1, blockType);
            case IS_DEFINED_CALL:
                return IRRuntimeHelpers.isDefinedCall(context, self, (IRubyObject) arg1, ((StringLiteral) operands[1]).getString());
            case IS_DEFINED_CONSTANT_OR_METHOD:
                return IRRuntimeHelpers.isDefinedConstantOrMethod(context, (IRubyObject) arg1,
                        ((FrozenString) operands[1]).getString());
            case IS_DEFINED_INSTANCE_VAR:
                return IRRuntimeHelpers.isDefinedInstanceVar(context, (IRubyObject) arg1, ((StringLiteral) operands[1]).getString());
            case IS_DEFINED_CLASS_VAR:
                return IRRuntimeHelpers.isDefinedClassVar(context, (RubyModule) arg1, ((StringLiteral) operands[1]).getString());
            case IS_DEFINED_SUPER:
                return IRRuntimeHelpers.isDefinedSuper(context, (IRubyObject) arg1);
            case IS_DEFINED_METHOD:
                return IRRuntimeHelpers.isDefinedMethod(context, (IRubyObject) arg1,
                        ((StringLiteral) operands[1]).getString(),
                        ((Boolean) operands[2]).isTrue());
            case MERGE_KWARGS:
                return IRRuntimeHelpers.mergeKeywordArguments(context, (IRubyObject) arg1,
                        (IRubyObject) getArgs()[1].retrieve(context, self, currScope, currDynScope, temp));
        }

        throw new RuntimeException("Unknown IR runtime helper method: " + helperMethod + "; INSTR: " + this);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RuntimeHelperCall(this);
    }
}
