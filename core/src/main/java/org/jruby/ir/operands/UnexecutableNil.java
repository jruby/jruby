package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This operand is used to represent the result of an explicit return instruction.
// So, "return <blah>" will return this operand so ruby expressions like
//    "foo || return false"
// have something in the second slot of the || instruction.  But since control flow
// can never go beyond the return, this value itself can never be interpreted or executed.
// It exists only to fulfil the needs of IR to not have null operands.
public class UnexecutableNil extends Nil {
    public static final UnexecutableNil U_NIL = new UnexecutableNil();

    private UnexecutableNil() { }

    @Override
    public String toString() {
        return "nil(unexecutable)";
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly interpreted");
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UnexecutableNil(this);
    }
}
