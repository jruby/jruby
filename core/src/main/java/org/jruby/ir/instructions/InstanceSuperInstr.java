package org.jruby.ir.instructions;

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

public class InstanceSuperInstr extends CallInstr {
    public InstanceSuperInstr(Variable result, Operand definingModule, MethAddr superMeth, Operand[] args, Operand closure) {
        super(Operation.SUPER, CallType.SUPER, result, superMeth, definingModule, args, closure);
    }

    public Operand getDefiningModule() {
        return getReceiver();
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new InstanceSuperInstr(ii.getRenamedVariable(getResult()), getDefiningModule().cloneForInlining(ii), (MethAddr)getMethodAddr().cloneForInlining(ii),
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
        String methodName = methAddr.getName();
        RubyModule definingModule = (RubyModule) getDefiningModule().retrieve(context, self, currDynScope, temp);
        RubyClass superClass = definingModule.getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        Object rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                                           : method.call(context, self, superClass, methodName, args, block);
        return hasUnusedResult() ? null : rVal;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InstanceSuperInstr(this);
    }
}
