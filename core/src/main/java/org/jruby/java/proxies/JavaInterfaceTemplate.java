package org.jruby.java.proxies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodN;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOne;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOneBlock;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtilities;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaInterfaceTemplate {

    public static RubyModule createJavaInterfaceTemplateModule(ThreadContext context) {
        final Ruby runtime = context.runtime;
        RubyModule JavaInterfaceTemplate = runtime.defineModule("JavaInterfaceTemplate");

        RubyClass singleton = JavaInterfaceTemplate.getSingletonClass();
        singleton.addReadAttribute(context, "java_class");
        singleton.defineAnnotatedMethods(JavaInterfaceTemplate.class);

        JavaInterfaceTemplate.defineAnnotatedMethods(JavaProxy.ClassMethods.class);

        return JavaInterfaceTemplate;
    }

    @Deprecated // not used - should go away in >= 9.2
    // not intended to be called directly by users (private)
    // OLD TODO from Ruby code:
    // This should be implemented in JavaClass.java, where we can
    // check for reserved Ruby names, conflicting methods, etc.
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public static IRubyObject implement(ThreadContext context, IRubyObject self, IRubyObject clazz) {
        if ( ! (clazz instanceof RubyModule) ) {
            final Ruby runtime = context.runtime;
            throw runtime.newTypeError(clazz, runtime.getModule());
        }

        final RubyModule targetModule = (RubyModule) clazz;
        final JavaClass javaClass = getJavaClassForInterface(self);

        final Method[] javaInstanceMethods = javaClass.javaClass().getMethods();
        final DynamicMethod dummyMethodImpl = new DummyMethodImpl(targetModule);

        for (int i = 0; i < javaInstanceMethods.length; i++) {
            final Method javaMethod = javaInstanceMethods[i];
            final String name = javaMethod.getName();
            if ( targetModule.searchMethod(name).isUndefined() ) {
                targetModule.addMethod(name, dummyMethodImpl); // only those not-defined
            }
        }

        return context.nil;
    }

    private static class DummyMethodImpl extends org.jruby.internal.runtime.methods.JavaMethod {

        DummyMethodImpl(RubyModule targetModule) {
            super(targetModule, Visibility.PUBLIC);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return context.nil; // dummy bodies for default impls
        }

    }

    @JRubyMethod(frame = true) // framed for invokeSuper
    public static IRubyObject append_features(ThreadContext context, IRubyObject self, IRubyObject clazz, Block block) {
        if ( clazz instanceof RubyClass ) {
            appendFeaturesToClass(context, self, (RubyClass) clazz);
        }
        else if ( clazz instanceof RubyModule ) {
            appendFeaturesToModule(context, self, (RubyModule) clazz);
        }
        else {
            throw context.runtime.newTypeError("received " + clazz + ", expected Class/Module");
        }

        return Helpers.invokeSuper(context, self, clazz, block);
    }

    private static void appendFeaturesToClass(ThreadContext context, final IRubyObject self, final RubyClass clazz) {
        final Ruby runtime = context.runtime;
        checkAlreadyReified(clazz, runtime);

        final JavaClass javaClass = getJavaClassForInterface(self);
        RubyArray javaInterfaces;
        if ( ! clazz.hasInstanceVariable("@java_interfaces") ) {
            javaInterfaces = RubyArray.newArray(runtime, javaClass);
            clazz.setInstanceVariable("@java_interfaces", javaInterfaces);

            initInterfaceImplMethods(context, clazz);
        }
        else {
            javaInterfaces = (RubyArray) clazz.getInstanceVariable("@java_interfaces");
            // we've already done the above priming logic, just add another interface
            // to the list of intentions unless we're past the point of no return or
            // already intend to implement the given interface
            if ( ! ( javaInterfaces.isFrozen() || javaInterfaces.includes(context, javaClass) ) ) {
                javaInterfaces.append(javaClass);
            }
        }
    }

    private static void checkAlreadyReified(final RubyClass clazz, Ruby runtime) throws RaiseException {
        // not allowed for original (non-generated) Java classes
        // note: not allowing for any previously created class right now;
        // this restriction might be loosened later for generated classes
        if ( ( Java.NEW_STYLE_EXTENSION && clazz.getReifiedClass() != null )
                ||
                ( clazz.hasInstanceVariable("@java_class")
                    && clazz.getInstanceVariable("@java_class").isTrue()
                    && !clazz.getSingletonClass().isMethodBound("java_proxy_class", false) )
                ||
                ( clazz.hasInstanceVariable("@java_proxy_class")
                    && clazz.getInstanceVariable("@java_proxy_class").isTrue() ) ) {
            throw runtime.newArgumentError("can not add Java interface to existing Java class");
        }
    }

    private static void initInterfaceImplMethods(ThreadContext context, final RubyClass clazz) {
        // setup new, etc unless this is a ConcreteJavaProxy subclass
        // For JRUBY-4571, check both these, since JavaProxy extension stuff adds the former and this code adds the latter
        if (!(clazz.isMethodBound("__jcreate!", false) || clazz.isMethodBound("__jcreate_meta!", false))) {
            // First we make modifications to the class, to adapt it to being
            // both a Ruby class and a proxy for a Java type

            RubyClass singleton = clazz.getSingletonClass();

            // list of interfaces we implement
            singleton.addReadAttribute(context, "java_interfaces");

            if ( ( ! Java.NEW_STYLE_EXTENSION && clazz.getSuperClass().getRealClass().hasInstanceVariable("@java_class") )
                || RubyInstanceConfig.INTERFACES_USE_PROXY ) {
                // superclass is a Java class...use old style impl for now

                // The replacement "new" allocates and inits the Ruby object as before, but
                // also instantiates our proxified Java object by calling __jcreate!
                final ObjectAllocator proxyAllocator = clazz.getAllocator();
                clazz.setAllocator(new ObjectAllocator() {
                    public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                        IRubyObject newObj = proxyAllocator.allocate(runtime, klazz);
                        Helpers.invoke(runtime.getCurrentContext(), newObj, "__jcreate!");
                        return newObj;
                    }
                });

                // jcreate instantiates the proxy object which implements all interfaces
                // and which is wrapped and implemented by this object
                clazz.addMethod("__jcreate!", new InterfaceProxyFactory(clazz));
            } else {
                // The new "new" actually generates a real Java class to use for the Ruby class's
                // backing store, instantiates that, and then calls initialize on it.
                addRealImplClassNew(clazz);
            }

            // Next, we define a few private methods that we'll use to manipulate
            // the Java object contained within this Ruby object

            // Used by our duck-typification of Proc into interface types, to allow
            // coercing a simple proc into an interface parameter.
            clazz.addMethod("__jcreate_meta!", new InterfaceProxyFactory(clazz));

            // If we hold a Java object, we need a java_class accessor
            clazz.addMethod("java_class", new JavaClassAccessor(clazz));
        }

        // Now we add an "implement" and "implement_all" methods to the class
        if ( ! clazz.isMethodBound("implement", false) ) {
            final RubyClass singleton = clazz.getSingletonClass();

            // implement is called to force this class to create stubs for all methods in the given interface,
            // so they'll show up in the list of methods and be invocable without passing through method_missing
            singleton.addMethod("implement", new JavaMethodOne(clazz, Visibility.PRIVATE) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject iface) {
                    final RubyArray ifaces = getJavaInterfaces(self);
                    if ( ifaces != null && ifaces.includes(context, iface) ) {
                        return Helpers.invoke(context, iface, "implement", self);
                    }
                    return context.nil;
                }
            });

            // implement all forces implementation of all interfaces we intend for this class to implement
            singleton.addMethod("implement_all", new JavaMethodOne(clazz, Visibility.PRIVATE) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                    final RubyArray ifaces = getJavaInterfaces(self);
                    if ( ifaces == null ) return context.nil;
                    for ( int i = 0; i < ifaces.size(); i++ ) {
                        final RubyModule iface = Java.get_interface_module(context.runtime, ifaces.eltInternal(i));
                        Helpers.invoke(context, iface, "implement", self);
                    }
                    return ifaces;
                }
            });
        }
    }

    private static final class InterfaceProxyFactory extends JavaMethodN { // __jcreate! and __jcreate_meta!

        InterfaceProxyFactory(final RubyClass clazz) { super(clazz, Visibility.PRIVATE); }

        @Override // will be called with zero args
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return newInterfaceProxy(self);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            return newInterfaceProxy(self);
        }

    }

    private static class JavaClassAccessor extends JavaMethodZero {

        JavaClassAccessor(final RubyClass klass) { super(klass, Visibility.PUBLIC); }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            final Object wrapped = self.dataGetStruct();
            final Class<?> javaClass;
            if ( wrapped != null ) {
                javaClass = ((JavaObject) wrapped).getJavaClass();
            }
            else {
                javaClass = self.getClass(); // NOTE what is this for?
            }
            return JavaClass.get(context.runtime, javaClass);
        }

    }

    public static void addRealImplClassNew(final RubyClass clazz) {
        clazz.setAllocator(new ObjectAllocator() {
            private Constructor proxyConstructor;

            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                // if we haven't been here before, reify the class
                Class reifiedClass = klazz.getReifiedClass();
                if (proxyConstructor == null || proxyConstructor.getDeclaringClass() != reifiedClass) {
                    if (reifiedClass == null) {
                        reifiedClass = Java.generateRealClass(klazz);
                    }
                    proxyConstructor = Java.getRealClassConstructor(runtime, reifiedClass);
                }
                IRubyObject newObj = Java.constructProxy(runtime, proxyConstructor, klazz);

                return newObj;
            }
        });
    }

    private static IRubyObject newInterfaceProxy(final IRubyObject self) {
        final RubyClass current = self.getMetaClass();
        // construct the new interface impl and set it into the object
        JavaObject newObject = Java.newInterfaceImpl(self, Java.getInterfacesFromRubyClass(current));
        JavaUtilities.set_java_object(self, self, newObject); // self.dataWrapStruct(newObject);
        return newObject;
    }

    private static void appendFeaturesToModule(ThreadContext context, final IRubyObject self, final RubyModule module) {
        // assuming the user wants a collection of interfaces that can be
        // included together. make it so.
        final Ruby runtime = context.runtime;

        final IRubyObject java_class = module.getInstanceVariables().getInstanceVariable("@java_class");
        // not allowed for existing Java interface modules
        if (java_class != null && java_class.isTrue()) {
            throw runtime.newTypeError("can not add Java interface to existing Java interface");
        }

        // To turn a module into an "interface collection" we add a class instance
        // variable to hold the list of interfaces, and modify append_features
        // for this module to call append_features on each of those interfaces as
        // well
        synchronized (module) {
            if ( initInterfaceModules(self, module) ) { // true - initialized
                final RubyClass singleton = module.getSingletonClass();
                singleton.addMethod("append_features", new AppendFeatures(singleton));
            }
            else {
                // already set up append_features, just add the interface if we haven't already
                final RubyArray interfaceModules = getInterfaceModules(module);
                if ( ! interfaceModules.includes(context, self) ) {
                    interfaceModules.append(self);
                }
            }
        }
    }

    private static class AppendFeatures extends JavaMethodOneBlock {

        AppendFeatures(RubyModule singletonClass) { super(singletonClass, Visibility.PUBLIC); }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block) {
            if ( ! ( arg instanceof RubyModule ) ) {
                throw context.runtime.newTypeError("append_features called with non-module");
            }

            final RubyModule target = (RubyModule) arg;
            target.include( getInterfaceModules(self).toJavaArrayMaybeUnsafe() );

            return Helpers.invokeAs(context, clazz.getSuperClass(), self, name, arg, block);
        }

    }

    @JRubyMethod
    public static IRubyObject extended(ThreadContext context, IRubyObject self, IRubyObject object) {
        RubyClass singleton = object.getSingletonClass();
        singleton.include(context, self);
        return singleton;
    }

    @JRubyMethod(name = "[]", rest = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return JavaProxy.op_aref(context, self, args);
    }

    @JRubyMethod(name = "impl", rest = true) // impl(methods = true)
    public static IRubyObject impl(ThreadContext context, IRubyObject self, IRubyObject[] args, final Block implBlock) {
        final Ruby runtime = context.runtime;

        if ( ! implBlock.isGiven() ) {
            throw runtime.newArgumentError("block required to call #impl on a Java interface");
        }

        boolean allMethods = true;
        final IRubyObject[] methodNames;
        if ( args.length == 0 ) methodNames = null;
        else if ( args.length == 1 && args[0] instanceof RubyBoolean ) {
            allMethods = args[0].isTrue(); // impl(false) ... allMethods = false
            methodNames = null;
        }
        else {
            methodNames = args.clone();
            Arrays.sort(methodNames); // binarySearch needs a sorted array
            // RubySymbol implements a Java compareTo thus will always work
        }

        RubyClass implClass = RubyClass.newClass(runtime, runtime.getObject()); // ImplClass = Class.new
        implClass.include(context, self); // ImplClass.include Interface

        final BlockInterfaceImpl ifaceImpl = new BlockInterfaceImpl(implClass, implBlock, methodNames);
        implClass.addMethod("method_missing", ifaceImpl); // def ImplClass.method_missing ...

        final Class<?> ifaceClass = JavaClass.getJavaClass(context, ((RubyModule) self));
        if ( methodNames == null ) {
            final BlockInterfaceImpl.ConcreteMethod implMethod = ifaceImpl.getConcreteMethod();
            for ( Method method : ifaceClass.getMethods() ) {
                if ( method.isBridge() || method.isSynthetic() ) continue;
                if ( Modifier.isStatic( method.getModifiers() ) ) continue;
                // override default methods (by default) - users should pass down method names or impl(false) { ... }
                if ( ! allMethods && ! Modifier.isAbstract( method.getModifiers() ) ) continue;
                implClass.addMethodInternal(method.getName(), implMethod); // might add twice - its fine
            }
        }
        else {
            final BlockInterfaceImpl.ConcreteMethod implMethod = ifaceImpl.getConcreteMethod();
            final Method[] decMethods = ifaceClass.getDeclaredMethods();
            loop: for ( IRubyObject methodName : methodNames ) {
                final String name = methodName.toString();
                for ( int i = 0; i < decMethods.length; i++ ) {
                    final Method method = decMethods[i];
                    if ( method.isBridge() || method.isSynthetic() ) continue;
                    if ( Modifier.isStatic( method.getModifiers() ) ) continue;
                    // add if its a declared method of the interface or its super-interfaces
                    if ( name.equals(decMethods[i].getName()) ) {
                        implClass.addMethodInternal(name, implMethod);
                        continue loop;
                    }
                }
                // did not continue (main) loop - passed method name not found in interface
                runtime.getWarnings().warn("`" + name + "' is not a declared method in interface " + ifaceClass.getName());
            }
        }

        return implClass.callMethod(context, "new"); // ImplClass.new
    }

    private static final class BlockInterfaceImpl extends JavaMethod {

        private final IRubyObject[] methodNames; // RubySymbol[]
        private final Block implBlock;

        BlockInterfaceImpl(final RubyClass implClass, final Block implBlock, final IRubyObject[] methodNames) {
            super(implClass, Visibility.PUBLIC);
            this.implBlock = implBlock; this.methodNames = methodNames;
        }

        @Override // method_missing impl
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            Arity.checkArgumentCount(context.runtime, name, args.length, 1, -1);

            return callImpl(context, clazz, block, args);
        }

        private IRubyObject callImpl(ThreadContext context, RubyModule clazz, Block block, IRubyObject... args) {
            if ( methodNames == null ) return implBlock.call(context, args); // "hot" path

            if ( methodNames.length == 1 ) {
                if ( methodNames[0].equals(args[0]) ) return implBlock.call(context, args);
            }
            else if ( Arrays.binarySearch(methodNames, args[0]) >= 0 ) {
                return implBlock.call(context, args);
            }

            return clazz.getSuperClass().callMethod(context, "method_missing", args, block);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
            return callImpl(context, klazz, block); // avoids checkArgumentCount
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, Block block) {
            return callImpl(context, klazz, block, arg0); // avoids checkArgumentCount
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return callImpl(context, klazz, block, arg0, arg1); // avoids checkArgumentCount
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return callImpl(context, klazz, block, arg0, arg1, arg2); // avoids checkArgumentCount
        }

        public DynamicMethod dup() { return this; }

        final ConcreteMethod getConcreteMethod() { return new ConcreteMethod(); }

        private final class ConcreteMethod extends JavaMethod {

            ConcreteMethod() {
                super(BlockInterfaceImpl.this.implementationClass, Visibility.PUBLIC);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
                final IRubyObject[] nargs = new IRubyObject[] { context.runtime.newSymbol(name) };
                return BlockInterfaceImpl.this.callImpl(context, klazz, block, nargs);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, Block block) {
                final IRubyObject[] nargs = new IRubyObject[] { context.runtime.newSymbol(name), arg0 };
                return BlockInterfaceImpl.this.callImpl(context, klazz, block, nargs);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
                final IRubyObject[] nargs = new IRubyObject[] { context.runtime.newSymbol(name), arg0, arg1 };
                return BlockInterfaceImpl.this.callImpl(context, klazz, block, nargs);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
                final IRubyObject[] nargs = new IRubyObject[] { context.runtime.newSymbol(name), arg0, arg1, arg2 };
                return BlockInterfaceImpl.this.callImpl(context, klazz, block, nargs);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
                switch (args.length) {
                    case 0:
                        return call(context, self, klazz, name, block);
                    case 1:
                        return call(context, self, klazz, name, args[0], block);
                    case 2:
                        return call(context, self, klazz, name, args[0], args[1], block);
                    case 3:
                        return call(context, self, klazz, name, args[0], args[1], args[2], block);
                    default:
                        final IRubyObject[] nargs = new IRubyObject[args.length + 1];
                        nargs[0] = context.runtime.newSymbol(name);
                        System.arraycopy(args, 0, nargs, 1, args.length);
                        return BlockInterfaceImpl.this.callImpl(context, klazz, block, nargs);
                }
            }

        }

    }

    private static JavaClass getJavaClassForInterface(final IRubyObject module) {
        return (JavaClass) module.getInstanceVariables().getInstanceVariable("@java_class");
    }

    private static RubyArray getJavaInterfaces(final IRubyObject clazz) {
        return (RubyArray) clazz.getInstanceVariables().getInstanceVariable("@java_interfaces");
    }

    private static RubyArray getInterfaceModules(final IRubyObject module) {
        return (RubyArray) module.getInstanceVariables().getInstanceVariable("@java_interface_mods");
    }

    private static boolean initInterfaceModules(final IRubyObject self, final IRubyObject module) {
        if ( ! module.getInstanceVariables().hasInstanceVariable("@java_interface_mods") ) {
            final RubyArray interfaceMods = RubyArray.newArray(self.getRuntime(), self);
            module.getInstanceVariables().setInstanceVariable("@java_interface_mods", interfaceMods);
            return true;
        }
        return false;
    }

}
