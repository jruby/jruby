package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyNil;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.OptoFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class LiteralValueBootstrap {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final Handle CONTEXT_VALUE_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(LiteralValueBootstrap.class),
            "contextValue",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);
    public static final Handle CONTEXT_VALUE_STRING_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(LiteralValueBootstrap.class),
            "contextValueString",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class),
            false);
    
    private static final MethodHandle RUNTIME_HANDLE =
            Binder
                    .from(Ruby.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, LiteralValueBootstrap.class, "runtime");
    private static final MethodHandle NIL_HANDLE =
            Binder
                    .from(IRubyObject.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, LiteralValueBootstrap.class, "nil");
    private static final MethodHandle TRUE_HANDLE =
            Binder
                    .from(IRubyObject.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, LiteralValueBootstrap.class, "True");
    private static final MethodHandle FALSE_HANDLE =
            Binder
                    .from(IRubyObject.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, LiteralValueBootstrap.class, "False");
    private static final MethodHandle RUBY_ENCODING_HANDLE =
            Binder
                    .from(RubyEncoding.class, ThreadContext.class, MutableCallSite.class, String.class)
                    .invokeStaticQuiet(LOOKUP, LiteralValueBootstrap.class, "rubyEncoding");
    private static final MethodHandle ENCODING_HANDLE =
            Binder
                    .from(Encoding.class, ThreadContext.class, MutableCallSite.class, String.class)
                    .invokeStaticQuiet(LOOKUP, LiteralValueBootstrap.class, "encoding");

    public static CallSite contextValue(MethodHandles.Lookup lookup, String name, MethodType type) {
        MutableCallSite site = new MutableCallSite(type);

        MethodHandle dmh;
        switch (name) {
            case "runtime":
                dmh = RUNTIME_HANDLE;
                break;
            case "nil":
                dmh = NIL_HANDLE;
                break;
            case "True":
                dmh = TRUE_HANDLE;
                break;
            case "False":
                dmh = FALSE_HANDLE;
                break;
            case "rubyEncoding":
                dmh = RUBY_ENCODING_HANDLE;
                break;
            case "encoding":
                dmh = ENCODING_HANDLE;
                break;
            default:
                throw new RuntimeException("BUG: invalid context value " + name);
        }

        site.setTarget(Binder.from(type).append(site).invoke(dmh));

        return site;
    }

    public static CallSite contextValueString(MethodHandles.Lookup lookup, String name, MethodType type, String str) {
        MutableCallSite site = new MutableCallSite(type);

        MethodHandle dmh;
        switch (name) {
            case "rubyEncoding":
                dmh = RUBY_ENCODING_HANDLE;
                break;
            case "encoding":
                dmh = ENCODING_HANDLE;
                break;
            default:
                throw new RuntimeException("BUG: invalid context value " + name);
        }

        site.setTarget(Binder.from(type).append(site, str).invoke(dmh));
        return site;
    }

    public static IRubyObject nil(ThreadContext context, MutableCallSite site) {
        RubyNil nil = (RubyNil) context.nil;

        MethodHandle constant = (MethodHandle) nil.constant();
        if (constant == null) constant = (MethodHandle) OptoFactory.newConstantWrapper(IRubyObject.class, context.nil);

        site.setTarget(constant);

        return nil;
    }

    public static IRubyObject True(ThreadContext context, MutableCallSite site) {
        MethodHandle constant = (MethodHandle)context.tru.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(IRubyObject.class, context.tru);

        site.setTarget(constant);

        return context.tru;
    }

    public static IRubyObject False(ThreadContext context, MutableCallSite site) {
        MethodHandle constant = (MethodHandle)context.fals.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(IRubyObject.class, context.fals);

        site.setTarget(constant);

        return context.fals;
    }

    public static Ruby runtime(ThreadContext context, MutableCallSite site) {
        MethodHandle constant = (MethodHandle)context.runtime.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(Ruby.class, context.runtime);

        site.setTarget(constant);

        return context.runtime;
    }

    public static RubyEncoding rubyEncoding(ThreadContext context, MutableCallSite site, String name) {
        RubyEncoding rubyEncoding = IRRuntimeHelpers.retrieveEncoding(context, name);

        MethodHandle constant = (MethodHandle)rubyEncoding.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(RubyEncoding.class, rubyEncoding);

        site.setTarget(constant);

        return rubyEncoding;
    }

    public static Encoding encoding(ThreadContext context, MutableCallSite site, String name) {
        Encoding encoding = IRRuntimeHelpers.retrieveJCodingsEncoding(context, name);

        MethodHandle constant = MethodHandles.constant(Encoding.class, encoding);
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(Encoding.class, encoding);

        site.setTarget(constant);

        return encoding;
    }
}
