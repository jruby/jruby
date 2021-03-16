package org.jruby.internal.runtime.methods;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class CompiledIRMethod extends AbstractIRMethod implements Compilable<DynamicMethod>  {

    public static final MethodType CALL_SPECIFIC_SITE = MethodType.methodType(CallSpecific.class);
    public static final MethodType CALL_VARIABLE_SITE = MethodType.methodType(CallVariable.class);

    public interface CallSpecific {
        IRubyObject call(ThreadContext context, StaticScope scope, IRubyObject self, Block block, RubyModule clazz, String name);
        IRubyObject call(ThreadContext context, StaticScope scope, IRubyObject self, IRubyObject arg0, Block block, RubyModule clazz, String name);
        IRubyObject call(ThreadContext context, StaticScope scope, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block, RubyModule clazz, String name);
        IRubyObject call(ThreadContext context, StaticScope scope, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block, RubyModule clazz, String name);
    }

    public interface CallVariable {
        IRubyObject call(ThreadContext context, StaticScope scope, IRubyObject self, IRubyObject[] args, Block block, RubyModule clazz, String name);
    }

    private final MethodHandle specific;
    private final CallSpecific callSpecific;
    private final CallVariable callVariable;

    private final int specificArity;
    private final String encodedArgumentDescriptors;
    private final boolean needsToFindImplementer;

    // Used by spec:compiler
    public CompiledIRMethod(MethodHandle variable, IRScope method, Visibility visibility, RubyModule implementationClass,
                            String encodedArgumentDescriptors) {
        this(variable, null, -1, method.getId(), method.getLine(), method.getStaticScope(),
                visibility, implementationClass, encodedArgumentDescriptors, method.receivesKeywordArgs(),
                !(method instanceof IRMethod && !method.getFullInterpreterContext().getFlags().contains(IRFlags.REQUIRES_CLASS)));
    }

    // Used by spec:compiler
    public CompiledIRMethod(MethodHandle variable, MethodHandle specific, int specificArity, IRScope method,
                            Visibility visibility, RubyModule implementationClass, String encodedArgumentDescriptors) {
        this(variable, specific, specificArity, method.getId(), method.getLine(), method.getStaticScope(),
                visibility, implementationClass, encodedArgumentDescriptors, method.receivesKeywordArgs(),
                !(method instanceof IRMethod && !method.getFullInterpreterContext().getFlags().contains(IRFlags.REQUIRES_CLASS)));
    }

    // Ruby Class/Module constructor (feels like we should maybe have a subtype here...
    public CompiledIRMethod(MethodHandles.Lookup lookup, MethodHandle variable, String id, int line, StaticScope scope,
                            Visibility visibility, RubyModule implementationClass) {
        this(lookup, variable, null, -1, id, line, scope, visibility, implementationClass, "", false, false);
    }

    public CompiledIRMethod(MethodHandle variable, MethodHandle specific, int specificArity, String id, int line,
                            StaticScope scope, Visibility visibility, RubyModule implementationClass,
                            String encodedArgumentDescriptors, boolean receivesKeywordArgs, boolean needsToFindImplementer) {
        super(scope, id, line, visibility, implementationClass);

        // cannot bind CallSpecific without lookup from JIT class
        this.specific = null;
        this.specificArity = -1;
        this.callSpecific = null;

        staticScope.determineModule();

        this.encodedArgumentDescriptors = encodedArgumentDescriptors;
        //assert method.hasExplicitCallProtocol();

        setHandle(variable);
        callVariable = null;

        this.needsToFindImplementer = needsToFindImplementer;

        // FIXME: inliner breaks with this line commented out
        // method.compilable = this;
    }

    public CompiledIRMethod(MethodHandles.Lookup lookup, MethodHandle variable, MethodHandle specific, int specificArity, String id, int line,
                            StaticScope scope, Visibility visibility, RubyModule implementationClass,
                            String encodedArgumentDescriptors, boolean receivesKeywordArgs, boolean needsToFindImplementer) {
        super(scope, id, line, visibility, implementationClass);

        this.specific = specific;
        // deopt unboxing if we have to process kwargs hash (although this really has nothing to do with arg
        // unboxing -- it was a simple path to hacking this in).
        this.specificArity = receivesKeywordArgs ? -1 : specificArity;

        CallSpecific callSpecific = null;
        if (this.specificArity != -1) {
            try {
                MethodType type = specific.type();
                callSpecific = (CallSpecific) LambdaMetafactory.metafactory(lookup, "call", CALL_SPECIFIC_SITE, type, specific, type).getTarget().invokeExact();
            } catch (Throwable lce) {
                lce.printStackTrace();
            }
        }
        this.callSpecific = callSpecific;

        staticScope.determineModule();

        this.encodedArgumentDescriptors = encodedArgumentDescriptors;
        //assert method.hasExplicitCallProtocol();

        setHandle(variable);
        CallVariable callVariable = null;
        if (this.specificArity != -1) {
            try {
                MethodType type = JVMVisitor.METHOD_SIGNATURE_VARARGS.type();
                callVariable = (CallVariable) LambdaMetafactory.metafactory(lookup, "call", CALL_VARIABLE_SITE, type, variable, type).getTarget().invokeExact();
            } catch (Throwable lce) {
                lce.printStackTrace();
            }
        }
        this.callVariable = callVariable;


        this.needsToFindImplementer = needsToFindImplementer;

        // FIXME: inliner breaks with this line commented out
        // method.compilable = this;
    }

    public MethodHandle getHandleFor(int arity) {
        if (specificArity != -1 && arity == specificArity) {
            return specific;
        }

        return null;
    }

    public void setVariable(MethodHandle variable) {
        super.setHandle(variable);
    }

    public void setSpecific(MethodHandle specific) {
//        this.specific = specific;
    }


    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ArgumentDescriptor.decode(implementationClass.getRuntime(), encodedArgumentDescriptors);
    }

    @Override
    public void completeBuild(DynamicMethod buildResult) {
        // unused but part of compilable interface.  jit task uses setVariable and setSpecific to update code.
    }

    @Override
    protected void printMethodIR() {
        // no-op
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        CallVariable callVariable = this.callVariable;
        if (callVariable != null) {
            return callVariable.call(context, staticScope, self, args, block, clazz, name);
        }

        try {
            return (IRubyObject) ((MethodHandle) this.handle).invokeExact(context, staticScope, self, args, block, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);

        return this.callSpecific.call(context, staticScope, self, block, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, block);

        return this.callSpecific.call(context, staticScope, self, arg0, block, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, block);

        return this.callSpecific.call(context, staticScope, self, arg0, arg1, block, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, block);

        return this.callSpecific.call(context, staticScope, self, arg0, arg1, arg2, block, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        CallVariable callVariable = this.callVariable;
        if (callVariable != null) {
            return callVariable.call(context, staticScope, self, args, Block.NULL_BLOCK, clazz, name);
        }

        try {
            return (IRubyObject) ((MethodHandle) this.handle).invokeExact(context, staticScope, self, args, Block.NULL_BLOCK, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);

        return this.callSpecific.call(context, staticScope, self, Block.NULL_BLOCK, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, Block.NULL_BLOCK);

        return this.callSpecific.call(context, staticScope, self, arg0, Block.NULL_BLOCK, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, Block.NULL_BLOCK);

        return this.callSpecific.call(context, staticScope, self, arg0, arg1, Block.NULL_BLOCK, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, Block.NULL_BLOCK);

        return this.callSpecific.call(context, staticScope, self, arg0, arg1, arg2, Block.NULL_BLOCK, clazz, name);
    }

    public boolean needsToFindImplementer() {
        return needsToFindImplementer;
    }
}
