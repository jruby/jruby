/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class GetErrorInfoInstr extends Instr implements ResultInstr {
        private Operand[] operands;
    
    public GetErrorInfoInstr(Variable result) {
        super(Operation.GET_ERROR_INFO);
        
        this.operands = new Operand[] { result };
    }

    @Override
    public Operand[] getOperands() {
        return operands;
    }
    
    public Variable getResult() {
        return (Variable) operands[0];
    }

    public void updateResult(Variable v) {
        operands[0] = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new GetErrorInfoInstr((Variable) getResult().cloneForInlining(inlinerInfo));
    }

    @Override
    public String toString() {
        return super.toString() + "()";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return context.getErrorInfo();
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    } 
}
