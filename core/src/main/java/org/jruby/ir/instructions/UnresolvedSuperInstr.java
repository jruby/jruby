package org.jruby.ir.instructions;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class UnresolvedSuperInstr extends CallInstr {
	 // SSS FIXME: receiver is never used -- being passed in only to meet requirements of CallInstr
    public UnresolvedSuperInstr(Variable result, Operand receiver, Operand[] args, Operand closure) {
        super(Operation.SUPER, CallType.SUPER, result, MethAddr.UNKNOWN_SUPER_TARGET, receiver, args, closure);
    }

    public UnresolvedSuperInstr(Operation op, Variable result, Operand receiver, Operand closure) {
        super(op, CallType.SUPER, result, MethAddr.UNKNOWN_SUPER_TARGET, receiver, EMPTY_OPERANDS, closure);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new UnresolvedSuperInstr(ii.getRenamedVariable(getResult()), getReceiver().cloneForInlining(ii), cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
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
        RubyBasicObject objClass = context.runtime.getObject();
		  // We have to rely on the frame stack to find the implementation class
        RubyModule klazz = context.getFrameKlazz();
        String methodName = context.getCurrentFrame().getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyClass superClass = Helpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;

        Object rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                                           : method.call(context, self, superClass, methodName, args, block);

        return hasUnusedResult() ? null : rVal;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String methodName) {
        // FIXME: super/zsuper in top-level script still seems to have a frameClass so it will not make it into this if
        if (frameClass == null) {
            if (methodName == null || methodName != "") {
                throw context.runtime.newNameError("superclass method '" + methodName + "' disabled", methodName);
            } else {
                throw context.runtime.newNoMethodError("super called outside of method", null, context.runtime.getNil());
            }
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UnresolvedSuperInstr(this);
    }
}
