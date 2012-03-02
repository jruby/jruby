package org.jruby.compiler.ir.targets;

import com.headius.invoke.binder.Binder;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.invokedynamic.JRubyCallSite;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.util.Arrays;

import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.callMethodMissing;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.methodMissing;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.pollAndGetClass;
import static org.jruby.util.CodegenUtils.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class Bootstrap {
    public static CallSite string(Lookup lookup, String name, MethodType type, String value, int encoding) {
        MethodHandle handle = Binder
                .from(IRubyObject.class, ThreadContext.class)
                .insert(0, value, encoding)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "string");
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

    public static CallSite ivar(Lookup lookup, String name, MethodType type) {
        String[] bits = name.split(":");
        String getSet = bits[0];
        String varName = JavaNameMangler.demangleMethodName(bits[1]);

        MethodHandle handle = Binder
                .from(type)
                .insert(0, varName)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, getSet);
        return new ConstantCallSite(handle);
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

    public static Handle ivar() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "ivar", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static IRubyObject string(String value, int encoding, ThreadContext context) {
        // obviously wrong: not caching bytelist, not using encoding
        return RubyString.newStringNoCopy(context.runtime, value.getBytes(RubyEncoding.ISO));
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(site, method, 0);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(site, method, 1);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(site, method, 2);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(site, method, 3);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(site, method, 0);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self);
    }

    private static final int[][] PERMUTES = new int[][] {
            new int[]{1, 0},
            new int[]{1, 0, 2},
            new int[]{1, 0, 2, 3},
            new int[]{1, 0, 2, 3, 4},
    };

    private static MethodHandle getHandle(InvokeSite site, DynamicMethod method, int arity) throws Throwable {
        MethodHandle mh = null;
        if (method.getNativeCall() != null) {
            DynamicMethod.NativeCall nc = method.getNativeCall();
            if (method.getArity().isFixed()) {
                if (method.getArity().getValue() <= 3) {
                    Binder b = Binder.from(site.type());
                    if (!nc.hasContext()) {
                        b.drop(0);
                    }

                    if (nc.hasBlock()) {
                        b.insert(site.type().parameterCount() - 1, Block.NULL_BLOCK);
                    }


                    if (nc.isStatic()) {
                        if (b.type().parameterCount() == nc.getNativeSignature().length) {
                            mh = b
                                    .cast(nc.getNativeReturn(), nc.getNativeSignature())
                                    .invokeStaticQuiet(MethodHandles.lookup(), nc.getNativeTarget(), nc.getNativeName());
//                            System.out.println(mh);
                        }
                    } else {
//                        System.out.println(b.type());
//                        System.out.println(Arrays.toString(nc.getNativeSignature()));
                        if (b.type().parameterCount() == nc.getNativeSignature().length + 1) {
                            // only threadcontext-receivers right now
                            mh = b
                                    .permute(PERMUTES[arity])
                                    .cast(MethodType.methodType(nc.getNativeReturn(), nc.getNativeTarget(), nc.getNativeSignature()))
                                    .invokeVirtualQuiet(MethodHandles.lookup(), nc.getNativeName());
//                            System.out.println(mh);
                        }
                    }
                }
            }
        }

        if (mh == null) {
            mh = Binder
                    .from(site.type())
                    .insert(0, site.name)
                    .invokeStatic(MethodHandles.lookup(), Bootstrap.class, "invokeSelfSimple");
        }

        return mh;
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self) {
        return self.getMetaClass().invoke(context, self, name, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(site, method, 1);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0) {
        return self.getMetaClass().invoke(context, self, name, arg0, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1);
        }

        MethodHandle mh = getHandle(site, method, 2);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        return self.getMetaClass().invoke(context, self, name, arg0, arg1, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String methodName = site.name;
//        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1, arg2);
        }

        MethodHandle mh = getHandle(site, method, 3);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1, arg2);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getMetaClass().invoke(context, self, name, arg0, arg1, arg2, CallType.FUNCTIONAL);
    }

    public static IRubyObject ivarGet(String name, IRubyObject self) {
        return self.getInstanceVariables().getInstanceVariable(name);
    }

    public static void ivarSet(String name, IRubyObject self, IRubyObject value) {
        self.getInstanceVariables().setInstanceVariable(name, value);
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

    ///////////////////////////////////////////////////////////////////////////
    // COMPLETED WORK BELOW

    ///////////////////////////////////////////////////////////////////////////
    // Symbol binding

    public static Handle symbol() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "symbol", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class));
    }

    public static CallSite symbol(Lookup lookup, String name, MethodType type, String sym) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = Binder
                .from(IRubyObject.class, ThreadContext.class)
                .insert(0, site, sym)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "symbol");
        site.setTarget(handle);
        return site;
    }

    public static IRubyObject symbol(MutableCallSite site, String name, ThreadContext context) {
        RubySymbol symbol = RubySymbol.newSymbol(context.runtime, name);
        site.setTarget(Binder
                .from(IRubyObject.class, ThreadContext.class)
                .drop(0)
                .constant(symbol)
        );
        return symbol;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fixnum binding

    public static Handle fixnum() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "fixnum", sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class));
    }

    public static CallSite fixnum(Lookup lookup, String name, MethodType type, long value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = Binder
                .from(IRubyObject.class, ThreadContext.class)
                .insert(0, site, value)
                .cast(IRubyObject.class, MutableCallSite.class, long.class, ThreadContext.class)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "fixnum");
        site.setTarget(handle);
        return site;
    }

    public static IRubyObject fixnum(MutableCallSite site, long value, ThreadContext context) {
        RubyFixnum fixnum = RubyFixnum.newFixnum(context.runtime, value);
        site.setTarget(Binder
                .from(IRubyObject.class, ThreadContext.class)
                .drop(0)
                .constant(fixnum)
        );
        return fixnum;
    }
}
