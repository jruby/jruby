package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CheckArityInstr extends Instr {
    private final Fixnum required;
    private final Fixnum opt;
    private final Fixnum rest;
    
    public CheckArityInstr(Fixnum required, Fixnum opt, Fixnum rest) {
        super(Operation.CHECK_ARITY);
        
        this.required = required;
        this.opt = opt;
        this.rest = rest;
    }
    
    @Override
    public Operand[] getOperands() {
        return new Operand[] { required, opt, rest };
    }
    
    /**
     * This will either end up removing this instruction since we know arity
     * at a callsite or we will add a ArgumentError since we know arity is wrong.
     */
    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        int requiredInt = required.value.intValue();
        int optInt = opt.value.intValue();
        int restInt = rest.value.intValue();
        int numArgs = ii.getArgsCount();
        
        if ((numArgs < requiredInt) || ((restInt == -1) && (numArgs > (requiredInt + optInt)))) {
            return new RaiseArgumentErrorInstr(required, opt, rest, rest);
        }

        return null;
    }

    @Override
    public Label interpret(InterpreterContext interp, IRExecutionScope scope, ThreadContext context, IRubyObject self, org.jruby.runtime.Block block) {
        int requiredInt = required.value.intValue();
        int optInt = opt.value.intValue();
        int restInt = rest.value.intValue();
        int numArgs = interp.getParameterCount();
        
        if ((numArgs < requiredInt) || ((restInt == -1) && (numArgs > (requiredInt + optInt)))) {
            Arity.raiseArgumentError(context.getRuntime(), numArgs, requiredInt, requiredInt + optInt);
        }

        return null;
    }
}
