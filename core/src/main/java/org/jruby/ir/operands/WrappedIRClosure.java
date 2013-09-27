package org.jruby.ir.operands;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

public class WrappedIRClosure extends Operand {
    private final IRClosure closure;

    public WrappedIRClosure(IRClosure scope) {
        this.closure = scope;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Nothing to o */
    }

    public IRClosure getClosure() {
        return closure;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public String toString() {
        return closure.toString();
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return new WrappedIRClosure(closure.cloneForClonedInstr(ii));
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        BlockBody body = closure.getBlockBody();
        closure.getStaticScope().determineModule();
        Binding binding = context.currentBinding(self, currDynScope);

        return new Block(body, binding);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.WrappedIRClosure(this);
    }
}
