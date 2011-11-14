package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyRegexp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class MatchInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand receiver;
    
    public MatchInstr(Variable result, Operand receiver) {
        super(Operation.MATCH);
        
        assert result != null: "MatchInstr result is null";
        
        this.result = result;
        this.receiver = receiver;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { receiver };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        receiver = receiver.getSimplifiedOperand(valueMap, force);
    }

    public Variable getResult() {
        return result;
    }
    
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new MatchInstr((Variable) result.cloneForInlining(ii), receiver.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        RubyRegexp regexp = (RubyRegexp) receiver.retrieve(context, self, temp);
        result.store(context, self, temp, regexp.op_match2(context));
        return null;
    }
}
