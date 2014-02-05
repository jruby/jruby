package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
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
import org.jruby.ir.operands.UnboxedBoolean;

public class YieldInstr extends Instr implements ResultInstr, FixedArityInstr {
    public final boolean unwrapArray;
    private Operand blockArg;
    private Operand yieldArg;
    private Variable result;

    public YieldInstr(Variable result, Operand block, Operand arg, boolean unwrapArray) {
        super(Operation.YIELD);

        assert result != null: "YieldInstr result is null";

        this.blockArg = block;
        this.yieldArg = arg == null ? UndefinedValue.UNDEFINED : arg;
        this.unwrapArray = unwrapArray;
        this.result = result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // FIXME: Is it necessary to clone a yield instruction in a method
        // that is being inlined, i.e. in METHOD_INLINE clone mode?
        // Fix BasicBlock.java:cloneForInlining!!
        return new YieldInstr(ii.getRenamedVariable(result), blockArg.cloneForInlining(ii), yieldArg.cloneForInlining(ii), unwrapArray);
    }

    public Operand getBlockArg() {
        return blockArg;
    }

    public Operand getYieldArg() {
        return yieldArg;
    }

    @Override
    public String toString() {
        return unwrapArray ? (super.toString() + "(" + blockArg + ", UNWRAP(" + yieldArg + "))") : (super.toString() + "(" + blockArg + ", " + yieldArg + ")");
    }

    public boolean isUnwrapArray() {
        return unwrapArray;
    }

    // if unwrapArray, maybe convert yieldArg into a CompoundArray operand?
    @Override
    public Operand[] getOperands() {
        return new Operand[] {blockArg, yieldArg, new UnboxedBoolean(unwrapArray) };
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand[] getNonBlockOperands() {
        return new Operand[] {yieldArg};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        blockArg = blockArg.getSimplifiedOperand(valueMap, force);
        yieldArg = yieldArg.getSimplifiedOperand(valueMap, force);
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object blk = blockArg.retrieve(context, self, currDynScope, temp);
        if (yieldArg == UndefinedValue.UNDEFINED) {
            return IRRuntimeHelpers.yieldSpecific(context, blk);
        } else {
            IRubyObject yieldVal = (IRubyObject)yieldArg.retrieve(context, self, currDynScope, temp);
            return IRRuntimeHelpers.yield(context, blk, yieldVal, unwrapArray);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.YieldInstr(this);
    }
}
