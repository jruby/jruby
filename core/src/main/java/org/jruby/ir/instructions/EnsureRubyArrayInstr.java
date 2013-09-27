package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class EnsureRubyArrayInstr extends Instr implements ResultInstr {
    private Operand object;
    private Variable result;

    public EnsureRubyArrayInstr(Variable result, Operand s) {
        super(Operation.ENSURE_RUBY_ARRAY);

        assert result != null : "EnsureRubyArray result is null";

        this.object = s;
        this.result = result;
    }

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        return (object instanceof Array) ? object : null;
    }

    public Operand getObject() {
        return object;
    }

    public Operand[] getOperands() {
        return new Operand[]{object};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        object = object.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + object + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new EnsureRubyArrayInstr(ii.getRenamedVariable(result), object.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject val = (IRubyObject)object.retrieve(context, self, currDynScope, temp);
        if (!(val instanceof RubyArray)) {
            val = ArgsUtil.convertToRubyArray(context.runtime, val, false);
        }
        return val;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.EnsureRubyArrayInstr(this);
    }
}
