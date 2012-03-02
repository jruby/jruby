package org.jruby.compiler.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class SuperInstr extends CallInstr {
    public SuperInstr(Variable result, Operand receiver, MethAddr superMeth, Operand[] args, Operand closure) {
        super(Operation.SUPER, CallType.SUPER, result, superMeth, receiver, args, closure);
    }

    public SuperInstr(Operation op, Variable result, Operand closure) {
        super(op, CallType.SUPER, result, MethAddr.UNKNOWN_SUPER_TARGET, null, EMPTY_OPERANDS, closure);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new SuperInstr(ii.getRenamedVariable(getResult()), getReceiver().cloneForInlining(ii), (MethAddr)getMethodAddr().cloneForInlining(ii),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    // We cannot convert this into a NoCallResultInstr
    @Override
    public Instr discardResult() {
        return this;
    }

    @Override
    public CallBase specializeForInterpretation() {
        return this;
    }
    
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block aBlock) {
        IRubyObject[] args = prepareArguments(context, self, getCallArgs(), currDynScope, temp);
        Block block = prepareBlock(context, self, currDynScope, temp);
        return interpretSuper(context, self, args, block);
    }

    protected Object interpretSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        // SSS FIXME: We should check in the current module (for instance methods) or the current module's meta class (for class methods)
        //
        // RubyModule currM = context.getCurrentScope().getStaticScope().getModule();
        // RubyModule klazz = (isInstanceMethodSuper) ? currM : currM.getMetaClass();
        //
        // The question is how do we know what this 'super' ought to do?
        // For 'super' that occurs in a method scope, this is easy to figure out.
        // But, what about 'super' that occurs in block scope?  How do we figure that out? 
        RubyModule klazz = context.getFrameKlazz();

        // SSS FIXME: Even though we may know the method name in some instances,
        // we are not making use of it here.
        String methodName = context.getCurrentFrame().getName(); // methAddr.getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyClass superClass = RuntimeHelpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        
        Object rVal = method.isUndefined() ? RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                                           : method.call(context, self, superClass, methodName, args, block);

        return hasUnusedResult() ? null : rVal;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String methodName) {
        // FIXME: super/zsuper in top-level script still seems to have a frameClass so it will not make it into this if
        if (frameClass == null) {
            if (methodName == null || methodName != "") {
                throw context.getRuntime().newNameError("superclass method '" + methodName + "' disabled", methodName);
            } else {
                throw context.getRuntime().newNoMethodError("super called outside of method", null, context.getRuntime().getNil());
            }
        }
    }
}
