package org.jruby.internal.runtime;

import java.util.ArrayList;
import java.util.List;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.interpreter.InterpreterContext;
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
    private MethodData methodData;

    public AbstractIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, method.getName());
        this.method = method;
        this.staticScope = method.getStaticScope();
        this.staticScope.determineModule();
        this.signature = staticScope.getSignature();

        // -1 jit.threshold is way of having interpreter not promote full builds.
        if (Options.JIT_THRESHOLD.load() == -1) callCount = -1;

        // If we are printing, do the build right at creation time so we can see it
        if (Options.IR_PRINT.load()) {
            ensureInstrsReady();
        }
    }

    public IRScope getIRScope() {
        return method;
    }

    public void setCallCount(int callCount) {
        this.callCount = callCount;
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

    public String getClassName(ThreadContext context) {
        return null;
    }

    public String getFile() {
        return method.getFileName();
    }

    public int getLine() {
        return method.getLineNumber();
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
                        ivarNames.add(((GetFieldInstr) i).getRef());
                        break;
                    case PUT_FIELD:
                        ivarNames.add(((PutFieldInstr) i).getRef());
                        break;
                }
            }
            methodData = new MethodData(method.getName(), method.getFileName(), ivarNames);
        }

        return methodData;
    }
}
