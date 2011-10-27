package org.jruby.compiler.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.RubyModule;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.interpreter.InterpreterContext;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.javasupport.util.RuntimeHelpers;

public class SuperInstr extends CallInstr {
    public SuperInstr(Variable result, Operand receiver, MethAddr superMeth, Operand[] args, Operand closure) {
        super(Operation.SUPER, CallType.SUPER, result, superMeth, receiver, args, closure);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new SuperInstr(ii.getRenamedVariable(getResult()), getReceiver().cloneForInlining(ii), methAddr,
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // FIXME: Receiver is not being used...should we be retrieving it?
        IRubyObject receiver = (IRubyObject)getReceiver().retrieve(interp, context, self);
        IRubyObject[] args = prepareArguments(interp, context, self, getCallArgs());
        Block block = prepareBlock(interp, context, self);
        RubyModule klazz = context.getFrameKlazz();
        // SSS FIXME: Even though we may know the method name in some instances,
        // we are not making use of it here.  It is cleaner in the sense of not
        // relying on implicit information whose data flow doesn't show up in the IR.
        String methodName = context.getCurrentFrame().getName(); // methAddr.getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyClass superClass = RuntimeHelpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        
        Object rVal = method.isUndefined() ? RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                                           : method.call(context, self, superClass, methodName, args, block);

        getResult().store(interp, context, self, rVal);

        return null;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String methodName) {
        if (frameClass == null) {
            if ((methodName == null) || (methodName != "")) {
                throw context.getRuntime().newNameError("superclass method '" + methodName + "' disabled", methodName);
            } else {
                throw context.getRuntime().newNoMethodError("super called outside of method", null, context.getRuntime().getNil());
            }
        }
    }
}
