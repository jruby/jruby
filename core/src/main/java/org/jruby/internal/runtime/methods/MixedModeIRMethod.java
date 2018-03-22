package org.jruby.internal.runtime.methods;

import java.io.ByteArrayOutputStream;
import org.jruby.MetaClass;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class MixedModeIRMethod extends AbstractIRMethod implements Compilable<DynamicMethod> {
    private volatile int callCount = 0;
    private final InterpretedIRMethod baseMethod;
    private volatile DynamicMethod jittedMethod;

    public MixedModeIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(method, visibility, implementationClass);

        this.baseMethod = new InterpretedIRMethod(method, visibility, implementationClass);

        getStaticScope().determineModule();

        // disable JIT if threshold is below zero
        if (Options.JIT_THRESHOLD.load() < 0) {
            callCount = -1;
        }
    }

    protected MixedModeIRMethod(MixedModeIRMethod copy) {
        super(copy.getIRScope(), copy.getVisibility(), copy.getImplementationClass());

        this.serialNumber = copy.serialNumber;
        this.callCount = copy.callCount;
        this.baseMethod = copy.baseMethod;
        this.jittedMethod = copy.jittedMethod;
    }

    public DynamicMethod getActualMethod() {
        return jittedMethod != null ? jittedMethod : baseMethod;
    }

    // FIXME: for subclasses we should override this method since it can be simple get
    // FIXME: to avoid cost of synch call in lazilyacquire we can save the ic here
    public InterpreterContext ensureInstrsReady() {
        return baseMethod.ensureInstrsReady();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (callCount >= 0) tryJit(context);

        return getActualMethod().call(context, self, clazz, name, args, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (callCount >= 0) tryJit(context);

        return getActualMethod().call(context, self, clazz, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (callCount >= 0) tryJit(context);

        return getActualMethod().call(context, self, clazz, name, arg0, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (callCount >= 0) tryJit(context);

        return getActualMethod().call(context, self, clazz, name, arg0, arg1, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (callCount >= 0) tryJit(context);

        return getActualMethod().call(context, self, clazz, name, arg0, arg1, arg2, block);
    }

    @Override
    public void completeBuild(DynamicMethod newMethod) {
        setCallCount(-1);
        newMethod.serialNumber = this.serialNumber;
        jittedMethod = newMethod;
        getImplementationClass().invalidateCacheDescendants();
    }

    private void tryJit(ThreadContext context) {
        if (context.runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't JIT during runtime boot

        synchronized (this) {
            if (callCount >= 0 && callCount++ >= Options.JIT_THRESHOLD.load()) {
                context.runtime.getJITCompiler().buildThresholdReached(context, this);
            }
        }
    }

    public String getClassName(ThreadContext context) {
        String className;
        if (implementationClass.isSingleton()) {
            MetaClass metaClass = (MetaClass)implementationClass;
            RubyClass realClass = metaClass.getRealClass();
            // if real class is Class
            if (realClass == context.runtime.getClassClass()) {
                // use the attached class's name
                className = ((RubyClass)metaClass.getAttached()).getName();
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

    public void setCallCount(int callCount) {
        synchronized (this) {
            this.callCount = callCount;
        }
    }

    @Override
    public DynamicMethod dup() {
        return new MixedModeIRMethod(this);
    }

    @Override
    public void setImplementationClass(RubyModule implClass) {
        super.setImplementationClass(implClass);
        baseMethod.setImplementationClass(implClass);
        if (jittedMethod != null) jittedMethod.setImplementationClass(implClass);
    }

    @Override
    public void setDefinedClass(RubyModule definedClass) {
        super.setDefinedClass(definedClass);
        baseMethod.setDefinedClass(definedClass);
        if (jittedMethod != null) jittedMethod.setDefinedClass(definedClass);
    }
}
