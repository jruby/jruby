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

import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;

import org.jruby.javasupport.util.RuntimeHelpers;

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

/***
    // SSS FIXME: Dont optimize these yet!
    @Override
    public IRMethod getTargetMethodWithReceiver(Operand receiver) {
        return null;
    }
***/

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RubyInternalCallInstr(ii.getRenamedVariable(result),
                (MethAddr) methAddr.cloneForInlining(ii), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp) {
        MethAddr ma = getMethodAddr();
        if (ma == MethAddr.DEFINE_ALIAS) {
            Operand[] args = getCallArgs(); // Guaranteed 2 args by parser

            IRubyObject self = (IRubyObject)getReceiver().retrieve(interp);
            RuntimeHelpers.defineAlias(interp.getContext(), self, args[0].retrieve(interp).toString(), (String) args[1].retrieve(interp).toString());
        } else if ((ma == MethAddr.SUPER) || (ma == MethAddr.ZSUPER)) {
            IRubyObject   self = (IRubyObject)getReceiver().retrieve(interp);
            IRubyObject[] args = prepareArguments(getCallArgs(), interp);
            getResult().store(interp, RuntimeHelpers.invokeSuper(interp.getContext(), self, args, prepareBlock(interp)));
        } else if (ma == MethAddr.GVAR_ALIAS) {
            throw new RuntimeException("GVAR_ALIAS: Not implemented yet!");
        } else if (ma == MethAddr.FOR_EACH) {
            // SSS FIXME: Correct?
            super.interpret(interp);
        } else {
            super.interpret(interp);
        }
        return null;
    }

    // SSS FIXME: Copied from runtime/callsite/SuperCallSite!  Probably worth putting in a library
    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String frameName) {
        if (frameClass == null) {
            if (frameName != null) {
                throw context.getRuntime().newNameError("superclass method '" + frameName + "' disabled", frameName);
            } else {
                throw context.getRuntime().newNoMethodError("super called outside of method", null, context.getRuntime().getNil());
            }
        }
    }
}
