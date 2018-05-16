package org.jruby.internal.runtime.methods;

import java.lang.invoke.MethodHandle;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class CompiledIRMethod extends AbstractIRMethod {
    private final MethodHandle variable;

    private final MethodHandle specific;
    private final int specificArity;

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
        this.hasKwargs = hasKwargs;

        assert method.hasExplicitCallProtocol();

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

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (hasKwargs) args = IRRuntimeHelpers.frobnicateKwargsArgument(context, args, signature);

        try {
            return (IRubyObject) this.variable.invokeExact(context, staticScope, self, args, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, arg2, block, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        if (hasKwargs) args = IRRuntimeHelpers.frobnicateKwargsArgument(context, args, signature);

        try {
            return (IRubyObject) this.variable.invokeExact(context, staticScope, self, args, Block.NULL_BLOCK, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, Block.NULL_BLOCK, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, Block.NULL_BLOCK, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, Block.NULL_BLOCK, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, arg2, Block.NULL_BLOCK, implementationClass, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
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

    public boolean hasKwargs() {
        return hasKwargs;
    }

}
