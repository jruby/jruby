package org.jruby.ir.operands;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.globalVariables;

public class GlobalVariable extends Reference {
    public GlobalVariable(RubySymbol name) {
        super(name);
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.GLOBAL_VARIABLE;
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof GlobalVariable)) return 0;

        return getName().compareTo(((GlobalVariable) arg0).getName());
    }

    @Interp
    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return globalVariables(context).get(getId());
    }

    public static GlobalVariable decode(IRReaderDecoder d) {
        return new GlobalVariable(d.decodeSymbol());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GlobalVariable(this);
    }
}
