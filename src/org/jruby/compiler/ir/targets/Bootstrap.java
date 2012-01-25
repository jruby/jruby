package org.jruby.compiler.ir.targets;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static org.jruby.util.CodegenUtils.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class Bootstrap {
    public static CallSite fixnum(Lookup lookup, String name, MethodType type, long value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle =
                insertArguments(
                        findStatic(
                                lookup,
                                Bootstrap.class,
                                name,
                                type.insertParameterTypes(0, MutableCallSite.class, long.class)),
                        0,
                        site,
                        value);
        site.setTarget(handle);
        return site;
    }

    public static CallSite string(Lookup lookup, String name, MethodType type, String value, int encoding) {
        MethodHandle handle =
                insertArguments(
                        findStatic(
                                lookup,
                                Bootstrap.class,
                                name,
                                type.insertParameterTypes(0, String.class, int.class)),
                        0,
                        value,
                        encoding);
        CallSite site = new ConstantCallSite(handle);
        return site;
    }

    private static class InvokeSite extends MutableCallSite {
        public InvokeSite(MethodType type, String name) {
            super(type);
            this.name = name;
        }

        public final String name;
    }
    public static CallSite invoke(Lookup lookup, String name, MethodType type) {
        InvokeSite site = new InvokeSite(type, JavaNameMangler.demangleMethodName(name.split(":")[1]));
        MethodHandle handle =
                insertArguments(
                        findStatic(
                                lookup,
                                Bootstrap.class,
                                "invoke",
                                type.insertParameterTypes(0, InvokeSite.class)),
                        0,
                        site);
        site.setTarget(handle);
        return site;
    }
    public static CallSite invokeSelf(Lookup lookup, String name, MethodType type) {
        InvokeSite site = new InvokeSite(type, JavaNameMangler.demangleMethodName(name.split(":")[1]));
        MethodHandle handle =
                insertArguments(
                        findStatic(
                                lookup,
                                Bootstrap.class,
                                "invokeSelf",
                                type.insertParameterTypes(0, InvokeSite.class)),
                        0,
                        site);
        site.setTarget(handle);
        return site;
    }

    public static Handle fixnum() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "fixnum", sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class));
    }
    public static Handle string() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "string", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class));
    }
    public static Handle invoke() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "invoke", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }
    public static Handle invokeSelf() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "invokeSelf", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static IRubyObject fixnum(MutableCallSite site, long value, ThreadContext context) {
        RubyFixnum fixnum = RubyFixnum.newFixnum(context.runtime, value);
        site.setTarget(
                dropArguments(
                        constant(IRubyObject.class, fixnum),
                        0,
                        ThreadContext.class
                )
        );
        return fixnum;
    }

    public static IRubyObject string(String value, int encoding, ThreadContext context) {
        // obviously wrong: not caching bytelist, not using encoding
        return RubyString.newStringNoCopy(context.runtime, value.getBytes(RubyEncoding.ISO));
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0) {
        return self.getMetaClass().invoke(context, self, site.name, arg0, CallType.NORMAL);
    }
    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        return self.getMetaClass().invoke(context, self, site.name, arg0, CallType.NORMAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0) {
        return self.getMetaClass().invoke(context, self, site.name, arg0, CallType.FUNCTIONAL);
    }

    private static MethodHandle findStatic(Class target, String name, MethodType type) {
        return findStatic(lookup(), target, name, type);
    }

    private static MethodHandle findStatic(Lookup lookup, Class target, String name, MethodType type) {
        try {
            return lookup.findStatic(target, name, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
