package org.jruby.compiler.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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

    /**
     * Find the closest ClassMetaObject that contains this metaobject.  Note that it
     * may be itself a class and return itself.
     * @return
     */
    public Operand getNearestClass() {
        if (isClass()) return this;
        
        Operand parent = getContainer();
        while ((parent instanceof MetaObject) && !(((MetaObject)parent).isClass())) {
            parent = ((MetaObject)parent).getContainer();
        }

        return parent;
    }

    public Operand getContainer() {
        return scope.getContainer();
    }

    // SSS FIXME: Incomplete!
    @Override
    public IRClass getTargetClass() {
        return (scope instanceof IRModule) ? IRClass.getCoreClass("Module") : null;
    }

    public RubyModule interpretBody(InterpreterContext interp, ThreadContext context, RubyModule module) {
        scope.getStaticScope().setModule(module);
        IRMethod rootMethod = ((IRModule) scope).getRootMethod();
        DynamicMethod method = new InterpretedIRMethod(rootMethod, module.getMetaClass());

        method.call(context, module, module.getMetaClass(), "", new IRubyObject[]{});

        return module;
    }

    public RubyModule getContainer(InterpreterContext interp, Ruby runtime) {
        return scope.getContainer() != null ?
            (RubyModule) scope.getContainer().retrieve(interp) : runtime.getObject();
    }
}
