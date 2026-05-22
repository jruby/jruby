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

package org.jruby.java.proxies;

import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.Visibility.PUBLIC;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.api.Convert;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.SplitSuperState;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.MethodSplitState;
import org.jruby.ir.interpreter.ExitableInterpreterContext;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.ir.JIT;
import org.jruby.java.invokers.RubyToJavaInvoker;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.Java.JCreateMethod;
import org.jruby.javasupport.ConstructorCache;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

public class ConcreteJavaProxy extends JavaProxy {

    public ConcreteJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public ConcreteJavaProxy(Ruby runtime, RubyClass klazz, Object object) {
        super(runtime, klazz, object);
    }

    public static RubyClass createConcreteJavaProxy(final ThreadContext context, RubyClass JavaProxy) {
        var ConcreteJavaProxy = defineClass(context, "ConcreteJavaProxy", JavaProxy, ConcreteJavaProxy::new);
        addInitializeMethod(context, ConcreteJavaProxy);
        return ConcreteJavaProxy;
    }

    private static final class InitializeMethod extends org.jruby.internal.runtime.methods.JavaMethod {

        private final CallSite jcreateSite = MethodIndex.getFunctionalCallSite("__jcreate!");

        InitializeMethod(final RubyClass clazz) { super(clazz, Visibility.PRIVATE, "initialize"); }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return jcreateSite.call(context, self, self, args, block);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return jcreateSite.call(context, self, self, block);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            return jcreateSite.call(context, self, self, arg0, block);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return jcreateSite.call(context, self, self, arg0, arg1, block);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return jcreateSite.call(context, self, self, arg0, arg1, arg2, block);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            return jcreateSite.call(context, self, self, args);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return jcreateSite.call(context, self, self);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            return jcreateSite.call(context, self, self, arg0);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return jcreateSite.call(context, self, self, arg0, arg1);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return jcreateSite.call(context, self, self, arg0, arg1, arg2);
        }

    }

    private static final class NewMethod extends org.jruby.internal.runtime.methods.JavaMethod {
        final DynamicMethod newMethod;

        NewMethod(final RubyClass clazz) {
            super(clazz, Visibility.PUBLIC, "new");
            newMethod = clazz.searchMethod("new");
        }

        private DynamicMethod reifyAndNewMethod(ThreadContext context, IRubyObject self) {
            final RubyClass parent = (RubyClass) self;
            if (parent.getJavaProxy()) return newMethod;

            // overridden class: reify and re-lookup new as reification changes it
            if (parent.reifiedClass() == null) {
                parent.reifyWithAncestors();
                if (parent.reifiedClass() == null) {
                    throw typeError(context, "requested class " + parent.getName(context) + " was not reifiable");
                }
            }

            RubyClass singleton = parent.singletonClass(context);
            DynamicMethod method = singleton.searchMethod("new");
            if (method instanceof NewMethodReified) return method;
            if (!(method instanceof NewMethod)) return method;

            method = new NewMethodReified(parent, parent.getReifiedJavaClass());
            singleton.addMethod(context, "new", method);
            return method;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new_proxy", args, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new", block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new", arg0, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new", arg0, arg1, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new", arg0, arg1, arg2, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new", args);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new_proxy");
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new_proxy", arg0);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new", arg0, arg1);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new", arg0, arg1, arg2);
        }

    }

    /**
     * Manually added as an override of `new` for Concrete Extension
     */
    public static class StaticJCreateMethod extends JavaMethodNBlock {

        private final Constructor<? extends ReifiedJavaProxy> withBlock;
        final DynamicMethod oldInit;

        StaticJCreateMethod(RubyModule implClass, Constructor<? extends ReifiedJavaProxy> javaProxyConstructor, DynamicMethod oldinit) {
            super(implClass, PUBLIC, "__jcreate_static!");
            this.withBlock = javaProxyConstructor;
            // ensure we don't use a wrapper (jruby/jruby#8148)
            this.oldInit = oldinit == null ? null : oldinit.getRealMethod();
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
                IRubyObject[] args, Block block) {
            try {
                ConcreteJavaProxy cjp = (ConcreteJavaProxy) self;
                if (cjp.getObject() == null) {
                    // first-time init: invoke the generated reified constructor (which sets cjp.object internally)
                    withBlock.newInstance(cjp, args, block, clazz);
                } else if (oldInit != null) {
                    // re-entry into initialize on an already-constructed proxy - delegate to the prior initialize
                    return oldInit.call(context, self, clazz, name, args, block);
                }
            } catch (InstantiationException | InvocationTargetException e) {
                throw JavaProxyConstructor.throwInstantiationExceptionCause(context.runtime, e);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw JavaProxyConstructor.mapInstantiationException(context.runtime, e);
            }
            return self;
        }

        public static void tryInstall(ThreadContext context, RubyClass clazz,
                Class<? extends ReifiedJavaProxy> reified, boolean overwriteInitialize) {
            try {
                Constructor<? extends ReifiedJavaProxy> withBlock = reified.getConstructor(new Class[] {
                        ConcreteJavaProxy.class, IRubyObject[].class, Block.class, RubyClass.class });
                if (overwriteInitialize) clazz.addMethod(context, "initialize",
                    new StaticJCreateMethod(clazz, withBlock, clazz.getMethodLocation().searchMethod("initialize")));
                clazz.addMethod(context, "__jallocate!", new StaticJCreateMethod(clazz, withBlock, null));
            } catch (SecurityException | NoSuchMethodException e) {
                // class lacks the expected (ConcreteJavaProxy, IRubyObject[], Block, RubyClass) ctor -
                // ignore and leave the existing initialize/__jallocate! in place
            }
        }
    }

    public static final class NewMethodReified extends org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock {

        private final DynamicMethod initialize;
        private final Constructor<? extends ReifiedJavaProxy> constructor;

        public NewMethodReified(final RubyClass clazz, Class<? extends ReifiedJavaProxy> reified) {
            super(clazz, Visibility.PUBLIC, "new");
            initialize = clazz.searchMethod("__jcreate!");

            Constructor<? extends ReifiedJavaProxy> constructor;
            try {
                constructor = reified.getConstructor(ConcreteJavaProxy.class, IRubyObject[].class, Block.class, RubyClass.class);
            } catch (SecurityException | NoSuchMethodException e) {
                // ignore, don't install
                constructor = null;
            }
            this.constructor = constructor;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz,
                                String name, IRubyObject[] args, Block block) {
            // a subclass extended the base after we were installed; rebuild and dispatch via a fresh NewMethod
            if (self != implementationClass) {
                return new NewMethod((RubyClass) self).call(context, self, clazz, name, args, block);
            }

            if (constructor == null) {
                ReifiedJavaProxy proxy = JavaUtil.unwrapJava(context, initialize.call(context, self, clazz, "new", args));
                return proxy.___jruby$rubyObject();
            }

            // assume no easy conversions, use ruby fallback.
            ConcreteJavaProxy object = new ConcreteJavaProxy(context.runtime, (RubyClass) self);
            try {
                // the generated ctor uses `self` as its ruby class; note: it sets self.object = the discarded
                // return of the new java object internally
                constructor.newInstance(object, args, block, self);
                return object;
            } catch (InstantiationException | InvocationTargetException e) {
                throw JavaProxyConstructor.throwInstantiationExceptionCause(context.runtime, e);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw JavaProxyConstructor.mapInstantiationException(context.runtime, e);
            }
        }

    }

    /**
     * Used by reified classes, this class is tightly coupled with RealClassGenerator, splitInitialize, &amp; finishInitialize
     * Do not refactor without looking at RCG
     */
    public static final class SplitCtorData {
        // public fields used by RealClassGenerator's generated code
        public final Object[] arguments;
        public final int ctorIndex;

        // public field used by finishInitialized &  (ruby < ruby < java) generated classes
        public final IRubyObject[] rbarguments;
        public final Block block;

        // fields below are only used in ConcreteJavaProxy finishInitialize
        private final AbstractIRMethod method;
        private final RubyModule clazz;
        private final String name;
        private final SplitSuperState<?> state;
        private final SplitCtorData nested;

        /**
         * Picks and converts arguments for the super call
         * Leaves ctorIndex and arguments ready for the super call
         */
        SplitCtorData(Ruby runtime, IRubyObject[] args, ConstructorCache cache) {
            this(runtime, args, cache, null, null, null, null, Block.NULL_BLOCK, null);
        }

        SplitCtorData(Ruby runtime, IRubyObject[] args, ConstructorCache cache,
                      AbstractIRMethod method, RubyModule clazz, String name, Block block) {
            this(runtime, args, cache, method, clazz, name, null, block, null);
        }

        SplitCtorData(Ruby runtime, IRubyObject[] args, ConstructorCache cache,
                      AbstractIRMethod method, RubyModule clazz, String name, SplitSuperState<?> state, Block block) {
            this(runtime, args, cache, method, clazz, name, state, block, null);
        }

        private SplitCtorData(Ruby runtime, IRubyObject[] args, ConstructorCache cache,
                              AbstractIRMethod method, RubyModule clazz, String name,
                              SplitSuperState<?> state, Block block, SplitCtorData nested) {
            rbarguments = args;
            if (cache == null) { // (ruby < ruby < java) super call from one IRO to another IRO ctor
                ctorIndex = -1;
                arguments = null;
            } else if (args.length == 0 && cache.noArgConstructorIndex >= 0) {
                ctorIndex = cache.noArgConstructorIndex;
                arguments = null;
            } else {
                ctorIndex = JCreateMethod.forTypes(runtime, args, cache);
                arguments = RubyToJavaInvoker.convertArguments(cache.constructors[ctorIndex], args);
            }

            this.method = method;
            this.clazz = clazz;
            this.name = name;
            this.state = state;
            this.block = block;
            this.nested = nested;
        }

        private SplitCtorData(SplitCtorData ctorData, IRubyObject[] args,
                              AbstractIRMethod method, RubyModule clazz, String name, SplitSuperState<?> state, Block block) {
            this.arguments = ctorData.arguments;
            this.ctorIndex = ctorData.ctorIndex;
            this.rbarguments = args;
            this.method = method;
            this.clazz = clazz;
            this.name = name;
            this.state = state;
            this.block = block;
            this.nested = ctorData;
        }

        /**
         * Returns true if this {@code SplitCtorData} represents a leaf Java constructor terminator: a chain end
         * with no nested Ruby super, no continuation method, and no captured split state. Such terminators are
         * fully immutable and safe to cache across calls.
         */
        boolean isLeafJavaTerminator() {
            return method == null && state == null && nested == null;
        }
    }

    public static final class SplitCtorPlan {
        private final RubyClass base;
        private final int token;
        private final String name;
        private final RubyModule methodSource;
        private final RubyClass sourceLocation;
        private final RubyModule effectiveSource;
        private final boolean isLateral;
        private final DynamicMethod method;

        // Cached strategy fields - precomputed once per (base, generation).
        private final AbstractIRMethod air;
        private final ExitableInterpreterContext eic;
        private final boolean prependedJavaCtorWrapper;

        // Lazily-resolved IRubyObject[] for terminal literal super calls; shared across invocations.
        private volatile IRubyObject[] cachedTerminalLiteralArgs;

        // cached next plan in the recursive split-constructor chain
        // (one of these is populated based on which Ruby super source we recurse into)
        private SplitCtorPlan cachedSuperPlan;
        private SplitCtorPlan cachedPrependedSuperPlan;

        // cached <init> data reachable from this plan via a terminal literal-args super call
        // when the chain terminates immediately in a Java constructor (no Ruby `super`)
        volatile SplitCtorData cachedLiteralSuperTerminator;

        private SplitCtorPlan(RubyClass base, CacheEntry methodEntry) {
            this.base = base;
            this.token = methodEntry.token;
            this.name = base.getClassConfig().javaCtorMethodName;
            this.methodSource = methodEntry.sourceModule;
            this.sourceLocation = findIncludedPrependedModule(methodEntry.sourceModule, base);
            RubyModule effectiveSource = sourceLocation != null ? sourceLocation : methodEntry.sourceModule;
            this.isLateral = isClassOrIncludedPrependedModule(methodEntry.sourceModule, base);
            DynamicMethod method = methodEntry.method.getRealMethod(); // ensure we don't use a wrapper (jruby/jruby#8148)

            if (method instanceof StaticJCreateMethod) {
                method = ((StaticJCreateMethod) method).oldInit;
                if (method != null) effectiveSource = method.getImplementationClass();
            }

            this.effectiveSource = effectiveSource;
            this.method = method;

            AbstractIRMethod air = method instanceof AbstractIRMethod ? (AbstractIRMethod) method : null;
            this.air = air;
            this.eic = air != null ? air.getJavaConstructorContext() : null;
            this.prependedJavaCtorWrapper = isPrependedJavaCtorWrapper(sourceLocation, base);
        }

        private boolean isValid(final RubyClass base) {
            return this.base == base && token == base.getGeneration();
        }

        /**
         * Returns true when {@code args} can be forwarded directly to the Java superclass constructor without
         * entering the split interpreter (e.g. {@code def initialize(*args); super(*args); end}).
         */
        private boolean canSkipDirectSuper(IRubyObject[] args) {
            ExitableInterpreterContext ic = eic;
            return ic != null && (ic.directSuperAllArgs() ||
                    args.length == 0 && ic.directSuperNoArgs() ||
                    ic.directSuperRequiredArgs() == args.length);
        }

        /**
         * Returns resolved Java constructor args when the Ruby initialize body is a terminal {@code super(...)}
         * with only immutable literal arguments; {@code null} otherwise.
         *
         * @implNote resolved array is cached on first use and shared across invocations - safe because literals are
         * immutable and downstream callers ({@code JCreateMethod.forTypes}, {@code RubyToJavaInvoker.convertArguments})
         * only read from the array.
         */
        private IRubyObject[] terminalLiteralSuperArgs(ThreadContext context) {
            ExitableInterpreterContext ic = eic;
            if (ic == null || !ic.terminalLiteralSuper()) return null;

            IRubyObject[] cached = cachedTerminalLiteralArgs;
            if (cached != null) return cached;

            cached = ic.getTerminalLiteralSuperArgs(context);
            cachedTerminalLiteralArgs = cached;
            return cached;
        }

        /**
         * Returns (and caches) the recursive plan reached when traversing into the next Ruby super source
         * via {@code splitSuperInitialized}, stable per current plan and super base.
         */
        private SplitCtorPlan superPlan(RubyClass nextBase) {
            SplitCtorPlan cached = cachedSuperPlan;
            if (cached != null && cached.isValid(nextBase)) return cached;

            cached = new SplitCtorPlan(nextBase, nextBase.searchWithCache(nextBase.getClassConfig().javaCtorMethodName));
            cachedSuperPlan = cached;
            return cached;
        }

        /**
         * Returns (and caches) the plan reached when traversing into the prepended Java ctor wrapper's super source.
         */
        private SplitCtorPlan prependedSuperPlan(RubyClass nextBase) {
            SplitCtorPlan cached = cachedPrependedSuperPlan;
            if (cached != null && cached.isValid(nextBase)) return cached;

            cached = new SplitCtorPlan(nextBase, nextBase.searchWithCache(nextBase.getClassConfig().javaCtorMethodName));
            cachedPrependedSuperPlan = cached;
            return cached;
        }
    }

    /**
     * Used by reified classes, this method is tightly coupled with RealClassGenerator, finishInitialize
     * Do not refactor without looking at RCG
     * @return An object used by reified code and the finishInitialize method
     */
    public SplitCtorData splitInitialized(RubyClass base, IRubyObject[] args, Block block, ConstructorCache jcc) {
        return splitInitialized(topLevelSplitCtorPlan(base, jcc), args, block, jcc, false);
    }

    private SplitCtorData splitInitialized(SplitCtorPlan plan, IRubyObject[] args, Block block, ConstructorCache jcc, boolean fromRubySuper) {
        final Ruby runtime = getRuntime();
        final AbstractIRMethod air = plan.air;
        final ExitableInterpreterContext eic = plan.eic;
        final String name = plan.name;
        final RubyClass sourceLocation = plan.sourceLocation;
        final RubyModule effectiveSource = plan.effectiveSource;

        if (plan.prependedJavaCtorWrapper && air != null) {
            if (plan.canSkipDirectSuper(args)) {
                return splitInitialized(plan.prependedSuperPlan(sourceLocation.getSuperClass()), args, block, jcc, true);
            }

            IRubyObject[] literalArgs = plan.terminalLiteralSuperArgs(runtime.getCurrentContext());
            if (literalArgs != null) {
                return splitInitialized(plan.prependedSuperPlan(sourceLocation.getSuperClass()), literalArgs, block, jcc, true);
            }

            SplitSuperState<?> state = air.startSplitSuperCall(runtime.getCurrentContext(), this, effectiveSource, name, args, block, eic);
            IRubyObject[] forwardedArgs = state == null ? args : state.callArgs;
            Block forwardedBlock = state == null ? block : state.callBlockArgs;
            SplitCtorData ctorData = splitInitialized(plan.prependedSuperPlan(sourceLocation.getSuperClass()), forwardedArgs, forwardedBlock, jcc, true);
            return new SplitCtorData(ctorData, forwardedArgs, air, effectiveSource, name, state, forwardedBlock);
        }

        // jcreate is for nested ruby classes from a java class
        if (shouldSplitJavaConstructorInitialize(plan.isLateral, fromRubySuper, plan.methodSource) && air != null) {
            if (plan.canSkipDirectSuper(args)) {
                return splitSuperInitialized(plan, args, block, jcc);
            }

            IRubyObject[] literalArgs = plan.terminalLiteralSuperArgs(runtime.getCurrentContext());
            if (literalArgs != null) {
                SplitCtorData cachedTerminator = plan.cachedLiteralSuperTerminator;
                if (cachedTerminator != null) return cachedTerminator;

                SplitCtorData terminator = splitSuperInitialized(plan, literalArgs, block, jcc);
                if (terminator.isLeafJavaTerminator()) plan.cachedLiteralSuperTerminator = terminator;
                return terminator;
            }

            SplitSuperState<?> state = air.startSplitSuperCall(runtime.getCurrentContext(), this, effectiveSource, name, args, block, eic);
            if (state == null) { // no super in method
                return new SplitCtorData(runtime, args, jcc, air, effectiveSource, name, block);
            }

            IRubyObject[] forwardedArgs = state.callArgs;
            Block forwardedBlock = state.callBlockArgs;
            SplitCtorData ctorData = splitSuperInitialized(plan, forwardedArgs, forwardedBlock, jcc);
            return new SplitCtorData(ctorData, forwardedArgs, air, effectiveSource, name, state, forwardedBlock);
        }
        return new SplitCtorData(runtime, args, jcc);
    }

    private static SplitCtorPlan topLevelSplitCtorPlan(final RubyClass base, final ConstructorCache jcc) {
        if (jcc != null) {
            SplitCtorPlan plan = jcc.getSplitCtorPlan();
            if (plan != null && plan.isValid(base)) return plan;

            plan = new SplitCtorPlan(base, base.searchWithCache(base.getClassConfig().javaCtorMethodName));
            jcc.setSplitCtorPlan(plan);

            return plan;
        }

        return new SplitCtorPlan(base, base.searchWithCache(base.getClassConfig().javaCtorMethodName));
    }

    private SplitCtorData splitSuperInitialized(final SplitCtorPlan plan, final IRubyObject[] args,
                                               final Block block, final ConstructorCache jcc) {
        RubyClass next = nextRubyConstructorSource(plan.effectiveSource, jcc);

        if (next == null) return new SplitCtorData(getRuntime(), args, jcc);
        if (!next.getDelegate().getJavaProxy()) return new SplitCtorData(getRuntime(), args, null);

        return splitInitialized(plan.superPlan(next), args, block, jcc, true);
    }

    private static RubyClass nextRubyConstructorSource(final RubyModule methodSource, final ConstructorCache jcc) {
        if (jcc == null || methodSource.getDelegate().getJavaProxy()) return null;

        return methodSource.getMethodLocation().getSuperClass();
    }

    private static boolean shouldSplitJavaConstructorInitialize(final boolean isLateral,
                                                               final boolean fromRubySuper,
                                                               final RubyModule methodSource) {
        return isLateral || (fromRubySuper && methodSource.getDelegate().getJavaProxy());
    }

    private static boolean isPrependedJavaCtorWrapper(final RubyClass methodSource, final RubyClass klass) {
        if (methodSource == null) return false;

        return methodSource != klass && methodSource.isIncluded() &&
                methodSource.getSuperClass() != null && methodSource.getSuperClass().getDelegate() == klass;
    }

    private static RubyClass findIncludedPrependedModule(final RubyModule methodSource, final RubyClass klass) {
        RubyModule ceiling = klass.getMethodLocation();

        for (RubyClass candidate = klass.getSuperClass(); candidate != null && candidate != ceiling; candidate = candidate.getSuperClass()) {
            if (candidate.getOrigin() == methodSource.getOrigin()) return candidate;
        }

        return null;
    }

    private static boolean isClassOrIncludedPrependedModule(final RubyModule methodSource, final RubyClass klass) {
        if (methodSource == klass || methodSource.getDelegate() == klass) return true;

        RubyClass candidate = klass.getSuperClass();
        while (candidate != null && (candidate.isIncluded() || candidate.isPrepended())) { // up till 'real' superclass
            if (candidate == methodSource || candidate.getDelegate() == methodSource.getDelegate()) return true;
            candidate = candidate.getSuperClass();
        }

        return false;
    }

    /**
     * Used by reified classes, this method is tightly coupled with RealClassGenerator, splitInitialize
     * Do not refactor without looking at RCG
     * <p>Note: invoked from generated byte-code</p>
     */
    public void finishInitialize(SplitCtorData returned) {
        if (returned.nested != null) finishInitialize(returned.nested);

        if (returned.method == null) return; // leaf java terminator - no Ruby continuation

        if (returned.state != null) {
            finishSplitCall(returned.method, returned.state);
        } else { // no super in this Ruby initialize, direct call to run the body
            final ThreadContext context = getRuntime().getCurrentContext();
            returned.method.call(context, this, returned.clazz, returned.name, returned.rbarguments, returned.block);
        }
    }

    private void finishSplitCall(final AbstractIRMethod method, final SplitSuperState state) {
        final MethodSplitState methodState = (MethodSplitState) state.state;
        if (methodState.getInterpreterContext().exitsAtReturn()) return;

        final ThreadContext context = getRuntime().getCurrentContext();
        method.interpretSplit(context, methodState, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    protected static void addInitializeMethod(ThreadContext context, final RubyClass concreteJavaProxy) {
        concreteJavaProxy.addMethod(context, "initialize", new InitializeMethod(concreteJavaProxy));
        // We define a custom "new" method to ensure that __jcreate! is getting called,
        // so that if the user doesn't call super in their subclasses, the object will
        // still get set up properly. See JRUBY-4704.
        RubyClass singleton = concreteJavaProxy.singletonClass(context);
        singleton.addMethod(context, "new", new NewMethod(singleton));
    }

    // This alternate ivar logic is disabled because it can cause self-referencing
    // chains to keep the original object alive. See JRUBY-4832.
//    @Override
//    public Object getVariable(int index) {
//        return getRuntime().getJavaSupport().getJavaObjectVariable(this, index);
//    }
//
//    @Override
//    public void setVariable(int index, Object value) {
//        getRuntime().getJavaSupport().setJavaObjectVariable(this, index, value);
//    }

    @Deprecated(since = "10.0.3.0")
    @Override
    public IRubyObject id() {
        return RubyFixnum.newFixnum(getRuntime(), System.identityHashCode(getObject()));
    }

    /**
     * Because we can't physically associate an ID with a Java object, we can
     * only use the identity hashcode here.
     *
     * @return The identity hashcode for the Java object.
     */
    @Override
    public RubyInteger __id__(ThreadContext context) {
        return Convert.asFixnum(context, System.identityHashCode(getObject()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T toJava(Class<T> type) {
        final Object object = getObject();
        if (object == null) {
            throw getRuntime().newRuntimeError("Java proxy not initialized. Did you call super() yet?");
        }
        final Class clazz = object.getClass();

        if ( type.isPrimitive() ) {
            if ( type == Void.TYPE ) return null;

            if ( object instanceof Number && type != Boolean.TYPE ||
                 object instanceof Character && type == Character.TYPE ||
                 object instanceof Boolean && type == Boolean.TYPE ) {
                // FIXME in more permissive call paths, like invokedynamic, this can allow
                // precision-loading downcasts to happen silently
                return (T) object;
            }
        }
        else if ( type.isAssignableFrom(clazz) ) {
            if ( Java.OBJECT_PROXY_CACHE || metaClass.getCacheProxy() ) {
                getRuntime().getJavaSupport().getObjectProxyCache().put(object, this);
            }
            return type.cast(object);
        }
        else if ( type.isAssignableFrom(getClass()) ) return type.cast(this); // e.g. IRubyObject.class

        throw typeError(getRuntime().getCurrentContext(), "failed to coerce " + clazz.getName() + " to " + type.getName());
    }
}
