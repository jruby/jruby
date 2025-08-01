package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.Integer;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Stringable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

import static org.jruby.ir.IRFlags.REQUIRES_BACKREF;
import static org.jruby.ir.IRFlags.REQUIRES_CLASS;

public class RuntimeHelperCall extends NOperandResultBaseInstr {
    public enum Methods {
        HANDLE_PROPAGATED_BREAK, HANDLE_NONLOCAL_RETURN, HANDLE_BREAK_AND_RETURNS_IN_LAMBDA,
        IS_DEFINED_BACKREF, IS_DEFINED_NTH_REF, IS_DEFINED_GLOBAL,
        IS_DEFINED_CLASS_VAR, IS_DEFINED_SUPER, IS_DEFINED_METHOD, IS_DEFINED_CALL,
        IS_DEFINED_CONSTANT_OR_METHOD, MERGE_KWARGS, IS_HASH_EMPTY, HASH_CHECK, ARRAY_LENGTH,
        TRACE_RESCUE, RESET_GVAR_UNDERSCORE;

        private static final Methods[] VALUES = values();

        static Methods fromOrdinal(int value) {
            return value < 0 || value >= VALUES.length ? null : VALUES[value];
        }
    }

    final Methods    helperMethod;

    public RuntimeHelperCall(Variable result, Methods helperMethod, Operand[] args) {
        super(Operation.RUNTIME_HELPER, result, args);
        this.helperMethod = helperMethod;
    }

    public Operand[] getArgs() {
        return getOperands();
    }

    public Methods getHelperMethod() {
        return helperMethod;
    }

    /**
     * Does this instruction do anything the scope is interested in?
     *
     * @param flags to be updated
     * @return true if it modified the scope.
     */
    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        boolean modifiedScope = false;

        // FIXME: Impl of this helper uses frame class.  Determine if we can do this another way.
        if (helperMethod == Methods.IS_DEFINED_SUPER) {
            modifiedScope = true;
            flags.add(REQUIRES_CLASS);
        } else if (helperMethod == Methods.IS_DEFINED_BACKREF || helperMethod == Methods.IS_DEFINED_NTH_REF) {
            modifiedScope = true;
            flags.add(REQUIRES_BACKREF);
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

    public Object callHelper(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self,
                             Object[] temp, Block block) {
        Operand[] operands = getOperands();

        // These have special operands[0] that we may not want to execute
        switch (helperMethod) {
            case RESET_GVAR_UNDERSCORE:
                return context.setErrorInfo((IRubyObject) operands[0].retrieve(context, self, currScope, currDynScope, temp));
            case IS_DEFINED_BACKREF:
                return IRRuntimeHelpers.isDefinedBackref(
                        context,
                        (IRubyObject) operands[0].retrieve(context, self, currScope, currDynScope, temp));
            case IS_DEFINED_NTH_REF:
                return IRRuntimeHelpers.isDefinedNthRef(
                        context,
                        (int) ((Fixnum) operands[0]).getValue(),
                        (IRubyObject) operands[1].retrieve(context, self, currScope, currDynScope, temp));
            case IS_DEFINED_GLOBAL:
                return IRRuntimeHelpers.isDefinedGlobal(
                        context,
                        ((Stringable) operands[0]).getString(),
                        (IRubyObject) operands[1].retrieve(context, self, currScope, currDynScope, temp));
            case TRACE_RESCUE:
                IRRuntimeHelpers.traceRescue(context, ((Stringable) operands[0]).getString(), (int) ((Integer) operands[1]).getValue());
                return context.nil;
        }

        Object arg1 = operands[0].retrieve(context, self, currScope, currDynScope, temp);

        switch (helperMethod) {
            case HANDLE_PROPAGATED_BREAK:
                return IRRuntimeHelpers.handlePropagatedBreak(context, currDynScope, arg1);
            case HANDLE_NONLOCAL_RETURN:
                return IRRuntimeHelpers.handleNonlocalReturn(currDynScope, arg1);
            case HANDLE_BREAK_AND_RETURNS_IN_LAMBDA:
                return IRRuntimeHelpers.handleBreakAndReturnsInLambdas(context, currDynScope, arg1, block);
            case IS_DEFINED_CALL:
                return IRRuntimeHelpers.isDefinedCall(
                        context,
                        self,
                        (IRubyObject) arg1,
                        ((Stringable) operands[1]).getString(),
                        (IRubyObject) operands[2].retrieve(context, self, currScope, currDynScope, temp));
            case IS_DEFINED_CONSTANT_OR_METHOD:
                return IRRuntimeHelpers.isDefinedConstantOrMethod(
                        context,
                        (IRubyObject) arg1,
                        ((FrozenString) operands[1]).retrieve(context, self, currScope, currDynScope, temp),
                        (IRubyObject) operands[2].retrieve(context, self, currScope, currDynScope, temp),
                        (IRubyObject) operands[3].retrieve(context, self, currScope, currDynScope, temp));
            case IS_DEFINED_CLASS_VAR:
                return IRRuntimeHelpers.isDefinedClassVar(
                        context,
                        (RubyModule) arg1,
                        ((Stringable) operands[1]).getString(),
                        (IRubyObject) operands[2].retrieve(context, self, currScope, currDynScope, temp));
            case IS_DEFINED_SUPER:
                return IRRuntimeHelpers.isDefinedSuper(
                        context,
                        (IRubyObject) arg1,
                        (IRubyObject) operands[1].retrieve(context, self, currScope, currDynScope, temp));
            case IS_DEFINED_METHOD:
                return IRRuntimeHelpers.isDefinedMethod(context, (IRubyObject) arg1,
                        ((Stringable) operands[1]).getString(),
                        ((Boolean) operands[2]).isTrue(),
                        (IRubyObject) operands[3].retrieve(context, self, currScope, currDynScope, temp));
            case MERGE_KWARGS:
                return IRRuntimeHelpers.mergeKeywordArguments(context, (IRubyObject) arg1,
                        (IRubyObject) getArgs()[1].retrieve(context, self, currScope, currDynScope, temp),
                        getArgs()[2] == context.runtime.getIRManager().getTrue());
            case IS_HASH_EMPTY:
                return IRRuntimeHelpers.isHashEmpty(context, (IRubyObject) arg1);
            case HASH_CHECK:
                return IRRuntimeHelpers.hashCheck(context, (IRubyObject) arg1);
            case ARRAY_LENGTH:
                return IRRuntimeHelpers.arrayLength((RubyArray) arg1);
        }

        throw new RuntimeException("Unknown IR runtime helper method: " + helperMethod + "; INSTR: " + this);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RuntimeHelperCall(this);
    }
}
