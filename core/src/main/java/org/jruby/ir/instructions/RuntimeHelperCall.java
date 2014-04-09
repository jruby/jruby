package org.jruby.ir.instructions;

import java.util.Arrays;

import org.jruby.RubyFixnum;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.parser.IRStaticScope;

import java.util.Map;
import org.jruby.ir.operands.StringLiteral;

public class RuntimeHelperCall extends Instr implements ResultInstr {
    public enum Methods {
        HANDLE_PROPAGATE_BREAK, HANDLE_NONLOCAL_RETURN, HANDLE_BREAK_AND_RETURNS_IN_LAMBDA,
        IS_DEFINED_BACKREF, IS_DEFINED_NTH_REF,
    };

    Variable  result;
    Methods    helperMethod;
    Operand[] args;

    public RuntimeHelperCall(Variable result, Methods helperMethod, Operand[] args) {
        super(Operation.RUNTIME_HELPER);
        this.result = result;
        this.helperMethod = helperMethod;
        this.args = args;
    }

    public Operand[] getArgs() {
        return args;
    }

    public Methods getHelperMethod() {
        return helperMethod;
    }

    @Override
    public Operand[] getOperands() {
        return getArgs();
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].getSimplifiedOperand(valueMap, force);
        }
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: array of args cloning should be part of utility class
        Operand[] clonedArgs = new Operand[args.length];
        for (int i = 0; i < args.length; i++) {
            clonedArgs[i] = args[i].cloneForInlining(ii);
        }
        Variable var = getResult();
        return new RuntimeHelperCall(var == null ? null : ii.getRenamedVariable(var), helperMethod, clonedArgs);
    }

    @Override
    public String toString() {
        return (getResult() == null ? "" : (getResult() + " = ")) + getOperation()  + "(" + helperMethod + ", " + Arrays.toString(args) + ")";
    }

    public IRubyObject callHelper(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block.Type blockType) {
        IRStaticScope scope = (IRStaticScope) currDynScope.getStaticScope();

        switch (helperMethod) {
            case HANDLE_PROPAGATE_BREAK:
                return IRRuntimeHelpers.handlePropagatedBreak(context, scope,
                        args[0].retrieve(context, self, currDynScope, temp), blockType);
            case HANDLE_NONLOCAL_RETURN:
                return IRRuntimeHelpers.handleNonlocalReturn(scope,
                        args[0].retrieve(context, self, currDynScope, temp), blockType);
            case HANDLE_BREAK_AND_RETURNS_IN_LAMBDA:
                return IRRuntimeHelpers.handleBreakAndReturnsInLambdas(context, scope,
                        args[0].retrieve(context, self, currDynScope, temp), blockType);
            case IS_DEFINED_BACKREF:
                return IRRuntimeHelpers.isDefinedBackref(context);
            case IS_DEFINED_NTH_REF:
                return IRRuntimeHelpers.isDefinedNthRef(context,
                        (int) ((RubyFixnum) args[0].retrieve(context, self, currDynScope, temp)).getLongValue());
        }

        throw new RuntimeException("Unknown IR runtime helper method: " + helperMethod + "; INSTR: " + this);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RuntimeHelperCall(this);
    }
}
