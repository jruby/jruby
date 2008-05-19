/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.java;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.util.HandleFactory;
import org.jruby.compiler.util.HandleFactory.Handle;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import static org.jruby.util.CodegenUtils.*;
import org.jruby.util.IdUtil;

/**
 *
 * @author headius
 */
public class MiniJava implements Library {
    public void load(Ruby runtime, boolean wrap) {
        runtime.getErr().print("Warning: minijava is experimental and subject to change\n");
        
        runtime.getKernel().defineAnnotatedMethods(MiniJava.class);

        // load up object and add a few useful methods
        RubyModule javaObject = getMirrorForClass(runtime, Object.class);

        javaObject.addMethod("to_s", new JavaMethod.JavaMethodZero(javaObject, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return context.getRuntime().newString(((JavaObjectWrapper) self).object.toString());
            }
        });

        javaObject.addMethod("hash", new JavaMethod.JavaMethodZero(javaObject, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return self.getRuntime().newFixnum(((JavaObjectWrapper) self).object.hashCode());
            }
        });

        javaObject.addMethod("==", new JavaMethod.JavaMethodOne(javaObject, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                if (arg instanceof JavaObjectWrapper) {
                    return context.getRuntime().newBoolean(((JavaObjectWrapper) self).object.equals(((JavaObjectWrapper) arg).object));
                } else {
                    return context.getRuntime().getFalse();
                }
            }
        });

        // open up the 'to_java' and 'as' coercion methods on Ruby Objects, via Kernel
        RubyModule rubyKernel = runtime.getKernel();
        rubyKernel.addModuleFunction("to_java", new JavaMethod.JavaMethodZeroOrOne(rubyKernel, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return ((RubyObject) self).to_java();
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                return ((RubyObject) self).as(getJavaClassFromObject(arg));
            }
        });
    }

    @JRubyMethod(name = "import", module = true)
    public static IRubyObject rb_import(ThreadContext context, IRubyObject self, IRubyObject name) {
        String className = name.toString();
        try {
            Class cls = findClass(context.getRuntime().getJRubyClassLoader(), className);

            RubyModule namespace;
            if (self instanceof RubyModule) {
                namespace = (RubyModule) self;
            } else {
                namespace = self.getMetaClass().getRealClass();
            }

            namespace.defineConstant(cls.getSimpleName(), getMirrorForClass(context.getRuntime(), cls));

            return context.getRuntime().getNil();
        } catch (Exception e) {
            if (context.getRuntime().getDebug().isTrue()) {
                e.printStackTrace();
            }
            throw context.getRuntime().newTypeError("Could not find class " + className + ", exception: " + e);
        }
    }

    @JRubyMethod(name = "import", module = true)
    public static IRubyObject rb_import(ThreadContext context, IRubyObject self, IRubyObject name, IRubyObject as) {
        String className = name.toString();
        try {
            Class cls = findClass(context.getRuntime().getJRubyClassLoader(), className);

            RubyModule namespace;
            if (self instanceof RubyModule) {
                namespace = (RubyModule) self;
            } else {
                namespace = self.getMetaClass().getRealClass();
            }

            namespace.defineConstant(as.toString(), getMirrorForClass(context.getRuntime(), cls));

            return context.getRuntime().getNil();
        } catch (Exception e) {
            if (context.getRuntime().getDebug().isTrue()) {
                e.printStackTrace();
            }
            throw context.getRuntime().newTypeError("Could not find class " + className + ", exception: " + e);
        }
    }

    protected static Class findClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (className.indexOf('.') == -1 && Character.isLowerCase(className.charAt(0))) {
            // probably a primitive
            switch (className.charAt(0)) {
            case 'b':
                return byte.class;
            case 's':
                return short.class;
            case 'c':
                return char.class;
            case 'i':
                return int.class;
            case 'l':
                return long.class;
            case 'f':
                return float.class;
            case 'd':
                return double.class;
            default:
                return classLoader.loadClass(className);
            }
        } else {
            return classLoader.loadClass(className);
        }
    }
    static Map<Class, RubyModule> classMap = new HashMap<Class, RubyModule>();

    public static RubyModule getMirrorForClass(Ruby ruby, Class cls) {
        if (cls == null) {
            return ruby.getObject();
        }

        RubyModule rubyCls = classMap.get(cls);

        if (rubyCls == null) {
            rubyCls = createMirrorForClass(ruby, cls);

            classMap.put(cls, rubyCls);
            populateMirrorForClass(rubyCls, cls);
            rubyCls = classMap.get(cls);
        }

        return rubyCls;
    }

    protected static RubyModule createMirrorForClass(Ruby ruby, Class cls) {
        if (cls.isInterface()) {
            // interfaces are handled as modules
            RubyModule rubyMod = RubyModule.newModule(ruby);
            return rubyMod;
        } else {
            // construct the mirror class and parent classes
            RubyClass rubyCls = RubyClass.newClass(ruby, (RubyClass) getMirrorForClass(ruby, cls.getSuperclass()));
            return rubyCls;
        }
    }

    protected static void populateMirrorForClass(RubyModule rubyMod, final Class cls) {
        Ruby ruby = rubyMod.getRuntime();

        // set the full name
        rubyMod.setBaseName(cls.getCanonicalName());

        // include all interfaces
        Class[] interfaces = cls.getInterfaces();
        for (Class ifc : interfaces) {
            rubyMod.includeModule(getMirrorForClass(ruby, ifc));
        }

        // if it's an inner class and it's not public, we can't access it;
        // skip population of declared elements
        if (cls.getEnclosingClass() != null && !Modifier.isPublic(cls.getModifiers())) {
            return;
        }

        RubyModule rubySing = rubyMod.getSingletonClass();

        // if it's an array, only add methods for aref, aset, and length
        if (cls.isArray()) {
            populateMirrorForArrayClass(rubyMod, cls);
        } else {
            populateDeclaredMethods(rubyMod, cls, true);

            populateConstructors(rubySing, cls);

            populateArrayConstructors(rubySing);

            populateFields(rubyMod, cls);
        }

        populateSpecialMethods(rubyMod, rubySing, cls);
    }
    private static void populateArrayConstructors(RubyModule rubySing) {
        // add array construction methods
        rubySing.addMethod("[]", new JavaMethod.JavaMethodOneOrTwoOrThree(rubySing, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                Class javaClass = getJavaClassFromObject(self);
                int size = RubyFixnum.fix2int(arg.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.newInstance(javaClass, size));
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                Class javaClass = getJavaClassFromObject(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.newInstance(javaClass, new int[]{x, y}));
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
                Class javaClass = getJavaClassFromObject(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                int z = RubyFixnum.fix2int(arg2.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.newInstance(javaClass, new int[]{x, y, z}));
            }
        });
    }

    private static void populateConstructors(RubyModule rubySing, final Class cls) {
        // add all constructors
        Constructor[] constructors = cls.getConstructors();
        for (final Constructor constructor : constructors) {
            // only public constructors
            if (!Modifier.isPublic(constructor.getModifiers())) {
                continue;
            }

            DynamicMethod dynMethod;
            if (constructor.getParameterTypes().length == 0) {
                dynMethod = new JavaMethod.JavaMethodZero(rubySing, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                        try {
                            return javaToRuby(context.getRuntime(), constructor.newInstance());
                        } catch (Exception e) {
                            if (context.getRuntime().getDebug().isTrue()) {
                                e.printStackTrace();
                            }
                            throw context.getRuntime().newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + pretty(cls, constructor.getParameterTypes()));
                        }
                    }
                };
            } else {
                dynMethod = new JavaMethod.JavaMethodNoBlock(rubySing, Visibility.PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] rubyArgs) {
                        Object[] args = new Object[rubyArgs.length];

                        for (int i = 0; i < args.length; i++) {
                            args[i] = rubyToJava(rubyArgs[i]);
                        }
                        try {
                            return javaToRuby(context.getRuntime(), constructor.newInstance(args));
                        } catch (Exception e) {
                            if (context.getRuntime().getDebug().isTrue()) {
                                e.printStackTrace();
                            }
                            throw context.getRuntime().newTypeError("Could not instantiate " + cls.getCanonicalName() + " using " + pretty(cls, constructor.getParameterTypes()));
                        }
                    }
                };
            }

            // if not already defined, we add a 'new' that guesses at which signature to use
            // TODO: just adding first one right now...add in signature-guessing logic
            if (rubySing.getMethods().get("new") == null) {
                rubySing.addMethod("new", dynMethod);
            }

            // add 'new' with full signature, so it's guaranteed to be directly accessible
            // TODO: no need for this to be a full, formal JVM signature
            rubySing.addMethod("new" + pretty(cls, constructor.getParameterTypes()), dynMethod);
        }
    }

    private static void populateDeclaredMethods(RubyModule rubyMod, final Class cls, boolean includeStatic) throws SecurityException {
        // add all instance and static methods
        Method[] methods = cls.getDeclaredMethods();
        for (final Method method : methods) {
            String name = method.getName();
            RubyModule target;

            // only public methods
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            if (Modifier.isStatic(method.getModifiers())) {
                if (!includeStatic) continue; // only include static methods if specified
                
                target = rubyMod.getSingletonClass();
            } else {
                target = rubyMod;
            }

            JavaMethodFactory factory = getMethodFactory(method.getReturnType());
            DynamicMethod dynMethod = factory.createMethod(target, method);

            // if not overloaded, we add a method that guesses at which signature to use
            // TODO: just adding first one right now...add in signature-guessing logic
            if (target.getMethods().get(name) == null) {
                target.addMethod(name, dynMethod);
            }

            // add method with full signature, so it's guaranteed to be directly accessible
            // TODO: no need for this to be a full, formal JVM signature
            name = name + pretty(method.getReturnType(), method.getParameterTypes());
            target.addMethod(name, dynMethod);
        }
    }
    
    private static void populateSpecialMethods(RubyModule rubyMod, RubyModule rubySing, final Class cls) {
        // add a few type-specific special methods
        rubySing.addMethod("java_class", new JavaMethod.JavaMethodZero(rubySing, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return javaToRuby(context.getRuntime(), cls);
            }
        });
    }

    private static void populateFields(RubyModule rubyMod, final Class cls) throws RaiseException, SecurityException {
        Ruby ruby = rubyMod.getRuntime();
        
        // add all static variables
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            // only public static fields that are valid constants
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && IdUtil.isConstant(field.getName())) {
                Object value = null;
                try {
                    value = field.get(null);
                } catch (Exception e) {
                    throw ruby.newTypeError("Could not access field " + cls.getCanonicalName() + "::" + field.getName() + " using " + ci(field.getType()));
                }
                rubyMod.defineConstant(field.getName(), new JavaObjectWrapper((RubyClass) getMirrorForClass(ruby, value.getClass()), value));
            }
        }
    }

    protected static void populateMirrorForArrayClass(RubyModule rubyMod, Class cls) {
        rubyMod.addMethod("[]", new JavaMethod.JavaMethodOneOrTwo(rubyMod, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.get(array, x));
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                return javaToRuby(context.getRuntime(), Array.get(Array.get(array, x), y));
            }
        });

        rubyMod.addMethod("[]=", new JavaMethod.JavaMethodTwoOrThree(rubyMod, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                Object obj = rubyToJava(arg1);
                Array.set(array, x, obj);

                return arg1;
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
                Object array = rubyToJava(self);
                int x = RubyFixnum.fix2int(arg0.convertToInteger());
                int y = RubyFixnum.fix2int(arg1.convertToInteger());
                Object obj = rubyToJava(arg2);
                Array.set(Array.get(array, x), y, obj);

                return arg2;
            }
        });

        rubyMod.addMethod("length", new JavaMethod.JavaMethodZero(rubyMod, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                Object array = rubyToJava(self);
                return javaToRuby(context.getRuntime(), Array.getLength(array));
            }
        });
    }
    static final Map<Class, JavaMethodFactory> methodFactories = new HashMap();
    static final JavaMethodFactory JAVA_OBJECT_METHOD_FACTORY = new JavaMethodFactory() {
        public DynamicMethod createMethod(RubyClass klazz, Method method) {
            return new JavaObjectWrapperMethod(klazz, method);
        }
    };

    protected static JavaMethodFactory getMethodFactory(Class returnType) {
        JavaMethodFactory factory = methodFactories.get(returnType);

        if (factory == null) {
            return JAVA_OBJECT_METHOD_FACTORY;
        }

        return factory;
    }
    

    static {
        methodFactories.put(void.class, new JavaMethodFactory() {
            @Override
            public DynamicMethod createMethod(RubyModule klazz, Method method) {
                Class[] parameters = method.getParameterTypes();
                if (parameters.length > 0) {
                    return new JavaVoidWrapperMethod(klazz, method);
                } else {
                    return new JavaVoidWrapperMethodZero(klazz, method);
                }
            }
        });
    }

    public static class JavaMethodFactory {
        public DynamicMethod createMethod(RubyModule klazz, Method method) {
            Class[] params = method.getParameterTypes();
            if (params.length > 0) {
                return new JavaObjectWrapperMethod(klazz, method);
            } else {
                return new JavaObjectWrapperMethodZero(klazz, method);
            }
        }
    }

    public static abstract class AbstractJavaWrapperMethodZero extends JavaMethod.JavaMethodZero {
        protected final Handle handle;
        protected final boolean isStatic;
        protected final String className;
        protected final String methodName;
        protected final String prettySig;

        public AbstractJavaWrapperMethodZero(RubyModule klazz, Method method) {
            super(klazz, Visibility.PUBLIC);

            this.handle = HandleFactory.createHandle(klazz.getRuntime().getJRubyClassLoader(), method);
            this.isStatic = Modifier.isStatic(method.getModifiers());
            this.className = method.getDeclaringClass().getCanonicalName();
            this.methodName = method.getName();
            this.prettySig = pretty(method.getReturnType(), method.getParameterTypes());
        }

        protected RaiseException error(ThreadContext context, Exception e) throws RaiseException {
            if (context.getRuntime().getDebug().isTrue()) {
                e.printStackTrace();
            }
            throw context.getRuntime().newTypeError("Could not dispatch to " + className + "#" + methodName + " using " + prettySig);
        }
    }

    public static abstract class AbstractJavaWrapperMethod extends JavaMethod {
        protected final Handle handle;
        protected final boolean isStatic;
        protected final String className;
        protected final String methodName;
        protected final String prettySig;

        public AbstractJavaWrapperMethod(RubyModule klazz, Method method) {
            super(klazz, Visibility.PUBLIC);

            this.handle = HandleFactory.createHandle(klazz.getRuntime().getJRubyClassLoader(), method);
            this.isStatic = Modifier.isStatic(method.getModifiers());
            this.className = method.getDeclaringClass().getCanonicalName();
            this.methodName = method.getName();
            this.prettySig = pretty(method.getReturnType(), method.getParameterTypes());
        }

        protected RaiseException error(ThreadContext context, Exception e) throws RaiseException {
            if (context.getRuntime().getDebug().isTrue()) {
                e.printStackTrace();
            }
            throw context.getRuntime().newTypeError("Could not dispatch to " + className + "#" + methodName + " using " + prettySig);
        }
    }

    protected static class JavaObjectWrapperMethodZero extends AbstractJavaWrapperMethodZero {
        public JavaObjectWrapperMethodZero(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }
    }

    protected static class JavaObjectWrapperMethod extends AbstractJavaWrapperMethod {
        public JavaObjectWrapperMethod(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            try {
                Object result = (Object) handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

                return javaToRuby(context.getRuntime(), result);
            } catch (Exception e) {
                throw error(context, e);
            }
        }
    }

    protected static class JavaVoidWrapperMethod extends AbstractJavaWrapperMethod {
        public JavaVoidWrapperMethod(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                newArgs[i] = rubyToJava(arg);
            }

            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, newArgs);

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0));

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1));

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object, rubyToJava(arg0), rubyToJava(arg1), rubyToJava(arg2));

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }
    }

    protected static class JavaVoidWrapperMethodZero extends AbstractJavaWrapperMethodZero {
        public JavaVoidWrapperMethodZero(RubyModule klazz, Method method) {
            super(klazz, method);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            try {
                handle.invoke(isStatic ? null : ((JavaObjectWrapper) self).object);

                return self;
            } catch (Exception e) {
                throw error(context, e);
            }
        }
    }

    public static Object rubyToJava(IRubyObject object) {
        if (object.isNil()) {
            return null;
        } else if (object instanceof JavaObjectWrapper) {
            return ((JavaObjectWrapper) object).object;
        } else {
            return object;
        }
    }

    public static IRubyObject javaToRuby(Ruby ruby, Object object) {
        if (object == null) {
            return ruby.getNil();
        } else if (object instanceof IRubyObject) {
            return (IRubyObject) object;
        } else {
            return new JavaObjectWrapper((RubyClass) getMirrorForClass(ruby, object.getClass()), object);
        }
    }

    public static class JavaObjectWrapper extends RubyObject {
        Object object;

        public JavaObjectWrapper(RubyClass klazz, Object object) {
            super(klazz.getRuntime(), klazz);
            this.object = object;
        }
    };

    public static Class getJavaClassFromObject(IRubyObject obj) {
        if (!obj.respondsTo("java_class")) {
            throw obj.getRuntime().newTypeError(obj.getMetaClass().getBaseName() + " is not a Java type");
        } else {
            return (Class) rubyToJava(obj.callMethod(obj.getRuntime().getCurrentContext(), "java_class"));
        }
    }
}
