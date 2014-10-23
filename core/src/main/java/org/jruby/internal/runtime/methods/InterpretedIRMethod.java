package org.jruby.internal.runtime.methods;

import java.util.ArrayList;
import java.util.List;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ir.*;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.ir.IRFlags.HAS_EXPLICIT_CALL_PROTOCOL;

public class InterpretedIRMethod extends DynamicMethod implements IRMethodArgs, PositionAware {
    private static final Logger LOG = LoggerFactory.getLogger("InterpretedIRMethod");

    private Arity arity;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    protected final IRScope method;

    // For synthetic methods and for module/class bodies we do not want these added to
    // our backtraces.
    private boolean isSynthetic;

    protected static class DynamicMethodBox {
        public DynamicMethod actualMethod;
        public int callCount = 0;
    }

    protected DynamicMethodBox box = new DynamicMethodBox();

    public InterpretedIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone, method.getName());
        this.method = method;
        this.method.getStaticScope().determineModule();
        this.arity = calculateArity();
        if (!implementationClass.getRuntime().getInstanceConfig().getCompileMode().shouldJIT()) {
            this.box.callCount = -1;
        }
        isSynthetic = method instanceof IRModuleBody;
    }

    public boolean isSynthetic() {
        return isSynthetic;
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

    public List<String[]> getParameterList() {
        return (method instanceof IRMethod) ? ((IRMethod)method).getArgDesc() : new ArrayList<String[]>();
    }

    private Arity calculateArity() {
        StaticScope s = method.getStaticScope();
        if (s.getOptionalArgs() > 0 || s.getRestArg() >= 0) return Arity.required(s.getRequiredArgs());

        return Arity.createArity(s.getRequiredArgs());
    }

    @Override
    public Arity getArity() {
        return this.arity;
    }

    public InterpreterContext ensureInstrsReady() {
        // Try unsync access first before calling more expensive method for getting IC
        InterpreterContext ic = method.getInterpreterContext();

        return ic == null ? method.prepareForInterpretation() : ic;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod actualMethod = box.actualMethod;
        if (actualMethod != null) return actualMethod.call(context, self, clazz, name, args, block);

        InterpreterContext ic = ensureInstrsReady();

        if (IRRuntimeHelpers.isDebug()) doDebug();

        return Interpreter.INTERPRET_METHOD(context, this, self, name, args, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod actualMethod = box.actualMethod;
        if (actualMethod != null) return actualMethod.call(context, self, clazz, name, block);

        InterpreterContext ic = ensureInstrsReady();

        if (IRRuntimeHelpers.isDebug()) doDebug();

        return Interpreter.INTERPRET_METHOD(context, this, self, name, IRubyObject.NULL_ARRAY, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod actualMethod = box.actualMethod;
        if (actualMethod != null) return actualMethod.call(context, self, clazz, name, arg0, block);

        InterpreterContext ic = ensureInstrsReady();

        if (IRRuntimeHelpers.isDebug()) doDebug();

        return Interpreter.INTERPRET_METHOD(context, this, self, name, Helpers.arrayOf(arg0), block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod actualMethod = box.actualMethod;
        if (actualMethod != null) return actualMethod.call(context, self, clazz, name, arg0, arg1, block);

        InterpreterContext ic = ensureInstrsReady();

        if (IRRuntimeHelpers.isDebug()) doDebug();

        return Interpreter.INTERPRET_METHOD(context, this, self, name, Helpers.arrayOf(arg0, arg1), block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod actualMethod = box.actualMethod;
        if (actualMethod != null) return actualMethod.call(context, self, clazz, name, arg0, arg1, arg2, block);

        InterpreterContext ic = ensureInstrsReady();

        if (IRRuntimeHelpers.isDebug()) doDebug();

        return Interpreter.INTERPRET_METHOD(context, this, self, name, Helpers.arrayOf(arg0, arg1, arg2), block);
    }

    protected void doDebug() {
        // FIXME: name should probably not be "" ever.
        String realName = name == null || "".equals(name) ? method.getName() : name;
        LOG.info("Executing '" + realName + "'");
        if (displayedCFG == false) {
            CFG cfg = method.getCFG();
            LOG.info("Graph:\n" + cfg.toStringGraph());
            LOG.info("CFG:\n" + cfg.toStringInstrs());
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

    protected void dupBox(InterpretedIRMethod orig) {
        this.box = orig.box;
    }

    @Override
    public DynamicMethod dup() {
        InterpretedIRMethod x = new InterpretedIRMethod(method, visibility, implementationClass);
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
