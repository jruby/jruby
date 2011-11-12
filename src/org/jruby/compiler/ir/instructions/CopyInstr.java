package org.jruby.compiler.ir.instructions;

// This is of the form:
//   d = s

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CopyInstr extends Instr implements ResultInstr {
    private Operand arg;
    private Variable result;

    public CopyInstr(Variable result, Operand s) {
        super(Operation.COPY);

        assert result != null: "CopyInstr result is null";
        assert s != null;
        
        this.arg = s;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{arg};
    }
    
    public Variable getResult() {
        return result;
    }
    
    public Operand getSource() {
        return arg;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        arg = arg.getSimplifiedOperand(valueMap);
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        
        return arg;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), arg.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        result.store(context, self, temp, arg.retrieve(context, self, temp));
        return null;
    }

    @Override
    public String toString() { 
        return (arg instanceof Variable) ? (super.toString() + "(" + arg + ")") : (result + " = " + arg);
    }

}
