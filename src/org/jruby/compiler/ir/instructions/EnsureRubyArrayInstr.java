package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;

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
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        return (object instanceof Array) ? object : null;
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
    public Object interpret(ThreadContext context, IRubyObject self, Object[] temp, Block block) {
        IRubyObject val = (IRubyObject)object.retrieve(context, self, temp);
        if (!(val instanceof RubyArray)) val = ArgsUtil.convertToRubyArray(context.getRuntime(), val, false);
        result.store(context, temp, val);
        return null;
    }
}
