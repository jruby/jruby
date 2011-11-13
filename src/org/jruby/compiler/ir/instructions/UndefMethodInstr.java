package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public class UndefMethodInstr extends Instr implements ResultInstr {
    private final Variable result;
    private Operand methodName;
    
    public UndefMethodInstr(Variable result, Operand methodName) {
        super(Operation.UNDEF_METHOD);
        
        this.result = result;
        this.methodName = methodName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { methodName };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        methodName = methodName.getSimplifiedOperand(valueMap, force);
    }
    
    public Variable getResult() {
        return result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new UndefMethodInstr((Variable) result.cloneForInlining(ii),
                methodName.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        result.store(context, self, temp, RuntimeHelpers.undefMethod(context, methodName.retrieve(context, self, temp)));
        return null;
    }
    
}
