package org.jruby.compiler.ir.operands;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;

public class MetaObject extends Operand {
    public final IRScope scope;

    protected MetaObject(IRScope scope) {
        this.scope = scope;
    }

    public static MetaObject create(IRScope scope) {
        // Walk up lexical scopes to find the nearest lexical scope that contains the method
        if (scope instanceof IRMethod) scope = scope.getNearestModule();

        if (scope instanceof IRClass) return new ClassMetaObject((IRClass) scope);
        if (scope instanceof IRModule) return new ModuleMetaObject((IRModule) scope);
        if (scope instanceof IRClosure) return new ClosureMetaObject((IRClosure) scope);

        assert false : "IRSCript created";
        return new MetaObject(scope);
    }

    @Override
    public String toString() {
        return scope == null ? "<NULL SCOPE>" : scope.toString();
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public boolean isClass() {
        return false;
    }

    public boolean isModule() {
        return false;
    }

    public boolean isClosure() {
        return false;
    }

    public IRScope getScope() {
        return scope;
    }

    // SSS FIXME: Incomplete!
    @Override
    public IRClass getTargetClass() {
        return (scope instanceof IRModule) ? IRClass.getCoreClass("Module") : null;
    }
}
