package org.jruby.internal.runtime.methods;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ir.*;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class MixedModeIRMethod extends DynamicMethod implements IRMethodArgs, PositionAware {
    private static final Logger LOG = LoggerFactory.getLogger("InterpretedIRMethod");

    private Arity arity;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    protected final IRScope method;

    protected static class DynamicMethodBox {
        public DynamicMethod actualMethod;
        public int callCount = 0;
    }

    protected DynamicMethodBox box = new DynamicMethodBox();

    public MixedModeIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone, method.getName());
        this.method = method;
        this.method.getStaticScope().determineModule();
        this.arity = calculateArity();

        // disable JIT if JIT is disabled
        // FIXME: kinda hacky, but I use IRMethod data in JITCompiler.
        if (!implementationClass.getRuntime().getInstanceConfig().getCompileMode().shouldJIT()) {
            this.box.callCount = -1;
        }
    }

    public IRScope getIRMethod() {
        return method;
    }

    public DynamicMethod getActualMethod() {
        return box.actualMethod;
    }

    public void setCallCount(int callCount) {
        box.callCount = callCount;
    }

    public StaticScope getStaticScope() {
        return method.getStaticScope();
    }

    public String[] getParameterList() {
        ensureInstrsReady(); // Make sure method is minimally built before returning this info
        return ((IRMethod) method).getArgDesc();
    }

    private Arity calculateArity() {
        StaticScope s = method.getStaticScope();
        if (s.getOptionalArgs() > 0 || s.hasRestArg()) return Arity.required(s.getRequiredArgs());

        return Arity.createArity(s.getRequiredArgs());
    }

    @Override
    public Arity getArity() {
        return this.arity;
    }

    protected void post(InterpreterContext ic, ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (ic.popDynScope()) {
            context.popScope();
        }
    }

    protected void pre(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block, RubyModule implClass) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(implClass, name, self, block);
        if (ic.pushNewDynScope()) {
            context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope()));
        }
        context.setCurrentVisibility(getVisibility());
    }

    // FIXME: for subclasses we should override this method since it can be simple get
    // FIXME: to avoid cost of synch call in lazilyacquire we can save the ic here
    public InterpreterContext ensureInstrsReady() {
        if (method instanceof IRMethod) {
            return ((IRMethod) method).lazilyAcquireInterpreterContext();
        }
        return method.getInterpreterContext();
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
            return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, args, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject[] args, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, args, block, null);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, args, block, null);
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
            return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                               IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, block, null);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, block, null);
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
            return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, arg0, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject arg1, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, arg1, block, null);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, arg1, block, null);
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
            return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, arg0, arg1, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2,  Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, block, null);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, block, null);
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
            return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, arg0, arg1, arg2, block);
        }
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, arg3, block, null);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, arg3, block, null);
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

    public DynamicMethod getMethodForCaching() {
        DynamicMethod method = box.actualMethod;
        if (method instanceof CompiledIRMethod) {
            return method;
        }
        return this;
    }

    public void switchToJitted(CompiledIRMethod newMethod) {
        this.box.actualMethod = newMethod;
        this.box.actualMethod.serialNumber = this.serialNumber;
        this.box.callCount = -1;
        getImplementationClass().invalidateCacheDescendants();
    }


    protected void tryJit(ThreadContext context, DynamicMethodBox box) {
        Ruby runtime = context.runtime;

        // don't JIT during runtime boot
        if (runtime.isBooting()) return;

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


        if (box.callCount++ >= Options.JIT_THRESHOLD.load()) {
            context.runtime.getJITCompiler().jitThresholdReached(this, context.runtime.getInstanceConfig(), context, className, name);
        }
    }

    public void setActualMethod(CompiledIRMethod method) {
        this.box.actualMethod = method;
    }

    protected void dupBox(MixedModeIRMethod orig) {
        this.box = orig.box;
    }

    @Override
    public DynamicMethod dup() {
        MixedModeIRMethod x = new MixedModeIRMethod(method, visibility, implementationClass);
        x.box = box;

        return x;
    }

    public String getFile() {
        return method.getFileName();
    }

    public int getLine() {
        return method.getLineNumber();
   }
}
