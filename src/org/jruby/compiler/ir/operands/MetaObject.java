package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRScopeImpl;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.builtin.IRubyObject;

public class MetaObject extends Operand {
    public final IRScope scope;

    public MetaObject(IRScope s) {
        scope = s;
    }

    @Override
    public String toString() {
        return scope.toString();
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public boolean isClass() {
        return scope instanceof IRClass;
    }

    public boolean isClosure() {
        return scope instanceof IRClosure;
    }

    public Operand getContainer() {
        return scope.getContainer();
    }

    // SSS FIXME: Incomplete!
    @Override
    public IRClass getTargetClass() {
        return (scope instanceof IRModule) ? IRClass.getCoreClass("Module") : null;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        if (isClosure()) {
            BlockBody body = ((IRClosure) scope).getBlockBody();
            Binding binding = interp.getContext().currentBinding((IRubyObject) interp.getSelf());

            return new Block(body, binding);
        }

        // Otherwise it is a module/script/class/sclass
        IRScopeImpl scopeImpl = (IRScopeImpl) scope;

        StaticScope staticScope = scopeImpl.getStaticScope();
        Object module = staticScope.getModule();

        return null;
    }

    @Override
    public Object store(InterpreterContext interp, Object value) {
        return super.store(interp, value);
    }


}
