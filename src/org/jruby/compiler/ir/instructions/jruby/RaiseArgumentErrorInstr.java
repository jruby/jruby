package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public class RaiseArgumentErrorInstr extends Instr {
    private final Fixnum required;
    private final Fixnum opt;
    private final Fixnum rest;
    private final Fixnum numArgs;
    
    public RaiseArgumentErrorInstr(Fixnum required, Fixnum opt, Fixnum rest, Fixnum numArgs) {
        super(Operation.RAISE_ARGUMENT_ERROR);
        
        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.numArgs = numArgs;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { required, opt, rest, numArgs };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RaiseArgumentErrorInstr(required, opt, rest, numArgs);
    }

    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block) {
        int requiredInt = required.value.intValue();
        int optInt = opt.value.intValue();
        int restInt = rest.value.intValue();
        int numArgsInt = numArgs.value.intValue();
        
        Arity.raiseArgumentError(context.getRuntime(), numArgsInt, requiredInt, requiredInt + optInt);
        
        return null;
    }
    
}
