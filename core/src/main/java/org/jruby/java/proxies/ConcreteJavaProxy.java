package org.jruby.java.proxies;

import static org.jruby.runtime.Visibility.PUBLIC;

import java.lang.reflect.*;
import java.util.ArrayList;

import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.*;
import org.jruby.internal.runtime.methods.*;
import org.jruby.internal.runtime.methods.JavaMethod.*;
import org.jruby.ir.IRMethod;
import org.jruby.ir.interpreter.ExitableInterpreterContext;
import org.jruby.ir.interpreter.ExitableInterpreterEngine;
import org.jruby.ir.interpreter.ExitableInterpreterEngineState;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.dispatch.CallableSelector.CallableCache;
import org.jruby.javasupport.*;
import org.jruby.javasupport.Java.JCtorCache;
import org.jruby.javasupport.proxy.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.collections.NonBlockingHashMapLong;

public class ConcreteJavaProxy extends JavaProxy {

    public ConcreteJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public ConcreteJavaProxy(Ruby runtime, RubyClass klazz, Object object) {
        super(runtime, klazz, object);
    }

    public static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
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
        	//System.err.println(parent.getName() + " is, (from NewMethod, original, a proxy) " + parent.getJavaProxy());// TODO: remove
        	if (parent.getJavaProxy()) return newMethod;
        	
        	// overridden class: reify and re-lookup new as reification changes it
            if (parent.getReifiedAnyClass() == null) {
            	parent.reifyWithAncestors(); // TODO: is this good?
            }
            //System.err.println(parent.getName() + " is " + parent.getJavaProxy());
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
				IRubyObject[] args, Block block)
		{
			try
			{
				if (oldInit == null)
				{
					System.out.println("ys");
				}
				ConcreteJavaProxy cjp = (ConcreteJavaProxy)self;
				//TODO: Insead of selectively overwriting, silently fail? or only use the other method/this method?
				if (cjp.getObject() == null)
				{
					withBlock.newInstance(cjp, args, block, context.runtime, clazz);
					// note: the generated ctor sets self.object = our discarded return of the new object
				}
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace(); //TODO: print?
				if (e instanceof InvocationTargetException)
				{
					InvocationTargetException ite = (InvocationTargetException) e; // TODO: move to mapIE?
					if (ite.getCause() instanceof RaiseException)
					{
						throw (RaiseException)ite.getCause();
					}
				}
				throw JavaProxyConstructor.mapInstantiationException(context.runtime, e);
			}
            return self;
		}
        
        public DynamicMethod getOriginal()
        {
        	return oldInit;
        }
        
        

		public static void tryInstall(Ruby runtime, RubyClass clazz, JavaProxyClass proxyClass,
				Class<? extends ReifiedJavaProxy> reified, boolean overwriteInitialize)
		{
			try
			{
				Constructor<? extends ReifiedJavaProxy> withBlock = reified.getConstructor(
							new Class[] { ConcreteJavaProxy.class, IRubyObject[].class, Block.class,
										Ruby.class, RubyClass.class});
				//TODO: move initialize to real_initialize
				//TODO: don't lock in this initialize method
				if (overwriteInitialize)
					clazz.addMethod("initialize", new StaticJCreateMethod(clazz, withBlock, clazz.searchMethod("initialize")));
				clazz.addMethod("__jallocate!", new StaticJCreateMethod(clazz, withBlock, null));
			}
			catch (SecurityException | NoSuchMethodException e)
			{
				// TODO log?
				//e.printStackTrace();
				// ignore, don't install
			}
		}
    }

  //TODO: cleanup
      public static final class NewMethodReified extends org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock {

          private final DynamicMethod initialize;
          private final Constructor<? extends ReifiedJavaProxy> ctor;

          //TODO: package?
          public NewMethodReified(final RubyClass clazz, Class<? extends ReifiedJavaProxy> reified) {
              super(clazz, Visibility.PUBLIC, "new");
              initialize = clazz.searchMethod("__jcreate!");

              Constructor<? extends ReifiedJavaProxy> withBlock;
              try
	  			{
	  				withBlock = reified.getConstructor(
	  							new Class[] { ConcreteJavaProxy.class, IRubyObject[].class, Block.class,
	  										Ruby.class, RubyClass.class});
	  			}
	  			catch (SecurityException | NoSuchMethodException e)
	  			{
	  				// ignore, don't install
	  				withBlock = null;
	  			}
              ctor = withBlock;
          }

  		@Override
  		public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
  				IRubyObject[] args, Block blk)
  		{
			//TODO: deduplicate this method, and decide on order of preference after testing 
  			if (ctor == null)
  			{
	  			JavaObject jo = (JavaObject)initialize.call(context, self, clazz, "new", args);
	  			return ((ReifiedJavaProxy)jo.getValue()).___jruby$rubyObject();
  			}
  			else
  			{
  				/*try
  				{

  		  			JavaObject jo = (JavaObject)initialize.call(context, self, clazz, "new", args);
  		  			return ((ReifiedJavaProxy)jo.getValue()).___jruby$rubyObject();
  				}//TODO: the latter two shouldn't be caught here
  				catch (ArgumentError | AssertionError | ClassCastException ae)*/
  				{
  					//System.out.println("AE");
  					// assume no easy conversions, use ruby fallback.
	  				ConcreteJavaProxy object = new ConcreteJavaProxy(context.runtime, (RubyClass) self);
	  				try
	  				{
	  					ctor.newInstance(object, args, blk, context.runtime, self);// TODO: clazz?
	  					// note: the generated ctor sets self.object = our discarded return of the new object
	  					return object;
	  				}
	  				catch (InstantiationException | IllegalAccessException | IllegalArgumentException
	  						| InvocationTargetException e)
	  				{
	  					//e.printStackTrace();
	  					throw JavaProxyConstructor.mapInstantiationException(context.runtime, e);
	  				}
  				}
  			}
  		}

      }
      

    //TODO: cleanup
    public static final class SimpleJavaInitializes {

        public static Object[] freshMethodArray(DynamicMethod initialize, Ruby runtime, IRubyObject self, RubyModule clazz, String name,
				IRubyObject[] args)
        {

			return new Object[] {
					runtime.newArray(args), //TODO:? 
		            runtime.newProc(Block.Type.LAMBDA, new Block(new JavaInternalBlockBody(runtime, Signature.from(initialize.getArity()))
					{
						
						@Override
						public IRubyObject yield(ThreadContext _context, IRubyObject[] _args)
						{
							return initialize.call(_context, self, clazz, name, args);
						}
						
					}))
			};
        }
        
        public static Object[] freshNopArray(Ruby runtime, IRubyObject[] args)
        {

			return new Object[] {
					runtime.newArray(args), 
		            runtime.newProc(Block.Type.LAMBDA, new Block(new JavaInternalBlockBody(runtime, Signature.OPTIONAL)
					{
						@Override
						public IRubyObject yield(ThreadContext _context, IRubyObject[] _args)
						{
							return _context.nil; // no body/super is java
						}
						
					}))
			};
        }

    }
    
    public static int findSuperLine(Ruby runtime, DynamicMethod dm, int start)
    {
    	// TODO: ???
		return start;
    }
    

	//TODO: test thar calls jcrwates new vs initialize
    // used by reified classes
    public Object[] splitInitialized(RubyClass base, IRubyObject[] args, Block blk)
    {
    	String name = base.getClassConfig().javaCtorMethodName;
		DynamicMethod dm = base.searchMethod(name);
		if (dm != null && (dm instanceof StaticJCreateMethod))
			dm = ((StaticJCreateMethod)dm).getOriginal();
		DynamicMethod dm1 = base.searchMethodLateral(name); // only on ourself //TODO: missing default
		if ((dm1 != null && !(dm instanceof InitializeMethod)&& !(dm instanceof StaticJCreateMethod))) //jcreate is for nested ruby classes from a java class
		{
            //TODO: if not defined, then ctors = all valid superctors
			
			AbstractIRMethod air = (AbstractIRMethod)dm; // TODO: getMetaClass() ? or base? (below     v)
			SplitSuperState<?> state = air.startSplitSuperCall(getRuntime().getCurrentContext(), this, getMetaClass(), name, args, blk);
			if (state == null) // no super in method
			{
				return new Object[] { getRuntime().newArray(args), air, name, blk };
			}
			else
			{
				return new Object[] { state.callArrayArgs, // TODO: nils?
						air, state };
			}
		}
		else
		{
			return new Object[] { getRuntime().newArray(args) }; //TODO: super if parent not java?
		}
    }

	

    // called from concrete reified code
    public void finishInitialize(Object[] returned)
    {
    	if (returned.length == 3)
    	{
            ((AbstractIRMethod)returned[1]).finishSplitCall((SplitSuperState<?>)returned[2]);
    	}
    	else if (returned.length == 4) // no super, direct call
    	{	
    		((AbstractIRMethod)returned[1]).call(getRuntime().getCurrentContext(), this, getMetaClass(), (String)returned[2], ((RubyArray)returned[0]).toJavaArrayMaybeUnsafe(), (Block)returned[3]);
    	}
    	// Ignore other cases
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
