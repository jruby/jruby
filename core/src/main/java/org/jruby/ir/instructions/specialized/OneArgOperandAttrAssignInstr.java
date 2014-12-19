package org.jruby.ir.instructions.specialized;

import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

public class OneArgOperandAttrAssignInstr extends AttrAssignInstr {
    public OneArgOperandAttrAssignInstr(AttrAssignInstr instr) {
        super(instr);
    }

    @Override
    public String toString() {
        return super.toString() + "{1O}";
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        Operand[] args = getCallArgs();
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject value = (IRubyObject) args[0].retrieve(context, self, currScope, dynamicScope, temp);

        CallType callType = self == object ? CallType.FUNCTIONAL : CallType.NORMAL;
        Helpers.invoke(context, object, getName(), value, callType, Block.NULL_BLOCK);
        return null;
    }
}
