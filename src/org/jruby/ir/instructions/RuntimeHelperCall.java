package org.jruby.ir.instructions;

import java.util.Arrays;

import org.jruby.RubyArray;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class RuntimeHelperCall extends Instr implements ResultInstr {
    Variable  result;
    String    helperMethod;
    Operand[] args;

    public RuntimeHelperCall(Variable result, String helperMethod, Operand[] args) {
        super(Operation.RUNTIME_HELPER);
        this.result = result;
        this.helperMethod = helperMethod;
        this.args = args;
    }

    public Operand[] getOperands() {
        return args;
    }

    public Variable getResult() {
        return result;
    }

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
            clonedArgs[i++] = args[i].cloneForInlining(ii);
        }
        return new RuntimeHelperCall(ii.getRenamedVariable(getResult()), helperMethod, clonedArgs);
    }

    @Override
    public String toString() {
        return "" + getOperation()  + "(" + helperMethod + ", " + Arrays.toString(args) + ")";
    }

    public IRubyObject callHelper(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, IRScope scope, Block.Type blockType) {
        if (helperMethod.equals("handlePropagatedBreak")) {
            Object exc = args[0].retrieve(context, self, currDynScope, temp);
            return IRRuntimeHelpers.handlePropagatedBreak(context, scope, exc, blockType);
        } else {
            // Unknown helper method!
            return null;
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RuntimeHelperCall(this);
    }
}
