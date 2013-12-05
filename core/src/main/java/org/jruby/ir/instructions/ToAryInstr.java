package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class ToAryInstr extends Instr implements ResultInstr {
    private Variable result;
    private final BooleanLiteral dontToAryArrays;
    private Operand array;

    public ToAryInstr(Variable result, Operand array, BooleanLiteral dontToAryArrays) {
        super(Operation.TO_ARY);

        assert result != null: "ToArtInstr result is null";

        this.result = result;
        this.array = array;
        this.dontToAryArrays = dontToAryArrays;
    }

    public Operand getArrayArg() {
        return array;
    }

    public boolean dontToAryArrays() {
        return dontToAryArrays.isTrue();
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { array };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        array = array.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        return dontToAryArrays.isTrue() && (array.getValue(valueMap) instanceof Array) ? array : null;
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ToAryInstr((Variable) result.cloneForInlining(ii), array.cloneForInlining(ii),
                (BooleanLiteral) dontToAryArrays.cloneForInlining(ii));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + array + ", dont_to_ary_arrays: " + dontToAryArrays + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object receiver = array.retrieve(context, self, currDynScope, temp);
        return Helpers.irToAry(context, (IRubyObject)receiver, dontToAryArrays.isTrue());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ToAryInstr(this);
    }
}
