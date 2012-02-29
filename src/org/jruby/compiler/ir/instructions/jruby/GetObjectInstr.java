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
 * We want object soo much we have our own instruction for it.  Much faster
 * than const searching.
 */
public class GetObjectInstr extends Instr implements ResultInstr {
    private Variable result;
    
    public GetObjectInstr(Variable result) {
        super(Operation.GET_OBJECT);
        
        this.result = result;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new GetObjectInstr((Variable) getResult().cloneForInlining(inlinerInfo));
    }

    @Override
    public String toString() {
        return super.toString() + "()";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return context.runtime.getObject();
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    }
}
