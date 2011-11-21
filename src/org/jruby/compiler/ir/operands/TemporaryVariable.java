package org.jruby.compiler.ir.operands;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A set of variables which are only used in a particular scope and never
 * visible to Ruby itself.
 */
public class TemporaryVariable extends Variable {
    final int offset;
	 String name;

    public TemporaryVariable(int offset) {
        this.offset = offset;
		  this.name = getPrefix() + offset;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TemporaryVariable)) return false;

        return getName().equals(((TemporaryVariable) obj).getName());
    }

    public int compareTo(Object other) {
        if (!(other instanceof TemporaryVariable)) return 0;
        
        TemporaryVariable temporary = (TemporaryVariable) other;
        int prefixCompare = getPrefix().compareTo(temporary.getPrefix());
        if (prefixCompare != 0) return prefixCompare;

        if (offset < temporary.offset) {
            return -1;
        } else if (offset > temporary.offset) {
            return 1;
        }

        return 0;
    }

    public String getPrefix() {
        return "%v_";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, Object[] temp) {
        return temp[offset];
    }

    @Override
    public Object store(ThreadContext context, IRubyObject self, Object[] temp, Object value) {
        Object old = temp[offset];
        temp[offset] = value;
        return old;
    }
}
