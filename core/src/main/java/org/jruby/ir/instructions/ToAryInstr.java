package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class ToAryInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;
    private Operand array;

    public ToAryInstr(Variable result, Operand array) {
        super(Operation.TO_ARY);

        assert result != null: "ToAryInstr result is null";

        this.result = result;
        this.array = array;
    }

    public Operand getArrayArg() {
        return array;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { array };
    }

    @Override
    public boolean canBeDeleted(IRScope s) {
        // This is an instruction that can be safely deleted
        // since it is inserted by JRuby to facilitate other operations
        // and has no real side effects. Currently, this has been marked
        // as side-effecting in Operation.java. FIXME: Revisit that!
        return true;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        array = array.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        Operand a = array.getValue(valueMap);
        return a instanceof Array ? a : null;
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
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ToAryInstr((Variable) result.cloneForInlining(ii), array.cloneForInlining(ii));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + array + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object receiver = array.retrieve(context, self, currDynScope, temp);
        return Helpers.irToAry(context, (IRubyObject)receiver);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ToAryInstr(this);
    }
}
