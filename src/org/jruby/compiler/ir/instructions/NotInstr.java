package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class NotInstr extends Instr implements ResultInstr {
    private Operand arg;
    private Variable result;

    public NotInstr(Variable result, Operand arg) {
        super(Operation.NOT);
        
        assert result != null: "NotInstr result is null";
        
        this.arg = arg;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{arg};
    }

    public Variable getResult() {
        return result;
    }
    
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        arg = arg.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + arg + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new NotInstr(ii.getRenamedVariable(result), arg.cloneForInlining(ii));
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        return (arg instanceof BooleanLiteral) ? ((BooleanLiteral) arg).logicalNot() : null;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        boolean not = !((IRubyObject) arg.retrieve(context, self, currDynScope, temp)).isTrue();
        return context.getRuntime().newBoolean(not);
    }
}
