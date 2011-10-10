package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JRubyImplCallInstr;
import org.jruby.compiler.ir.instructions.JRubyImplCallInstr.JRubyImplementationMethod;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CheckArityInstr extends CallInstr {
    public CheckArityInstr(Variable result, Operand receiver, Operand[] args) {
        super(Operation.CHECK_ARITY, CallType.FUNCTIONAL, result, JRubyImplementationMethod.CHECK_ARITY.getMethAddr(), receiver, args, null);
    }

    /**
     * This will either end up removing this instruction since we know arity
     * at a callsite or we will add a ArgumentError since we know arity is wrong.
     */
    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        Operand[] args = getCallArgs();
        int required = ((Fixnum) args[0]).value.intValue();
        int opt = ((Fixnum) args[1]).value.intValue();
        int rest = ((Fixnum) args[2]).value.intValue();
        int numArgs = ii.getArgsCount();
        
        if ((numArgs < required) || ((rest == -1) && (numArgs > (required + opt)))) {
            // Argument error! Throw it at runtime
            return new JRubyImplCallInstr(null, JRubyImplementationMethod.RAISE_ARGUMENT_ERROR, null,
                    new Operand[]{args[0], args[1], args[2], new Fixnum((long) numArgs)});
        }

        return null;
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Operand[] args = getCallArgs();
        int required = ((Fixnum) args[0]).value.intValue();
        int opt = ((Fixnum) args[1]).value.intValue();
        int rest = ((Fixnum) args[2]).value.intValue();
        int numArgs = interp.getParameterCount();
        
        if ((numArgs < required) || ((rest == -1) && (numArgs > (required + opt)))) {
            Arity.raiseArgumentError(context.getRuntime(), numArgs, required, 
                    required + opt);
        }

        return null;
    }
}
