package org.jruby.ir.operands;

import org.jruby.RubyRegexp;
import org.jruby.ir.IRVisitor;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a $1 .. $9 node in Ruby code

// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
public class NthRef extends Reference {
    final public int matchNumber;

    public NthRef(int matchNumber) {
        super("$" + matchNumber);
        this.matchNumber = matchNumber;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return RubyRegexp.nth_match(matchNumber, context.getBackRef());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NthRef(this);
    }
}
