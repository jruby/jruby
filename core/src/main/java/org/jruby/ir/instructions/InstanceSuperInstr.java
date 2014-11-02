package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceSuperInstr extends CallInstr {
    public InstanceSuperInstr(Variable result, Operand definingModule, MethAddr superMeth, Operand[] args, Operand closure) {
        super(Operation.INSTANCE_SUPER, CallType.SUPER, result, superMeth, definingModule, args, closure);
    }

    public Operand getDefiningModule() {
        return getReceiver();
    }

    @Override
    public Instr clone(CloneInfo ii) {
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
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject[] args = prepareArguments(context, self, getCallArgs(), currScope, currDynScope, temp);
        Block block = prepareBlock(context, self, currScope, currDynScope, temp);
        RubyModule definingModule = ((RubyModule) getDefiningModule().retrieve(context, self, currScope, currDynScope, temp)).getMethodLocation();
        String methodName = methAddr.getName();
        return IRRuntimeHelpers.instanceSuper(context, self, methodName, definingModule, args, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InstanceSuperInstr(this);
    }
}
