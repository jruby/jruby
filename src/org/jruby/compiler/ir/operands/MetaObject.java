package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRScopeImpl;
import org.jruby.compiler.ir.IRScript;
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
        if (scope instanceof IRClass) {
            return "Class " + ((IRClass) scope).getName();
        } else if (scope instanceof IRModule) {
            return "Module " + ((IRModule) scope).getName();
        } else if (scope instanceof IRMethod) {
            return "Method " + ((IRMethod) scope).getName();
        } else if (scope instanceof IRScript) {
            return "Script " + ((IRScript) scope).fileName;
        } else {
            return ((IRClosure) scope).toString().replace("\t", "\t\t");
        }
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

        Object module = scopeImpl.getStaticScope().getModule();
        if (module == null) return module;

        return null;
    }

    @Override
    public Object store(InterpreterContext interp, Object value) {
        return super.store(interp, value);
    }


}
