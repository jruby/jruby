package org.jruby.internal.runtime.methods;

import org.jruby.MetaClass;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.*;
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

import java.io.ByteArrayOutputStream;

public class MixedModeIRMethod extends AbstractIRMethod implements Compilable<DynamicMethod> {
    private static final Logger LOG = LoggerFactory.getLogger(MixedModeIRMethod.class);

    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    protected static class DynamicMethodBox {
        public volatile DynamicMethod actualMethod;
        public volatile int callCount = 0;
    }

    protected DynamicMethodBox box = new DynamicMethodBox();

    public MixedModeIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(method, visibility, implementationClass);
        getStaticScope().determineModule();

        // disable JIT if JIT is disabled
        if (!implementationClass.getRuntime().getInstanceConfig().getCompileMode().shouldJIT() ||
                Options.JIT_THRESHOLD.load() < 0) {
            this.box.callCount = -1;
        }
    }

    public DynamicMethod getActualMethod() {
        return box.actualMethod;
    }

    protected void post(InterpreterContext ic, ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (ic.popDynScope()) {
            context.popScope();
        }
    }

    protected void pre(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(getImplementationClass().getMethodLocation(), name, self, block);
        if (ic.pushNewDynScope()) {
            context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope()));
        }
    }

    public void setInterpreterContext(InterpreterContext context) {
        method.setInterpreterContext(context);
    }

    // FIXME: for subclasses we should override this method since it can be simple get
    // FIXME: to avoid cost of synch call in lazilyacquire we can save the ic here
    public InterpreterContext ensureInstrsReady() {
        if (method instanceof IRMethod) {
            return ((IRMethod) method).lazilyAcquireInterpreterContext();
        }

        InterpreterContext ic = method.getInterpreterContext();

        if (Options.IR_PRINT.load()) {
            ByteArrayOutputStream baos = IRDumper.printIR(method, false);

            LOG.info("Printing simple IR for " + method.getName() + ":\n" + new String(baos.toByteArray()));
        }

        return ic;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod jittedMethod = box.actualMethod;

        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, args, block);
        } else {
            return INTERPRET_METHOD(context, ensureInstrsReady(), self, name, args, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic,
                                               IRubyObject self, String name, IRubyObject[] args, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, this, null, self, ic, name, args, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block);
                    return ic.getEngine().interpret(context, this, null, self, ic, name, args, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod jittedMethod = box.actualMethod;

        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, block);
        } else {
            return INTERPRET_METHOD(context, ensureInstrsReady(), self, name, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic,
                                               IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, this, null, self, ic, name, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block);
                    return ic.getEngine().interpret(context, this, null, self, ic, name, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod jittedMethod = box.actualMethod;

        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, arg0, block);
        } else {
            return INTERPRET_METHOD(context, ensureInstrsReady(), self, name, arg0, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic,
                                               IRubyObject self, String name, IRubyObject arg1, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, this, null, self, ic, name, arg1, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block);
                    return ic.getEngine().interpret(context, this, null, self, ic, name, arg1, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod jittedMethod = box.actualMethod;

        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, arg0, arg1, block);
        } else {
            return INTERPRET_METHOD(context, ensureInstrsReady(), self, name, arg0, arg1, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic,
                                               IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2,  Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, this, null, self, ic, name, arg1, arg2, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block);
                    return ic.getEngine().interpret(context, this, null, self, ic, name, arg1, arg2, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod jittedMethod = box.actualMethod;

        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, arg0, arg1, arg2, block);
        } else {
            return INTERPRET_METHOD(context, ensureInstrsReady(), self, name, arg0, arg1, arg2, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic,
                                               IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, this, null, self, ic, name, arg1, arg2, arg3, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block);
                    return ic.getEngine().interpret(context, this, null, self, ic, name, arg1, arg2, arg3, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }

    }

    protected void doDebug() {
        // FIXME: This is printing out IRScope CFG but JIT may be active and it might not reflect
        // currently executing.  Move into JIT and into interp since they will be getting CFG from
        // different sources
        // FIXME: This is only printing out CFG once.  If we keep applying more passes then we
        // will want to print out after those new passes.
        ensureInstrsReady();
        LOG.info("Executing '" + method.getName() + "'");
        if (!displayedCFG) {
            LOG.info(method.debugOutput());
            displayedCFG = true;
        }
    }

    @Override
    public void completeBuild(DynamicMethod newMethod) {
        this.box.actualMethod = newMethod;
        this.box.actualMethod.serialNumber = this.serialNumber;
        this.box.callCount = -1;
        getImplementationClass().invalidateCacheDescendants();
    }

    protected void tryJit(ThreadContext context, DynamicMethodBox box) {
        if (context.runtime.isBooting() && !Options.JIT_KERNEL.load()) return;  // don't JIT during runtime boot

        synchronized (this) {
            if (box.callCount >= 0) {
                if (box.callCount++ >= Options.JIT_THRESHOLD.load()) {
                    box.callCount = -1;
                    context.runtime.getJITCompiler().buildThresholdReached(context, this);
                }
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

    @Override
    public DynamicMethod dup() {
        MixedModeIRMethod x = new MixedModeIRMethod(method, getVisibility(), implementationClass);
        x.box = box;

        return x;
    }

    public void setCallCount(int callCount) {
        box.callCount = callCount;
    }
}
