package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
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
import org.jruby.runtime.Helpers;

public class CompiledIRMethod extends DynamicMethod implements PositionAware {
    private static final Logger LOG = LoggerFactory.getLogger("CompiledIRMethod");

    private final MethodHandle method;
    private final String name;
    private final String file;
    private final int line;
    private final StaticScope scope;
    private Arity arity;
    boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    public CompiledIRMethod(MethodHandle method, String name, String file, int line, StaticScope scope, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone);
        this.method = method;
        this.name = name;
        this.file = file;
        this.line = line;
        this.scope = scope;
        this.arity = calculateArity();

        setHandle(method);
    }

    private Arity calculateArity() {
        StaticScope s = scope;
        if (s.getOptionalArgs() > 0 || s.getRestArg() >= 0) return Arity.required(s.getRequiredArgs());

        return Arity.createArity(s.getRequiredArgs());
    }

    @Override
    public Arity getArity() {
        return this.arity;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (IRRuntimeHelpers.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? this.name : name;
            LOG.info("Executing '" + realName + "'");
        }

        try {
            // update call stacks (push: frame, class, scope, etc.)
            RubyModule implementationClass = getImplementationClass();
            context.preMethodFrameAndScope(implementationClass, name, self, block, scope);
            context.setCurrentVisibility(getVisibility());
            return (IRubyObject)this.method.invokeWithArguments(context, scope, self, args, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        } finally {
            // update call stacks (pop: ..)
            context.popFrame();
            context.postMethodScopeOnly();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (IRRuntimeHelpers.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? this.name : name;
            LOG.info("Executing '" + realName + "'");
        }

        try {
            // update call stacks (push: frame, class, scope, etc.)
            RubyModule implementationClass = getImplementationClass();
            context.preMethodFrameAndScope(implementationClass, name, self, block, scope);
            context.setCurrentVisibility(getVisibility());
            return (IRubyObject)this.method.invokeWithArguments(context, scope, self, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        } finally {
            // update call stacks (pop: ..)
            context.popFrame();
            context.postMethodScopeOnly();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (IRRuntimeHelpers.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? this.name : name;
            LOG.info("Executing '" + realName + "'");
        }

        try {
            // update call stacks (push: frame, class, scope, etc.)
            RubyModule implementationClass = getImplementationClass();
            context.preMethodFrameAndScope(implementationClass, name, self, block, scope);
            context.setCurrentVisibility(getVisibility());
            return (IRubyObject)this.method.invokeWithArguments(context, scope, self, arg0, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        } finally {
            // update call stacks (pop: ..)
            context.popFrame();
            context.postMethodScopeOnly();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (IRRuntimeHelpers.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? this.name : name;
            LOG.info("Executing '" + realName + "'");
        }

        try {
            // update call stacks (push: frame, class, scope, etc.)
            RubyModule implementationClass = getImplementationClass();
            context.preMethodFrameAndScope(implementationClass, name, self, block, scope);
            context.setCurrentVisibility(getVisibility());
            return (IRubyObject)this.method.invokeWithArguments(context, scope, self, arg0, arg1, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        } finally {
            // update call stacks (pop: ..)
            context.popFrame();
            context.postMethodScopeOnly();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (IRRuntimeHelpers.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? this.name : name;
            LOG.info("Executing '" + realName + "'");
        }

        try {
            // update call stacks (push: frame, class, scope, etc.)
            RubyModule implementationClass = getImplementationClass();
            context.preMethodFrameAndScope(implementationClass, name, self, block, scope);
            context.setCurrentVisibility(getVisibility());
            return (IRubyObject)this.method.invokeWithArguments(context, scope, self, arg0, arg1, arg2, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            // not reached
            return null;
        } finally {
            // update call stacks (pop: ..)
            context.popFrame();
            context.postMethodScopeOnly();
        }
    }

    @Override
    public DynamicMethod dup() {
        return new CompiledIRMethod(method, name, file, line, scope, visibility, implementationClass);
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
	}

    public StaticScope getStaticScope() {
        return scope;
    }
}
