package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

import org.jruby.runtime.Helpers;

public class CompiledIRMethod extends JavaMethod implements MethodArgs2, PositionAware {
    private static final Logger LOG = LoggerFactory.getLogger("CompiledIRMethod");

    protected final MethodHandle handle;

    protected final IRScope method;
    private final Arity arity;
    private String[] parameterList;

    public CompiledIRMethod(MethodHandle handle, IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone, method.getName());
        this.handle = handle;
        this.method = method;
        this.method.getStaticScope().determineModule();
        this.arity = calculateArity();

        setHandle(handle);
    }

    public IRScope getIRMethod() {
        return method;
    }

    public StaticScope getStaticScope() {
        return method.getStaticScope();
    }

    public String[] getParameterList() {
        if (parameterList != null) return parameterList;

        return parameterList = Helpers.irMethodArgsToParameters(((IRMethod)method).getArgDesc());
    }

    private Arity calculateArity() {
        StaticScope s = getStaticScope();
        if (s.getOptionalArgs() > 0 || s.getRestArg() >= 0) return Arity.required(s.getRequiredArgs());

        return Arity.createArity(s.getRequiredArgs());
    }

    @Override
    public Arity getArity() {
        return this.arity;
    }

    protected void post(ThreadContext context) {
        if (!method.hasExplicitCallProtocol()) {
            // update call stacks (pop: ..)
            context.popFrame();
            context.postMethodScopeOnly();
        }
    }

    protected void pre(ThreadContext context, IRubyObject self, String name, Block block) {
        if (!method.hasExplicitCallProtocol()) {
            // update call stacks (push: frame, class, scope, etc.)
            RubyModule implementationClass = getImplementationClass();
            context.preMethodFrameAndScope(implementationClass, name, self, block, method.getStaticScope());
            // FIXME: does not seem right to use this method's visibility as current!!!
            // See also PushFrame instruction in org.jruby.ir.targets.JVMVisitor
            context.setCurrentVisibility(Visibility.PUBLIC);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        pre(context, self, name, block);

        try {
            return (IRubyObject)this.handle.invokeExact(context, method.getStaticScope(), self, args, block, implementationClass);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        } finally {
            post(context);
        }
    }

    public boolean hasExplicitCallProtocol() {
        return method.hasExplicitCallProtocol();
    }

    @Override
    public DynamicMethod dup() {
        return new CompiledIRMethod(handle, method, visibility, implementationClass);
    }

    public String getFile() {
        return method.getFileName();
    }

    public int getLine() {
        return method.getLineNumber();
    }
}
