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

    public void updateResult(Variable v) {
        this.result = v;
    }
    
    public Operand getSource() {
        return arg;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        arg = arg.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        
        return arg;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), arg.cloneForInlining(ii));
    }

    @Override
    public String toString() { 
        return (arg instanceof Variable) ? (super.toString() + "(" + arg + ")") : (result + " = " + arg);
    }

}
