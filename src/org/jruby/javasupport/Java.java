/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import org.jruby.java.util.BlankSlateWrapper;
import org.jruby.java.util.SystemPropertiesMap;
import org.jruby.java.proxies.JavaInterfaceTemplate;
import org.jruby.java.addons.KernelJavaAddons;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyClassPathVariable;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyUnboundMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodN;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.java.addons.ArrayJavaAddons;
import org.jruby.java.addons.IOJavaAddons;
import org.jruby.java.addons.StringJavaAddons;
import org.jruby.java.codegen.RealClassGenerator;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.invokers.InstanceMethodInvoker;
import org.jruby.java.invokers.MethodInvoker;
import org.jruby.java.invokers.StaticMethodInvoker;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.ArrayJavaProxyCreator;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.MapJavaProxy;
import org.jruby.java.proxies.InterfaceJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.proxies.RubyObjectHolderProxy;
import org.jruby.util.ClassCache.OneShotClassLoader;
import org.jruby.util.cli.Options;

@JRubyModule(name = "Java")
public class Java implements Library {
    public static final boolean NEW_STYLE_EXTENSION = Options.JI_NEWSTYLEEXTENSION.load();
    public static final boolean OBJECT_PROXY_CACHE = Options.JI_OBJECTPROXYCACHE.load();

    public void load(Ruby runtime, boolean wrap) throws IOException {
        createJavaModule(runtime);

        RubyModule jpmt = runtime.defineModule("JavaPackageModuleTemplate");
        jpmt.getSingletonClass().setSuperClass(new BlankSlateWrapper(runtime, jpmt.getMetaClass().getSuperClass(), runtime.getKernel()));

        runtime.getLoadService().require("jruby/java");
        
        // rewite ArrayJavaProxy superclass to point at Object, so it inherits Object behaviors
        RubyClass ajp = runtime.getClass("ArrayJavaProxy");
        ajp.setSuperClass(runtime.getJavaSupport().getObjectJavaClass().getProxyClass());
        ajp.includeModule(runtime.getEnumerable());
        
        RubyClassPathVariable.createClassPathVariable(runtime);

        // modify ENV_JAVA to be a read/write version
        Map systemProps = new SystemPropertiesMap();
        runtime.getObject().setConstantQuiet(
                "ENV_JAVA",
                new MapJavaProxy(
                        runtime,
                        (RubyClass)Java.getProxyClass(runtime, JavaClass.get(runtime, SystemPropertiesMap.class)),
                        systemProps));
    }

    public static RubyModule createJavaModule(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        RubyModule javaModule = runtime.defineModule("Java");
        
        javaModule.defineAnnotatedMethods(Java.class);

        JavaObject.createJavaObjectClass(runtime, javaModule);
        JavaArray.createJavaArrayClass(runtime, javaModule);
        JavaClass.createJavaClassClass(runtime, javaModule);
        JavaMethod.createJavaMethodClass(runtime, javaModule);
        JavaConstructor.createJavaConstructorClass(runtime, javaModule);
        JavaField.createJavaFieldClass(runtime, javaModule);
        
        // set of utility methods for Java-based proxy objects
        JavaProxyMethods.createJavaProxyMethods(context);
        
        // the proxy (wrapper) type hierarchy
        JavaProxy.createJavaProxy(context);
        ArrayJavaProxyCreator.createArrayJavaProxyCreator(context);
        ConcreteJavaProxy.createConcreteJavaProxy(context);
        InterfaceJavaProxy.createInterfaceJavaProxy(context);
        ArrayJavaProxy.createArrayJavaProxy(context);

        // creates ruby's hash methods' proxy for Map interface
        MapJavaProxy.createMapJavaProxy(context);

        // also create the JavaProxy* classes
        JavaProxyClass.createJavaProxyModule(runtime);

        // The template for interface modules
        JavaInterfaceTemplate.createJavaInterfaceTemplateModule(context);

        RubyModule javaUtils = runtime.defineModule("JavaUtilities");
        
        javaUtils.defineAnnotatedMethods(JavaUtilities.class);

        JavaArrayUtilities.createJavaArrayUtilitiesModule(runtime);
        
        // Now attach Java-related extras to core classes
        runtime.getArray().defineAnnotatedMethods(ArrayJavaAddons.class);
        runtime.getKernel().defineAnnotatedMethods(KernelJavaAddons.class);
        runtime.getString().defineAnnotatedMethods(StringJavaAddons.class);
        runtime.getIO().defineAnnotatedMethods(IOJavaAddons.class);

        if (runtime.getObject().isConstantDefined("StringIO")) {
            ((RubyClass)runtime.getObject().getConstant("StringIO")).defineAnnotatedMethods(IOJavaAddons.AnyIO.class);
        }
        
        // add all name-to-class mappings
        addNameClassMappings(runtime, runtime.getJavaSupport().getNameClassMap());
        
        // add some base Java classes everyone will need
        runtime.getJavaSupport().setObjectJavaClass(JavaClass.get(runtime, Object.class));

        return javaModule;
    }

    public static class OldStyleExtensionInherited {
        @JRubyMethod
        public static IRubyObject inherited(IRubyObject recv, IRubyObject arg0) {
            return Java.concrete_proxy_inherited(recv, arg0);
        }
    };

    public static class NewStyleExtensionInherited {
        @JRubyMethod
        public static IRubyObject inherited(IRubyObject recv, IRubyObject arg0) {
            if (!(arg0 instanceof RubyClass)) {
                throw recv.getRuntime().newTypeError(arg0, recv.getRuntime().getClassClass());
            }
            
            JavaInterfaceTemplate.addRealImplClassNew((RubyClass)arg0);
            return recv.getRuntime().getNil();
        }
    };
    
    /**
     * This populates the master map from short-cut names to JavaClass instances for
     * a number of core Java types.
     * 
     * @param runtime
     * @param nameClassMap
     */
    private static void addNameClassMappings(Ruby runtime, Map<String, JavaClass> nameClassMap) {
        JavaClass booleanPrimClass = JavaClass.get(runtime, Boolean.TYPE);
        JavaClass booleanClass = JavaClass.get(runtime, Boolean.class);
        nameClassMap.put("boolean", booleanPrimClass);
        nameClassMap.put("Boolean", booleanClass);
        nameClassMap.put("java.lang.Boolean", booleanClass);
        
        JavaClass bytePrimClass = JavaClass.get(runtime, Byte.TYPE);
        JavaClass byteClass = JavaClass.get(runtime, Byte.class);
        nameClassMap.put("byte", bytePrimClass);
        nameClassMap.put("Byte", byteClass);
        nameClassMap.put("java.lang.Byte", byteClass);
        
        JavaClass shortPrimClass = JavaClass.get(runtime, Short.TYPE);
        JavaClass shortClass = JavaClass.get(runtime, Short.class);
        nameClassMap.put("short", shortPrimClass);
        nameClassMap.put("Short", shortClass);
        nameClassMap.put("java.lang.Short", shortClass);
        
        JavaClass charPrimClass = JavaClass.get(runtime, Character.TYPE);
        JavaClass charClass = JavaClass.get(runtime, Character.class);
        nameClassMap.put("char", charPrimClass);
        nameClassMap.put("Character", charClass);
        nameClassMap.put("Char", charClass);
        nameClassMap.put("java.lang.Character", charClass);
        
        JavaClass intPrimClass = JavaClass.get(runtime, Integer.TYPE);
        JavaClass intClass = JavaClass.get(runtime, Integer.class);
        nameClassMap.put("int", intPrimClass);
        nameClassMap.put("Integer", intClass);
        nameClassMap.put("Int", intClass);
        nameClassMap.put("java.lang.Integer", intClass);
        
        JavaClass longPrimClass = JavaClass.get(runtime, Long.TYPE);
        JavaClass longClass = JavaClass.get(runtime, Long.class);
        nameClassMap.put("long", longPrimClass);
        nameClassMap.put("Long", longClass);
        nameClassMap.put("java.lang.Long", longClass);
        
        JavaClass floatPrimClass = JavaClass.get(runtime, Float.TYPE);
        JavaClass floatClass = JavaClass.get(runtime, Float.class);
        nameClassMap.put("float", floatPrimClass);
        nameClassMap.put("Float", floatClass);
        nameClassMap.put("java.lang.Float", floatClass);
        
        JavaClass doublePrimClass = JavaClass.get(runtime, Double.TYPE);
        JavaClass doubleClass = JavaClass.get(runtime, Double.class);
        nameClassMap.put("double", doublePrimClass);
        nameClassMap.put("Double", doubleClass);
        nameClassMap.put("java.lang.Double", doubleClass);
        
        JavaClass bigintClass = JavaClass.get(runtime, BigInteger.class);
        nameClassMap.put("big_int", bigintClass);
        nameClassMap.put("big_integer", bigintClass);
        nameClassMap.put("BigInteger", bigintClass);
        nameClassMap.put("java.math.BigInteger", bigintClass);
        
        JavaClass bigdecimalClass = JavaClass.get(runtime, BigDecimal.class);
        nameClassMap.put("big_decimal", bigdecimalClass);
        nameClassMap.put("BigDecimal", bigdecimalClass);
        nameClassMap.put("java.math.BigDecimal", bigdecimalClass);
        
        JavaClass objectClass = JavaClass.get(runtime, Object.class);
        nameClassMap.put("object", objectClass);
        nameClassMap.put("Object", objectClass);
        nameClassMap.put("java.lang.Object", objectClass);
        
        JavaClass stringClass = JavaClass.get(runtime, String.class);
        nameClassMap.put("string", stringClass);
        nameClassMap.put("String", stringClass);
        nameClassMap.put("java.lang.String", stringClass);
    }

    private static final ClassProvider JAVA_PACKAGE_CLASS_PROVIDER = new ClassProvider() {

        public RubyClass defineClassUnder(RubyModule pkg, String name, RubyClass superClazz) {
            // shouldn't happen, but if a superclass is specified, it's not ours
            if (superClazz != null) {
                return null;
            }
            IRubyObject packageName;
            // again, shouldn't happen. TODO: might want to throw exception instead.
            if ((packageName = pkg.getInstanceVariables().getInstanceVariable("@package_name")) == null) {
                return null;
            }
            Ruby runtime = pkg.getRuntime();
            return (RubyClass) get_proxy_class(
                    runtime.getJavaSupport().getJavaUtilitiesModule(),
                    JavaClass.forNameVerbose(runtime, packageName.asJavaString() + name));
        }

        public RubyModule defineModuleUnder(RubyModule pkg, String name) {
            IRubyObject packageName;
            // again, shouldn't happen. TODO: might want to throw exception instead.
            if ((packageName = pkg.getInstanceVariables().getInstanceVariable("@package_name")) == null) {
                return null;
            }
            Ruby runtime = pkg.getRuntime();
            return (RubyModule) get_interface_module(
                    runtime,
                    JavaClass.forNameVerbose(runtime, packageName.asJavaString() + name));
        }
    };

    private static final Map<String, Boolean> JAVA_PRIMITIVES = new HashMap<String, Boolean>();
    static {
        String[] primitives = {"boolean", "byte", "char", "short", "int", "long", "float", "double"};
        for (String primitive : primitives) {
            JAVA_PRIMITIVES.put(primitive, Boolean.TRUE);
        }
    }

    public static IRubyObject create_proxy_class(
            IRubyObject recv,
            IRubyObject constant,
            IRubyObject javaClass,
            IRubyObject module) {
        Ruby runtime = recv.getRuntime();

        if (!(module instanceof RubyModule)) {
            throw runtime.newTypeError(module, runtime.getModule());
        }
        IRubyObject proxyClass = get_proxy_class(recv, javaClass);
        RubyModule m = (RubyModule)module;
        String constName = constant.asJavaString();
        IRubyObject existing = m.getConstantNoConstMissing(constName);

        if (existing != null
                && existing != RubyBasicObject.UNDEF
                && existing != proxyClass) {
            runtime.getWarnings().warn("replacing " + existing + " with " + proxyClass + " in constant '" + constName + " on class/module " + m);
        }
        
        return ((RubyModule) module).setConstantQuiet(constant.asJavaString(), get_proxy_class(recv, javaClass));
    }

    public static IRubyObject get_java_class(IRubyObject recv, IRubyObject name) {
        try {
            return JavaClass.for_name(recv, name);
        } catch (Exception e) {
            recv.getRuntime().getJavaSupport().handleNativeException(e, null);
            return recv.getRuntime().getNil();
        }
    }

    /**
     * Same as Java#getInstance(runtime, rawJavaObject, false).
     */
    public static IRubyObject getInstance(Ruby runtime, Object rawJavaObject) {
        return getInstance(runtime, rawJavaObject, false);
    }
    
    /**
     * Returns a new proxy instance of a type corresponding to rawJavaObject's class,
     * or the cached proxy if we've already seen this object.  Note that primitives
     * and strings are <em>not</em> coerced to corresponding Ruby types; use
     * JavaUtil.convertJavaToUsableRubyObject to get coerced types or proxies as
     * appropriate.
     * 
     * @param runtime the JRuby runtime
     * @param rawJavaObject the object to get a wrapper for
     * @param forceCache whether to force the use of the proxy cache
     * @return the new (or cached) proxy for the specified Java object
     * @see JavaUtil#convertJavaToUsableRubyObject
     */
    public static IRubyObject getInstance(Ruby runtime, Object rawJavaObject, boolean forceCache) {
        if (rawJavaObject != null) {
            RubyClass proxyClass = (RubyClass) getProxyClass(runtime, JavaClass.get(runtime, rawJavaObject.getClass()));

            if (OBJECT_PROXY_CACHE || forceCache || proxyClass.getCacheProxy()) {
                return runtime.getJavaSupport().getObjectProxyCache().getOrCreate(rawJavaObject, proxyClass);
            } else {
                return allocateProxy(rawJavaObject, proxyClass);
            }
        }
        return runtime.getNil();
    }

    public static RubyModule getInterfaceModule(Ruby runtime, JavaClass javaClass) {
        if (!javaClass.javaClass().isInterface()) {
            throw runtime.newArgumentError(javaClass.toString() + " is not an interface");
        }
        RubyModule interfaceModule;
        if ((interfaceModule = javaClass.getProxyModule()) != null) {
            return interfaceModule;
        }
        javaClass.lockProxy();
        try {
            if ((interfaceModule = javaClass.getProxyModule()) == null) {
                interfaceModule = (RubyModule) runtime.getJavaSupport().getJavaInterfaceTemplate().dup();
                interfaceModule.setInstanceVariable("@java_class", javaClass);
                javaClass.setupInterfaceModule(interfaceModule);
                // include any interfaces we extend
                Class<?>[] extended = javaClass.javaClass().getInterfaces();
                for (int i = extended.length; --i >= 0;) {
                    JavaClass extendedClass = JavaClass.get(runtime, extended[i]);
                    RubyModule extModule = getInterfaceModule(runtime, extendedClass);
                    interfaceModule.includeModule(extModule);
                }
                addToJavaPackageModule(interfaceModule, javaClass);
            }
        } finally {
            javaClass.unlockProxy();
        }
        return interfaceModule;
    }

    public static IRubyObject get_interface_module(Ruby runtime, IRubyObject javaClassObject) {
        JavaClass javaClass;
        if (javaClassObject instanceof RubyString) {
            javaClass = JavaClass.forNameVerbose(runtime, javaClassObject.asJavaString());
        } else if (javaClassObject instanceof JavaClass) {
            javaClass = (JavaClass) javaClassObject;
        } else {
            throw runtime.newArgumentError("expected JavaClass, got " + javaClassObject);
        }
        return getInterfaceModule(runtime, javaClass);
    }

    public static RubyClass getProxyClassForObject(Ruby runtime, Object object) {
        return (RubyClass)getProxyClass(runtime, JavaClass.get(runtime, object.getClass()));
    }

    public static RubyModule getProxyClass(Ruby runtime, JavaClass javaClass) {
        RubyClass proxyClass;
        final Class<?> c = javaClass.javaClass();
        if ((proxyClass = javaClass.getProxyClass()) != null) {
            return proxyClass;
        }
        if (c.isInterface()) {
            return getInterfaceModule(runtime, javaClass);
        }
        javaClass.lockProxy();
        try {
            if ((proxyClass = javaClass.getProxyClass()) == null) {

                if (c.isArray()) {
                    proxyClass = createProxyClass(runtime,
                            runtime.getJavaSupport().getArrayProxyClass(),
                            javaClass, true);

                } else if (c.isPrimitive()) {
                    proxyClass = createProxyClass(runtime,
                            runtime.getJavaSupport().getConcreteProxyClass(),
                            javaClass, true);

                } else if (c == Object.class) {
                    // java.lang.Object is added at root of java proxy classes
                    proxyClass = createProxyClass(runtime,
                            runtime.getJavaSupport().getConcreteProxyClass(),
                            javaClass, true);
                    if (NEW_STYLE_EXTENSION) {
                        proxyClass.getMetaClass().defineAnnotatedMethods(NewStyleExtensionInherited.class);
                    } else {
                        proxyClass.getMetaClass().defineAnnotatedMethods(OldStyleExtensionInherited.class);
                    }
                    addToJavaPackageModule(proxyClass, javaClass);

                } else {
                    // other java proxy classes added under their superclass' java proxy
                    proxyClass = createProxyClass(runtime,
                        (RubyClass) getProxyClass(runtime, JavaClass.get(runtime, c.getSuperclass())),
                        javaClass, false);
                    // include interface modules into the proxy class
                    Class<?>[] interfaces = c.getInterfaces();
                    for (int i = interfaces.length; --i >= 0;) {
                        JavaClass ifc = JavaClass.get(runtime, interfaces[i]);
                        // java.util.Map type object has its own proxy, but following
                        // is needed. Unless kind_of?(is_a?) test will fail.
                        //if (interfaces[i] != java.util.Map.class) {
                            proxyClass.includeModule(getInterfaceModule(runtime, ifc));
                        //}
                    }
                    if (Modifier.isPublic(c.getModifiers())) {
                        addToJavaPackageModule(proxyClass, javaClass);
                    }
                }

                // JRUBY-1000, fail early when attempting to subclass a final Java class;
                // solved here by adding an exception-throwing "inherited"
                if (Modifier.isFinal(c.getModifiers())) {
                    proxyClass.getMetaClass().addMethod("inherited", new org.jruby.internal.runtime.methods.JavaMethod(proxyClass, PUBLIC) {
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                            throw context.getRuntime().newTypeError("can not extend final Java class: " + c.getCanonicalName());
                        }
                    });
                }
            }
        } finally {
            javaClass.unlockProxy();
        }
        return proxyClass;
    }

    public static IRubyObject get_proxy_class(IRubyObject recv, IRubyObject java_class_object) {
        Ruby runtime = recv.getRuntime();
        JavaClass javaClass;
        if (java_class_object instanceof RubyString) {
            javaClass = JavaClass.for_name(recv, java_class_object);
        } else if (java_class_object instanceof JavaClass) {
            javaClass = (JavaClass) java_class_object;
        } else {
            throw runtime.newTypeError(java_class_object, runtime.getJavaSupport().getJavaClassClass());
        }
        return getProxyClass(runtime, javaClass);
    }

    private static RubyClass createProxyClass(Ruby runtime, RubyClass baseType,
            JavaClass javaClass, boolean invokeInherited) {
        // JRUBY-2938 the proxy class might already exist
        RubyClass proxyClass = javaClass.getProxyClass();
        if (proxyClass != null) return proxyClass;

        // this needs to be split, since conditional calling #inherited doesn't fit standard ruby semantics
        RubyClass.checkInheritable(baseType);
        RubyClass superClass = (RubyClass) baseType;
        proxyClass = RubyClass.newClass(runtime, superClass);
        proxyClass.makeMetaClass(superClass.getMetaClass());
        try {
            javaClass.javaClass().asSubclass(java.util.Map.class);
            proxyClass.setAllocator(runtime.getJavaSupport().getMapJavaProxyClass().getAllocator());
            proxyClass.defineAnnotatedMethods(MapJavaProxy.class);
            proxyClass.includeModule(runtime.getEnumerable());
        } catch (ClassCastException e) {
            proxyClass.setAllocator(superClass.getAllocator());
        }
        if (invokeInherited) {
            proxyClass.inherit(superClass);
        }
        proxyClass.callMethod(runtime.getCurrentContext(), "java_class=", javaClass);
        javaClass.setupProxy(proxyClass);

        // add java_method for unbound use
        proxyClass.defineAnnotatedMethods(JavaProxyClassMethods.class);
        return proxyClass;
    }

    public static class JavaProxyClassMethods {
        @JRubyMethod(meta = true)
        public static IRubyObject java_method(ThreadContext context, IRubyObject proxyClass, IRubyObject rubyName) {
            String name = rubyName.asJavaString();

            return getRubyMethod(context, proxyClass, name);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_method(ThreadContext context, IRubyObject proxyClass, IRubyObject rubyName, IRubyObject argTypes) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);

            return getRubyMethod(context, proxyClass, name, argTypesClasses);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName) {
            String name = rubyName.asJavaString();
            Ruby runtime = context.getRuntime();

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(runtime, recv, name));
            return method.invokeStaticDirect();
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName, IRubyObject argTypes) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            Ruby runtime = context.getRuntime();

            if (argTypesAry.size() != 0) {
                Class[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argTypesAry.size()]);
                throw JavaMethod.newArgSizeMismatchError(runtime, argTypesClasses);
            }

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(runtime, recv, name));
            return method.invokeStaticDirect();
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName, IRubyObject argTypes, IRubyObject arg0) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            Ruby runtime = context.getRuntime();

            if (argTypesAry.size() != 1) {
                throw JavaMethod.newArgSizeMismatchError(runtime, (Class) argTypesAry.eltInternal(0).toJava(Class.class));
            }

            Class argTypeClass = (Class) argTypesAry.eltInternal(0).toJava(Class.class);

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(runtime, recv, name, argTypeClass));
            return method.invokeStaticDirect(arg0.toJava(argTypeClass));
        }

        @JRubyMethod(required = 4, rest = true, meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            Ruby runtime = context.getRuntime();

            String name = args[0].asJavaString();
            RubyArray argTypesAry = args[1].convertToArray();
            int argsLen = args.length - 2;

            if (argTypesAry.size() != argsLen) {
                throw JavaMethod.newArgSizeMismatchError(runtime, (Class[]) argTypesAry.toArray(new Class[argTypesAry.size()]));
            }

            Class[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argsLen]);

            Object[] argsAry = new Object[argsLen];
            for (int i = 0; i < argsLen; i++) {
                argsAry[i] = args[i + 2].toJava(argTypesClasses[i]);
            }

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(runtime, recv, name, argTypesClasses));
            return method.invokeStaticDirect(argsAry);
        }

        @JRubyMethod(meta = true, visibility = PRIVATE)
        public static IRubyObject java_alias(ThreadContext context, IRubyObject proxyClass, IRubyObject newName, IRubyObject rubyName) {
            return java_alias(context, proxyClass, newName, rubyName, context.getRuntime().newEmptyArray());
        }

        @JRubyMethod(meta = true, visibility = PRIVATE)
        public static IRubyObject java_alias(ThreadContext context, IRubyObject proxyClass, IRubyObject newName, IRubyObject rubyName, IRubyObject argTypes) {
            String name = rubyName.asJavaString();
            String newNameStr = newName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            Class[] argTypesClasses = (Class[])argTypesAry.toArray(new Class[argTypesAry.size()]);
            Ruby runtime = context.getRuntime();
            RubyClass rubyClass;

            if (proxyClass instanceof RubyClass) {
                rubyClass = (RubyClass)proxyClass;
            } else {
                throw runtime.newTypeError(proxyClass, runtime.getModule());
            }

            Method method = getMethodFromClass(runtime, proxyClass, name, argTypesClasses);
            MethodInvoker invoker = getMethodInvokerForMethod(rubyClass, method);

            if (Modifier.isStatic(method.getModifiers())) {
                // add alias to meta
                rubyClass.getSingletonClass().addMethod(newNameStr, invoker);
            } else {
                rubyClass.addMethod(newNameStr, invoker);
            }

            return runtime.getNil();
        }
    }

    private static IRubyObject getRubyMethod(ThreadContext context, IRubyObject proxyClass, String name, Class... argTypesClasses) {
        Ruby runtime = context.getRuntime();
        RubyClass rubyClass;
        
        if (proxyClass instanceof RubyClass) {
            rubyClass = (RubyClass)proxyClass;
        } else {
            throw runtime.newTypeError(proxyClass, runtime.getModule());
        }

        Method jmethod = getMethodFromClass(runtime, proxyClass, name, argTypesClasses);
        String prettyName = name + CodegenUtils.prettyParams(argTypesClasses);
        
        if (Modifier.isStatic(jmethod.getModifiers())) {
            MethodInvoker invoker = new StaticMethodInvoker(rubyClass, jmethod);
            return RubyMethod.newMethod(rubyClass, prettyName, rubyClass, name, invoker, proxyClass);
        } else {
            MethodInvoker invoker = new InstanceMethodInvoker(rubyClass, jmethod);
            return RubyUnboundMethod.newUnboundMethod(rubyClass, prettyName, rubyClass, name, invoker);
        }
    }

    public static Method getMethodFromClass(Ruby runtime, IRubyObject proxyClass, String name, Class... argTypes) {
        Class jclass = (Class)((JavaClass)proxyClass.callMethod(runtime.getCurrentContext(), "java_class")).getValue();

        try {
            return jclass.getMethod(name, argTypes);
        } catch (NoSuchMethodException nsme) {
            String prettyName = name + CodegenUtils.prettyParams(argTypes);
            String errorName = jclass.getName() + "." + prettyName;
            throw runtime.newNameError("Java method not found: " + errorName, name);
        }
    }

    private static MethodInvoker getMethodInvokerForMethod(RubyClass metaClass, Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return new StaticMethodInvoker(metaClass.getMetaClass(), method);
        } else {
            return new InstanceMethodInvoker(metaClass, method);
        }
    }

    public static IRubyObject concrete_proxy_inherited(IRubyObject recv, IRubyObject subclass) {
        Ruby runtime = recv.getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        JavaSupport javaSupport = runtime.getJavaSupport();
        RubyClass javaProxyClass = javaSupport.getJavaProxyClass().getMetaClass();
        RuntimeHelpers.invokeAs(tc, javaProxyClass, recv, "inherited", subclass,
                Block.NULL_BLOCK);
        return setupJavaSubclass(tc, subclass, recv.callMethod(tc, "java_class"));
    }

    private static IRubyObject setupJavaSubclass(ThreadContext context, IRubyObject subclass, IRubyObject java_class) {
        final Ruby runtime = context.getRuntime();

        if (!(subclass instanceof RubyClass)) {
            throw runtime.newTypeError(subclass, runtime.getClassClass());
        }
        RubyClass rubySubclass = (RubyClass)subclass;
        rubySubclass.getInstanceVariables().setInstanceVariable("@java_proxy_class", runtime.getNil());

        // Subclasses of Java classes can safely use ivars, so we set this to silence warnings
        rubySubclass.setCacheProxy(true);

        RubyClass subclassSingleton = rubySubclass.getSingletonClass();
        subclassSingleton.addReadWriteAttribute(context, "java_proxy_class");
        subclassSingleton.addMethod("java_interfaces", new JavaMethodZero(subclassSingleton, PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                IRubyObject javaInterfaces = self.getInstanceVariables().getInstanceVariable("@java_interfaces");
                if (javaInterfaces != null) return javaInterfaces.dup();
                return context.getRuntime().getNil();
            }
        });

        rubySubclass.addMethod("__jcreate!", new JavaMethodN(subclassSingleton, PUBLIC) {
            private final Map<Integer, ParameterTypes> methodCache = new HashMap<Integer, ParameterTypes>();
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                IRubyObject proxyClass = self.getMetaClass().getInstanceVariables().getInstanceVariable("@java_proxy_class");
                if (proxyClass == null || proxyClass.isNil()) {
                    proxyClass = JavaProxyClass.get_with_class(self, self.getMetaClass());
                    self.getMetaClass().getInstanceVariables().setInstanceVariable("@java_proxy_class", proxyClass);
                }
                
                JavaProxyClass realProxyClass = (JavaProxyClass)proxyClass;
                RubyArray constructors = realProxyClass.constructors();
                ArrayList<JavaProxyConstructor> forArity = new ArrayList<JavaProxyConstructor>();
                for (int i = 0; i < constructors.size(); i++) {
                    JavaProxyConstructor constructor = (JavaProxyConstructor)constructors.eltInternal(i);
                    if (constructor.getParameterTypes().length == args.length) {
                        forArity.add(constructor);
                    }
                }
                
                if (forArity.size() == 0) {
                    throw runtime.newArgumentError("wrong number of arguments for constructor");
                }

                JavaProxyConstructor matching = (JavaProxyConstructor)CallableSelector.matchingCallableArityN(
                        runtime, methodCache,
                        forArity.toArray(new JavaProxyConstructor[forArity.size()]), args, args.length);

                if (matching == null) {
                    throw runtime.newArgumentError("wrong number of arguments for constructor");
                }

                Object[] newArgs = new Object[args.length];
                Class[] parameterTypes = matching.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    newArgs[i] = args[i].toJava(parameterTypes[i]);
                }
                
                JavaObject newObject = matching.newInstance(self, newArgs);
                return JavaUtilities.set_java_object(self, self, newObject);
            }
        });

        return runtime.getNil();
    }

    // package scheme 2: separate module for each full package name, constructed 
    // from the camel-cased package segments: Java::JavaLang::Object, 
    private static void addToJavaPackageModule(RubyModule proxyClass, JavaClass javaClass) {
        Ruby runtime = proxyClass.getRuntime();
        Class<?> clazz = javaClass.javaClass();
        String fullName;
        if ((fullName = clazz.getName()) == null) {
            return;
        }
        int endPackage = fullName.lastIndexOf('.');
        RubyModule parentModule;
        String className;

        // inner classes must be nested
        if (fullName.indexOf('$') != -1) {
            IRubyObject declClass = javaClass.declaring_class();
            if (declClass.isNil()) {
                // no containing class for a $ class; treat it as internal and don't define a constant
                return;
            }
            parentModule = getProxyClass(runtime, (JavaClass)declClass);
            className = clazz.getSimpleName();
        } else {
            String packageString = endPackage < 0 ? "" : fullName.substring(0, endPackage);
            parentModule = getJavaPackageModule(runtime, packageString);
            className = parentModule == null ? fullName : fullName.substring(endPackage + 1);
        }
        
        if (parentModule != null && IdUtil.isConstant(className)) {
            if (parentModule.getConstantAt(className) == null) {
                parentModule.setConstant(className, proxyClass);
            }
        }
    }

    private static RubyModule getJavaPackageModule(Ruby runtime, String packageString) {
        String packageName;
        int length = packageString.length();
        if (length == 0) {
            packageName = "Default";
        } else {
            StringBuilder buf = new StringBuilder();
            for (int start = 0, offset = 0; start < length; start = offset + 1) {
                if ((offset = packageString.indexOf('.', start)) == -1) {
                    offset = length;
                }
                buf.append(Character.toUpperCase(packageString.charAt(start))).append(packageString.substring(start + 1, offset));
            }
            packageName = buf.toString();
        }

        RubyModule javaModule = runtime.getJavaSupport().getJavaModule();
        IRubyObject packageModule = javaModule.getConstantAt(packageName);
        if (packageModule == null) {
            return createPackageModule(javaModule, packageName, packageString);
        } else if (packageModule instanceof RubyModule) {
            return (RubyModule) packageModule;
        } else {
            return null;
        }
    }

    private static RubyModule createPackageModule(RubyModule parent, String name, String packageString) {
        Ruby runtime = parent.getRuntime();
        RubyModule packageModule = (RubyModule) runtime.getJavaSupport().getPackageModuleTemplate().dup();
        packageModule.setInstanceVariable("@package_name", runtime.newString(
                packageString.length() > 0 ? packageString + '.' : packageString));

        // this is where we'll get connected when classes are opened using
        // package module syntax.
        packageModule.addClassProvider(JAVA_PACKAGE_CLASS_PROVIDER);

        parent.const_set(runtime.newSymbol(name), packageModule);
        MetaClass metaClass = (MetaClass) packageModule.getMetaClass();
        metaClass.setAttached(packageModule);
        return packageModule;
    }
    private static final Pattern CAMEL_CASE_PACKAGE_SPLITTER = Pattern.compile("([a-z][0-9]*)([A-Z])");

    private static RubyModule getPackageModule(Ruby runtime, String name) {
        RubyModule javaModule = runtime.getJavaSupport().getJavaModule();
        IRubyObject value;
        if ((value = javaModule.getConstantAt(name)) instanceof RubyModule) {
            return (RubyModule) value;
        }
        String packageName;
        if ("Default".equals(name)) {
            packageName = "";
        } else {
            Matcher m = CAMEL_CASE_PACKAGE_SPLITTER.matcher(name);
            packageName = m.replaceAll("$1.$2").toLowerCase();
        }
        return createPackageModule(javaModule, name, packageName);
    }

    public static IRubyObject get_package_module(IRubyObject recv, IRubyObject symObject) {
        return getPackageModule(recv.getRuntime(), symObject.asJavaString());
    }

    public static IRubyObject get_package_module_dot_format(IRubyObject recv, IRubyObject dottedName) {
        Ruby runtime = recv.getRuntime();
        RubyModule module = getJavaPackageModule(runtime, dottedName.asJavaString());
        return module == null ? runtime.getNil() : module;
    }

    private static RubyModule getProxyOrPackageUnderPackage(ThreadContext context, final Ruby runtime,
            RubyModule parentPackage, String sym) {
        IRubyObject packageNameObj = parentPackage.getInstanceVariable("@package_name");
        if (packageNameObj == null) {
            throw runtime.newArgumentError("invalid package module");
        }
        String packageName = packageNameObj.asJavaString();
        final String name = sym.trim().intern();
        if (name.length() == 0) {
            throw runtime.newArgumentError("empty class or package name");
        }
        String fullName = packageName + name;
        if (!Character.isUpperCase(name.charAt(0))) {
            // filter out any Java primitive names
            // TODO: should check against all Java reserved names here, not just primitives
            if (JAVA_PRIMITIVES.containsKey(name)) {
                throw runtime.newArgumentError("illegal package name component: " + name);
            }
            
            // this covers the rare case of lower-case class names (and thus will
            // fail 99.999% of the time). fortunately, we'll only do this once per
            // package name. (and seriously, folks, look into best practices...)
            try {
                return getProxyClass(runtime, JavaClass.forNameQuiet(runtime, fullName));
            } catch (RaiseException re) { /* expected */
                RubyException rubyEx = re.getException();
                if (rubyEx.kind_of_p(context, runtime.getStandardError()).isTrue()) {
                    RuntimeHelpers.setErrorInfo(runtime, runtime.getNil());
                }
            } catch (Exception e) { /* expected */ }

            // Haven't found a class, continue on as though it were a package
            final RubyModule packageModule = getJavaPackageModule(runtime, fullName);
            // TODO: decompose getJavaPackageModule so we don't parse fullName
            if (packageModule == null) {
                return null;
            }

            // save package in singletonized parent, so we don't come back here
            memoizePackageOrClass(parentPackage, name, packageModule);
            
            return packageModule;
        } else {
            try {
                // First char is upper case, so assume it's a class name
                final RubyModule javaModule = getProxyClass(runtime, JavaClass.forNameVerbose(runtime, fullName));
                
                // save class in singletonized parent, so we don't come back here
                memoizePackageOrClass(parentPackage, name, javaModule);

                return javaModule;
            } catch (Exception e) {
                if (RubyInstanceConfig.UPPER_CASE_PACKAGE_NAME_ALLOWED) {
                    // but for those not hip to conventions and best practices,
                    // we'll try as a package
                    return getJavaPackageModule(runtime, fullName);
                } else {
                    throw runtime.newNameError("uppercase package names not accessible this way (`" + fullName + "')", fullName);
                }
            }

        }
    }

    public static IRubyObject get_proxy_or_package_under_package(
            ThreadContext context,
            IRubyObject recv,
            IRubyObject parentPackage,
            IRubyObject sym) {
        Ruby runtime = recv.getRuntime();
        if (!(parentPackage instanceof RubyModule)) {
            throw runtime.newTypeError(parentPackage, runtime.getModule());
        }
        RubyModule result;
        if ((result = getProxyOrPackageUnderPackage(context, runtime,
                (RubyModule) parentPackage, sym.asJavaString())) != null) {
            return result;
        }
        return runtime.getNil();
    }

    private static RubyModule getTopLevelProxyOrPackage(ThreadContext context, final Ruby runtime, String sym) {
        final String name = sym.trim().intern();
        if (name.length() == 0) {
            throw runtime.newArgumentError("empty class or package name");
        }
        if (Character.isLowerCase(name.charAt(0))) {
            // this covers primitives and (unlikely) lower-case class names
            try {
                return getProxyClass(runtime, JavaClass.forNameQuiet(runtime, name));
            } catch (RaiseException re) { /* not primitive or lc class */
                RubyException rubyEx = re.getException();
                if (rubyEx.kind_of_p(context, runtime.getStandardError()).isTrue()) {
                    RuntimeHelpers.setErrorInfo(runtime, runtime.getNil());
                }
            } catch (Exception e) { /* not primitive or lc class */ }

            // TODO: check for Java reserved names and raise exception if encountered

            final RubyModule packageModule = getJavaPackageModule(runtime, name);
            // TODO: decompose getJavaPackageModule so we don't parse fullName
            if (packageModule == null) {
                return null;
            }
            RubyModule javaModule = runtime.getJavaSupport().getJavaModule();
            if (javaModule.getMetaClass().isMethodBound(name, false)) {
                return packageModule;
            }

            memoizePackageOrClass(javaModule, name, packageModule);
            
            return packageModule;
        } else {
            RubyModule javaModule = null;
            try {
                // we do loadJavaClass here to handle things like LinkageError through
                Class cls = runtime.getJavaSupport().loadJavaClass(name);
                javaModule = getProxyClass(runtime, JavaClass.get(runtime, cls));
            } catch (ExceptionInInitializerError eiie) {
                throw runtime.newNameError("cannot initialize Java class " + name, name, eiie, false);
            } catch (LinkageError le) {
                throw runtime.newNameError("cannot link Java class " + name, name, le, false);
            } catch (SecurityException se) {
                throw runtime.newSecurityError(se.getLocalizedMessage());
            } catch (ClassNotFoundException e) { /* not a class */ }

            // upper-case package name
            // TODO: top-level upper-case package was supported in the previous (Ruby-based)
            // implementation, so leaving as is.  see note at #getProxyOrPackageUnderPackage
            // re: future approach below the top-level.
            if (javaModule == null) {
                javaModule = getPackageModule(runtime, name);
            }

            memoizePackageOrClass(runtime.getJavaSupport().getJavaModule(), name, javaModule);

            return javaModule;
        }
    }

    private static void memoizePackageOrClass(final RubyModule parentPackage, final String name, final IRubyObject value) {
        RubyClass singleton = parentPackage.getSingletonClass();
        singleton.addMethod(name, new org.jruby.internal.runtime.methods.JavaMethod(singleton, PUBLIC) {
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                if (args.length != 0) {
                    throw context.getRuntime().newArgumentError(
                            "Java package `"
                            + parentPackage.callMethod("package_name")
                            + "' does not have a method `"
                            + name
                            + "'");
                }
                return call(context, self, clazz, name);
            }

            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return value;
            }

            @Override
            public Arity getArity() {
                return Arity.noArguments();
            }
        });
    }

    public static IRubyObject get_top_level_proxy_or_package(ThreadContext context, IRubyObject recv, IRubyObject sym) {
        Ruby runtime = context.getRuntime();
        RubyModule result = getTopLevelProxyOrPackage(context, runtime, sym.asJavaString());

        return result != null ? result : runtime.getNil();
    }

    public static IRubyObject wrap(Ruby runtime, IRubyObject java_object) {
        return getInstance(runtime, ((JavaObject) java_object).getValue());
    }
    
    /**
     * High-level object conversion utility function 'java_to_primitive' is the low-level version
     */
    @Deprecated
    @JRubyMethod(frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject java_to_ruby(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        try {
            return JavaUtil.java_to_ruby(recv.getRuntime(), object);
        } catch (RuntimeException e) {
            recv.getRuntime().getJavaSupport().handleNativeException(e, null);
            // This point is only reached if there was an exception handler installed.
            return recv.getRuntime().getNil();
        }
    }

    // TODO: Formalize conversion mechanisms between Java and Ruby
    /**
     * High-level object conversion utility.
     */
    @Deprecated
    @JRubyMethod(frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject ruby_to_java(final IRubyObject recv, IRubyObject object, Block unusedBlock) {
        return JavaUtil.ruby_to_java(recv, object, unusedBlock);
    }

    @Deprecated
    @JRubyMethod(frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        return JavaUtil.java_to_primitive(recv, object, unusedBlock);
    }


    // TODO: Formalize conversion mechanisms between Java and Ruby
    @JRubyMethod(required = 2, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject new_proxy_instance2(IRubyObject recv, final IRubyObject wrapper, IRubyObject ifcs, Block block) {
        IRubyObject[] javaClasses = ((RubyArray)ifcs).toJavaArray();

        // Create list of interface names to proxy (and make sure they really are interfaces)
        // Also build a hashcode from all classes to use for retrieving previously-created impl
        Class[] interfaces = new Class[javaClasses.length];
        for (int i = 0; i < javaClasses.length; i++) {
            if (!(javaClasses[i] instanceof JavaClass) || !((JavaClass) javaClasses[i]).interface_p().isTrue()) {
                throw recv.getRuntime().newArgumentError("Java interface expected. got: " + javaClasses[i]);
            }
            interfaces[i] = ((JavaClass) javaClasses[i]).javaClass();
        }

        return newInterfaceImpl(wrapper, interfaces);
    }

    public static IRubyObject newInterfaceImpl(final IRubyObject wrapper, Class[] interfaces) {
        final Ruby runtime = wrapper.getRuntime();
        ClassDefiningClassLoader classLoader;

        Class[] tmp_interfaces = interfaces;
        interfaces = new Class[tmp_interfaces.length + 1];
        System.arraycopy(tmp_interfaces, 0, interfaces, 0, tmp_interfaces.length);
        interfaces[tmp_interfaces.length] = RubyObjectHolderProxy.class;

        // hashcode is a combination of the interfaces and the Ruby class we're using
        // to implement them
        if (!RubyInstanceConfig.INTERFACES_USE_PROXY) {
            int interfacesHashCode = interfacesHashCode(interfaces);
            // if it's a singleton class and the real class is proc, we're doing closure conversion
            // so just use Proc's hashcode
            if (wrapper.getMetaClass().isSingleton() && wrapper.getMetaClass().getRealClass() == runtime.getProc()) {
                interfacesHashCode = 31 * interfacesHashCode + runtime.getProc().hashCode();
                classLoader = runtime.getJRubyClassLoader();
            } else {
                // normal new class implementing interfaces
                interfacesHashCode = 31 * interfacesHashCode + wrapper.getMetaClass().getRealClass().hashCode();
                classLoader = new OneShotClassLoader(runtime.getJRubyClassLoader());
            }
            String implClassName = "org.jruby.gen.InterfaceImpl" + Math.abs(interfacesHashCode);
            Class proxyImplClass;
            try {
                proxyImplClass = Class.forName(implClassName, true, runtime.getJRubyClassLoader());
            } catch (ClassNotFoundException cnfe) {
                proxyImplClass = RealClassGenerator.createOldStyleImplClass(interfaces, wrapper.getMetaClass(), runtime, implClassName, classLoader);
            }

            try {
                Constructor proxyConstructor = proxyImplClass.getConstructor(IRubyObject.class);
                return JavaObject.wrap(runtime, proxyConstructor.newInstance(wrapper));
            } catch (NoSuchMethodException nsme) {
                throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + nsme);
            } catch (InvocationTargetException ite) {
                throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + ite);
            } catch (InstantiationException ie) {
                throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + ie);
            } catch (IllegalAccessException iae) {
                throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + iae);
            }
        } else {
            Object proxyObject = Proxy.newProxyInstance(runtime.getJRubyClassLoader(), interfaces, new InvocationHandler() {
                private Map parameterTypeCache = new ConcurrentHashMap();

                public Object invoke(Object proxy, Method method, Object[] nargs) throws Throwable {
                    String methodName = method.getName();
                    int length = nargs == null ? 0 : nargs.length;

                    // FIXME: wtf is this? Why would these use the class?
                    if (methodName == "toString" && length == 0) {
                        return proxy.getClass().getName();
                    } else if (methodName == "hashCode" && length == 0) {
                        return Integer.valueOf(proxy.getClass().hashCode());
                    } else if (methodName == "equals" && length == 1) {
                        Class[] parameterTypes = (Class[]) parameterTypeCache.get(method);
                        if (parameterTypes == null) {
                            parameterTypes = method.getParameterTypes();
                            parameterTypeCache.put(method, parameterTypes);
                        }
                        if (parameterTypes[0].equals(Object.class)) {
                            return Boolean.valueOf(proxy == nargs[0]);
                        }
                    } else if (methodName == "__ruby_object" && length == 0) {
                        return wrapper;
                    }

                    IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(runtime, nargs);
                    try {
                        return RuntimeHelpers.invoke(runtime.getCurrentContext(), wrapper, methodName, rubyArgs).toJava(method.getReturnType());
                    } catch (RuntimeException e) { e.printStackTrace(); throw e; }
                }
            });
            return JavaObject.wrap(runtime, proxyObject);
        }
    }

    public static Class generateRealClass(final RubyClass clazz) {
        final Ruby runtime = clazz.getRuntime();
        final Class[] interfaces = getInterfacesFromRubyClass(clazz);

        // hashcode is a combination of the interfaces and the Ruby class we're using
        // to implement them
        int interfacesHashCode = interfacesHashCode(interfaces);
        // normal new class implementing interfaces
        interfacesHashCode = 31 * interfacesHashCode + clazz.hashCode();
        
        String implClassName;
        if (clazz.getBaseName() == null) {
            // no-name class, generate a bogus name for it
            implClassName = "anon_class" + Math.abs(System.identityHashCode(clazz)) + "_" + Math.abs(interfacesHashCode);
        } else {
            implClassName = clazz.getName().replaceAll("::", "\\$\\$") + "_" + Math.abs(interfacesHashCode);
        }
        Class proxyImplClass;
        try {
            proxyImplClass = Class.forName(implClassName, true, runtime.getJRubyClassLoader());
        } catch (ClassNotFoundException cnfe) {
            // try to use super's reified class; otherwise, RubyObject (for now)
            Class superClass = clazz.getSuperClass().getRealClass().getReifiedClass();
            if (superClass == null) {
                superClass = RubyObject.class;
            }
            proxyImplClass = RealClassGenerator.createRealImplClass(superClass, interfaces, clazz, runtime, implClassName);
            
            // add a default initialize if one does not already exist and this is a Java-hierarchy class
            if (NEW_STYLE_EXTENSION &&
                    !(RubyBasicObject.class.isAssignableFrom(proxyImplClass) || clazz.getMethods().containsKey("initialize"))
                    ) {
                clazz.addMethod("initialize", new JavaMethodZero(clazz, PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                        return context.getRuntime().getNil();
                    }
                });
            }
        }
        clazz.setReifiedClass(proxyImplClass);
        clazz.setRubyClassAllocator(proxyImplClass);

        return proxyImplClass;
    }

    public static Constructor getRealClassConstructor(Ruby runtime, Class proxyImplClass) {
        try {
            return proxyImplClass.getConstructor(Ruby.class, RubyClass.class);
        } catch (NoSuchMethodException nsme) {
            throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + nsme);
        }
    }

    public static IRubyObject constructProxy(Ruby runtime, Constructor proxyConstructor, RubyClass clazz) {
        try {
            return (IRubyObject)proxyConstructor.newInstance(runtime, clazz);
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
            throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + ite);
        } catch (InstantiationException ie) {
            throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + ie);
        } catch (IllegalAccessException iae) {
            throw runtime.newTypeError("Exception instantiating generated interface impl:\n" + iae);
        }
    }
    
    public static IRubyObject allocateProxy(Object javaObject, RubyClass clazz) {
        IRubyObject proxy = clazz.allocate();
        if (proxy instanceof JavaProxy) {
            ((JavaProxy)proxy).setObject(javaObject);
        } else {
            JavaObject wrappedObject = JavaObject.wrap(clazz.getRuntime(), javaObject);
            proxy.dataWrapStruct(wrappedObject);
        }

        return proxy;
    }

    public static IRubyObject wrapJavaObject(Ruby runtime, Object object) {
        return allocateProxy(object, getProxyClassForObject(runtime, object));
    }

    public static Class[] getInterfacesFromRubyClass(RubyClass klass) {
        Set<Class> interfaces = new HashSet<Class>();
        // walk all superclasses aggregating interfaces
        while (klass != null) {
            IRubyObject maybeInterfaces = klass.getInstanceVariables().getInstanceVariable("@java_interfaces");
            if (maybeInterfaces instanceof RubyArray) {
                RubyArray moreInterfaces = (RubyArray)maybeInterfaces;
                if (!moreInterfaces.isFrozen()) moreInterfaces.setFrozen(true);

                interfaces.addAll(moreInterfaces);
            }
            klass = klass.getSuperClass();
        }

        return interfaces.toArray(new Class[interfaces.size()]);
    }
    
    private static int interfacesHashCode(Class[] a) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (Class element : a)
            result = 31 * result + (element == null ? 0 : element.hashCode());

        return result;
    }
}
