package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.Ruby;
import org.jruby.runtime.Block;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/* Receive the closure argument (either implicit or explicit in Ruby source code) */
public class ReceiveClosureInstr extends Instr implements ResultInstr {
    private Variable result;
    
    public ReceiveClosureInstr(Variable result) {
        super(Operation.RECV_CLOSURE);
        
        assert result != null: "ReceiveClosureInstr result is null";
        
        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }    

    public Instr cloneForInlining(InlinerInfo ii) {
		  // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
		  // operand type that converts the static code block to a proc which is a closure.
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    @Interp
    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block, Object exception, Object[] temp) {
        Ruby  runtime = context.getRuntime();
        result.store(interp, context, self, block == Block.NULL_BLOCK ? runtime.getNil() : runtime.newProc(Type.PROC, block), temp);
        return null;
    }
}
