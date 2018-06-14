package org.jruby.java.proxies;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.CodegenUtils;
import org.jruby.util.StringSupport;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

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

        private transient CallSite jcreateSite;
        final DynamicMethod newMethod;

        NewMethod(final RubyClass clazz) {
            super(clazz, Visibility.PUBLIC, "new");
            newMethod = clazz.searchMethod("new");
        }

        private CallSite jcreateSite() { // most of the time we won't need to instantiate
            CallSite callSite = jcreateSite;
            if (callSite == null) {
                callSite = jcreateSite = MethodIndex.getFunctionalCallSite("__jcreate!");
            }
            return callSite;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", args, block);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, args, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", block);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, block);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, arg0, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1, block);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, arg0, arg1, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1, arg2, block);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, arg0, arg1, arg2, block);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", args);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, args);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy");
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, arg0);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, arg0, arg1);
            return proxy;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            IRubyObject proxy = newMethod.call(context, self, clazz, "new_proxy", arg0, arg1, arg2);
            if ( ((JavaProxy) proxy).object == null ) jcreateSite().call(context, proxy, proxy, arg0, arg1, arg2);
            return proxy;
        }

    }

    protected static void initialize(final RubyClass ConcreteJavaProxy) {
        ConcreteJavaProxy.addMethod("initialize", new InitializeMethod(ConcreteJavaProxy));
        // We define a custom "new" method to ensure that __jcreate! is getting called,
        // so that if the user doesn't call super in their subclasses, the object will
        // still get set up properly. See JRUBY-4704.
        RubyClass singleton = ConcreteJavaProxy.getSingletonClass();
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

    @JRubyMethod
    public RubyString reflective_inspect(ThreadContext context) {
        return RubyString.newString(context.runtime, reflectiveToString(context, null));
    }

    @JRubyMethod
    public RubyString reflective_inspect(ThreadContext context, IRubyObject opts) {
        return RubyString.newString(context.runtime, reflectiveToString(context, opts == context.nil ? null : opts.convertToHash()));
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return inspect(context.runtime, null); // -> toStringImpl
    }

    @Override
    CharSequence toStringImpl(final Ruby runtime, final IRubyObject opts) {
        final ThreadContext context = runtime.getCurrentContext();
        DynamicMethod toString = toStringIfNotFromObject(context);
        if (toString == null) {
            return reflectiveToString(context, (RubyHash) opts); // a replacement for java.lang.Object#toString
        }
        IRubyObject str = toString.call(context, this, getMetaClass(), "toString");
        return str == context.nil ? "null" : str.convertToString(); // we don't return a nil (unlike to_s)
    }

    private transient CachingCallSite toStringSite;

    private DynamicMethod toStringIfNotFromObject(final ThreadContext context) {
        if (toStringSite == null) toStringSite = new FunctionalCachingCallSite("toString");

        CacheEntry cache = toStringSite.retrieveCache(getMetaClass());

        return (cache.method.getImplementationClass() != context.runtime.getJavaSupport().getObjectClass()) ? cache.method : null;
    }

    private StringBuilder reflectiveToString(final ThreadContext context, final RubyHash opts) {
        String[] excludedFields = null;
        if (opts != null) { // NOTE: support nested excludes?!?
            IRubyObject[] args = ArgsUtil.extractKeywordArgs(context, opts, "exclude");
            if (args[0] instanceof RubyArray) {
                excludedFields = (String[]) ((RubyArray) args[0]).toArray(StringSupport.EMPTY_STRING_ARRAY);
            }
        }

        final StringBuilder buffer = new StringBuilder(192);

        final Object obj = getObject();
        Class<?> clazz = obj.getClass();

        // TODO use meta-class name?
        buffer.append("#<").append(getMetaClass().getRealClass().getName())
              .append(":0x").append(Integer.toHexString(inspectHashCode()));

        final Ruby runtime = context.runtime;

        if (runtime.isInspecting(obj)) return buffer.append(" ...>");

        try {
            runtime.registerInspecting(obj);

            char sep = appendFields(context, buffer, obj, clazz, '\0', excludedFields);

            while (clazz.getSuperclass() != null) {
                clazz = clazz.getSuperclass();
                sep = appendFields(context, buffer, obj, clazz, sep, excludedFields);
            }

            return buffer.append('>');
        }
        finally {
            runtime.unregisterInspecting(obj);
        }
    }

    private static char appendFields(final ThreadContext context, final StringBuilder buffer,
                                     Object obj, Class<?> clazz, char sep, final String[] excludedFields) {
        final Field[] fields = clazz.getDeclaredFields();
        AccessibleObject.setAccessible(fields, true);

        for (final Field field : fields) {
            if (accept(field, excludedFields)) {
                buffer.append(' ');
                if (sep == '\0') sep = ',';
                else buffer.append(sep);

                try {
                    buffer.append(field.getName()).append('=').append(toStringValue(context, field.get(obj)));
                }
                catch (IllegalAccessException ex) { throw new AssertionError(ex); } // we've set accessible
            }
        }
        return sep;
    }

    private static CharSequence toStringValue(final ThreadContext context, final Object value) {
        if (value == null) return "null";
        if (value instanceof CharSequence) return (CharSequence) value; // String
        final Class<?> klass = value.getClass();
        // take short-cuts for known Java toString impls :
        if (klass.isPrimitive() || klass.isEnum()) return value.toString();
        if (klass == Integer.class || klass == Long.class ||
            klass == Short.class || klass == Byte.class ||
            klass == Double.class || klass == Float.class ||
            klass == Boolean.class || klass == Character.class) return value.toString();
        //if (klass.isSynthetic()) return value.toString(); // TODO skip - unwrap class!?!
        if (value instanceof IRubyObject) {
            if (value instanceof JavaProxy) {
                return ((JavaProxy) value).toStringImpl(context.runtime, null);
            }
            return ((IRubyObject) value).inspect().convertToString();
        }
        return ((JavaProxy) Java.wrapJavaObject(context.runtime, value)).toStringImpl(context.runtime, null);
    }

    private static final char INNER_CLASS_SEPARATOR_CHAR = '$';

    private static boolean accept(final Field field, final String[] excludedFields) {
        final int mod = field.getModifiers();
        if (Modifier.isTransient(mod) || Modifier.isStatic(mod)) return false;

        final String name = field.getName();
        if (name.indexOf(INNER_CLASS_SEPARATOR_CHAR) != -1) return false;
        if (excludedFields != null && Arrays.binarySearch(excludedFields, name) >= 0) {
            return false;
        }

        return true;
    }

}
