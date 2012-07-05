package org.jruby.java.proxies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodN;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOne;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOneBlock;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtilities;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaInterfaceTemplate {
    public static RubyModule createJavaInterfaceTemplateModule(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        RubyModule javaInterfaceTemplate = runtime.defineModule("JavaInterfaceTemplate");

        RubyClass singleton = javaInterfaceTemplate.getSingletonClass();
        singleton.addReadAttribute(context, "java_class");
        singleton.defineAnnotatedMethods(JavaInterfaceTemplate.class);

        return javaInterfaceTemplate;
    }

    // not intended to be called directly by users (private)
    // OLD TODO from Ruby code:
    // This should be implemented in JavaClass.java, where we can
    // check for reserved Ruby names, conflicting methods, etc.
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public static IRubyObject implement(ThreadContext context, IRubyObject self, IRubyObject clazz) {
        Ruby runtime = context.getRuntime();

        if (!(clazz instanceof RubyModule)) {
            throw runtime.newTypeError(clazz, runtime.getModule());
        }

        RubyModule targetModule = (RubyModule)clazz;
        JavaClass javaClass = (JavaClass)self.getInstanceVariables().getInstanceVariable("@java_class");
        
        Method[] javaInstanceMethods = javaClass.javaClass().getMethods();
        DynamicMethod dummyMethod = new org.jruby.internal.runtime.methods.JavaMethod(targetModule, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                // dummy bodies for default impls
                return context.getRuntime().getNil();
            }
        };
        
        for (int i = 0; i < javaInstanceMethods.length; i++) {
            Method method = javaInstanceMethods[i];
            String name = method.getName();
            if (targetModule.searchMethod(name) != UndefinedMethod.INSTANCE) continue;
            
            targetModule.addMethod(name, dummyMethod);
        }
        
        return runtime.getNil();
    }

    // framed for invokeSuper
    @JRubyMethod(frame = true)
    public static IRubyObject append_features(ThreadContext context, IRubyObject self, IRubyObject clazz, Block block) {
        if (clazz instanceof RubyClass) {
            appendFeaturesToClass(context, self, (RubyClass)clazz);
        } else if (clazz instanceof RubyModule) {
            appendFeaturesToModule(context, self, (RubyModule)clazz);
        } else {
            throw context.getRuntime().newTypeError("received " + clazz + ", expected Class/Module");
        }

        return RuntimeHelpers.invokeSuper(context, self, clazz, block);
    }

    private static void appendFeaturesToClass(ThreadContext context, IRubyObject self, final RubyClass clazz) {
        Ruby runtime = context.getRuntime();
        checkAlreadyReified(clazz, runtime);

        IRubyObject javaClassObj = RuntimeHelpers.getInstanceVariable(self, runtime, "@java_class");
        IRubyObject javaInterfaces;
        if (!clazz.hasInstanceVariable("@java_interfaces")) {
            javaInterfaces = RubyArray.newArray(runtime, javaClassObj);
            RuntimeHelpers.setInstanceVariable(javaInterfaces, clazz, "@java_interfaces");

            initInterfaceImplMethods(context, clazz);
        } else {
            javaInterfaces = RuntimeHelpers.getInstanceVariable(clazz, runtime, "@java_interfaces");
            // we've already done the above priming logic, just add another interface
            // to the list of intentions unless we're past the point of no return or
            // already intend to implement the given interface
            if (!(javaInterfaces.isFrozen() || ((RubyArray)javaInterfaces).includes(context, javaClassObj))) {
                ((RubyArray)javaInterfaces).append(javaClassObj);
            }
        }
    }

    private static void checkAlreadyReified(final RubyClass clazz, Ruby runtime) throws RaiseException {
        // not allowed for original (non-generated) Java classes
        // note: not allowing for any previously created class right now;
        // this restriction might be loosened later for generated classes
        if ((Java.NEW_STYLE_EXTENSION && clazz.getReifiedClass() != null)
                ||
                (clazz.hasInstanceVariable("@java_class")
                    && clazz.getInstanceVariable("@java_class").isTrue()
                    && !clazz.getSingletonClass().isMethodBound("java_proxy_class", false))
                ||
                (clazz.hasInstanceVariable("@java_proxy_class")
                    && clazz.getInstanceVariable("@java_proxy_class").isTrue())) {
            throw runtime.newArgumentError("can not add Java interface to existing Java class");
        }
    }

    private static void initInterfaceImplMethods(ThreadContext context, RubyClass clazz) {
        // setup new, etc unless this is a ConcreteJavaProxy subclass
        // For JRUBY-4571, check both these, since JavaProxy extension stuff adds the former and this code adds the latter
        if (!(clazz.isMethodBound("__jcreate!", false) || clazz.isMethodBound("__jcreate_meta!", false))) {
            // First we make modifications to the class, to adapt it to being
            // both a Ruby class and a proxy for a Java type

            RubyClass singleton = clazz.getSingletonClass();

            // list of interfaces we implement
            singleton.addReadAttribute(context, "java_interfaces");

            if (
                    (!Java.NEW_STYLE_EXTENSION && clazz.getSuperClass().getRealClass().hasInstanceVariable("@java_class"))
                    || RubyInstanceConfig.INTERFACES_USE_PROXY) {
                // superclass is a Java class...use old style impl for now

                // The replacement "new" allocates and inits the Ruby object as before, but
                // also instantiates our proxified Java object by calling __jcreate!
                final ObjectAllocator proxyAllocator = clazz.getAllocator();
                clazz.setAllocator(new ObjectAllocator() {
                    public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                        IRubyObject newObj = proxyAllocator.allocate(runtime, klazz);
                        RuntimeHelpers.invoke(runtime.getCurrentContext(), newObj, "__jcreate!");
                        return newObj;
                    }
                });

                // jcreate instantiates the proxy object which implements all interfaces
                // and which is wrapped and implemented by this object
                clazz.addMethod("__jcreate!", new JavaMethodN(clazz, Visibility.PRIVATE) {

                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                        return jcreateProxy(self);
                    }
                });
            } else {
                // The new "new" actually generates a real Java class to use for the Ruby class's
                // backing store, instantiates that, and then calls initialize on it.
                addRealImplClassNew(clazz);
            }

            // Next, we define a few private methods that we'll use to manipulate
            // the Java object contained within this Ruby object

            // Used by our duck-typification of Proc into interface types, to allow
            // coercing a simple proc into an interface parameter.
            clazz.addMethod("__jcreate_meta!", new JavaMethodN(clazz, Visibility.PRIVATE) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                    IRubyObject result = jcreateProxy(self);
                    return result;
                }
            });

            // If we hold a Java object, we need a java_class accessor
            clazz.addMethod("java_class", new JavaMethodZero(clazz, Visibility.PUBLIC) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                    return ((JavaObject) self.dataGetStruct()).java_class();
                }
            });

            // Because we implement Java interfaces now, we need a new === that's
            // aware of those additional "virtual" supertypes
            if (!clazz.searchMethod("===").isUndefined()) {
                clazz.defineAlias("old_eqq", "===");
                clazz.addMethod("===", new JavaMethodOne(clazz, Visibility.PUBLIC) {

                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                        // TODO: WRONG - get interfaces from class
                        if (arg.respondsTo("java_object")) {
                            IRubyObject interfaces = self.getMetaClass().getInstanceVariables().getInstanceVariable("@java_interfaces");
                            assert interfaces instanceof RubyArray : "interface list was not an array";

                            return context.getRuntime().newBoolean(((RubyArray) interfaces).op_diff(
                                    ((JavaClass) ((JavaObject) arg.dataGetStruct()).java_class()).interfaces()).equals(RubyArray.newArray(context.getRuntime())));
                        } else {
                            return RuntimeHelpers.invoke(context, self, "old_eqq", arg);
                        }
                    }
                });
            }
        }

        // Now we add an "implement" and "implement_all" methods to the class
        if (!clazz.isMethodBound("implement", false)) {
            RubyClass singleton = clazz.getSingletonClass();

            // implement is called to force this class to create stubs for all
            // methods in the given interface, so they'll show up in the list
            // of methods and be invocable without passing through method_missing
            singleton.addMethod("implement", new JavaMethodOne(clazz, Visibility.PRIVATE) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                    IRubyObject javaInterfaces = self.getInstanceVariables().getInstanceVariable("@java_interfaces");
                    if (javaInterfaces != null && ((RubyArray) javaInterfaces).includes(context, arg)) {
                        return RuntimeHelpers.invoke(context, arg, "implement", self);
                    }
                    return context.getRuntime().getNil();
                }
            });

            // implement all forces implementation of all interfaces we intend
            // for this class to implement
            singleton.addMethod("implement_all", new JavaMethodOne(clazz, Visibility.PRIVATE) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                    RubyArray javaInterfaces = (RubyArray) self.getInstanceVariables().getInstanceVariable("@java_interfaces");
                    for (int i = 0; i < javaInterfaces.size(); i++) {
                        RuntimeHelpers.invoke(context, JavaUtilities.get_interface_module(self, javaInterfaces.eltInternal(i)), "implement", self);
                    }
                    return javaInterfaces;
                }
            });
        }
    }

    public static void addRealImplClassNew(RubyClass clazz) {
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

    private static IRubyObject jcreateProxy(IRubyObject self) {
        RubyClass current = self.getMetaClass();

        // construct the new interface impl and set it into the object
        IRubyObject newObject = Java.newInterfaceImpl(self, Java.getInterfacesFromRubyClass(current));
        return JavaUtilities.set_java_object(self, self, newObject);
    }

    private static void appendFeaturesToModule(ThreadContext context, IRubyObject self, RubyModule module) {
        // assuming the user wants a collection of interfaces that can be
        // included together. make it so.
        
        Ruby runtime = context.getRuntime();

        // not allowed for existing Java interface modules
        if (module.getInstanceVariables().hasInstanceVariable("@java_class") &&
                module.getInstanceVariables().getInstanceVariable("@java_class").isTrue()) {
            throw runtime.newTypeError("can not add Java interface to existing Java interface");
        }
        
        // To turn a module into an "interface collection" we add a class instance
        // variable to hold the list of interfaces, and modify append_features
        // for this module to call append_features on each of those interfaces as
        // well
        synchronized (module) {
            if (!module.getInstanceVariables().hasInstanceVariable("@java_interface_mods")) {
                RubyArray javaInterfaceMods = RubyArray.newArray(runtime, self);
                module.getInstanceVariables().setInstanceVariable("@java_interface_mods", javaInterfaceMods);
                RubyClass singleton = module.getSingletonClass();

                singleton.addMethod("append_features", new JavaMethodOneBlock(singleton, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block) {
                        if (!(arg instanceof RubyClass)) {
                            throw context.getRuntime().newTypeError("append_features called with non-class");
                        }
                        RubyClass target = (RubyClass)arg;
                        RubyArray javaInterfaceMods = (RubyArray)self.getInstanceVariables().getInstanceVariable("@java_interface_mods");

                        target.include(javaInterfaceMods.toJavaArray());

                        return RuntimeHelpers.invokeAs(context, clazz.getSuperClass(), self, name, arg, block);
                    }
                });
            } else {
                // already set up append_features, just add the interface if we haven't already
                RubyArray javaInterfaceMods =(RubyArray)module.getInstanceVariables().getInstanceVariable("@java_interface_mods");
                if (!javaInterfaceMods.includes(context, self)) {
                    javaInterfaceMods.append(self);
                }
            }
        }
    }

    @JRubyMethod
    public static IRubyObject extended(ThreadContext context, IRubyObject self, IRubyObject object) {
        if (!(self instanceof RubyModule)) {
            throw context.getRuntime().newTypeError(self, context.getRuntime().getModule());
        }
        RubyClass singleton = object.getSingletonClass();
        singleton.include(new IRubyObject[] {self});
        return singleton;
    }

    @JRubyMethod(name = "[]", rest = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return JavaProxy.op_aref(context, self, args);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject impl(ThreadContext context, IRubyObject self, IRubyObject[] args, final Block implBlock) {
        Ruby runtime = context.getRuntime();

        if (!implBlock.isGiven()) throw runtime.newArgumentError("block required to call #impl on a Java interface");

        final RubyArray methodNames = (args.length > 0) ? runtime.newArray(args) : null;

        RubyClass implClass = RubyClass.newClass(runtime, runtime.getObject());
        implClass.include(new IRubyObject[] {self});

        IRubyObject implObject = implClass.callMethod(context, "new");

        implClass.addMethod("method_missing",
                new org.jruby.internal.runtime.methods.JavaMethod(implClass, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                        Arity.checkArgumentCount(context.runtime, name, args.length, 1, -1);

                        if (methodNames == null || methodNames.include_p(context, args[0]).isTrue()) {
                            return implBlock.call(context, args);
                        } else {
                            return clazz.getSuperClass().callMethod(context, "method_missing", args, block);
                        }
                    }
                });

        return implObject;
    }

    @JRubyMethod(name = "new", rest = true)
    public static IRubyObject rbNew(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();

        RubyClass implClass = (RubyClass)self.getInstanceVariables().getInstanceVariable("@__implementation");
        if (implClass == null) {
            implClass = RubyClass.newClass(runtime, (RubyClass)runtime.getClass("InterfaceJavaProxy"));
            implClass.include(new IRubyObject[] {self});
            RuntimeHelpers.setInstanceVariable(implClass, self, "@__implementation");
        }

        return RuntimeHelpers.invoke(context, implClass, "new", args, block);
    }
}
