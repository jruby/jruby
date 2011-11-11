package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class RestoreArgumentsInstr extends Instr {
    private Operand newArgs;

    public RestoreArgumentsInstr(Variable newArgs) {
        super(Operation.RESTORE_ARGS);
        
        this.newArgs = newArgs;
    }

    public Operand[] getOperands() {
        return new Operand[]{newArgs};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        newArgs = newArgs.getSimplifiedOperand(valueMap);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new RestoreArgumentsInstr((Variable) newArgs.cloneForInlining(ii));
    }

    @Interp
    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block) {
        interp.setNewParameters((IRubyObject[]) newArgs.retrieve(interp, context, self));

        return null;
    }
}
