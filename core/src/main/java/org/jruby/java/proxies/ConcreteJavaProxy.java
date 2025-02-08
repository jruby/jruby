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
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.SplitSuperState;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.ir.JIT;
import org.jruby.java.invokers.RubyToJavaInvoker;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.Java.JCreateMethod;
import org.jruby.javasupport.Java.JCtorCache;
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
        initialize(ConcreteJavaProxy);
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

        // TODO: reload this on method changes?
        private DynamicMethod reifyAndNewMethod(ThreadContext context, IRubyObject clazz) {
            RubyClass parent = ((RubyClass) clazz);
            if (parent.getJavaProxy()) return newMethod;

            // overridden class: reify and re-lookup new as reification changes it
            if (parent.reifiedClass() == null) {
                parent.reifyWithAncestors();
                if (parent.reifiedClass() == null) {
                    throw typeError(context, "requested class " + parent.getName(context) + " was not reifiable");
                }
            }

            return new NewMethodReified(parent, parent.getReifiedJavaClass());
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return reifyAndNewMethod(context, self).call(context, self, clazz, "new_proxy", args, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz, "new",block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz, "new",arg0, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz, "new",arg0, arg1, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz, "new",arg0, arg1, arg2, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz, "new",args);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz,"new_proxy");
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz, "new_proxy",arg0);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz,"new", arg0, arg1);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        	return reifyAndNewMethod(context, self).call(context, self, clazz,"new", arg0, arg1, arg2);
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
            this.oldInit = oldinit == null ? oldinit : oldinit.getRealMethod(); // ensure we don't use a wrapper (jruby/jruby#8148)
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
                IRubyObject[] args, Block block) {
            try {
                ConcreteJavaProxy cjp = (ConcreteJavaProxy) self;
                // TODO: Insead of selectively overwriting, silently fail? or only use the other method/this method?
                if (cjp.getObject() == null) {
                    withBlock.newInstance(cjp, args, block, context.runtime, clazz);
                    // note: the generated ctor sets self.object = our discarded return of the new object
                }
            } catch (InstantiationException | InvocationTargetException e) {
                throw JavaProxyConstructor.throwInstantiationExceptionCause(context.runtime, e);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw JavaProxyConstructor.mapInstantiationException(context.runtime, e);
            }
            return self;
        }

        public static void tryInstall(Ruby runtime, RubyClass clazz, JavaProxyClass proxyClass,
                Class<? extends ReifiedJavaProxy> reified, boolean overwriteInitialize) {
            try {
                Constructor<? extends ReifiedJavaProxy> withBlock = reified.getConstructor(new Class[] {
                        ConcreteJavaProxy.class, IRubyObject[].class, Block.class, Ruby.class, RubyClass.class });
                // TODO: move initialize to real_initialize
                // TODO: don't lock in this initialize method
                var context = runtime.getCurrentContext();
                if (overwriteInitialize) clazz.addMethod(context,"initialize",
                        new StaticJCreateMethod(clazz, withBlock, clazz.searchMethod("initialize")));
                clazz.addMethod(context, "__jallocate!", new StaticJCreateMethod(clazz, withBlock, null));
            } catch (SecurityException | NoSuchMethodException e) {
                // TODO log?
                // e.printStackTrace();
                // ignore, don't install
            }
        }
    }

    public static final class NewMethodReified extends org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock {

        private final DynamicMethod initialize;
        private final Constructor<? extends ReifiedJavaProxy> ctor;

        public NewMethodReified(final RubyClass clazz, Class<? extends ReifiedJavaProxy> reified) {
            super(clazz, Visibility.PUBLIC, "new");
            initialize = clazz.searchMethod("__jcreate!");

            Constructor<? extends ReifiedJavaProxy> withBlock;
            try {
                Class _clazz;
                if (Map.class.isAssignableFrom(reified)) {
                    _clazz = MapJavaProxy.class;
                } else {
                    _clazz = ConcreteJavaProxy.class;
                }
                withBlock = reified.getConstructor(new Class[] { _clazz, IRubyObject[].class,
                        Block.class, Ruby.class, RubyClass.class });
            } catch (SecurityException | NoSuchMethodException e) {
                // ignore, don't install
                withBlock = null;
            }
            ctor = withBlock;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
                IRubyObject[] args, Block blk) {
            // TODO: is there a better way to shake off old reified NewMethodReified methods on extension?
            if (self != implementationClass) { // someone extended the base, we are no longer them, re-reify
                return new NewMethod((RubyClass)self).call(context, self, clazz, name, args, blk);
            }

            if (ctor == null) {
                ReifiedJavaProxy proxy = JavaUtil.unwrapJava(context, initialize.call(context, self, clazz, "new", args));
                return proxy.___jruby$rubyObject();
            } else {

                // assume no easy conversions, use ruby fallback.
                ConcreteJavaProxy object = new ConcreteJavaProxy(context.runtime, (RubyClass) self);
                try {
                    ctor.newInstance(object, args, blk, context.runtime, self);// TODO: clazz?
                    // note: the generated ctor sets self.object = our discarded return of the new object
                    return object;
                } catch (InstantiationException | InvocationTargetException e) {
                    throw JavaProxyConstructor.throwInstantiationExceptionCause(context.runtime, e);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    throw JavaProxyConstructor.mapInstantiationException(context.runtime, e);
                }
            }
        }

    }

    public static int findSuperLine(Ruby runtime, DynamicMethod dm, int start) {
        // TODO: ???
        return start;
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
        private final String name;
        private final SplitSuperState<?> state;

        /**
         * Picks and converts arguments for the super call
         * Leaves ctorIndex and arguments ready for the super call
         */
        SplitCtorData(Ruby runtime, IRubyObject[] args, JCtorCache cache) {
            this(runtime, args, cache, null, null, null, Block.NULL_BLOCK);
        }

        SplitCtorData(Ruby runtime, IRubyObject[] args, JCtorCache cache,
                      AbstractIRMethod method, SplitSuperState<?> state, Block block) {
            this(runtime, args, cache, method, null, state, block);
        }

        SplitCtorData(Ruby runtime, IRubyObject[] args, JCtorCache cache,
                      AbstractIRMethod method, String name, Block block) {
            this(runtime, args, cache, method, name, null, block);
        }

        private SplitCtorData(Ruby runtime, IRubyObject[] args, JCtorCache cache,
                              AbstractIRMethod method, String name, SplitSuperState<?> state, Block block) {
            rbarguments = args;
            if (cache == null) { // (ruby < ruby < java) super call from one IRO to another IRO ctor
                ctorIndex = -1;
                arguments = null;
            } else {
                ctorIndex = JCreateMethod.forTypes(runtime, args, cache);
                arguments = RubyToJavaInvoker.convertArguments(cache.constructors[ctorIndex], args);
            }

            this.method = method;
            this.name = name;
            this.state = state;
            this.block = block;
        }
    }

    /**
     * Used by reified classes, this method is tightly coupled with RealClassGenerator, finishInitialize
     * Do not refactor without looking at RCG
     * @return An object used by reified code and the finishInitialize method
     */
    public SplitCtorData splitInitialized(RubyClass base, IRubyObject[] args, Block block, JCtorCache jcc) {
        final Ruby runtime = getRuntime();
        final String name = base.getClassConfig().javaCtorMethodName;
        final CacheEntry methodEntry = base.searchWithCache(name);
        final boolean isLateral = isClassOrIncludedPrependedModule(methodEntry.sourceModule, base);
        DynamicMethod method = methodEntry.method.getRealMethod(); // ensure we don't use a wrapper (jruby/jruby#8148)
        if (method instanceof StaticJCreateMethod) method = ((StaticJCreateMethod) method).oldInit;

        // jcreate is for nested ruby classes from a java class
        if (isLateral && method instanceof AbstractIRMethod) {

            AbstractIRMethod air = (AbstractIRMethod) method; // TODO: getMetaClass() ? or base? (below v)

            SplitSuperState<?> state = air.startSplitSuperCall(runtime.getCurrentContext(), this, getMetaClass(), name, args, block);
            if (state == null) { // no super in method
                return new SplitCtorData(runtime, args, jcc, air, name, block);
            }
            return new SplitCtorData(runtime, state.callArrayArgs.toJavaArrayMaybeUnsafe(), jcc, air, state, block);
        }
        return new SplitCtorData(runtime, args, jcc);
    }

    private static boolean isClassOrIncludedPrependedModule(final RubyModule methodSource, final RubyClass klass) {
        if (methodSource == klass) return true;

        RubyClass candidate = klass.getSuperClass();
        while (candidate != null && (candidate.isIncluded() || candidate.isPrepended())) { // up till 'real' superclass
            if (candidate == klass) return true;
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
        if (returned.method != null) {
            if (returned.state != null) {
                returned.method.finishSplitCall(returned.state);
            } else { // no super, direct call
                returned.method.call(getRuntime().getCurrentContext(), this, getMetaClass(),
                        returned.name, returned.rbarguments, returned.block);
            }
        }
        // Ignore other cases
    }

    // used by reified classes
    public void ensureThis(Object self) {
        if (getObject() == null) setObject(self);
    }

    @Deprecated(since = "10.0")
    protected static void initialize(final RubyClass concreteJavaProxy) {
        initialize(concreteJavaProxy.getRuntime().getCurrentContext(), concreteJavaProxy);
    }

    protected static void initialize(ThreadContext context, final RubyClass concreteJavaProxy) {
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

    /**
     * Because we can't physically associate an ID with a Java object, we can
     * only use the identity hashcode here.
     *
     * @return The identity hashcode for the Java object.
     */
    @Override
    public IRubyObject id() {
        return RubyFixnum.newFixnum(getRuntime(), System.identityHashCode(getObject()));
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
