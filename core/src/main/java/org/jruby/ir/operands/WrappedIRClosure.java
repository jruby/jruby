package org.jruby.ir.operands;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

public class WrappedIRClosure extends Operand {
    private final Variable self;
    private final IRClosure closure;

    public WrappedIRClosure(Variable self, IRClosure closure) {
        super(OperandType.WRAPPED_IR_CLOSURE);

        this.self = self;
        this.closure = closure;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        l.add(self);
    }

    public Variable getSelf() {
        return self;
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
        return self + ":" + closure.toString();
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newSelf = self.getSimplifiedOperand(valueMap, force);
        return newSelf == self ? this : new WrappedIRClosure((Variable)newSelf, closure);
    }

    @Override
    public Operand cloneForInlining(CloneInfo info) {
        // Making interp instrs so that if JIT hits IRClosure we will not concurrently modify the same IRScope.
        if (info instanceof SimpleCloneInfo && !((SimpleCloneInfo) info).isEnsureBlockCloneMode()) {
            // FIXME: It really bothers me we do not clone closure here but cloning like main clone case loses interpContext + other things.
            return new WrappedIRClosure(info.getRenamedVariable(self), closure);
        }

        return new WrappedIRClosure(info.getRenamedVariable(self), closure.cloneForInlining(info));
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        BlockBody body = closure.getBlockBody();
        closure.getStaticScope().determineModule();

        // In non-inlining scenarios, this.self will always be %self.
        // However, in inlined scenarios, this.self will be the self in the original scope where the closure
        // was present before inlining.
        IRubyObject selfVal = (this.self instanceof Self) ? self : (IRubyObject)this.self.retrieve(context, self, currScope, currDynScope, temp);
        Binding binding = context.currentBinding(selfVal, currDynScope);

        return new Block(body, binding);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.WrappedIRClosure(this);
    }
}
