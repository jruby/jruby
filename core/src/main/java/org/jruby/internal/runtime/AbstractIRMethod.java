package org.jruby.internal.runtime;

import java.util.ArrayList;
import java.util.List;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.JIT;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.ivars.MethodData;
import org.jruby.util.cli.Options;

public abstract class AbstractIRMethod extends DynamicMethod implements IRMethodArgs, PositionAware, Cloneable {

    protected final Signature signature;
    protected final IRScope method;
    protected final StaticScope staticScope;
    protected InterpreterContext interpreterContext = null;
    protected int callCount = 0;
    private transient MethodData methodData;

    public AbstractIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, method.getId());
        this.method = method;
        this.staticScope = method.getStaticScope();
        this.staticScope.determineModule();
        this.signature = staticScope.getSignature();

        final Ruby runtime = implementationClass.getRuntime();
        // If we are printing, do the build right at creation time so we can see it
        if (IRRuntimeHelpers.shouldPrintIR(runtime)) {
            ensureInstrsReady();
        }
    }

    public static <T extends AbstractIRMethod & Compilable> void tryJit(ThreadContext context, T self) {
        final Ruby runtime = context.runtime;
        if (runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't JIT during runtime boot

        if (self.callCount < 0) return;
        // we don't synchronize callCount++ it does not matter if count isn't accurate
        if (self.callCount++ >= runtime.getInstanceConfig().getJitThreshold()) {
            synchronized (self) { // disable same jit tasks from entering queue twice
                if (self.callCount >= 0) {
                    self.callCount = Integer.MIN_VALUE; // so that callCount++ stays < 0

                    runtime.getJITCompiler().buildThresholdReached(context, self);
                }
            }
        }
    }

    public final void setCallCount(int callCount) {
        synchronized (this) {
            this.callCount = callCount;
        }
    }

    public IRScope getIRScope() {
        return method;
    }

    public StaticScope getStaticScope() {
        return staticScope;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        ensureInstrsReady(); // Make sure method is minimally built before returning this info
        return ((IRMethod) method).getArgumentDescriptors();
    }

    public abstract InterpreterContext ensureInstrsReady();

    public Signature getSignature() {
        return signature;
    }

    @Override
    public Arity getArity() {
        return signature.arity();
    }

    @Override
    public DynamicMethod dup() {
        return (DynamicMethod) clone();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("not cloneable: " + this);
        }
    }

    @JIT
    public String getClassName(ThreadContext context) {
        String className;
        if (implementationClass.isSingleton()) {
            MetaClass metaClass = (MetaClass)implementationClass;
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
        return method.getFile();
    }

    public int getLine() {
        return method.getLine();
    }

    /**
     * Additional metadata about this method.
     */
    public MethodData getMethodData() {
        if (methodData == null) {
            List<String> ivarNames = new ArrayList<>();
            InterpreterContext context = ensureInstrsReady();
            for (Instr i : context.getInstructions()) {
                switch (i.getOperation()) {
                    case GET_FIELD:
                        ivarNames.add(((GetFieldInstr) i).getId());
                        break;
                    case PUT_FIELD:
                        ivarNames.add(((PutFieldInstr) i).getId());
                        break;
                }
            }
            methodData = new MethodData(method.getId(), method.getFile(), ivarNames);
        }

        return methodData;
    }
}
