/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author enebo
 *
 * SSS FIXME: Do we create a SelfVariable that extends LocalVariable?
 * That we can trap writes to %self, special-case lookup for %self, and the isSelf method
 */
public class LocalVariable extends Variable {
    final public String name;
    private int location;

    // FIXME: We should resolve to an index into an array but localvariable has no allocator
    public LocalVariable(String name, int location) {
        this.name = name;
        this.location = location;
    }

/**
 * SSS FIXME: Unused code
    public void setLocation(int slot) {
        this.location = slot;
    }
**/

    public int getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean isSelf() {
        return name.equals("%self");  // SSS FIXME: This is potentially bug-prone.
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LocalVariable)) return false;

        return name.equals(((LocalVariable) obj).name);
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof LocalVariable)) return 0;

        return name.compareTo(((LocalVariable) arg0).name);
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // SSS FIXME: Should we have a special case for self?
        //return interp.getLocalVariable(getName());
        return isSelf() ? self : interp.getLocalVariable(context, location);
    }

    @Override
    public Object store(InterpreterContext interp, ThreadContext context, IRubyObject self, Object value) {
        return isSelf() ? self : interp.setLocalVariable(location, value);
    }
}
