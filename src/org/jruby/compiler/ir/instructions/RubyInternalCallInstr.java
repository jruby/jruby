package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// Rather than building a zillion instructions that capture calls to ruby implementation internals,
// we are building one that will serve as a placeholder for internals-specific call optimizations.
public class RubyInternalCallInstr extends CallInstr {
    public RubyInternalCallInstr(Variable result, MethAddr methAddr, Operand receiver,
            Operand[] args) {
        super(Operation.RUBY_INTERNALS, result, methAddr, receiver, args, null);
    }

    public RubyInternalCallInstr(Variable result, MethAddr methAddr, Operand receiver,
            Operand[] args, Operand closure) {
        super(result, methAddr, receiver, args, closure);
    }

    @Override
    public boolean isRubyInternalsCall() {
        return true;
    }

    @Override
    public boolean isStaticCallTarget() {
        return true;
    }

    // SSS FIXME: Dont optimize these yet!
    @Override
    public IRMethod getTargetMethodWithReceiver(Operand receiver) {
        return null;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RubyInternalCallInstr(ii.getRenamedVariable(result),
                (MethAddr) methAddr.cloneForInlining(ii), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        if (getMethodAddr() == MethAddr.DEFINE_ALIAS) {
            Operand[] args = getCallArgs(); // Guaranteed 2 args by parser

            RubyModule clazz = self instanceof RubyModule ? (RubyModule) self : self.getMetaClass();
            clazz.defineAlias((String) args[0].retrieve(interp).toString(), (String) args[1].retrieve(interp).toString());
        } else {
            super.interpret(interp, self);
        }
        return null;
    }
}
