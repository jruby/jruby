package org.jruby.ir.instructions.specialized;

import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class OneArgOperandAttrAssignInstr extends AttrAssignInstr {
    public OneArgOperandAttrAssignInstr(AttrAssignInstr instr) {
        super(instr);
    }

    @Override
    public String toString() {
        return super.toString() + "{1O}";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp, Block block) {
        Operand[] args = getCallArgs();
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);
        IRubyObject value = (IRubyObject) args[0].retrieve(context, self, dynamicScope, temp);

        CallType callType = self == object ? CallType.FUNCTIONAL : CallType.NORMAL;
        Helpers.invoke(context, object, getMethodAddr().getName(), value, callType, Block.NULL_BLOCK);
        return null;
    }
}
