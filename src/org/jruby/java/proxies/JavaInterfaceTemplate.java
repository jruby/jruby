package org.jruby.java.proxies;

import org.jruby.javasupport.*;
import java.lang.reflect.Method;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodN;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOne;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOneBlock;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
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
    @JRubyMethod(backtrace = true, visibility = Visibility.PRIVATE)
    public static IRubyObject implement(ThreadContext context, IRubyObject self, IRubyObject clazz) {
        Ruby runtime = context.getRuntime();

        if (!(clazz instanceof RubyModule)) {
            throw runtime.newTypeError(clazz, runtime.getModule());
        }

        RubyModule targetModule = (RubyModule)clazz;
        JavaClass javaClass = (JavaClass)self.getInstanceVariables().fastGetInstanceVariable("@java_class");
        
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

    private static void appendFeaturesToClass(ThreadContext context, IRubyObject self, RubyClass clazz) {
        Ruby runtime = context.getRuntime();
        IRubyObject javaClassObj = self.getInstanceVariables().fastGetInstanceVariable("@java_class");

        // initialize this if it hasn't been
        if (javaClassObj == null) {
            javaClassObj = runtime.getNil();
            self.getInstanceVariables().setInstanceVariable("@java_class", javaClassObj);
        }

        // initialize these if they haven't been
        IRubyObject javaClass = clazz.getInstanceVariables().fastGetInstanceVariable("@java_class");
        if (javaClass == null) {
            javaClass = runtime.getNil();
            clazz.getInstanceVariables().fastSetInstanceVariable("@java_class", javaClass);
        }
        IRubyObject javaProxyClass = clazz.getInstanceVariables().fastGetInstanceVariable("@java_proxy_class");
        if (javaProxyClass == null) {
            javaProxyClass = runtime.getNil();
            clazz.getInstanceVariables().fastSetInstanceVariable("@java_proxy_class", javaProxyClass);
        }

        // not allowed for original (non-generated) Java classes
        // note: not allowing for any previously created class right now;
        // this restriction might be loosened later for generated classes
        if ((javaClass.isTrue() && !clazz.getSingletonClass().isMethodBound("java_proxy_class", false)) ||
                javaProxyClass.isTrue()) {
            throw runtime.newArgumentError("can not add Java interface to existing Java class");
        }
        
        IRubyObject javaInterfaces = clazz.getInstanceVariables().fastGetInstanceVariable("@java_interfaces");
        if (javaInterfaces == null) {
            javaInterfaces = RubyArray.newArray(runtime, javaClassObj);
            clazz.getInstanceVariables().fastSetInstanceVariable("@java_interfaces", javaInterfaces);

            // setup new, etc unless this is a ConcreteJavaProxy subclass
            if (!clazz.isMethodBound("__jcreate!", false)) {
                // First we make modifications to the class, to adapt it to being
                // both a Ruby class and a proxy for a Java type

                RubyClass singleton = clazz.getSingletonClass();

                // list of interfaces we implement
                singleton.addReadAttribute(context, "java_interfaces");
                
                // We capture the original "new" and make it private
                DynamicMethod newMethod = singleton.searchMethod("new").dup();
                singleton.addMethod("__jredef_new", newMethod);
                newMethod.setVisibility(Visibility.PRIVATE);

                // The replacement "new" allocates and inits the Ruby object as before, but
                // also instantiates our proxified Java object by calling __jcreate!
                singleton.addMethod("new", new org.jruby.internal.runtime.methods.JavaMethod(singleton, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                        assert self instanceof RubyClass : "new defined on non-class";

                        RubyClass clazzSelf = (RubyClass)self;
                        IRubyObject newObj = clazzSelf.allocate();
                        RuntimeHelpers.invoke(context, newObj, "__jcreate!", args, block);
                        RuntimeHelpers.invoke(context, newObj, "initialize", args, block);

                        return newObj;
                    }
                });
                
                // Next, we define a few private methods that we'll use to manipulate
                // the Java object contained within this Ruby object
                
                // jcreate instantiates the proxy object which implements all interfaces
                // and which is wrapped and implemented by this object
                clazz.addMethod("__jcreate!", new JavaMethodN(clazz, Visibility.PRIVATE) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                        return jcreateProxy(self, args);
                    }
                });
                
                // Used by our duck-typification of Proc into interface types, to allow
                // coercing a simple proc into an interface parameter.
                clazz.addMethod("__jcreate_meta!", new JavaMethodN(clazz, Visibility.PRIVATE) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                        IRubyObject result = jcreateProxy(self, args);
                        return result;
                    }
                });

                clazz.includeModule(runtime.getModule("JavaProxyMethods"));

                // If we hold a Java object, we need a java_class accessor
                clazz.addMethod("java_class", new JavaMethodZero(clazz, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                        return ((JavaObject)self.dataGetStruct()).java_class();
                    }
                });
                
                // Because we implement Java interfaces now, we need a new === that's
                // aware of those additional "virtual" supertypes
                clazz.defineAlias("old_eqq", "===");
                clazz.addMethod("===", new JavaMethodOne(clazz, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                        // TODO: WRONG - get interfaces from class
                        if (arg.respondsTo("java_object")) {
                            IRubyObject interfaces = self.getMetaClass().getInstanceVariables().fastGetInstanceVariable("@java_interfaces");
                            assert interfaces instanceof RubyArray : "interface list was not an array";

                            return context.getRuntime().newBoolean(((RubyArray)interfaces)
                                    .op_diff(
                                        ((JavaClass)
                                            ((JavaObject)arg.dataGetStruct()).java_class()
                                        ).interfaces()
                                    ).equals(RubyArray.newArray(context.getRuntime())));
                        } else {
                            return RuntimeHelpers.invoke(context, self, "old_eqq", arg);
                        }
                    }
                });
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
                        IRubyObject javaInterfaces = self.getInstanceVariables().fastGetInstanceVariable("@java_interfaces");
                        if (javaInterfaces != null && ((RubyArray)javaInterfaces).includes(context, arg)) {
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
                        RubyArray javaInterfaces = (RubyArray)self.getInstanceVariables().fastGetInstanceVariable("@java_interfaces");
                        for (int i = 0; i < javaInterfaces.size(); i++) {
                            RuntimeHelpers.invoke(context, JavaUtilities.get_interface_module(self, javaInterfaces.eltInternal(i)), "implement", self);
                        }
                        return javaInterfaces;
                    }
                });
            }
        } else {
            // we've already done the above priming logic, just add another interface
            // to the list of intentions unless we're past the point of no return or
            // already intend to implement the given interface
            if (!(javaInterfaces.isFrozen() || ((RubyArray)javaInterfaces).includes(context, javaClassObj))) {
                ((RubyArray)javaInterfaces).append(javaClassObj);
            }
        }
    }

    private static IRubyObject jcreateProxy(IRubyObject self, IRubyObject[] args) {
        RubyClass current = self.getMetaClass();
        RubyArray interfaces2 = self.getRuntime().newArray();

        // walk all superclasses aggregating interfaces
        while (current != null) {
            IRubyObject maybeInterfaces = current.getInstanceVariables().getInstanceVariable("@java_interfaces");
            if (maybeInterfaces instanceof RubyArray) {
                RubyArray moreInterfaces = (RubyArray)maybeInterfaces;
                if (!moreInterfaces.isFrozen()) moreInterfaces.setFrozen(true);

                interfaces2 = (RubyArray)interfaces2.op_or(moreInterfaces);
            }
            current = current.getSuperClass();
        }

        // construct the new interface impl and set it into the object
        IRubyObject newObject = Java.new_proxy_instance2(self, self, interfaces2, Block.NULL_BLOCK);
        return JavaUtilities.set_java_object(self, self, newObject);
    }

    private static void appendFeaturesToModule(ThreadContext context, IRubyObject self, RubyModule module) {
        // assuming the user wants a collection of interfaces that can be
        // included together. make it so.
        
        Ruby runtime = context.getRuntime();

        // not allowed for existing Java interface modules
        if (module.getInstanceVariables().fastHasInstanceVariable("@java_class") &&
                module.getInstanceVariables().fastGetInstanceVariable("@java_class").isTrue()) {
            throw runtime.newTypeError("can not add Java interface to existing Java interface");
        }
        
        // To turn a module into an "interface collection" we add a class instance
        // variable to hold the list of interfaces, and modify append_features
        // for this module to call append_features on each of those interfaces as
        // well
        synchronized (module) {
            if (!module.getInstanceVariables().fastHasInstanceVariable("@java_interface_mods")) {
                RubyArray javaInterfaceMods = RubyArray.newArray(runtime, self);
                module.getInstanceVariables().fastSetInstanceVariable("@java_interface_mods", javaInterfaceMods);
                RubyClass singleton = module.getSingletonClass();

                singleton.addMethod("append_features", new JavaMethodOneBlock(singleton, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block) {
                        if (!(arg instanceof RubyClass)) {
                            throw context.getRuntime().newTypeError("append_features called with non-class");
                        }
                        RubyClass target = (RubyClass)arg;
                        RubyArray javaInterfaceMods = (RubyArray)self.getInstanceVariables().fastGetInstanceVariable("@java_interface_mods");

                        target.include(javaInterfaceMods.toJavaArray());

                        return RuntimeHelpers.invokeAs(context, clazz.getSuperClass(), self, name, arg, block);
                    }
                });
            } else {
                // already set up append_features, just add the interface if we haven't already
                RubyArray javaInterfaceMods =(RubyArray)module.getInstanceVariables().fastGetInstanceVariable("@java_interface_mods");
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

    @JRubyMethod(name = "[]", rest = true, backtrace = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // array-of-interface-type creation/identity
        if (args.length == 0) {
            // keep this variant for kind_of? testing
            return JavaUtilities.get_proxy_class(self,
                    ((JavaClass)RuntimeHelpers.invoke(context, self, "java_class")).array_class());
        } else {
            // array creation should use this variant
            RubyClass arrayJavaProxyCreator = context.getRuntime().getClass("ArrayJavaProxyCreator");
            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            System.arraycopy(args, 0, newArgs, 1, args.length);
            newArgs[0] = RuntimeHelpers.invoke(context, self, "java_class");
            return RuntimeHelpers.invoke(context, arrayJavaProxyCreator, "new", newArgs);
        }
    }

    @JRubyMethod(rest = true, backtrace = true)
    public static IRubyObject impl(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        IRubyObject proxy = getDeprecatedInterfaceProxy(
                context,
                self,
                self.getInstanceVariables().fastGetInstanceVariable("@java_class"));

        return RuntimeHelpers.invoke(context, proxy, "impl", args, block);
    }

    @JRubyMethod(name = "new", rest = true, backtrace = true)
    public static IRubyObject rbNew(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        IRubyObject proxy = getDeprecatedInterfaceProxy(
                context,
                self,
                self.getInstanceVariables().fastGetInstanceVariable("@java_class"));

        return RuntimeHelpers.invoke(context, proxy, "new", args, block);
    }

    private static IRubyObject getDeprecatedInterfaceProxy(ThreadContext context, IRubyObject recv, IRubyObject javaClassObject) {
        Ruby runtime = context.getRuntime();
        JavaClass javaClass;
        if (javaClassObject instanceof RubyString) {
            javaClass = JavaClass.for_name(recv, javaClassObject);
        } else if (javaClassObject instanceof JavaClass) {
            javaClass = (JavaClass) javaClassObject;
        } else {
            throw runtime.newArgumentError("expected JavaClass, got " + javaClassObject);
        }
        if (!javaClass.javaClass().isInterface()) {
            throw runtime.newArgumentError("expected Java interface class, got " + javaClassObject);
        }
        RubyClass proxyClass;
        if ((proxyClass = javaClass.getProxyClass()) != null) {
            return proxyClass;
        }
        javaClass.lockProxy();
        try {
            if ((proxyClass = javaClass.getProxyClass()) == null) {
                RubyModule interfaceModule = Java.getInterfaceModule(runtime, javaClass);
                RubyClass interfaceJavaProxy = runtime.fastGetClass("InterfaceJavaProxy");
                proxyClass = RubyClass.newClass(runtime, interfaceJavaProxy);
                proxyClass.setAllocator(interfaceJavaProxy.getAllocator());
                proxyClass.makeMetaClass(interfaceJavaProxy.getMetaClass());
                // parent.setConstant(name, proxyClass); // where the name should come from ?
                proxyClass.inherit(interfaceJavaProxy);
                proxyClass.callMethod(context, "java_class=", javaClass);
                // including interface module so old-style interface "subclasses" will
                // respond correctly to #kind_of?, etc.
                proxyClass.includeModule(interfaceModule);
                javaClass.setupProxy(proxyClass);
                // add reference to interface module
                if (proxyClass.fastGetConstantAt("Includable") == null) {
                    proxyClass.fastSetConstant("Includable", interfaceModule);
                }

            }
        } finally {
            javaClass.unlockProxy();
        }
        return proxyClass;
    }
}
