package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Symbol extends Reference {
    public static final Symbol KW_REST_ARG_DUMMY = new Symbol("", ASCIIEncoding.INSTANCE);

    private final Encoding encoding;

    public Symbol(String name, Encoding encoding) {
        super(OperandType.SYMBOL, name);

        this.encoding = encoding;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return RubySymbol.newSymbol(context.runtime, getName(), encoding);
    }

    @Override
    public String toString() {
        return ":'" + getName() + "'";
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getName());
        e.encode(getEncoding().toString());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Symbol(this);
    }
}
