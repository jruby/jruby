package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.runtime.Helpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.runtime.invokedynamic.MathLinker;
import org.jruby.runtime.invokedynamic.VariableSite;
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

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import org.jruby.RubyFloat;
import org.jruby.runtime.ivars.VariableAccessor;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.*;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class Bootstrap {
    public static CallSite string(Lookup lookup, String name, MethodType type, String value, int encoding) {
        MethodHandle handle = Binder
                .from(IRubyObject.class, ThreadContext.class)
                .insert(0, new Class[]{String.class, int.class}, value, encoding)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "string");
        CallSite site = new ConstantCallSite(handle);
        return site;
    }

    public static CallSite array(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(1, IRubyObject[].class)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "array");
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

    public static CallSite attrAssign(Lookup lookup, String name, MethodType type) {
        InvokeSite site = new InvokeSite(type, JavaNameMangler.demangleMethodName(name.split(":")[1]));
        MethodHandle handle =
                insertArguments(
                        findStatic(
                                lookup,
                                Bootstrap.class,
                                "attrAssign",
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

    public static CallSite ivar(Lookup lookup, String name, MethodType type) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = names[1];
        VariableSite site = new VariableSite(type, varName, "noname", 0);
        MethodHandle handle;

        handle = lookup.findStatic(Bootstrap.class, operation, type.insertParameterTypes(0, VariableSite.class));

        handle = handle.bindTo(site);
        site.setTarget(handle.asType(site.type()));

        return site;
    }

    public static CallSite searchConst(Lookup lookup, String name, MethodType type) {
        MutableCallSite site = new MutableCallSite(type);
        String[] bits = name.split(":");
        String constName = bits[1];

        MethodHandle handle = Binder
                .from(type)
                .insert(0, site, constName)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "searchConst");

        site.setTarget(handle);

        return site;
    }

    public static CallSite inheritanceSearchConst(Lookup lookup, String name, MethodType type) {
        MutableCallSite site = new MutableCallSite(type);
        String[] bits = name.split(":");
        String constName = bits[1];

        MethodHandle handle = Binder
                .from(type)
                .insert(0, site, constName)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "inheritanceSearchConst");

        site.setTarget(handle);

        return site;
    }

    public static Handle string() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "string", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class));
    }

    public static Handle array() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "array", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle invoke() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "invoke", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle invokeSelf() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "invokeSelf", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle invokeFixnumOp() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(MathLinker.class), "fixnumOperatorBootstrap", sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class, String.class, int.class));
    }

    public static Handle attrAssign() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "attrAssign", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle ivar() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "ivar", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle searchConst() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "searchConst", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle inheritanceSearchConst() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "inheritanceSearchConst", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static IRubyObject string(String value, int encoding, ThreadContext context) {
        // obviously wrong: not caching bytelist, not using encoding
        return RubyString.newStringNoCopy(context.runtime, value.getBytes(RubyEncoding.ISO));
    }

    public static IRubyObject array(ThreadContext context, IRubyObject[] elts) {
        return RubyArray.newArrayNoCopy(context.runtime, elts);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 0, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 1, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 2, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 3, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1, arg2);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 0, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 0, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, block);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 1, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, block);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 2, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1, block);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 3, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1, arg2, block);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 0, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, block);
    }

    public static IRubyObject attrAssign(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, self)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 1, false);

        mh = foldArguments(
                mh,
                Binder.from(site.type())
                        .drop(0, 2)
                        .identity());

        site.setTarget(mh);
        mh.invokeWithArguments(context, self, arg0);
        return arg0;
    }

    private static final int[][] PERMUTES = new int[][] {
            new int[]{1, 0},
            new int[]{1, 0, 2},
            new int[]{1, 0, 2, 3},
            new int[]{1, 0, 2, 3, 4},
    };

    private static MethodHandle getHandle(RubyClass selfClass, SwitchPoint switchPoint, InvokeSite site, DynamicMethod method, int arity, boolean block) throws Throwable {
        MethodHandle mh = null;
        if (method.getNativeCall() != null) {
            DynamicMethod.NativeCall nc = method.getNativeCall();
            if (method.getArity().isFixed()) {
                if (method.getArity().getValue() <= 3) {
                    Binder b = Binder.from(site.type());
                    if (!nc.hasContext()) {
                        b.drop(0);
                    }

                    if (nc.hasBlock() && !block) {
                        b.insert(site.type().parameterCount() - 1, Block.NULL_BLOCK);
                    } else if (!nc.hasBlock() && block) {
                        b.drop(site.type().parameterCount() - 2, 1);
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
//            if (mh != null) System.out.println("binding to native: " + site.name);
        }

        if (mh == null) {
            // attempt IR direct binding
            if (method instanceof CompiledIRMethod) {
                mh = (MethodHandle)((CompiledIRMethod)method).getHandle();

                if (!block) {
                    mh = MethodHandles.insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);
                }

                mh = MethodHandles.insertArguments(mh, 1, ((CompiledIRMethod)method).getStaticScope());
//                System.out.println("binding IR compiled direct: " + site.name);
            }
        }
//        System.out.println(site.name);
//        System.out.println("before: " + mh);

        if (mh == null) {
//            System.out.println(site.type());

            // use DynamicMethod binding
            MethodType type2 = site.type()
                    .insertParameterTypes(2, RubyModule.class, String.class)
                    .insertParameterTypes(0, DynamicMethod.class);
            mh = Binder.from(site.type())
                    .insert(2, selfClass, site.name)
//                    .printType()
                    .insert(0, method)
//                    .printType()
                    .cast(type2)
                    .invokeVirtual(MethodHandles.lookup(), "call");

//            System.out.println("binding to DynamicMethod: " + mh);
//            System.out.println("binding to DynamicMethod.call: " + site.name);
        }

        MethodHandle fallback = Binder
                .from(site.type())
                .insert(0, site.name)
//                .printType()
                .invokeStatic(MethodHandles.lookup(), Bootstrap.class, "invokeSelfSimple");
//        System.out.println("simple fallback: " + fallback);

        if (mh == null) {
            return fallback;
        } else {
            MethodHandle test = Binder
                    .from(site.type().changeReturnType(boolean.class))
                    .insert(0, new Class[]{RubyClass.class}, selfClass)
                    .invokeStatic(MethodHandles.lookup(), Bootstrap.class, "testType");
            mh = MethodHandles.guardWithTest(test, mh, fallback);
            mh = switchPoint.guardWithTest(mh, fallback);
        }

        return mh;
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self) {
        return self.getMetaClass().invoke(context, self, name, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 1, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0) {
        return self.getMetaClass().invoke(context, self, name, arg0, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 2, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        return self.getMetaClass().invoke(context, self, name, arg0, arg1, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1, arg2);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 3, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1, arg2);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getMetaClass().invoke(context, self, name, arg0, arg1, arg2, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().invoke(context, self, name, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 1, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, block);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().invoke(context, self, name, arg0, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 2, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1, block);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().invoke(context, self, name, arg0, arg1, CallType.FUNCTIONAL);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, self)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1, arg2);
        }

        MethodHandle mh = getHandle(selfClass, switchPoint, site, method, 3, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, self, arg0, arg1, arg2, block);
    }

    public static IRubyObject invokeSelfSimple(String name, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().invoke(context, self, name, arg0, arg1, arg2, CallType.FUNCTIONAL);
    }

    public static IRubyObject ivarGet(VariableSite site, IRubyObject self) throws Throwable {
        VariableAccessor accessor = self.getMetaClass().getRealClass().getVariableAccessorForRead(site.name);

        // produce nil if the variable has not been initialize
        MethodHandle nullToNil = findStatic(Helpers.class, "nullToNil", methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class));
        nullToNil = insertArguments(nullToNil, 1, self.getRuntime().getNil());
        nullToNil = explicitCastArguments(nullToNil, methodType(IRubyObject.class, Object.class));

        // get variable value and filter with nullToNil
        MethodHandle getValue = findVirtual(IRubyObject.class, "getVariable", methodType(Object.class, int.class));
        getValue = insertArguments(getValue, 1, accessor.getIndex());
        getValue = filterReturnValue(getValue, nullToNil);

        // prepare fallback
        MethodHandle fallback = null;
        if (site.getTarget() == null || site.chainCount() + 1 > RubyInstanceConfig.MAX_POLY_COUNT) {
//            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tget triggered site rebind " + self.getMetaClass().id);
            fallback = findStatic(InvokeDynamicSupport.class, "getVariableFallback", methodType(IRubyObject.class, VariableSite.class, IRubyObject.class));
            fallback = fallback.bindTo(site);
            site.clearChainCount();
        } else {
//            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tget added to PIC " + self.getMetaClass().id);
            fallback = site.getTarget();
            site.incrementChainCount();
        }

        // prepare test
        MethodHandle test = findStatic(InvocationLinker.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
        test = insertArguments(test, 0, accessor.getClassId());

        getValue = guardWithTest(test, getValue, fallback);

//        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tget on class " + self.getMetaClass().id + " bound directly");
        site.setTarget(getValue);

        return (IRubyObject)getValue.invokeWithArguments(self);
    }

    public static void ivarSet(VariableSite site, IRubyObject self, IRubyObject value) throws Throwable {
        VariableAccessor accessor = self.getMetaClass().getRealClass().getVariableAccessorForWrite(site.name);

        // set variable value and fold by returning value
        MethodHandle setValue = findVirtual(IRubyObject.class, "setVariable", methodType(void.class, int.class, Object.class));
        setValue = explicitCastArguments(setValue, methodType(void.class, IRubyObject.class, int.class, IRubyObject.class));
        setValue = insertArguments(setValue, 1, accessor.getIndex());

        // prepare fallback
        MethodHandle fallback = null;
        if (site.getTarget() == null || site.chainCount() + 1 > RubyInstanceConfig.MAX_POLY_COUNT) {
//            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tset triggered site rebind " + self.getMetaClass().id);
            fallback = findStatic(InvokeDynamicSupport.class, "setVariableFallback", methodType(void.class, VariableSite.class, IRubyObject.class, IRubyObject.class));
            fallback = fallback.bindTo(site);
            site.clearChainCount();
        } else {
//            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tset added to PIC " + self.getMetaClass().id);
            fallback = site.getTarget();
            site.incrementChainCount();
        }

        // prepare test
        MethodHandle test = findStatic(InvocationLinker.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
        test = insertArguments(test, 0, accessor.getClassId());
        test = dropArguments(test, 1, IRubyObject.class);

        setValue = guardWithTest(test, setValue, fallback);

//        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tset on class " + self.getMetaClass().id + " bound directly");
        site.setTarget(setValue);

        setValue.invokeWithArguments(self, value);
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

    public static boolean testType(RubyClass original, ThreadContext context, IRubyObject self) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
    }

    public static boolean testType(RubyClass original, ThreadContext context, IRubyObject self, IRubyObject arg0) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
    }

    public static boolean testType(RubyClass original, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
    }

    public static boolean testType(RubyClass original, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
    }

    public static boolean testType(RubyClass original, ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
    }

    ///////////////////////////////////////////////////////////////////////////
    // constant lookup

    public static IRubyObject searchConst(MutableCallSite site, String constName, ThreadContext context, StaticScope staticScope) throws Throwable {
        Ruby runtime = context.runtime;
        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();
        IRubyObject value = staticScope.getConstant(constName);

        if (value == null) {
            return staticScope.getModule().callMethod(context, "const_missing", runtime.fastNewSymbol(constName));
        }

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(value);
        MethodHandle fallback = Binder.from(site.type())
                .insert(0, site, constName)
                .invokeStatic(MethodHandles.lookup(), Bootstrap.class, "searchConst");

        site.setTarget(switchPoint.guardWithTest(target, fallback));

        return value;
    }

    public static IRubyObject inheritanceSearchConst(MutableCallSite site, String constName, ThreadContext context, IRubyObject cmVal) throws Throwable {
        Ruby runtime = context.runtime;
        RubyModule module;

        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw runtime.newTypeError(cmVal + " is not a type/class");
        }

        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();

        IRubyObject value = module.getConstantFromNoConstMissing(constName, false);
        if (value == null) {
            return (IRubyObject)UndefinedValue.UNDEFINED;
        }

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(value);
        MethodHandle fallback = Binder.from(site.type())
                .insert(0, site, constName)
                .invokeStatic(MethodHandles.lookup(), Bootstrap.class, "inheritanceSearchConst");

        site.setTarget(switchPoint.guardWithTest(target, fallback));

        return value;
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

    ///////////////////////////////////////////////////////////////////////////
    // Float binding

    public static Handle flote() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "flote", sig(CallSite.class, Lookup.class, String.class, MethodType.class, double.class));
    }

    public static CallSite flote(Lookup lookup, String name, MethodType type, double value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = Binder
                .from(IRubyObject.class, ThreadContext.class)
                .insert(0, site, value)
                .cast(IRubyObject.class, MutableCallSite.class, double.class, ThreadContext.class)
                .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "flote");
        site.setTarget(handle);
        return site;
    }

    public static IRubyObject flote(MutableCallSite site, double value, ThreadContext context) {
        RubyFloat flote = RubyFloat.newFloat(context.runtime, value);
        site.setTarget(Binder
                .from(IRubyObject.class, ThreadContext.class)
                .drop(0)
                .constant(flote)
        );
        return flote;
    }
}
