package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

public class CompiledIRMethod extends AbstractIRMethod {
    protected final MethodHandle variable;

    protected final MethodHandle specific;
    protected final int specificArity;

    private final boolean hasExplicitCallProtocol;
    private final boolean hasKwargs;

    public CompiledIRMethod(MethodHandle variable, IRScope method, Visibility visibility,
                            RubyModule implementationClass, boolean hasKwargs) {
        this(variable, null, -1, method, visibility, implementationClass, hasKwargs);
    }

    public CompiledIRMethod(MethodHandle variable, MethodHandle specific, int specificArity, IRScope method,
                            Visibility visibility, RubyModule implementationClass, boolean hasKwargs) {
        super(method, visibility, implementationClass);
        this.variable = variable;
        this.specific = specific;
        // deopt unboxing if we have to process kwargs hash (although this really has nothing to do with arg
        // unboxing -- it was a simple path to hacking this in).
        this.specificArity = hasKwargs ? -1 : specificArity;
        this.method.getStaticScope().determineModule();
        this.hasExplicitCallProtocol = method.hasExplicitCallProtocol();
        this.hasKwargs = hasKwargs;

        setHandle(variable);
    }

    public MethodHandle getHandleFor(int arity) {
        if (specificArity != -1 && arity == specificArity) {
            return specific;
        }

        return null;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ((IRMethod)method).getArgumentDescriptors();
    }

    @Override
    public InterpreterContext ensureInstrsReady() {
        // FIXME: duplicated from MixedModeIRMethod
        if (method instanceof IRMethod) {
            return ((IRMethod) method).lazilyAcquireInterpreterContext();
        }

        InterpreterContext ic = method.getInterpreterContext();

        return ic;
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
        if (!hasExplicitCallProtocol) return callNoProtocol(context, self, name, args, block);

        if (hasKwargs) IRRuntimeHelpers.frobnicateKwargsArgument(context, args, getSignature().required());

        return invokeExact(this.variable, context, staticScope, self, args, block, implementationClass, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);

        if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, block);

        return invokeExact(this.specific, context, staticScope, self, block, implementationClass, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, arg0, block);

        if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, block);

        return invokeExact(this.specific, context, staticScope, self, arg0, block, implementationClass, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, arg0, arg1, block);

        if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, block);

        return invokeExact(this.specific, context, staticScope, self, arg0, arg1, block, implementationClass, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (!hasExplicitCallProtocol) return callNoProtocol(context, self, clazz, name, arg0, arg1, arg2, block);

        if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, block);

        return invokeExact(this.specific, context, staticScope, self, arg0, arg1, arg2, block, implementationClass, name);
    }

    private IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        if (hasKwargs) IRRuntimeHelpers.frobnicateKwargsArgument(context, args, getSignature().required());

        try {
            return invokeExact(this.variable, context, staticScope, self, args, block, implementationClass, name);
        }
        finally { post(context); }
    }

    public final IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return invokeExact(this.specific, context, staticScope, self, block, implementationClass, name);
        }
        finally { post(context); }
    }

    public final IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (specificArity != 1) return call(context, self, clazz, name, Helpers.arrayOf(arg0), block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return invokeExact(this.specific, context, staticScope, self, arg0, block, implementationClass, name);
        }
        finally { post(context); }
    }

    public final IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (specificArity != 2) return call(context, self, clazz, name, Helpers.arrayOf(arg0, arg1), block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return invokeExact(this.specific, context, staticScope, self, arg0, arg1, block, implementationClass, name);
        }
        finally { post(context); }
    }

    public final IRubyObject callNoProtocol(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (specificArity != 3) return call(context, self, clazz, name, Helpers.arrayOf(arg0, arg1, arg2), block);

        StaticScope staticScope = this.staticScope;
        RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return invokeExact(this.specific, context, staticScope, self, arg0, arg1, arg2, block, implementationClass, name);
        }
        finally { post(context); }
    }

    @Override
    public DynamicMethod dup() {
        return new CompiledIRMethod(variable, specific, specificArity, method, getVisibility(), implementationClass, hasKwargs);
    }

    public String getFile() {
        return method.getFileName();
    }

    public int getLine() {
        return method.getLineNumber();
    }

    @Override
    public String toString() {
        return getClass().getName() + '@' + Integer.toHexString(hashCode()) + ' ' + method + ' ' + getSignature();
    }

    private static IRubyObject invokeExact(MethodHandle method,
            ThreadContext context, StaticScope staticScope, IRubyObject self,
            IRubyObject[] args, Block block,
            RubyModule implementationClass, String name) {
        try {
            return (IRubyObject) method.invokeExact(context, staticScope, self, args, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    private static IRubyObject invokeExact(MethodHandle method,
            ThreadContext context, StaticScope staticScope, IRubyObject self,
            Block block,
            RubyModule implementationClass, String name) {
        try {
            return (IRubyObject) method.invokeExact(context, staticScope, self, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    private static IRubyObject invokeExact(MethodHandle method,
            ThreadContext context, StaticScope staticScope, IRubyObject self,
            IRubyObject arg0, Block block,
            RubyModule implementationClass, String name) {
        try {
            return (IRubyObject) method.invokeExact(context, staticScope, self, arg0, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    private static IRubyObject invokeExact(MethodHandle method,
            ThreadContext context, StaticScope staticScope, IRubyObject self,
            IRubyObject arg0, IRubyObject arg1, Block block,
            RubyModule implementationClass, String name) {
        try {
            return (IRubyObject) method.invokeExact(context, staticScope, self, arg0, arg1, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    private static IRubyObject invokeExact(MethodHandle method,
            ThreadContext context, StaticScope staticScope, IRubyObject self,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block,
            RubyModule implementationClass, String name) {
        try {
            return (IRubyObject) method.invokeExact(context, staticScope, self, arg0, arg1, arg2, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

}
