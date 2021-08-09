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

import static org.jruby.runtime.Visibility.PUBLIC;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.SplitSuperState;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.java.invokers.RubyToJavaInvoker;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.Java.JCreateMethod;
import org.jruby.javasupport.Java.JCtorCache;
import org.jruby.javasupport.JavaConstructor;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.JavaInternalBlockBody;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class ConcreteJavaProxy extends JavaProxy {

    public ConcreteJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public ConcreteJavaProxy(Ruby runtime, RubyClass klazz, Object object) {
        super(runtime, klazz, object);
    }

    public static RubyClass createConcreteJavaProxy(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final RubyClass JavaProxy = runtime.getJavaSupport().getJavaProxyClass();
        RubyClass ConcreteJavaProxy = runtime.defineClass("ConcreteJavaProxy", JavaProxy, ConcreteJavaProxy::new);
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
        private DynamicMethod reifyAndNewMethod(IRubyObject clazz) {

            RubyClass parent = ((RubyClass) clazz);
            if (parent.getJavaProxy()) return newMethod;

            // overridden class: reify and re-lookup new as reification changes it
            if (parent.getReifiedClass() == null) {
                parent.reifyWithAncestors();
                if (parent.getReifiedClass() == null) {
                    throw clazz.getRuntime().newTypeError("requested class " + parent.getName() + " was not reifiable");
                }
            }

            return new NewMethodReified(parent, parent.getReifiedJavaClass());
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return reifyAndNewMethod(self).call(context, self, clazz, "new_proxy", args, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        	return reifyAndNewMethod(self).call(context, self, clazz, "new",block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        	return reifyAndNewMethod(self).call(context, self, clazz, "new",arg0, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        	return reifyAndNewMethod(self).call(context, self, clazz, "new",arg0, arg1, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        	return reifyAndNewMethod(self).call(context, self, clazz, "new",arg0, arg1, arg2, block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        	return reifyAndNewMethod(self).call(context, self, clazz, "new",args);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        	return reifyAndNewMethod(self).call(context, self, clazz,"new_proxy");
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        	return reifyAndNewMethod(self).call(context, self, clazz, "new_proxy",arg0);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        	return reifyAndNewMethod(self).call(context, self, clazz,"new", arg0, arg1);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        	return reifyAndNewMethod(self).call(context, self, clazz,"new", arg0, arg1, arg2);
        }

    }
    
    /**
     * Manually added as an override of `new` for Concrete Extension
     */
    public static class StaticJCreateMethod extends JavaMethodNBlock {

        private Constructor<? extends ReifiedJavaProxy> withBlock;
        private DynamicMethod oldInit;

        StaticJCreateMethod(RubyModule cls, Constructor<? extends ReifiedJavaProxy> withBlock2, DynamicMethod oldinit) {
            super(cls, PUBLIC, "__jcreate_static!");
            this.withBlock = withBlock2;
            this.oldInit = oldinit;
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

        public DynamicMethod getOriginal() {
            return oldInit;
        }

        public static void tryInstall(Ruby runtime, RubyClass clazz, JavaProxyClass proxyClass,
                Class<? extends ReifiedJavaProxy> reified, boolean overwriteInitialize) {
            try {
                Constructor<? extends ReifiedJavaProxy> withBlock = reified.getConstructor(new Class[] {
                        ConcreteJavaProxy.class, IRubyObject[].class, Block.class, Ruby.class, RubyClass.class });
                // TODO: move initialize to real_initialize
                // TODO: don't lock in this initialize method
                if (overwriteInitialize) clazz.addMethod("initialize",
                        new StaticJCreateMethod(clazz, withBlock, clazz.searchMethod("initialize")));
                clazz.addMethod("__jallocate!", new StaticJCreateMethod(clazz, withBlock, null));
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
                withBlock = reified.getConstructor(new Class[] { ConcreteJavaProxy.class, IRubyObject[].class,
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
                JavaObject jo = (JavaObject) initialize.call(context, self, clazz, "new", args);
                return ((ReifiedJavaProxy) jo.getValue()).___jruby$rubyObject();
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
     * Used by reified classes, this class is tightly coupled with RealClassGenerator, splitInitialize, & finishInitialize
     * Do not refactor without looking at RCG
     */
    public static final class SplitCtorData {
        // public fields used by RealClassGenerator's generated code
        public final Object[] arguments;
        public final int ctorIndex;
        
        // public field used by finishInitialized &  (ruby < ruby < java) generated classes
        public final IRubyObject[] rbarguments;
        public Block blk = Block.NULL_BLOCK;

        /**
         * Picks and converts arguments for the super call
         * Leaves ctorIndex and arguments ready for the super call
         */
        public SplitCtorData(IRubyObject[] args, JCtorCache cache, Ruby rt) {
            rbarguments = args;
            if (cache == null) { // (ruby < ruby < java) super call from one IRO to another IRO ctor
                ctorIndex = -1;
                arguments = null;
            } else {
                ctorIndex = JCreateMethod.forTypes(args, cache, rt);
                arguments = RubyToJavaInvoker.convertArguments(cache.constructors[ctorIndex], args);
            }
        }

        public SplitCtorData(IRubyObject[] args, JCtorCache cache, Ruby rt, AbstractIRMethod air2, SplitSuperState<?> state2,
                Block blk2) {
            this(args, cache, rt);
            air = air2;
            state =state2;
            blk = blk2;
        }

        public SplitCtorData(IRubyObject[] args, JCtorCache cache, Ruby rt, AbstractIRMethod air2, String name2, Block blk2) {
            this(args, cache, rt);
            air = air2;
            name = name2;
            blk = blk2;
        }

        // fields below are only used in ConcreteJavaProxy finishInitialize
        private AbstractIRMethod air = null;
        private String name = null;
        private SplitSuperState<?> state = null;
    }

    /**
     * Used by reified classes, this method is tightly coupled with RealClassGenerator, finishInitialize
     * Do not refactor without looking at RCG
     * @return An object used by reified code and the finishInitialize method
     */
    public SplitCtorData splitInitialized(RubyClass base, IRubyObject[] args, Block blk, JCtorCache jcc) {
        String name = base.getClassConfig().javaCtorMethodName;
        DynamicMethod dm = base.searchMethod(name);
        if (dm != null && (dm instanceof StaticJCreateMethod)) dm = ((StaticJCreateMethod) dm).getOriginal();
        DynamicMethod dm1 = base.searchMethodLateral(name); // only on ourself //TODO: missing default

        // jcreate is for nested ruby classes from a java class
        if ((dm1 != null && !(dm instanceof InitializeMethod) && !(dm instanceof StaticJCreateMethod))) {

            AbstractIRMethod air = (AbstractIRMethod) dm; // TODO: getMetaClass() ? or base? (below v)

            SplitSuperState<?> state = air.startSplitSuperCall(getRuntime().getCurrentContext(), this, getMetaClass(),
                    name, args, blk);
            if (state == null) { // no super in method
                return new SplitCtorData(args, jcc, getRuntime(), air, name, blk);
            } else {
                return new SplitCtorData(state.callArrayArgs.toJavaArrayMaybeUnsafe(), jcc, getRuntime(), air, state, blk);
            }
        } else {
            return new SplitCtorData(args, jcc, getRuntime());
        }
    }

    /**
     * Used by reified classes, this method is tightly coupled with RealClassGenerator, splitInitialize
     * Do not refactor without looking at RCG
     */
    public void finishInitialize(SplitCtorData returned) {
        if (returned.air != null) {
            if (returned.state != null) {
                returned.air.finishSplitCall(returned.state);
            } else { // no super, direct call
                returned.air.call(getRuntime().getCurrentContext(), this, getMetaClass(),
                        returned.name, returned.rbarguments, returned.blk);
            }
        }
        // Ignore other cases
    }

    // used by reified classes
    public void ensureThis(Object self) {
        if (getObject() == null) setObject(self);
    }

    protected static void initialize(final RubyClass concreteJavaProxy) {
        concreteJavaProxy.addMethod("initialize", new InitializeMethod(concreteJavaProxy));
        // We define a custom "new" method to ensure that __jcreate! is getting called,
        // so that if the user doesn't call super in their subclasses, the object will
        // still get set up properly. See JRUBY-4704.
        RubyClass singleton = concreteJavaProxy.getSingletonClass();
        singleton.addMethod("new", new NewMethod(singleton));
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
        return getRuntime().newFixnum(System.identityHashCode(getObject()));
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

        throw getRuntime().newTypeError("failed to coerce " + clazz.getName() + " to " + type.getName());
    }
}
