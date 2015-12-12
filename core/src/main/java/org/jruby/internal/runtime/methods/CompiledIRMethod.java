package org.jruby.internal.runtime.methods;

import org.jruby.MetaClass;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

import org.jruby.runtime.Helpers;

public class CompiledIRMethod extends DynamicMethod implements IRMethodArgs, PositionAware, Compilable<DynamicMethod> {
    public MethodHandle variable;

    public MethodHandle specific;
    protected final int specificArity;

    protected final IRScope method;
    private final StaticScope staticScope;
    private final boolean hasExplicitCallProtocol;
    private final boolean hasKwargs;

    public CompiledIRMethod(MethodHandle variable, IRScope method, Visibility visibility,
                            RubyModule implementationClass, boolean hasKwargs) {
        this(variable, null, -1, method, visibility, implementationClass, hasKwargs);
    }

    public CompiledIRMethod(MethodHandle variable, MethodHandle specific, int specificArity, IRScope method,
                            Visibility visibility, RubyModule implementationClass, boolean hasKwargs) {
        super(implementationClass, visibility, method.getName());
        this.variable = variable;
        this.specific = specific;
        // deopt unboxing if we have to process kwargs hash (although this really has nothing to do with arg
        // unboxing -- it was a simple path to hacking this in).
        this.specificArity = hasKwargs ? -1 : specificArity;
        this.method = method;
        this.method.getStaticScope().determineModule();
        this.staticScope = method.getStaticScope();
        this.hasExplicitCallProtocol = method.hasExplicitCallProtocol();
        this.hasKwargs = hasKwargs;

        setHandle(variable);
    }

    public IRScope getIRMethod() {
        return method;
    }

    public StaticScope getStaticScope() {
        return method.getStaticScope();
    }

    public MethodHandle getHandleFor(int arity) {
        if (specificArity != -1 && arity == specificArity) {
            return specific;
        }

        return null;
    }

    @Override
    public Signature getSignature() {
        return staticScope.getSignature();
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ((IRMethod)method).getArgumentDescriptors();
    }

    @Override
    public Arity getArity() {
        return getSignature().arity();
    }

    protected void post(ThreadContext context) {
        // update call stacks (pop: ..)
        context.postMethodFrameAndScope();
    }

    protected void pre(ThreadContext context, StaticScope staticScope, RubyModule implementationClass, IRubyObject self, String name, Block block) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameAndScope(implementationClass, name, self, block, staticScope);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        try {
            if (!hasExplicitCallProtocol) return callNoProtocol(context, self, name, args, block);

            if (hasKwargs) IRRuntimeHelpers.frobnicateKwargsArgument(context, getSignature().required(), args);

            return (IRubyObject)this.variable.invokeExact(context, staticScope, self, args, block, implementationClass, name);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        try {
            if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);

            if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, block);

            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, block, implementationClass, name);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        try {
            if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, arg0, block);

            if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, block);

            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, arg0, block, implementationClass, name);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        try {
            if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, arg0, arg1, block);

            if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, block);

            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, arg0, arg1, block, implementationClass, name);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, arg0, arg1, arg2, block);

            if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, block);

            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, arg0, arg1, arg2, block, implementationClass, name);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        }
    }

    private IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        if (hasKwargs) IRRuntimeHelpers.frobnicateKwargsArgument(context, getSignature().required(), args);

        try {
            return (IRubyObject)this.variable.invokeExact(context, staticScope, self, args, block, implementationClass, name);
        } finally {
            post(context);
        }
    }

    public IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) throws Throwable {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, block, implementationClass, name);
        } finally {
            post(context);
        }
    }

    public IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) throws Throwable {
        if (specificArity != 1) return call(context, self, clazz, name, Helpers.arrayOf(arg0), block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, arg0, block, implementationClass, name);
        } finally {
            post(context);
        }
    }

    public IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        if (specificArity != 2) return call(context, self, clazz, name, Helpers.arrayOf(arg0, arg1), block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, arg0, arg1, block, implementationClass, name);
        } finally {
            post(context);
        }
    }

    public IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        if (specificArity != 3) return call(context, self, clazz, name, Helpers.arrayOf(arg0, arg1, arg2), block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return (IRubyObject)this.specific.invokeExact(context, staticScope, self, arg0, arg1, arg2, block, implementationClass, name);
        } finally {
            post(context);
        }
    }

    @Override
    public DynamicMethod dup() {
        return new CompiledIRMethod(variable, specific, specificArity, method, getVisibility(), implementationClass, hasKwargs);
    }

    @Override
    public void setCallCount(int count) {

    }

    @Override
    public void completeBuild(DynamicMethod buildResult) {

    }

    @Override
    public IRScope getIRScope() {
        return getIRMethod();
    }

    @Override
    public InterpreterContext ensureInstrsReady() {
        return getIRMethod().getFullInterpreterContext();
    }

    @Override
    public String getClassName(ThreadContext context) {
        String className;
        if (implementationClass.isSingleton()) {
            MetaClass metaClass = (MetaClass) implementationClass;
            RubyClass realClass = metaClass.getRealClass();
            // if real class is Class
            if (realClass == context.runtime.getClassClass()) {
                // use the attached class's name
                className = ((RubyClass) metaClass.getAttached()).getName();
            } else {
                // use the real class name
                className = realClass.getName();
            }
        } else {
            // use the class name
            className = implementationClass.getName();
        }
        return className;
    }

    public String getFile() {
        return method.getFileName();
    }

    public int getLine() {
        return method.getLineNumber();
    }

    @Override
    public void setInterpreterContext(InterpreterContext interpreterContext) {
    }
}
