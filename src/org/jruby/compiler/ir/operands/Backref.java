package org.jruby.compiler.ir.operands;

// Represents a backref node in Ruby code

import org.jruby.RubyRegexp;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class Backref extends Operand {
    final public char type; 

    public Backref(char t) {
        type = t;
    }

    @Override
    public String toString() {
        return "$" + type;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, Object[] temp) {
        IRubyObject backref = context.getCurrentScope().getBackRef(context.getRuntime());
        
        switch (type) {
        case '&':
            return RubyRegexp.last_match(backref);
        case '`':
            return RubyRegexp.match_pre(backref);
        case '\'':
            return RubyRegexp.match_post(backref);
        case '+':
            return RubyRegexp.match_last(backref);
        default:
            assert false: "backref with invalid type";
            return null;
        } 
    }
}
