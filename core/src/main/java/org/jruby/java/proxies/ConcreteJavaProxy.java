package org.jruby.java.proxies;

import java.lang.reflect.Field;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
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

    ///jcreates site
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
//new override
    private static final class NewMethod extends org.jruby.internal.runtime.methods.JavaMethod {
    	final DynamicMethod newMethod;

        NewMethod(final RubyClass clazz) {
            super(clazz, Visibility.PUBLIC, "new");
            newMethod = clazz.searchMethod("new");
        }
// TODO: reload this on method changes?
        private DynamicMethod reifyAndNewMethod(IRubyObject clazz) { 

        	RubyClass parent = ((RubyClass)clazz);
        	System.err.println(parent.getName() + " is " + parent.getJavaProxy());// TODO: remove
        	if (parent.getJavaProxy()) return newMethod;
        	
        	// overridden class: reify and re-lookup new as reification changes it
            if (parent.getReifiedClass() == null) {
            	parent.reifyWithAncestors();
            }
            //System.err.println(parent.getName() + " is " + parent.getJavaProxy());
            return new NewMethodReified(parent, parent.getReifiedClass());
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
    
//TODO: cleanup
    public static final class NewMethodReified extends org.jruby.internal.runtime.methods.JavaMethod.JavaMethodN {

        private Field rubyObject;
        private final DynamicMethod initialize;

        //TODO: package?
        public NewMethodReified(final RubyClass clazz, final Class reified) {
            super(clazz, Visibility.PUBLIC, "new");
            initialize = clazz.searchMethod("__jcreate!");
        }

		@Override
		public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
				IRubyObject[] args)
		{
			
			JavaObject jo = (JavaObject)initialize.call(context, self, clazz, "new", args);
			return ((ReifiedJavaProxy)jo.getValue()).___jruby$rubyObject();
		}

    }
    
    // used by reified classes
    public RubyArray splitInitialized(IRubyObject[] args)
    {
    	///  TODO: move gen here
    	return callMethod("j_initialize", args).convertToArray();
    }
    
    // used by reified classes
    public void ensureThis(Object self)
    {
    	if (getObject() == null)
    		setObject(self);
    }

    protected static void initialize(final RubyClass concreteJavaProxy) {
        concreteJavaProxy.addMethod("initialize", new InitializeMethod(concreteJavaProxy));
        if (concreteJavaProxy.getName().equals("ConcreteJavaProxy"))
        {}
        else if (concreteJavaProxy.getName().equals("MapJavaProxy"))
        {}
        else
        System.err.println("adding to " + concreteJavaProxy.getName()); //TODO: remove
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
        if (object == null)
        {
        	System.out.println(":-(");
        	return null;
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
