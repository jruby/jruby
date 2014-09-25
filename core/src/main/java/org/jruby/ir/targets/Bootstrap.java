package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.internal.runtime.methods.*;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.runtime.invokedynamic.JRubyCallSite;
import org.jruby.runtime.invokedynamic.MathLinker;
import org.jruby.runtime.invokedynamic.VariableSite;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.RegexpOptions;
import org.jruby.util.cli.Options;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.*;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.*;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class Bootstrap {
    private static final Lookup LOOKUP = MethodHandles.lookup();

    public static CallSite string(Lookup lookup, String name, MethodType type, String value, String encodingName) {
        Encoding encoding;
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(encodingName.getBytes());
        if (entry == null) entry = EncodingDB.getAliases().get(encodingName.getBytes());
        if (entry == null) throw new RuntimeException("could not find encoding: " + encodingName);
        encoding = entry.getEncoding();
        ByteList byteList = new ByteList(value.getBytes(RubyEncoding.ISO), encoding);
        MethodHandle handle = Binder
                .from(RubyString.class, ThreadContext.class)
                .insert(0, ByteList.class, byteList)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "string");
        return new ConstantCallSite(handle);
    }

    public static CallSite bytelist(Lookup lookup, String name, MethodType type, String value, String encodingName) {
        Encoding encoding;
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(encodingName.getBytes());
        if (entry == null) entry = EncodingDB.getAliases().get(encodingName.getBytes());
        if (entry == null) throw new RuntimeException("could not find encoding: " + encodingName);
        encoding = entry.getEncoding();
        ByteList byteList = new ByteList(value.getBytes(RubyEncoding.ISO), encoding);
        return new ConstantCallSite(constant(ByteList.class, byteList));
    }

    public static CallSite regexp(Lookup lookup, String name, MethodType type, int options) {
        MutableCallSite site = new MutableCallSite(type);
        RegexpOptions o = RegexpOptions.fromEmbeddedOptions(options);
        MethodHandle handle;

        if (name.equals("dregexp")) {
            String[] argNames = new String[type.parameterCount()];
            Class[] argTypes = new Class[argNames.length];

            argNames[0] = "context";
            argTypes[0] = ThreadContext.class;

            for (int i = 1; i < argNames.length; i++) {
                argNames[i] = "part" + i;
                argTypes[i] = RubyString.class;
            }

            handle = SmartBinder
                    .from(lookup, RubyRegexp.class, argNames, argTypes)
                    .collect("parts", "part.*")
                    .append("site", site)
                    .append("options", o)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "dregexp").handle();
        } else {
            handle = Binder
                    .from(lookup, type)
                    .append(MutableCallSite.class, site)
                    .append(o)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "regexp");
        }

        site.setTarget(handle);
        return site;
    }

    public static CallSite array(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(1, IRubyObject[].class)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "array");
        CallSite site = new ConstantCallSite(handle);
        return site;
    }

    public static CallSite hash(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(1, IRubyObject[].class)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "hash");
        CallSite site = new ConstantCallSite(handle);
        return site;
    }

    public static CallSite bignum(Lookup lookup, String name, MethodType type, String value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = Binder
                .from(RubyBignum.class, ThreadContext.class)
                .append(value, site)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "bignum");
        site.setTarget(handle);
        return site;
    }

    public static CallSite objectArray(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(0, IRubyObject[].class)
                .identity();
        return new ConstantCallSite(handle);
    }

    private static class InvokeSite extends MutableCallSite {
        final Signature signature;
        final Signature fullSignature;
        final int arity;

        public InvokeSite(MethodType type, String name, CallType callType) {
            super(type);
            this.name = name;
            this.callType = callType;

            Signature startSig;
            int argOffset;

            if (callType == CallType.SUPER) {
                // super calls receive current class argument, so offsets and signature are different
                startSig = JRubyCallSite.STANDARD_SUPER_SIG;
                argOffset = 4;
            } else {
                startSig = JRubyCallSite.STANDARD_SITE_SIG;
                argOffset = 3;
            }

            int arity;
            if (type.parameterType(type.parameterCount() - 1) == Block.class) {
                arity = type.parameterCount() - (argOffset + 1);

                if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                    arity = -1;
                    startSig = startSig.appendArg("args", IRubyObject[].class);
                } else {
                    for (int i = 0; i < arity; i++) {
                        startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                    }
                }
                startSig = startSig.appendArg("block", Block.class);
                fullSignature = signature = startSig;
            } else {
                arity = type.parameterCount() - argOffset;

                if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                    arity = -1;
                    startSig = startSig.appendArg("args", IRubyObject[].class);
                } else {
                    for (int i = 0; i < arity; i++) {
                        startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                    }
                }
                signature = startSig;
                fullSignature = startSig.appendArg("block", Block.class);
            }

            this.arity = arity;
        }

        public final String name;
        public final CallType callType;
    }

    public static CallSite invoke(Lookup lookup, String name, MethodType type) {
        InvokeSite site = new InvokeSite(type, JavaNameMangler.demangleMethodName(name.split(":")[1]), CallType.NORMAL);
        MethodHandle handle;

        SmartBinder binder = SmartBinder.from(site.signature)
                .insert(0, "site", site);

        if (site.arity > 3) {
            binder = binder
                    .collect("args", "arg[0-9]+");
        }

        handle = binder.invokeStaticQuiet(lookup, Bootstrap.class, "invoke").handle();

        site.setTarget(handle);
        return site;
    }

    public static CallSite attrAssign(Lookup lookup, String name, MethodType type) {
        InvokeSite site = new InvokeSite(type, JavaNameMangler.demangleMethodName(name.split(":")[1]), CallType.NORMAL);
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
        InvokeSite site = new InvokeSite(type, JavaNameMangler.demangleMethodName(name.split(":")[1]), CallType.NORMAL);
        MethodHandle handle;

        SmartBinder binder = SmartBinder.from(site.signature)
                .insert(0, "site", site);

        if (site.arity > 3) {
            binder = binder
                    .collect("args", "arg[0-9]+");
        }

        handle = binder.invokeStaticQuiet(lookup, Bootstrap.class, "invokeSelf").handle();

        site.setTarget(handle);
        return site;
    }

    public static CallSite invokeSuper(Lookup lookup, String name, MethodType type, String splatmapString) {
        String[] targetAndMethod = name.split(":");
        String superName = JavaNameMangler.demangleMethodName(targetAndMethod[1]);

        InvokeSite site = new InvokeSite(type, name, CallType.SUPER);
        MethodHandle handle;

        boolean[] splatMap = decodeSplatmap(splatmapString);

        SmartBinder binder = SmartBinder.from(site.signature)
                .insert(
                        0,
                        arrayOf("site",           "name",       "splatMap"),
                        arrayOf(InvokeSite.class, String.class, boolean[].class),
                                site,             superName,    splatMap);

        if (site.arity > 0) {
            binder = binder
                    .collect("args", "arg[0-9]+");
        }

        handle = binder.invokeStaticQuiet(lookup, Bootstrap.class, targetAndMethod[0]).handle();

        site.setTarget(handle);

        return site;
    }

    public static boolean[] decodeSplatmap(String splatmapString) {
        boolean[] splatMap;
        if (splatmapString.length() > 0) {
            splatMap = new boolean[splatmapString.length()];

            for (int i = 0; i < splatmapString.length(); i++) {
                if (splatmapString.charAt(i) == '1') {
                    splatMap[i] = true;
                }
            }
        } else {
            splatMap = new boolean[0];
        }
        return splatMap;
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

    public static CallSite searchConst(Lookup lookup, String name, MethodType type, int noPrivateConsts) {
        MutableCallSite site = new MutableCallSite(type);
        String[] bits = name.split(":");
        String constName = bits[1];

        MethodHandle handle = Binder
                .from(lookup, type)
                .append(site, constName.intern())
                .append(noPrivateConsts == 0 ? false : true)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, bits[0]);

        site.setTarget(handle);

        return site;
    }

    public static Handle string() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "string", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class));
    }

    public static Handle bytelist() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "bytelist", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class));
    }

    public static Handle regexp() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "regexp", sig(CallSite.class, Lookup.class, String.class, MethodType.class, int.class));
    }

    public static Handle array() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "array", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle hash() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "hash", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle bignum() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "bignum", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class));
    }

    public static Handle objectArray() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "objectArray", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle invoke() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "invoke", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle invokeSelf() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "invokeSelf", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle invokeSuper() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "invokeSuper", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class));
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
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "searchConst", sig(CallSite.class, Lookup.class, String.class, MethodType.class, int.class));
    }

    public static RubyString string(ByteList value, ThreadContext context) {
        return RubyString.newStringShared(context.runtime, value);
    }

    public static IRubyObject array(ThreadContext context, IRubyObject[] elts) {
        return RubyArray.newArrayNoCopy(context.runtime, elts);
    }

    public static Handle contextValue() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "contextValue", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle contextValueString() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "contextValueString", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class));
    }

    public static CallSite contextValue(Lookup lookup, String name, MethodType type) {
        MutableCallSite site = new MutableCallSite(type);
        site.setTarget(Binder.from(type).append(site).invokeStaticQuiet(lookup, Bootstrap.class, name));
        return site;
    }

    public static CallSite contextValueString(Lookup lookup, String name, MethodType type, String str) {
        MutableCallSite site = new MutableCallSite(type);
        site.setTarget(Binder.from(type).append(site, str).invokeStaticQuiet(lookup, Bootstrap.class, name));
        return site;
    }

    public static IRubyObject nil(ThreadContext context, MutableCallSite site) {
        site.setTarget(dropArguments(constant(IRubyObject.class, context.nil), 0, ThreadContext.class));
        return context.nil;
    }

    public static IRubyObject True(ThreadContext context, MutableCallSite site) {
        site.setTarget(dropArguments(constant(IRubyObject.class, context.runtime.getTrue()), 0, ThreadContext.class));
        return context.runtime.getTrue();
    }

    public static IRubyObject False(ThreadContext context, MutableCallSite site) {
        site.setTarget(dropArguments(constant(IRubyObject.class, context.runtime.getFalse()), 0, ThreadContext.class));
        return context.runtime.getFalse();
    }

    public static Ruby runtime(ThreadContext context, MutableCallSite site) {
        site.setTarget(dropArguments(constant(Ruby.class, context.runtime), 0, ThreadContext.class));
        return context.runtime;
    }

    public static RubyEncoding encoding(ThreadContext context, MutableCallSite site, String name) {
        Encoding encoding = context.runtime.getEncodingService().findEncodingOrAliasEntry(name.getBytes()).getEncoding();
        RubyEncoding rubyEncoding = context.runtime.getEncodingService().getEncoding(encoding);
        site.setTarget(dropArguments(constant(RubyEncoding.class, rubyEncoding), 0, ThreadContext.class));
        return rubyEncoding;
    }

    public static IRubyObject hash(ThreadContext context, IRubyObject[] pairs) {
        Ruby runtime = context.runtime;
        RubyHash hash = RubyHash.newHash(runtime);
        for (int i = 0; i < pairs.length;) {
            hash.fastASetCheckString(runtime, pairs[i++], pairs[i++]);
        }
        return hash;
    }

    public static RubyRegexp regexp(ThreadContext context, RubyString pattern, MutableCallSite site, RegexpOptions options) {
        RubyRegexp regexp = RubyRegexp.newRegexp(context.runtime, pattern.getByteList(), options);
        regexp.setLiteral();

        site.setTarget(
                Binder.from(RubyRegexp.class, ThreadContext.class, RubyString.class)
                        .drop(0, 2)
                        .constant(regexp));
        return regexp;
    }

    public static RubyRegexp dregexp(ThreadContext context, RubyString[] pieces, MutableCallSite site, RegexpOptions options) {
        RubyString   pattern = RubyRegexp.preprocessDRegexp(context.runtime, pieces, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        if (options.isOnce()) {
            site.setTarget(
                    Binder.from(site.type())
                            .drop(0, site.type().parameterCount())
                            .constant(re));
        }

        return re;
    }

    public static RubyBignum bignum(ThreadContext context, String value, MutableCallSite site) {
        RubyBignum bignum = RubyBignum.newBignum(context.runtime, value);
        site.setTarget(Binder.from(site.type()).drop(0).constant(bignum));
        return bignum;
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0, arg1);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0, arg1, arg2);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1, arg2);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, args);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        // FIXME: this varargs path needs to splat to call against handle
        return method.call(context, self, selfClass, methodName, args);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, block);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, block);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0, block);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, block);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0, arg1, block);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1, block);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0, arg1, arg2, block);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1, arg2, block);
    }

    public static IRubyObject invoke(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, args, block);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        // FIXME: this varargs path needs to splat to call against handle
        return method.call(context, self, selfClass, methodName, args, block);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, Block block) throws Throwable {
        // TODO: literal block handling of break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, block);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, block);
    }

    public static IRubyObject invokeClassSuper(String methodName, ThreadContext context, IRubyObject self, IRubyObject definingModule, IRubyObject[] args, Block block) throws Throwable {
        RubyClass superClass = definingModule.getMetaClass().getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;

        Object rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                : method.call(context, self, superClass, methodName, args, block);

        return (IRubyObject)rVal;
    }

    public static IRubyObject invokeInstanceSuper(InvokeSite site, String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        args = IRRuntimeHelpers.splatArguments(args, splatMap);
        RubyClass superClass = definingModule.getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        IRubyObject rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                : method.call(context, self, superClass, methodName, args, block);
        return rVal;
    }

    public static IRubyObject invokeInstanceSuper(InvokeSite site, String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        RubyClass superClass = definingModule.getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        IRubyObject rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, IRubyObject.NULL_ARRAY, block)
                : method.call(context, self, superClass, methodName, IRubyObject.NULL_ARRAY, block);
        return rVal;
    }

    public static IRubyObject invokeClassSuper(InvokeSite site, String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        args = IRRuntimeHelpers.splatArguments(args, splatMap);
        RubyClass superClass = definingModule.getMetaClass().getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        IRubyObject rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                : method.call(context, self, superClass, methodName, args, block);
        return rVal;
    }

    public static IRubyObject invokeClassSuper(InvokeSite site, String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        RubyClass superClass = definingModule.getMetaClass().getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        IRubyObject rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, IRubyObject.NULL_ARRAY, block)
                : method.call(context, self, superClass, methodName, IRubyObject.NULL_ARRAY, block);
        return rVal;
    }

    public static IRubyObject invokeUnresolvedSuper(InvokeSite site, String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        args = IRRuntimeHelpers.splatArguments(args, splatMap);
        return IRRuntimeHelpers.unresolvedSuper(context, self, args, block);
    }

    public static IRubyObject invokeUnresolvedSuper(InvokeSite site, String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, Block block) throws Throwable {
        return IRRuntimeHelpers.unresolvedSuper(context, self, IRubyObject.NULL_ARRAY, block);
    }

    public static IRubyObject attrAssign(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.NORMAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.NORMAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

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

    private static MethodHandle getHandle(RubyClass selfClass, String fallbackName, SwitchPoint switchPoint, InvokeSite site, DynamicMethod method, boolean block) throws Throwable {
        MethodHandle mh = null;
        SmartBinder binder = null;
        if (method.getNativeCall() != null) {
            int nativeArgCount = getNativeArgCount(method, method.getNativeCall());

            DynamicMethod.NativeCall nc = method.getNativeCall();

            if (nc.isJava()) {
                // not supported yet, use DynamicMethod.call
            } else {
                if (nativeArgCount >= 0) { // native methods only support arity 3
                    if (nativeArgCount == site.arity) {
                        // nothing to do
                        binder = SmartBinder.from(lookup(), site.signature);
                    } else {
                        // arity mismatch...leave null and use DynamicMethod.call below
                    }
                } else {
                    // varargs
                    if (site.arity == -1) {
                        // ok, already passing []
                        binder = SmartBinder.from(lookup(), site.signature);
                    } else if (site.arity == 0) {
                        // no args, insert dummy
                        binder = SmartBinder.from(lookup(), site.signature)
                                .insert(2, "args", IRubyObject.NULL_ARRAY);
                    } else {
                        // 1 or more args, collect into []
                        binder = SmartBinder.from(lookup(), site.signature)
                                .collect("args", "arg.*");
                    }
                }

                if (binder != null) {

                    // clean up non-arguments, ordering, types
                    if (!nc.hasContext()) {
                        binder = binder.drop("context");
                    }

                    if (nc.hasBlock() && !block) {
                        binder = binder.append("block", Block.NULL_BLOCK);
                    } else if (!nc.hasBlock() && block) {
                        binder = binder.drop("block");
                    }

                    if (nc.isStatic()) {
                        mh = binder
                                .permute("context", "self", "arg.*", "block") // filter caller
                                .cast(nc.getNativeReturn(), nc.getNativeSignature())
                                .invokeStaticQuiet(LOOKUP, nc.getNativeTarget(), nc.getNativeName())
                                .handle();
                    } else {
                        mh = binder
                                .permute("self", "context", "arg.*", "block") // filter caller, move self
                                .castArg("self", nc.getNativeTarget())
                                .castVirtual(nc.getNativeReturn(), nc.getNativeTarget(), nc.getNativeSignature())
                                .invokeVirtualQuiet(LOOKUP, nc.getNativeName())
                                .handle();
                    }
                }
            }
        }

        // attempt IR direct binding
        // TODO: this will have to expand when we start specializing arities
        CompiledIRMethod compiledIRMethod = null;
        if (mh == null) {
            if (method instanceof CompiledIRMethod) {
                compiledIRMethod = (CompiledIRMethod)method;
            } else if (method instanceof InterpretedIRMethod) {
                DynamicMethod actualMethod = ((InterpretedIRMethod)method).getActualMethod();
                if (actualMethod instanceof CompiledIRMethod) {
                    compiledIRMethod = (CompiledIRMethod)actualMethod;
                }
            }

            if (compiledIRMethod != null) {
                assert compiledIRMethod.hasExplicitCallProtocol() : "all jitted methods must have call protocol";

                mh = (MethodHandle)compiledIRMethod.getHandle();

                binder = SmartBinder.from(site.signature)
                        .drop("caller");

                // IR compiled methods only support varargs right now
                if (site.arity == -1) {
                    // already [], nothing to do
                } else if (site.arity == 0) {
                    binder = binder.insert(2, "args", IRubyObject.NULL_ARRAY);
                } else {
                    binder = binder.collect("args", "arg.*");
                }

                if (!block) {
                    binder = binder.append("block", Block.class, Block.NULL_BLOCK);
                }

                binder = binder
                        .insert(1, "scope", StaticScope.class, compiledIRMethod.getStaticScope())
                        .append("class", RubyModule.class, compiledIRMethod.getImplementationClass());

                mh = binder.invoke(mh).handle();
            }
        }

        if (mh == null) {
            // use DynamicMethod binding
            binder = SmartBinder.from(site.signature)
                    .drop("caller")
                    .insert(2, new String[]{"rubyClass", "name"}, new Class[]{RubyModule.class, String.class}, selfClass, site.name)
                    .insert(0, "method", DynamicMethod.class, method);

            if (site.arity > 3) {
                binder = binder.collect("args", "arg.*");
            }
              
            mh = binder.invokeVirtualQuiet(LOOKUP, "call").handle();
        }

        SmartBinder fallbackBinder = SmartBinder
                .from(site.signature);
        MethodHandle fallback;
        if (site.arity > 3) {
            // fallbacks only support up to three arity
            fallbackBinder = fallbackBinder.collect("args", "arg.*");
        }
        fallback = fallbackBinder
                .insert(0, "site", site)
                .invokeStatic(LOOKUP, Bootstrap.class, fallbackName).handle();

        if (mh == null) {
            return fallback;
        } else {
            MethodHandle test = SmartBinder
                    .from(site.signature.changeReturn(boolean.class))
                    .permute("self")
                    .insert(0, "selfClass", RubyClass.class, selfClass)
                    .invokeStatic(LOOKUP, Bootstrap.class, "testType").handle();
            mh = MethodHandles.guardWithTest(test, mh, fallback);
            mh = switchPoint.guardWithTest(mh, fallback);
        }

        return mh;
    }

    public static int getNativeArgCount(DynamicMethod method, DynamicMethod.NativeCall nativeCall) {
        // if non-Java, must:
        // * exactly match arities or both are [] boxed
        // * 3 or fewer arguments
        return getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic());
    }

    private static int getArgCount(Class[] args, boolean isStatic) {
        int length = args.length;
        boolean hasContext = false;
        if (isStatic) {
            if (args.length > 1 && args[0] == ThreadContext.class) {
                length--;
                hasContext = true;
            }

            // remove self object
            assert args.length >= 1;
            length--;

            if (args.length > 1 && args[args.length - 1] == Block.class) {
                length--;
            }

            if (length == 1) {
                if (hasContext && args[2] == IRubyObject[].class) {
                    length = -1;
                } else if (args[1] == IRubyObject[].class) {
                    length = -1;
                }
            }
        } else {
            if (args.length > 0 && args[0] == ThreadContext.class) {
                length--;
                hasContext = true;
            }

            if (args.length > 0 && args[args.length - 1] == Block.class) {
                length--;
            }

            if (length == 1) {
                if (hasContext && args[1] == IRubyObject[].class) {
                    length = -1;
                } else if (args[0] == IRubyObject[].class) {
                    length = -1;
                }
            }
        }
        return length;
    }

    private static int getRubyArgCount(Class[] args) {
        int length = args.length;
        boolean hasContext = false;

        // remove script object
        length--;

        if (args.length > 2 && args[1] == ThreadContext.class) {
            length--;
            hasContext = true;
        }

        // remove self object
        assert args.length >= 2;
        length--;

        if (args.length > 2 && args[args.length - 1] == Block.class) {
            length--;
        }

        if (length == 1) {
            if (hasContext && args[3] == IRubyObject[].class) {
                length = -1;
            } else if (args[2] == IRubyObject[].class) {
                length = -1;
            }
        }

        return length;
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        return self.getMetaClass().finvoke(context, self, site.name, block);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self) {
        return self.getMetaClass().finvoke(context, self, site.name);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) {
        return self.getMetaClass().finvoke(context, self, site.name, arg0);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        return self.getMetaClass().finvoke(context, self, site.name, arg0, arg1);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1, arg2);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1, arg2);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getMetaClass().finvoke(context, self, site.name, arg0, arg1, arg2);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args) throws Throwable {
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, args);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, false);

        site.setTarget(mh);
        // FIXME: this varargs path needs to splat to call against handle
        return method.call(context, self, selfClass, methodName, args);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args) {
        return self.getMetaClass().finvoke(context, self, site.name, args);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, block);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().finvoke(context, self, site.name, arg0, block);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1, block);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().finvoke(context, self, site.name, arg0, arg1, block);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, arg0, arg1, arg2);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        return (IRubyObject)mh.invokeWithArguments(context, caller, self, arg0, arg1, arg2, block);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().finvoke(context, self, site.name, arg0, arg1, arg2, block);
    }

    public static IRubyObject invokeSelf(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        RubyClass selfClass = self.getMetaClass();
        String methodName = site.name;
        SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
        CacheEntry entry = selfClass.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(entry, CallType.FUNCTIONAL, methodName, caller)) {
            return callMethodMissing(entry, CallType.FUNCTIONAL, context, self, methodName, args);
        }

        MethodHandle mh = getHandle(selfClass, "invokeSelfSimple", switchPoint, site, method, true);

        site.setTarget(mh);
        // FIXME: this varargs path needs to splat to call against handle
        return method.call(context, self, selfClass, methodName, args, block);
    }

    public static IRubyObject invokeSelfSimple(InvokeSite site, ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        // TODO: literal block wrapper for break, etc
        return self.getMetaClass().finvoke(context, self, site.name, args, block);
    }

    public static IRubyObject ivarGet(VariableSite site, IRubyObject self) throws Throwable {
        VariableAccessor accessor = self.getMetaClass().getRealClass().getVariableAccessorForRead(site.name);

        // produce nil if the variable has not been initialize
        MethodHandle nullToNil = findStatic(Bootstrap.class, "instVarNullToNil", methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class, String.class));
        IRubyObject nil = self.getRuntime().getNil();
        nullToNil = insertArguments(nullToNil, 1, nil, site.name);
        nullToNil = explicitCastArguments(nullToNil, methodType(IRubyObject.class, Object.class));

        // get variable value and filter with nullToNil
        MethodHandle getValue = findVirtual(IRubyObject.class, "getVariable", methodType(Object.class, int.class));
        getValue = insertArguments(getValue, 1, accessor.getIndex());
        getValue = filterReturnValue(getValue, nullToNil);

        // prepare fallback
        MethodHandle fallback = null;
        if (site.getTarget() == null || site.chainCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
//            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tget triggered site rebind " + self.getMetaClass().id);
            fallback = findStatic(Bootstrap.class, "ivarGet", methodType(IRubyObject.class, VariableSite.class, IRubyObject.class));
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

        return Bootstrap.instVarNullToNil((IRubyObject)accessor.get(self), nil, site.name);
    }

    public static void ivarSet(VariableSite site, IRubyObject self, IRubyObject value) throws Throwable {
        VariableAccessor accessor = self.getMetaClass().getRealClass().getVariableAccessorForWrite(site.name);

        // set variable value and fold by returning value
        MethodHandle setValue = findVirtual(IRubyObject.class, "setVariable", methodType(void.class, int.class, Object.class));
        setValue = explicitCastArguments(setValue, methodType(void.class, IRubyObject.class, int.class, IRubyObject.class));
        setValue = insertArguments(setValue, 1, accessor.getIndex());

        // prepare fallback
        MethodHandle fallback = null;
        if (site.getTarget() == null || site.chainCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
//            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name + "\tset triggered site rebind " + self.getMetaClass().id);
            fallback = findStatic(Bootstrap.class, "ivarSet", methodType(void.class, VariableSite.class, IRubyObject.class, IRubyObject.class));
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

        accessor.set(self, value);
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

    public static boolean testType(RubyClass original, IRubyObject self) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
    }

    ///////////////////////////////////////////////////////////////////////////
    // constant lookup

    public static IRubyObject searchConst(ThreadContext context, StaticScope staticScope, MutableCallSite site, String constName, boolean noPrivateConsts) throws Throwable {

        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        IRubyObject constant = (staticScope == null) ? object.getConstant(constName) : staticScope.getConstantInner(constName);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);
        }

        // Call const_missing or cache
        if (constant == null) {
            return module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(constName));
        }

        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = Binder.from(site.type())
                .append(site, constName)
                .append(noPrivateConsts)
                .invokeStatic(LOOKUP, Bootstrap.class, "searchConst");

        site.setTarget(switchPoint.guardWithTest(target, fallback));

        return constant;
    }

    public static IRubyObject inheritanceSearchConst(ThreadContext context, IRubyObject cmVal, MutableCallSite site, String constName, boolean noPrivateConsts) throws Throwable {
        Ruby runtime = context.runtime;
        RubyModule module;

        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw runtime.newTypeError(cmVal + " is not a type/class");
        }

        IRubyObject constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = Binder.from(site.type())
                .append(site, constName)
                .append(noPrivateConsts)
                .invokeStatic(LOOKUP, Bootstrap.class, "inheritanceSearchConst");

        // test that module is same as before
        MethodHandle test = Binder.from(site.type().changeReturnType(boolean.class))
                .drop(0, 1)
                .insert(1, module.id)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "testArg0ModuleMatch");
        target = guardWithTest(test, target, fallback);
        site.setTarget(switchPoint.guardWithTest(target, fallback));

        return constant;
    }

    public static IRubyObject lexicalSearchConst(ThreadContext context, StaticScope scope, MutableCallSite site, String constName, boolean noPrivateConsts) throws Throwable {
        Ruby runtime = context.runtime;

        IRubyObject constant = scope.getConstantInner(constName);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = Binder.from(site.type())
                .append(site, constName)
                .append(noPrivateConsts)
                .invokeStatic(LOOKUP, Bootstrap.class, "lexicalSearchConst");

        site.setTarget(switchPoint.guardWithTest(target, fallback));

        return constant;
    }

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
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "symbol");
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
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "fixnum");
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
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "flote");
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

    public static IRubyObject instVarNullToNil(IRubyObject value, IRubyObject nil, String name) {
        if (value == null) {
            Ruby runtime = nil.getRuntime();
            if (runtime.isVerbose()) {
                nil.getRuntime().getWarnings().warning(IRubyWarnings.ID.IVAR_NOT_INITIALIZED, "instance variable " + name + " not initialized");
            }
            return nil;
        }
        return value;
    }

    public static boolean testArg0ModuleMatch(IRubyObject arg0, int id) {
        return arg0 instanceof RubyModule && ((RubyModule)arg0).id == id;
    }
}
