package org.jruby.ir.operands;

import org.jruby.api.Access;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class ChilledString extends MutableString implements Stringable, StringLiteral {
    /**
     * Used by persistence and by .freeze optimization
     */
    public ChilledString(ByteList bytelist, int coderange, String file, int line) {
        super(bytelist, coderange, file, line);
    }

    public ChilledString(FrozenString frozenString) {
        super(frozenString);
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.CHILLED_STRING;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ChilledString && frozenString.equals(((ChilledString) other).frozenString);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return frozenString.retrieve(context, self, currScope, currDynScope, temp).dupAsChilled(context.runtime, Access.stringClass(context), frozenString.file, frozenString.line);
    }

    @Override
    public String toString() {
        return "chilled:\"" + getByteList() + "\"";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ChilledString(this);
    }

    public static ChilledString decode(IRReaderDecoder d) {
        return new ChilledString((FrozenString)d.decodeOperand());
    }
}