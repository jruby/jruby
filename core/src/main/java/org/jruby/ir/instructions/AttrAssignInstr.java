package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneArgOperandAttrAssignInstr;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Instruction representing Ruby code of the form: "a[i] = 5"
// which is equivalent to: a.[](i,5)
public class AttrAssignInstr extends NoResultCallInstr {
    public AttrAssignInstr(Operand obj, MethAddr attr, Operand[] args) {
        super(Operation.ATTR_ASSIGN, CallType.UNKNOWN, attr, obj, args, null);
    }

    public AttrAssignInstr(AttrAssignInstr instr) {
        this(instr.getReceiver(), instr.getMethodAddr(), instr.getCallArgs());
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new AttrAssignInstr(receiver.cloneForInlining(ii),
                (MethAddr)getMethodAddr().cloneForInlining(ii), cloneCallArgs(ii));
    }

    @Override
    public CallBase specializeForInterpretation() {
        Operand[] callArgs = getCallArgs();
        if (containsSplat(callArgs)) return this;

        switch (callArgs.length) {
            case 1:
                return new OneArgOperandAttrAssignInstr(this);
        }
        return this;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);
        IRubyObject[] values = prepareArguments(context, self, getCallArgs(), dynamicScope, temp);

        CallType callType = self == object ? CallType.FUNCTIONAL : CallType.NORMAL;
        Helpers.invoke(context, object, getMethodAddr().getName(), values, callType, Block.NULL_BLOCK);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AttrAssignInstr(this);
    }
}
