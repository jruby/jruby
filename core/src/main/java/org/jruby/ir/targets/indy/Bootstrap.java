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
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.JIT;
import org.jruby.ir.runtime.IRRuntimeHelpers;
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
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.runtime.invokedynamic.MathLinker;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.scope.DynamicScopeGenerator;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;

import static java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class Bootstrap {
    public final static String BOOTSTRAP_BARE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class);
    public final static String BOOTSTRAP_LONG_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class, int.class, String.class, int.class);
    public final static String BOOTSTRAP_DOUBLE_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, double.class, int.class, String.class, int.class);
    public final static String BOOTSTRAP_INT_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, int.class, int.class);
    public final static String BOOTSTRAP_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, int.class);
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    private static final Lookup LOOKUP = MethodHandles.lookup();

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

    public static Handle callInfoHandle() {
        return getBootstrapHandle("callInfoBootstrap", BOOTSTRAP_INT_SIG);
    }

    public static Handle coverLineHandle() {
        return getBootstrapHandle("coverLineBootstrap", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class, int.class));
    }

    public static Handle getHeapLocalHandle() {
        return getBootstrapHandle("getHeapLocalBootstrap", BOOTSTRAP_INT_INT_SIG);
    }

    public static Handle getHeapLocalOrNilHandle() {
        return getBootstrapHandle("getHeapLocalOrNilBootstrap", BOOTSTRAP_INT_INT_SIG);
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

        // poll for events once since we've ended up back in fallback
        context.pollThreadEvents();
    }

    public static CallSite callInfoBootstrap(Lookup lookup, String name, MethodType type, int callInfo) throws Throwable {
        MethodHandle handle;
        if (callInfo == 0) {
            handle = lookup.findVirtual(ThreadContext.class, "clearCallInfo", methodType(void.class));
        } else {
            handle = lookup.findStatic(IRRuntimeHelpers.class, "setCallInfo", methodType(void.class, ThreadContext.class, int.class));
            handle = insertArguments(handle, 1, callInfo);
        }

        return new ConstantCallSite(handle);
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

    public static CallSite getHeapLocalBootstrap(Lookup lookup, String name, MethodType type, int depth, int location) throws Throwable {
        // no null checking needed for method bodies
        MethodHandle getter;
        Binder binder = Binder
                .from(type);

        if (depth == 0) {
            if (location < DynamicScopeGenerator.SPECIALIZED_GETS.size()) {
                getter = binder.invokeVirtualQuiet(LOOKUP, DynamicScopeGenerator.SPECIALIZED_GETS.get(location));
            } else {
                getter = binder
                        .insert(1, location)
                        .invokeVirtualQuiet(LOOKUP, "getValueDepthZero");
            }
        } else {
            getter = binder
                    .insert(1, arrayOf(int.class, int.class), location, depth)
                    .invokeVirtualQuiet(LOOKUP, "getValue");
        }

        ConstantCallSite site = new ConstantCallSite(getter);

        return site;
    }

    public static CallSite getHeapLocalOrNilBootstrap(Lookup lookup, String name, MethodType type, int depth, int location) throws Throwable {
        MethodHandle getter;
        Binder binder = Binder
                .from(type)
                .filter(1, LiteralValueBootstrap.contextValue(lookup, "nil", methodType(IRubyObject.class, ThreadContext.class)).dynamicInvoker());

        if (depth == 0) {
            if (location < DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.size()) {
                getter = binder.invokeVirtualQuiet(LOOKUP, DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.get(location));
            } else {
                getter = binder
                        .insert(1, location)
                        .invokeVirtualQuiet(LOOKUP, "getValueDepthZeroOrNil");
            }
        } else {
            getter = binder
                    .insert(1, arrayOf(int.class, int.class), location, depth)
                    .invokeVirtualQuiet(LOOKUP, "getValueOrNil");
        }

        ConstantCallSite site = new ConstantCallSite(getter);

        return site;
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
