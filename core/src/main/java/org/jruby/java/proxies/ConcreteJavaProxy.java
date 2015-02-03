package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
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

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new ConcreteJavaProxy(runtime, klazz);
        }
    };

    public static RubyClass createConcreteJavaProxy(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final RubyClass JavaProxy = runtime.getJavaSupport().getJavaProxyClass();
        RubyClass ConcreteJavaProxy = runtime.defineClass("ConcreteJavaProxy", JavaProxy, ALLOCATOR);
        initialize(ConcreteJavaProxy);
        return ConcreteJavaProxy;
    }

    private static class InitializeMethod extends org.jruby.internal.runtime.methods.JavaMethod {

        private final CallSite jcreateSite = MethodIndex.getFunctionalCallSite("__jcreate!");

        InitializeMethod(final RubyClass clazz) { super(clazz, Visibility.PRIVATE); }

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

    private static class NewMethod extends org.jruby.internal.runtime.methods.JavaMethod {

        private final CallSite jcreateSite = MethodIndex.getFunctionalCallSite("__jcreate!");
        final DynamicMethod newMethod;

        NewMethod(final RubyClass clazz) {
            super(clazz, Visibility.PUBLIC);
            newMethod = clazz.searchMethod("new");
        }

        private boolean needsCreate(IRubyObject proxy) {
            return ((JavaProxy) proxy).object == null;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", args, block);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, args, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", block);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, block);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, arg0, block);

            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1, block);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, arg0, arg1, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1, arg2, block);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, arg0, arg1, arg2, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", args);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, args);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy");
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, arg0);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, arg0, arg1);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1, arg2);
            if (needsCreate(proxy)) jcreateSite.call(context, proxy, proxy, arg0, arg1, arg2);
            return proxy;
        }

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
    public Object toJava(final Class type) {
        final Object object = getObject();
        final Class clazz = object.getClass();

        if ( type.isPrimitive() ) {
            if ( type == Void.TYPE ) return null;

            if ( object instanceof Number && type != Boolean.TYPE ||
                 object instanceof Character && type == Character.TYPE ||
                 object instanceof Boolean && type == Boolean.TYPE ) {
                // FIXME in more permissive call paths, like invokedynamic, this can allow
                // precision-loading downcasts to happen silently
                return object;
            }
        }
        else if (type.isAssignableFrom(clazz)) {
            if ( Java.OBJECT_PROXY_CACHE || metaClass.getCacheProxy() ) {
                getRuntime().getJavaSupport().getObjectProxyCache().put(object, this);
            }
            return object;
        }

        throw getRuntime().newTypeError("failed to coerce " + clazz.getName() + " to " + type.getName());
    }
}
