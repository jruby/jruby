/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.methods.*;
import org.jruby.ir.JIT;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.java.invokers.SingletonMethodInvoker;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.runtime.invokedynamic.GlobalSite;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.runtime.invokedynamic.MathLinker;
import org.jruby.runtime.ivars.FieldVariableAccessor;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.specialized.RubyArraySpecialized;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.*;
import java.math.BigInteger;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.Helpers.constructObjectArrayHandle;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.findStatic;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.findVirtual;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class Bootstrap {
    public final static String BOOTSTRAP_BARE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class);
    public final static String BOOTSTRAP_LONG_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class, int.class, String.class, int.class);
    public final static String BOOTSTRAP_DOUBLE_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, double.class, int.class, String.class, int.class);
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    static final Lookup LOOKUP = MethodHandles.lookup();
    public static final Handle EMPTY_STRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(Bootstrap.class),
            "emptyString",
            sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class),
            false);
    private static final String[] GENERIC_CALL_PERMUTE = {"context", "self", "arg.*"};

    public static CallSite string(Lookup lookup, String name, MethodType type, String value, String encodingName, int cr) {
        return new ConstantCallSite(insertArguments(STRING_HANDLE, 1, bytelist(value, encodingName), cr));
    }

    private static final MethodHandle STRING_HANDLE =
            Binder
                    .from(RubyString.class, ThreadContext.class, ByteList.class, int.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "string");

    public static CallSite fstring(Lookup lookup, String name, MethodType type, String value, String encodingName, int cr, String file, int line) {
        MutableCallSite site = new MutableCallSite(type);

        site.setTarget(insertArguments(FSTRING_HANDLE, 1, site, bytelist(value, encodingName), cr, file, line));

        return site;
    }

    private static final MethodHandle FSTRING_HANDLE =
            Binder
                    .from(RubyString.class, ThreadContext.class, MutableCallSite.class, ByteList.class, int.class, String.class, int.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "frozenString");

    public static CallSite emptyString(Lookup lookup, String name, MethodType type, String encodingName) {
        RubyString.EmptyByteListHolder holder = RubyString.getEmptyByteList(encodingFromName(encodingName));
        return new ConstantCallSite(insertArguments(STRING_HANDLE, 1, holder.bytes, holder.cr));
    }

    public static Handle isNilBoot() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "isNil",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class),
                false);
    }

    public static CallSite isNil(Lookup lookup, String name, MethodType type) {
        return new IsNilSite();
    }

    public static class IsNilSite extends MutableCallSite {

        public static final MethodType TYPE = methodType(boolean.class, IRubyObject.class);

        private static final MethodHandle INIT_HANDLE =
                Binder.from(TYPE.insertParameterTypes(0, IsNilSite.class)).invokeVirtualQuiet(LOOKUP, "init");

        private static final MethodHandle IS_NIL_HANDLE =
                Binder
                        .from(boolean.class, IRubyObject.class, RubyNil.class)
                        .invokeStaticQuiet(LOOKUP, IsNilSite.class, "isNil");

        public IsNilSite() {
            super(TYPE);

            setTarget(INIT_HANDLE.bindTo(this));
        }

        public boolean init(IRubyObject obj) {
            IRubyObject nil = obj.getRuntime().getNil();

            setTarget(insertArguments(IS_NIL_HANDLE, 1, nil));

            return nil == obj;
        }

        public static boolean isNil(IRubyObject obj, RubyNil nil) {
            return nil == obj;
        }
    }

    public static Handle isTrueBoot() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "isTrue",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class),
                false);
    }

    public static CallSite isTrue(Lookup lookup, String name, MethodType type) {
        return new IsTrueSite();
    }

    public static class IsTrueSite extends MutableCallSite {

        public static final MethodType TYPE = methodType(boolean.class, IRubyObject.class);

        public static final MethodHandle INIT_HANDLE = Binder.from(TYPE.insertParameterTypes(0, IsTrueSite.class)).invokeVirtualQuiet(LOOKUP, "init");

        private static final MethodHandle IS_TRUE_HANDLE =
                Binder
                        .from(boolean.class, IRubyObject.class, RubyNil.class, RubyBoolean.False.class)
                        .invokeStaticQuiet(LOOKUP, IsTrueSite.class, "isTruthy");

        public IsTrueSite() {
            super(TYPE);

            setTarget(INIT_HANDLE.bindTo(this));
        }

        public boolean init(IRubyObject obj) {
            Ruby runtime = obj.getRuntime();

            IRubyObject nil = runtime.getNil();
            IRubyObject fals = runtime.getFalse();

            setTarget(insertArguments(IS_TRUE_HANDLE, 1, nil, fals));

            return nil != obj && fals != obj;
        }

        public static boolean isTruthy(IRubyObject obj, RubyNil nil, RubyBoolean.False fals) {
            return nil != obj && fals != obj;
        }
    }

    public static CallSite bytelist(Lookup lookup, String name, MethodType type, String value, String encodingName) {
        return new ConstantCallSite(constant(ByteList.class, bytelist(value, encodingName)));
    }

    public static ByteList bytelist(String value, String encodingName) {
        Encoding encoding = encodingFromName(encodingName);

        if (value.length() == 0) {
            // special case empty string and don't create a new BL
            return RubyString.getEmptyByteList(encoding).bytes;
        }

        return new ByteList(RubyEncoding.encodeISO(value), encoding, false);
    }

    private static Encoding encodingFromName(String encodingName) {
        Encoding encoding;
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(encodingName.getBytes());
        if (entry == null) entry = EncodingDB.getAliases().get(encodingName.getBytes());
        if (entry == null) throw new RuntimeException("could not find encoding: " + encodingName);
        encoding = entry.getEncoding();
        return encoding;
    }

    public static final Handle CALLSITE = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(Bootstrap.class),
            "callSite",
            sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class),
            false);

    public static CallSite callSite(Lookup lookup, String name, MethodType type, String id, int callType) {
        return new ConstantCallSite(constant(CachingCallSite.class, callSite(id, callType)));
    }

    private static CachingCallSite callSite(String id, int callType) {
        switch (CallType.fromOrdinal(callType)) {
            case NORMAL:
                return new MonomorphicCallSite(id);
            case FUNCTIONAL:
                return new FunctionalCachingCallSite(id);
            case VARIABLE:
                return new VariableCachingCallSite(id);
            default:
                throw new RuntimeException("BUG: Unexpected call type " + callType + " in JVM6 invoke logic");
        }
    }

    public static final Handle OPEN_META_CLASS = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(Bootstrap.class),
            "openMetaClass",
            sig(CallSite.class, Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, MethodHandle.class, int.class, int.class, int.class),
            false);

    @JIT
    public static CallSite openMetaClass(Lookup lookup, String name, MethodType type, MethodHandle body, MethodHandle scope, MethodHandle setScope, int line, int dynscopeEliminated, int refinements) {
        try {
            StaticScope staticScope = (StaticScope) scope.invokeExact();
            return new ConstantCallSite(insertArguments(OPEN_META_CLASS_HANDLE, 4, body, staticScope, setScope, line, dynscopeEliminated == 1 ? true : false, refinements == 1 ? true : false));
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    private static final MethodHandle OPEN_META_CLASS_HANDLE =
            Binder
                    .from(DynamicMethod.class, ThreadContext.class, IRubyObject.class, String.class, StaticScope.class, MethodHandle.class, StaticScope.class, MethodHandle.class, int.class, boolean.class, boolean.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "openMetaClass");

    @JIT
    public static DynamicMethod openMetaClass(ThreadContext context, IRubyObject object, String descriptor, StaticScope parent, MethodHandle body, StaticScope scope, MethodHandle setScope, int line, boolean dynscopeEliminated, boolean refinements) throws Throwable {
        if (scope == null) {
            scope = Helpers.restoreScope(descriptor, parent);
            setScope.invokeExact(scope);
        }
        return IRRuntimeHelpers.newCompiledMetaClass(context, body, scope, object, line, dynscopeEliminated, refinements);
    }

    public static final Handle CHECK_ARITY = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(Bootstrap.class),
            "checkArity",
            sig(CallSite.class, Lookup.class, String.class, MethodType.class, int.class, int.class, int.class, int.class, int.class),
            false);

    @JIT
    public static CallSite checkArity(Lookup lookup, String name, MethodType type, int req, int opt, int rest, int key, int keyrest) {
        return new ConstantCallSite(insertArguments(CHECK_ARITY_HANDLE, 4, req, opt, rest == 0 ? false : true, key == 0 ? false : true, keyrest));
    }

    private static final MethodHandle CHECK_ARITY_HANDLE =
            Binder
                    .from(void.class, ThreadContext.class, StaticScope.class, Object[].class, Block.class, int.class, int.class, boolean.class, boolean.class, int.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "checkArity");

    @JIT
    public static void checkArity(ThreadContext context, StaticScope scope, Object[] args, Block block, int req, int opt, boolean rest, boolean key, int keyrest) {
        IRRuntimeHelpers.checkArity(context, scope, args, req, opt, rest, key, keyrest, block);
    }

    public static CallSite array(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(1, IRubyObject[].class)
                .invoke(ARRAY_HANDLE);

        return new ConstantCallSite(handle);
    }

    private static final MethodHandle ARRAY_HANDLE =
            Binder
                    .from(RubyArray.class, ThreadContext.class, IRubyObject[].class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "array");

    public static CallSite hash(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(lookup, type)
                .collect(1, IRubyObject[].class)
                .invoke(HASH_HANDLE);

        return new ConstantCallSite(handle);
    }

    private static final MethodHandle HASH_HANDLE =
            Binder
                    .from(RubyHash.class, ThreadContext.class, IRubyObject[].class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "hash");

    public static CallSite kwargsHash(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(lookup, type)
                .collect(2, IRubyObject[].class)
                .invoke(KWARGS_HASH_HANDLE);

        return new ConstantCallSite(handle);
    }

    private static final MethodHandle KWARGS_HASH_HANDLE =
            Binder
                    .from(RubyHash.class, ThreadContext.class, RubyHash.class, IRubyObject[].class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "kwargsHash");

    public static Handle string() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "string",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class, int.class),
                false);
    }

    public static Handle fstring() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "fstring",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class, int.class, String.class, int.class),
                false);
    }

    public static Handle bytelist() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "bytelist",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class),
                false);
    }

    public static Handle array() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "array",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class),
                false);
    }

    public static Handle hash() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "hash",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class),
                false);
    }

    public static Handle kwargsHash() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "kwargsHash",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class),
                false);
    }

    public static Handle invokeSuper() {
        return SuperInvokeSite.BOOTSTRAP;
    }

    public static Handle global() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "globalBootstrap",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class),
                false);
    }

    public static RubyString string(ThreadContext context, ByteList value, int cr) {
        return RubyString.newStringShared(context.runtime, value, cr);
    }

    public static RubyString frozenString(ThreadContext context, MutableCallSite site, ByteList value, int cr, String file, int line) {
        RubyString frozen = IRRuntimeHelpers.newFrozenString(context, value, cr, file, line);

        // Permanently bind to the new frozen string
        site.setTarget(dropArguments(constant(RubyString.class, frozen), 0, ThreadContext.class));

        return frozen;
    }

    public static RubyArray array(ThreadContext context, IRubyObject[] ary) {
        assert ary.length > RubyArraySpecialized.MAX_PACKED_SIZE;
        // Bootstrap.array() only dispatches here if ^^ holds
        return RubyArray.newArrayNoCopy(context.runtime, ary);
    }

    public static Handle contextValue() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "contextValue",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class),
                false);
    }

    public static Handle contextValueString() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "contextValueString",
                sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class),
                false);
    }

    public static CallSite contextValue(Lookup lookup, String name, MethodType type) {
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
            case "encoding":
                dmh = ENCODING_HANDLE;
                break;
            default:
                throw new RuntimeException("BUG: invalid context value " + name);
        }

        site.setTarget(Binder.from(type).append(site).invoke(dmh));

        return site;
    }

    public static CallSite contextValueString(Lookup lookup, String name, MethodType type, String str) {
        MutableCallSite site = new MutableCallSite(type);

        MethodHandle dmh;
        switch (name) {
            case "encoding":
                dmh = ENCODING_HANDLE;
                break;
            default:
                throw new RuntimeException("BUG: invalid context value " + name);
        }

        site.setTarget(Binder.from(type).append(site, str).invoke(dmh));
        return site;
    }

    private static final MethodHandle RUNTIME_HANDLE =
            Binder
                    .from(Ruby.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "runtime");

    private static final MethodHandle NIL_HANDLE =
            Binder
                    .from(IRubyObject.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "nil");

    private static final MethodHandle TRUE_HANDLE =
            Binder
                    .from(IRubyObject.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "True");

    private static final MethodHandle FALSE_HANDLE =
            Binder
                    .from(IRubyObject.class, ThreadContext.class, MutableCallSite.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "False");

    private static final MethodHandle ENCODING_HANDLE =
            Binder
                    .from(RubyEncoding.class, ThreadContext.class, MutableCallSite.class, String.class)
                    .invokeStaticQuiet(LOOKUP, Bootstrap.class, "encoding");

    public static IRubyObject nil(ThreadContext context, MutableCallSite site) {
        RubyNil nil = (RubyNil) context.nil;

        MethodHandle constant = (MethodHandle) nil.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(IRubyObject.class, context.nil);

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

    public static RubyEncoding encoding(ThreadContext context, MutableCallSite site, String name) {
        RubyEncoding rubyEncoding = IRRuntimeHelpers.retrieveEncoding(context, name);

        MethodHandle constant = (MethodHandle)rubyEncoding.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(RubyEncoding.class, rubyEncoding);

        site.setTarget(constant);

        return rubyEncoding;
    }

    public static RubyHash hash(ThreadContext context, IRubyObject[] pairs) {
        Ruby runtime = context.runtime;
        RubyHash hash = new RubyHash(runtime, pairs.length / 2 + 1);
        for (int i = 0; i < pairs.length;) {
            hash.fastASetCheckString(runtime, pairs[i++], pairs[i++]);
        }
        return hash;
    }

    public static RubyHash kwargsHash(ThreadContext context, RubyHash hash, IRubyObject[] pairs) {
        return IRRuntimeHelpers.dupKwargsHashAndPopulateFromArray(context, hash, pairs);
    }

    static MethodHandle buildIndyHandle(InvokeSite site, CacheEntry entry) {
        MethodHandle mh = null;
        Signature siteToDyncall = site.signature.insertArgs(3, arrayOf("class", "name"), arrayOf(RubyModule.class, String.class));
        DynamicMethod method = entry.method;

        if (method instanceof HandleMethod) {
            HandleMethod handleMethod = (HandleMethod)method;
            boolean blockGiven = site.signature.lastArgType() == Block.class;

            if (site.arity >= 0) {
                mh = handleMethod.getHandle(site.arity);
                if (mh != null) {
                    if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);
                    mh = dropArguments(mh, 1, IRubyObject.class);
                } else {
                    mh = handleMethod.getHandle(-1);
                    mh = dropArguments(mh, 1, IRubyObject.class);
                    if (site.arity == 0) {
                        if (!blockGiven) {
                            mh = insertArguments(mh, mh.type().parameterCount() - 2, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
                        } else {
                            mh = insertArguments(mh, mh.type().parameterCount() - 2, (Object)IRubyObject.NULL_ARRAY);
                        }
                    } else {
                        // bundle up varargs
                        if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);

                        mh = SmartBinder.from(lookup(), siteToDyncall)
                                .collect("args", "arg.*", Helpers.constructObjectArrayHandle(site.arity))
                                .invoke(mh)
                                .handle();
                    }
                }
            } else {
                mh = handleMethod.getHandle(-1);
                if (mh != null) {
                    mh = dropArguments(mh, 1, IRubyObject.class);
                    if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);

                    mh = SmartBinder.from(lookup(), siteToDyncall)
                            .invoke(mh)
                            .handle();
                }
            }

            if (mh != null) {
                mh = insertArguments(mh, 3, entry.sourceModule, site.name());

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(site.name() + "\tbound directly to handle " + Bootstrap.logMethod(method));
                }
            }
        }

        return mh;
    }

    static MethodHandle buildGenericHandle(InvokeSite site, CacheEntry entry) {
        SmartBinder binder;
        DynamicMethod method = entry.method;

        binder = SmartBinder.from(site.signature);

        binder = permuteForGenericCall(binder, method, GENERIC_CALL_PERMUTE);

        binder = binder
                .insert(2, new String[]{"rubyClass", "name"}, new Class[]{RubyModule.class, String.class}, entry.sourceModule, site.name())
                .insert(0, "method", DynamicMethod.class, method);

        if (site.arity > 3) {
            binder = binder.collect("args", "arg.*", constructObjectArrayHandle(site.arity));
        }

        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
            LOG.info(site.name() + "\tbound indirectly " + method + ", " + Bootstrap.logMethod(method));
        }

        return binder.invokeVirtualQuiet(LOOKUP, "call").handle();
    }

    private static SmartBinder permuteForGenericCall(SmartBinder binder, DynamicMethod method, String... basePermutes) {
        if (methodWantsBlock(method)) {
            binder = binder.permute(arrayOf(basePermutes, "block", String[]::new));
        } else {
            binder = binder.permute(basePermutes);
        }
        return binder;
    }

    private static boolean methodWantsBlock(DynamicMethod method) {
        // only include block if native signature receives block, whatever its arity
        boolean wantsBlock = true;
        if (method instanceof NativeCallMethod) {
            DynamicMethod.NativeCall nativeCall = ((NativeCallMethod) method).getNativeCall();
            if (nativeCall != null) {
                Class[] nativeSignature = nativeCall.getNativeSignature();

                // no args or last arg not a block, do no pass block
                if (nativeSignature.length == 0 || nativeSignature[nativeSignature.length - 1] != Block.class) {
                    wantsBlock = false;
                }
            }
        }
        return wantsBlock;
    }

    static MethodHandle buildMethodMissingHandle(InvokeSite site, CacheEntry entry, IRubyObject self) {
        SmartBinder binder;
        DynamicMethod method = entry.method;

        if (site.arity >= 0) {
            binder = SmartBinder.from(site.signature);

            binder = permuteForGenericCall(binder, method, GENERIC_CALL_PERMUTE)
                    .insert(2,
                            new String[]{"rubyClass", "name", "argName"}
                            , new Class[]{RubyModule.class, String.class, IRubyObject.class},
                            entry.sourceModule,
                            site.name(),
                            self.getRuntime().newSymbol(site.methodName))
                    .insert(0, "method", DynamicMethod.class, method)
                    .collect("args", "arg.*", Helpers.constructObjectArrayHandle(site.arity + 1));
        } else {
            SmartHandle fold = SmartBinder.from(
                    site.signature
                            .permute("context", "self", "args", "block")
                            .changeReturn(IRubyObject[].class))
                    .permute("args")
                    .insert(0, "argName", IRubyObject.class, self.getRuntime().newSymbol(site.methodName))
                    .invokeStaticQuiet(LOOKUP, Helpers.class, "arrayOf");

            binder = SmartBinder.from(site.signature);

            binder = permuteForGenericCall(binder, method, "context", "self", "args")
                    .fold("args2", fold);
            binder = permuteForGenericCall(binder, method, "context", "self", "args2")
                    .insert(2,
                            new String[]{"rubyClass", "name"}
                            , new Class[]{RubyModule.class, String.class},
                            entry.sourceModule,
                            site.name())
                    .insert(0, "method", DynamicMethod.class, method);
        }

        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
            LOG.info(site.name() + "\tbound to method_missing for " + method + ", " + Bootstrap.logMethod(method));
        }

        return binder.invokeVirtualQuiet(LOOKUP, "call").handle();
    }

    static MethodHandle buildAttrHandle(InvokeSite site, CacheEntry entry, IRubyObject self) {
        DynamicMethod method = entry.method;

        if (method instanceof AttrReaderMethod && site.arity == 0) {
            AttrReaderMethod attrReader = (AttrReaderMethod) method;
            String varName = attrReader.getVariableName();

            // we getVariableAccessorForWrite here so it is eagerly created and we don't cache the DUMMY
            VariableAccessor accessor = self.getType().getVariableAccessorForWrite(varName);

            // Ruby to attr reader
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (accessor instanceof FieldVariableAccessor) {
                    LOG.info(site.name() + "\tbound as field attr reader " + logMethod(method) + ":" + ((AttrReaderMethod)method).getVariableName());
                } else {
                    LOG.info(site.name() + "\tbound as attr reader " + logMethod(method) + ":" + ((AttrReaderMethod)method).getVariableName());
                }
            }

            return createAttrReaderHandle(site, self, self.getType(), accessor);
        } else if (method instanceof AttrWriterMethod && site.arity == 1) {
            AttrWriterMethod attrReader = (AttrWriterMethod)method;
            String varName = attrReader.getVariableName();

            // we getVariableAccessorForWrite here so it is eagerly created and we don't cache the DUMMY
            VariableAccessor accessor = self.getType().getVariableAccessorForWrite(varName);

            // Ruby to attr reader
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (accessor instanceof FieldVariableAccessor) {
                    LOG.info(site.name() + "\tbound as field attr writer " + logMethod(method) + ":" + ((AttrWriterMethod) method).getVariableName());
                } else {
                    LOG.info(site.name() + "\tbound as attr writer " + logMethod(method) + ":" + ((AttrWriterMethod) method).getVariableName());
                }
            }

            return createAttrWriterHandle(site, self, self.getType(), accessor);
        }

        return null;
    }

    private static MethodHandle createAttrReaderHandle(InvokeSite site, IRubyObject self, RubyClass cls, VariableAccessor accessor) {
        MethodHandle nativeTarget;

        MethodHandle filter = cls.getClassRuntime().getNullToNilHandle();

        MethodHandle getValue;

        if (accessor instanceof FieldVariableAccessor) {
            MethodHandle getter = ((FieldVariableAccessor)accessor).getGetter();
            getValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(Object.class, self.getClass()))
                    .invoke(getter);
        } else {
            getValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(Object.class, Object.class))
                    .prepend(accessor)
                    .invokeVirtualQuiet(LOOKUP, "get");
        }

        // NOTE: Must not cache the fully-bound handle in the method, since it's specific to this class

        return getValue;
    }

    public static IRubyObject valueOrNil(IRubyObject value, IRubyObject nil) {
        return value == null ? nil : value;
    }

    private static MethodHandle createAttrWriterHandle(InvokeSite site, IRubyObject self, RubyClass cls, VariableAccessor accessor) {
        MethodHandle nativeTarget;

        MethodHandle filter = Binder
                .from(IRubyObject.class, Object.class)
                .drop(0)
                .constant(cls.getRuntime().getNil());

        MethodHandle setValue;

        if (accessor instanceof FieldVariableAccessor) {
            MethodHandle setter = ((FieldVariableAccessor)accessor).getSetter();
            setValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(void.class, self.getClass(), Object.class))
                    .invoke(setter);
        } else {
            setValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(void.class, Object.class, Object.class))
                    .prepend(accessor)
                    .invokeVirtualQuiet(LOOKUP, "set");
        }

        return setValue;
    }

    static MethodHandle buildJittedHandle(InvokeSite site, CacheEntry entry, boolean blockGiven) {
        MethodHandle mh = null;
        SmartBinder binder;
        CompiledIRMethod compiledIRMethod = null;
        DynamicMethod method = entry.method;
        RubyModule sourceModule = entry.sourceModule;

        if (method instanceof CompiledIRMethod) {
            compiledIRMethod = (CompiledIRMethod)method;
        } else if (method instanceof MixedModeIRMethod) {
            DynamicMethod actualMethod = ((MixedModeIRMethod)method).getActualMethod();
            if (actualMethod instanceof CompiledIRMethod) {
                compiledIRMethod = (CompiledIRMethod) actualMethod;
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(site.name() + "\tfailed direct binding due to unjitted method " + Bootstrap.logMethod(method));
                }
            }
        }

        if (compiledIRMethod != null) {

            // attempt IR direct binding
            // TODO: this will have to expand when we start specializing arities

            binder = SmartBinder.from(site.signature)
                    .permute("context", "self", "arg.*", "block");

            if (site.arity == -1) {
                // already [], nothing to do
                mh = (MethodHandle)compiledIRMethod.getHandle();
            } else if (site.arity == 0) {
                MethodHandle specific;
                if ((specific = compiledIRMethod.getHandleFor(site.arity)) != null) {
                    mh = specific;
                } else {
                    mh = (MethodHandle)compiledIRMethod.getHandle();
                    binder = binder.insert(2, "args", IRubyObject.NULL_ARRAY);
                }
            } else {
                MethodHandle specific;
                if ((specific = compiledIRMethod.getHandleFor(site.arity)) != null) {
                    mh = specific;
                } else {
                    mh = (MethodHandle) compiledIRMethod.getHandle();
                    binder = binder.collect("args", "arg.*", Helpers.constructObjectArrayHandle(site.arity));
                }
            }

            if (!blockGiven) {
                binder = binder.append("block", Block.class, Block.NULL_BLOCK);
            }

            binder = binder
                    .insert(1, "scope", StaticScope.class, compiledIRMethod.getStaticScope())
                    .append("class", RubyModule.class, sourceModule)
                    .append("frameName", String.class, site.name());

            mh = binder.invoke(mh).handle();

            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(site.name() + "\tbound directly to jitted method " + Bootstrap.logMethod(method));
            }
        }

        return mh;
    }

    static MethodHandle buildNativeHandle(InvokeSite site, CacheEntry entry, boolean blockGiven) {
        MethodHandle mh = null;
        SmartBinder binder = null;
        DynamicMethod method = entry.method;

        if (method instanceof NativeCallMethod && ((NativeCallMethod) method).getNativeCall() != null) {
            NativeCallMethod nativeMethod = (NativeCallMethod)method;
            DynamicMethod.NativeCall nativeCall = nativeMethod.getNativeCall();

            DynamicMethod.NativeCall nc = nativeCall;

            if (nc.isJava()) {
                return createJavaHandle(site, method);
            } else {
                int nativeArgCount = getNativeArgCount(method, nativeCall);

                if (nativeArgCount >= 0) { // native methods only support arity 3
                    if (nativeArgCount == site.arity) {
                        // nothing to do
                        binder = SmartBinder.from(lookup(), site.signature);
                    } else {
                        // arity mismatch...leave null and use DynamicMethod.call below
                        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                            LOG.info(site.name() + "\tdid not match the primary arity for a native method " + Bootstrap.logMethod(method));
                        }
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
                                .collect("args", "arg.*", Helpers.constructObjectArrayHandle(site.arity));
                    }
                }

                if (binder != null) {

                    // clean up non-arguments, ordering, types
                    if (!nc.hasContext()) {
                        binder = binder.drop("context");
                    }

                    if (nc.hasBlock() && !blockGiven) {
                        binder = binder.append("block", Block.NULL_BLOCK);
                    } else if (!nc.hasBlock() && blockGiven) {
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

                    if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                        LOG.info(site.name() + "\tbound directly to JVM method " + Bootstrap.logMethod(method));
                    }
                }

                JRubyMethod anno = nativeCall.getMethod().getAnnotation(JRubyMethod.class);
                if (anno != null && anno.frame()) {
                    mh = InvocationLinker.wrapWithFrameOnly(site.signature, entry.sourceModule, site.name(), mh);
                }
            }
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

    public static boolean testType(RubyClass original, IRubyObject self) {
        // naive test
        return original == RubyBasicObject.getMetaClass(self);
    }


    ////////////////////////////////////////////////////////////////////////////
    // Dispatch from Ruby to Java via Java integration
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createJavaHandle(InvokeSite site, DynamicMethod method) {
        MethodHandle nativeTarget = (MethodHandle)method.getHandle();

        if (nativeTarget != null) return nativeTarget;

        MethodHandle returnFilter = null;

        Ruby runtime = method.getImplementationClass().getRuntime();

        if (!(method instanceof NativeCallMethod)) return null;

        DynamicMethod.NativeCall nativeCall = ((NativeCallMethod) method).getNativeCall();

        if (nativeCall == null) return null;

        boolean isStatic = nativeCall.isStatic();

        // This logic does not handle closure conversion yet
        if (site.fullSignature.lastArgType() == Block.class) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(site.name() + "\tpassed a closure to Java method " + nativeCall + ": " + Bootstrap.logMethod(method));
            }
            return null;
        }

        // mismatched arity not supported
        if (isStatic) {
            if (site.arity != nativeCall.getNativeSignature().length - 1) {
                return null;
            }
        } else if (site.arity != nativeCall.getNativeSignature().length) {
            return null;
        }

        // varargs broken, so ignore methods that take a trailing array
        Class[] signature = nativeCall.getNativeSignature();
        if (signature.length > 0 && signature[signature.length - 1].isArray()) {
            return null;
        }

        // Scala singletons have slightly different JI logic, so skip for now
        if (method instanceof SingletonMethodInvoker) return null;

        // the "apparent" type from the NativeCall, excluding receiver
        MethodType apparentType = methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature());

        if (isStatic) {
            nativeTarget = findStatic(nativeCall.getNativeTarget(), nativeCall.getNativeName(), apparentType);
        } else {
            nativeTarget = findVirtual(nativeCall.getNativeTarget(), nativeCall.getNativeName(), apparentType);
        }

        // the actual native type with receiver
        MethodType nativeType = nativeTarget.type();
        Class[] nativeParams = nativeType.parameterArray();
        Class nativeReturn = nativeType.returnType();

        // convert arguments
        MethodHandle[] argConverters = new MethodHandle[nativeType.parameterCount()];
        for (int i = 0; i < argConverters.length; i++) {
            MethodHandle converter;
            if (!isStatic && i == 0) {
                // handle non-static receiver specially
                converter = Binder
                        .from(nativeParams[0], IRubyObject.class)
                        .cast(Object.class, IRubyObject.class)
                        .invokeStaticQuiet(lookup(), JavaUtil.class, "objectFromJavaProxy");
            } else {
                // all other arguments use toJava
                converter = Binder
                        .from(nativeParams[i], IRubyObject.class)
                        .insert(1, nativeParams[i])
                        .cast(Object.class, IRubyObject.class, Class.class)
                        .invokeVirtualQuiet(lookup(), "toJava");
            }
            argConverters[i] = converter;
        }
        nativeTarget = filterArguments(nativeTarget, 0, argConverters);

        Class[] convertedParams = CodegenUtils.params(IRubyObject.class, nativeTarget.type().parameterCount());

        // handle return value
        if (nativeReturn == byte.class
                || nativeReturn == short.class
                || nativeReturn == char.class
                || nativeReturn == int.class
                || nativeReturn == long.class) {
            // native integral type, produce a Fixnum
            nativeTarget = explicitCastArguments(nativeTarget, methodType(long.class, convertedParams));
            returnFilter = insertArguments(
                    findStatic(RubyFixnum.class, "newFixnum", methodType(RubyFixnum.class, Ruby.class, long.class)),
                    0,
                    runtime);
        } else if (nativeReturn == Byte.class
                || nativeReturn == Short.class
                || nativeReturn == Character.class
                || nativeReturn == Integer.class
                || nativeReturn == Long.class) {
            // boxed integral type, produce a Fixnum or nil
            returnFilter = insertArguments(
                    findStatic(Bootstrap.class, "fixnumOrNil", methodType(IRubyObject.class, Ruby.class, nativeReturn)),
                    0,
                    runtime);
        } else if (nativeReturn == float.class
                || nativeReturn == double.class) {
            // native decimal type, produce a Float
            nativeTarget = explicitCastArguments(nativeTarget, methodType(double.class, convertedParams));
            returnFilter = insertArguments(
                    findStatic(RubyFloat.class, "newFloat", methodType(RubyFloat.class, Ruby.class, double.class)),
                    0,
                    runtime);
        } else if (nativeReturn == Float.class
                || nativeReturn == Double.class) {
            // boxed decimal type, produce a Float or nil
            returnFilter = insertArguments(
                    findStatic(Bootstrap.class, "floatOrNil", methodType(IRubyObject.class, Ruby.class, nativeReturn)),
                    0,
                    runtime);
        } else if (nativeReturn == boolean.class) {
            // native boolean type, produce a Boolean
            nativeTarget = explicitCastArguments(nativeTarget, methodType(boolean.class, convertedParams));
            returnFilter = insertArguments(
                    findStatic(RubyBoolean.class, "newBoolean", methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                    0,
                    runtime);
        } else if (nativeReturn == Boolean.class) {
            // boxed boolean type, produce a Boolean or nil
            returnFilter = insertArguments(
                    findStatic(Bootstrap.class, "booleanOrNil", methodType(IRubyObject.class, Ruby.class, Boolean.class)),
                    0,
                    runtime);
        } else if (CharSequence.class.isAssignableFrom(nativeReturn)) {
            // character sequence, produce a String or nil
            nativeTarget = explicitCastArguments(nativeTarget, methodType(CharSequence.class, convertedParams));
            returnFilter = insertArguments(
                    findStatic(Bootstrap.class, "stringOrNil", methodType(IRubyObject.class, Ruby.class, CharSequence.class)),
                    0,
                    runtime);
        } else if (nativeReturn == void.class) {
            // void return, produce nil
            returnFilter = constant(IRubyObject.class, runtime.getNil());
        } else if (nativeReturn == ByteList.class) {
            // not handled yet
        } else if (nativeReturn == BigInteger.class) {
            // not handled yet
        } else {
            // all other object types
            nativeTarget = explicitCastArguments(nativeTarget, methodType(Object.class, convertedParams));
            returnFilter = insertArguments(
                    findStatic(JavaUtil.class, "convertJavaToUsableRubyObject", methodType(IRubyObject.class, Ruby.class, Object.class)),
                    0,
                    runtime);
        }

        // we can handle this; do remaining transforms and return
        if (returnFilter != null) {
            Class[] newNativeParams = nativeTarget.type().parameterArray();
            Class newNativeReturn = nativeTarget.type().returnType();

            Binder exBinder = Binder
                    .from(newNativeReturn, Throwable.class, newNativeParams)
                    .drop(1, newNativeParams.length)
                    .insert(0, runtime);
            if (nativeReturn != void.class) {
                exBinder = exBinder
                        .filterReturn(Binder
                                .from(newNativeReturn)
                                .constant(nullValue(newNativeReturn)));
            }

            nativeTarget = Binder
                    .from(site.type())
                    .drop(0, isStatic ? 3 : 2)
                    .filterReturn(returnFilter)
                    .invoke(nativeTarget);

            method.setHandle(nativeTarget);
            return nativeTarget;
        }

        return null;
    }

    public static boolean subclassProxyTest(Object target) {
        return target instanceof ReifiedJavaProxy;
    }

    private static final MethodHandle IS_JAVA_SUBCLASS = findStatic(Bootstrap.class, "subclassProxyTest", methodType(boolean.class, Object.class));

    private static Object nullValue(Class type) {
        if (type == boolean.class || type == Boolean.class) return false;
        if (type == byte.class || type == Byte.class) return (byte)0;
        if (type == short.class || type == Short.class) return (short)0;
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == float.class || type == Float.class) return 0.0F;
        if (type == double.class || type == Double.class)return 0.0;
        return null;
    }

    public static IRubyObject fixnumOrNil(Ruby runtime, Byte b) {
        return b == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, b);
    }

    public static IRubyObject fixnumOrNil(Ruby runtime, Short s) {
        return s == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, s);
    }

    public static IRubyObject fixnumOrNil(Ruby runtime, Character c) {
        return c == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, c);
    }

    public static IRubyObject fixnumOrNil(Ruby runtime, Integer i) {
        return i == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, i);
    }

    public static IRubyObject fixnumOrNil(Ruby runtime, Long l) {
        return l == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, l);
    }

    public static IRubyObject floatOrNil(Ruby runtime, Float f) {
        return f == null ? runtime.getNil() : RubyFloat.newFloat(runtime, f);
    }

    public static IRubyObject floatOrNil(Ruby runtime, Double d) {
        return d == null ? runtime.getNil() : RubyFloat.newFloat(runtime, d);
    }

    public static IRubyObject booleanOrNil(Ruby runtime, Boolean b) {
        return b == null ? runtime.getNil() : RubyBoolean.newBoolean(runtime, b);
    }

    public static IRubyObject stringOrNil(Ruby runtime, CharSequence cs) {
        return cs == null ? runtime.getNil() : RubyString.newUnicodeString(runtime, cs);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fixnum binding

    public static boolean testModuleMatch(ThreadContext context, IRubyObject arg0, int id) {
        return arg0 instanceof RubyModule && ((RubyModule)arg0).id == id;
    }

    public static Handle getFixnumOperatorHandle() {
        return getBootstrapHandle("fixnumOperatorBootstrap", MathLinker.class, BOOTSTRAP_LONG_STRING_INT_SIG);  
    }

    public static Handle getFloatOperatorHandle() {
        return getBootstrapHandle("floatOperatorBootstrap", MathLinker.class, BOOTSTRAP_DOUBLE_STRING_INT_SIG);
    }

    public static Handle checkpointHandle() {
        return getBootstrapHandle("checkpointBootstrap", BOOTSTRAP_BARE_SIG);
    }

    public static Handle coverLineHandle() {
        return getBootstrapHandle("coverLineBootstrap", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class, int.class));
    }

    public static Handle getBootstrapHandle(String name, String sig) {
        return getBootstrapHandle(name, Bootstrap.class, sig);
    }

    public static Handle getBootstrapHandle(String name, Class type, String sig) {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(type),
                name,
                sig,
                false);
    }

    public static CallSite checkpointBootstrap(Lookup lookup, String name, MethodType type) throws Throwable {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = lookup.findStatic(Bootstrap.class, "checkpointFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class));

        handle = handle.bindTo(site);
        site.setTarget(handle);

        return site;
    }

    public static void checkpointFallback(MutableCallSite site, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        Invalidator invalidator = runtime.getCheckpointInvalidator();

        MethodHandle target = Binder
                .from(void.class, ThreadContext.class)
                .nop();
        MethodHandle fallback = lookup().findStatic(Bootstrap.class, "checkpointFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class));
        fallback = fallback.bindTo(site);

        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, fallback);

        site.setTarget(target);
    }

    public static CallSite coverLineBootstrap(Lookup lookup, String name, MethodType type, String filename, int line, int oneshot) throws Throwable {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = lookup.findStatic(Bootstrap.class, "coverLineFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class, String.class, int.class, boolean.class));

        handle = handle.bindTo(site);
        handle = insertArguments(handle, 1, filename, line, oneshot != 0);
        site.setTarget(handle);

        return site;
    }

    public static void coverLineFallback(MutableCallSite site, ThreadContext context, String filename, int line, boolean oneshot) throws Throwable {
        IRRuntimeHelpers.updateCoverage(context, filename, line);

        if (oneshot) site.setTarget(Binder.from(void.class, ThreadContext.class).dropAll().nop());
    }

    public static CallSite globalBootstrap(Lookup lookup, String name, MethodType type, String file, int line) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = JavaNameMangler.demangleMethodName(names[1]);
        GlobalSite site = new GlobalSite(type, varName, file, line);
        MethodHandle handle;

        if (operation.equals("get")) {
            handle = lookup.findStatic(Bootstrap.class, "getGlobalFallback", methodType(IRubyObject.class, GlobalSite.class, ThreadContext.class));
        } else {
            handle = lookup.findStatic(Bootstrap.class, "setGlobalFallback", methodType(void.class, GlobalSite.class, IRubyObject.class, ThreadContext.class));
        }

        handle = handle.bindTo(site);
        site.setTarget(handle);

        return site;
    }

    public static IRubyObject getGlobalFallback(GlobalSite site, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        GlobalVariable variable = runtime.getGlobalVariables().getVariable(site.name());

        if (site.failures() > Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() ||
                variable.getScope() != GlobalVariable.Scope.GLOBAL ||
                RubyGlobal.UNCACHED_GLOBALS.contains(site.name())) {

            // use uncached logic forever
            if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") uncacheable or rebound > " + Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() + " times, reverting to simple lookup");

            MethodHandle uncached = lookup().findStatic(Bootstrap.class, "getGlobalUncached", methodType(IRubyObject.class, GlobalVariable.class));
            uncached = uncached.bindTo(variable);
            uncached = dropArguments(uncached, 0, ThreadContext.class);
            site.setTarget(uncached);
            return (IRubyObject)uncached.invokeWithArguments(context);
        }

        Invalidator invalidator = variable.getInvalidator();
        IRubyObject value = variable.getAccessor().getValue();

        MethodHandle target = constant(IRubyObject.class, value);
        target = dropArguments(target, 0, ThreadContext.class);
        MethodHandle fallback = lookup().findStatic(Bootstrap.class, "getGlobalFallback", methodType(IRubyObject.class, GlobalSite.class, ThreadContext.class));
        fallback = fallback.bindTo(site);

        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, fallback);

        site.setTarget(target);

//        if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") cached");

        return value;
    }

    public static IRubyObject getGlobalUncached(GlobalVariable variable) throws Throwable {
        return variable.getAccessor().getValue();
    }

    public static void setGlobalFallback(GlobalSite site, IRubyObject value, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        GlobalVariable variable = runtime.getGlobalVariables().getVariable(site.name());
        MethodHandle uncached = lookup().findStatic(Bootstrap.class, "setGlobalUncached", methodType(void.class, GlobalVariable.class, IRubyObject.class));
        uncached = uncached.bindTo(variable);
        uncached = dropArguments(uncached, 1, ThreadContext.class);
        site.setTarget(uncached);
        uncached.invokeWithArguments(value, context);
    }

    public static void setGlobalUncached(GlobalVariable variable, IRubyObject value) throws Throwable {
        // FIXME: duplicated logic from GlobalVariables.set
        variable.getAccessor().setValue(value);
        variable.trace(value);
        variable.invalidate();
    }

    public static Handle prepareBlock() {
        return new Handle(
                Opcodes.H_INVOKESTATIC,
                p(Bootstrap.class),
                "prepareBlock",
                sig(CallSite.class, Lookup.class, String.class,
                        MethodType.class, MethodHandle.class,  MethodHandle.class, MethodHandle.class, MethodHandle.class, String.class, long.class,
                        String.class, int.class, String.class),
                false);
    }

    public static CallSite prepareBlock(Lookup lookup, String name, MethodType type,
                                        MethodHandle bodyHandle,  MethodHandle scopeHandle, MethodHandle setScopeHandle, MethodHandle parentHandle, String scopeDescriptor, long encodedSignature,
                                        String file, int line, String encodedArgumentDescriptors
                                        ) throws Throwable {
        StaticScope staticScope = (StaticScope) scopeHandle.invokeExact();

        if (staticScope == null) {
            staticScope = Helpers.restoreScope(scopeDescriptor, (StaticScope) parentHandle.invokeExact());
            setScopeHandle.invokeExact(staticScope);
        }

        CompiledIRBlockBody body = new CompiledIRBlockBody(bodyHandle, staticScope, file, line, encodedArgumentDescriptors, encodedSignature);

        Binder binder = Binder.from(type);

        binder = binder.fold(FRAME_SCOPE_BINDING);

        // This optimization can't happen until we can see into the method we're calling to know if it reifies the block
        if (false) {
            /*
            if (needsBinding) {
                if (needsFrame) {
            FullInterpreterContext fic = scope.getExecutionContext();
            if (fic.needsBinding()) {
                if (fic.needsFrame()) {
                    binder = binder.fold(FRAME_SCOPE_BINDING);
                } else {
                    binder = binder.fold(SCOPE_BINDING);
                }
            } else {
                if (needsFrame) {
                    binder = binder.fold(FRAME_BINDING);
                } else {
                    binder = binder.fold(SELF_BINDING);
                }
            }*/
        }

        MethodHandle blockMaker = binder.drop(1, 3)
                .append(body)
                .invoke(CONSTRUCT_BLOCK);

        return new ConstantCallSite(blockMaker);
    }

    static String logMethod(DynamicMethod method) {
        return "[#" + method.getSerialNumber() + " " + method.getImplementationClass().getMethodLocation() + "]";
    }

    static String logBlock(Block block) {
        return "[" + block.getBody().getFile() + ":" + block.getBody().getLine() + "]";
    }

    private static final Binder BINDING_MAKER_BINDER = Binder.from(Binding.class, ThreadContext.class, IRubyObject.class, DynamicScope.class);

    private static final MethodHandle FRAME_SCOPE_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, IRRuntimeHelpers.class, "newFrameScopeBinding");

    private static final MethodHandle FRAME_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, Bootstrap.class, "frameBinding");
    public static Binding frameBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        Frame frame = context.getCurrentFrame().capture();
        return new Binding(self, frame, frame.getVisibility());
    }

    private static final MethodHandle SCOPE_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, Bootstrap.class, "scopeBinding");
    public static Binding scopeBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        return new Binding(self, scope);
    }

    private static final MethodHandle SELF_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, Bootstrap.class, "selfBinding");
    public static Binding selfBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        return new Binding(self);
    }

    private static final MethodHandle CONSTRUCT_BLOCK = Binder.from(Block.class, Binding.class, CompiledIRBlockBody.class).invokeStaticQuiet(LOOKUP, Bootstrap.class, "constructBlock");
    public static Block constructBlock(Binding binding, CompiledIRBlockBody body) throws Throwable {
        return new Block(body, binding);
    }
}
