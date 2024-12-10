package org.jruby.runtime.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import org.jruby.Ruby;
import org.jruby.RubyGlobal;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class GlobalSite extends MutableCallSite {
    public static final Handle GLOBAL_BOOTSTRAP_H = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(GlobalSite.class),
            "globalBootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class),
            false);
    private static final Logger LOG = LoggerFactory.getLogger(GlobalSite.class);
    private final String name;
    private volatile int failures;
    private final String file;
    private final int line;

    public GlobalSite(MethodType type, String name, String file, int line) {
        super(type);
        this.name = name;
        this.file = file;
        this.line = line;
    }

    public void setTarget(MethodHandle target) {
        super.setTarget(target);
        incrementFailures();
    }

    public int failures() {
        return failures;
    }

    public void incrementFailures() {
        failures += 1;
    }

    public String name() {
        return name;
    }

    public String file() { return file; }

    public int line() { return line; }

    public static CallSite globalBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String file, int line) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = JavaNameMangler.demangleMethodName(names[1]);
        GlobalSite site = new GlobalSite(type, varName, file, line);
        MethodHandle handle;

        if (operation.equals("get")) {
            handle = lookup.findVirtual(GlobalSite.class, "getGlobalFallback", methodType(IRubyObject.class, ThreadContext.class));
        } else {
            handle = lookup.findVirtual(GlobalSite.class, "setGlobalFallback", methodType(void.class, IRubyObject.class, ThreadContext.class));
        }

        handle = handle.bindTo(site);
        site.setTarget(handle);

        return site;
    }

    public IRubyObject getGlobalFallback(ThreadContext context) throws Throwable {
        GlobalVariable variable = globalVariables(context).getVariable(name());

        if (failures() > Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() ||
                variable.getScope() != GlobalVariable.Scope.GLOBAL ||
                RubyGlobal.UNCACHED_GLOBALS.contains(name())) {

            // use uncached logic forever
            if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + name() + " (" + file() + ":" + line() + ") uncacheable or rebound > " + Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() + " times, reverting to simple lookup");

            MethodHandle uncached = lookup().findStatic(GlobalSite.class, "getGlobalUncached", methodType(IRubyObject.class, GlobalVariable.class));
            uncached = uncached.bindTo(variable);
            uncached = dropArguments(uncached, 0, ThreadContext.class);
            setTarget(uncached);
            return (IRubyObject)uncached.invokeWithArguments(context);
        }

        // get switchpoint before value
        SwitchPoint switchPoint = (SwitchPoint) variable.getInvalidator().getData();
        IRubyObject value = variable.getAccessor().getValue();

        MethodHandle target = constant(IRubyObject.class, value);
        target = dropArguments(target, 0, ThreadContext.class);
        MethodHandle fallback = lookup().findVirtual(GlobalSite.class, "getGlobalFallback", methodType(IRubyObject.class, ThreadContext.class));
        fallback = fallback.bindTo(this);

        target = switchPoint.guardWithTest(target, fallback);

        setTarget(target);

        if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + name() + " (" + file() + ":" + line() + ") cached");

        return value;
    }

    public static IRubyObject getGlobalUncached(GlobalVariable variable) throws Throwable {
        return variable.getAccessor().getValue();
    }

    public void setGlobalFallback(IRubyObject value, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        GlobalVariable variable = runtime.getGlobalVariables().getVariable(name());
        MethodHandle uncached = lookup().findStatic(GlobalSite.class, "setGlobalUncached", methodType(void.class, GlobalVariable.class, IRubyObject.class));
        uncached = uncached.bindTo(variable);
        uncached = dropArguments(uncached, 1, ThreadContext.class);
        setTarget(uncached);
        uncached.invokeWithArguments(value, context);
    }

    public static void setGlobalUncached(GlobalVariable variable, IRubyObject value) throws Throwable {
        // FIXME: duplicated logic from GlobalVariables.set
        variable.getAccessor().setValue(value);
        variable.trace(value);
        variable.invalidate();
    }
}
