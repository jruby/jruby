package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

//    is_true(a) = (!a.nil? && a != false) 
//
// Only nil and false compute to false
//
public class IsTrueInstr extends Instr implements ResultInstr {
    private Operand value;
    private Variable result;

    public IsTrueInstr(Variable result, Operand value) {
        super(Operation.IS_TRUE);
        
        assert result != null: "IsTrueInstr result is null";
        
        this.value = value;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{value};
    }
    
    public Variable getResult() {
        return result;
    }
    
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        value = value.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + value + ")";
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        if (!value.isConstant()) return null;

        return value == Nil.NIL || value == BooleanLiteral.FALSE ?
                    BooleanLiteral.FALSE : BooleanLiteral.TRUE;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new IsTrueInstr(ii.getRenamedVariable(result), value.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        // ENEBO: This seems like a lot of extra work...
        result.store(context, self, temp, context.getRuntime().newBoolean(((IRubyObject) value.retrieve(context, self, temp)).isTrue()));
        return null;
    }
}
