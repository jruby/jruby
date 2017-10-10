/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jcodings.Encoding;

import org.jruby.*;
import org.jruby.javasupport.binding.Initializer;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodN;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.java.addons.ArrayJavaAddons;
import org.jruby.java.addons.ClassJavaAddons;
import org.jruby.java.addons.IOJavaAddons;
import org.jruby.java.addons.KernelJavaAddons;
import org.jruby.java.addons.StringJavaAddons;
import org.jruby.java.codegen.RealClassGenerator;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.ArrayJavaProxyCreator;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.MapJavaProxy;
import org.jruby.java.proxies.InterfaceJavaProxy;
import org.jruby.java.proxies.JavaInterfaceTemplate;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.proxies.RubyObjectHolderProxy;
import org.jruby.java.util.SystemPropertiesMap;
import org.jruby.javasupport.proxy.JavaProxyClassFactory;
import org.jruby.util.*;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.NonBlockingHashMapLong;

import static org.jruby.java.invokers.RubyToJavaInvoker.convertArguments;
import static org.jruby.runtime.Visibility.*;

@JRubyModule(name = "Java")
public class Java implements Library {
    public static final boolean NEW_STYLE_EXTENSION = Options.JI_NEWSTYLEEXTENSION.load();
    public static final boolean OBJECT_PROXY_CACHE = Options.JI_OBJECTPROXYCACHE.load();

    @Override
    public void load(Ruby runtime, boolean wrap) {
        final RubyModule Java = createJavaModule(runtime);

        JavaPackage.createJavaPackageClass(runtime, Java);

        org.jruby.javasupport.ext.Kernel.define(runtime);

        org.jruby.javasupport.ext.JavaLang.define(runtime);
        org.jruby.javasupport.ext.JavaLangReflect.define(runtime);
        org.jruby.javasupport.ext.JavaUtil.define(runtime);
        org.jruby.javasupport.ext.JavaUtilRegex.define(runtime);
        org.jruby.javasupport.ext.JavaIo.define(runtime);
        org.jruby.javasupport.ext.JavaNet.define(runtime);

        // load Ruby parts of the 'java' library
        runtime.getLoadService().load("jruby/java.rb", false);

        // rewire ArrayJavaProxy superclass to point at Object, so it inherits Object behaviors
        final RubyClass ArrayJavaProxy = runtime.getClass("ArrayJavaProxy");
        ArrayJavaProxy.setSuperClass(runtime.getJavaSupport().getObjectJavaClass().getProxyClass());
        ArrayJavaProxy.includeModule(runtime.getEnumerable());

        RubyClassPathVariable.createClassPathVariable(runtime);

        runtime.setJavaProxyClassFactory(JavaProxyClassFactory.createFactory());

        // modify ENV_JAVA to be a read/write version
        final Map systemProperties = new SystemPropertiesMap();
        RubyClass proxyClass = (RubyClass) getProxyClass(runtime, SystemPropertiesMap.class);
        runtime.getObject().setConstantQuiet("ENV_JAVA", new MapJavaProxy(runtime, proxyClass, systemProperties));
    }

    @SuppressWarnings("deprecation")
    public static RubyModule createJavaModule(final Ruby runtime) {
        final ThreadContext context = runtime.getCurrentContext();

        final RubyModule Java = runtime.defineModule("Java");

        Java.defineAnnotatedMethods(Java.class);

        final RubyClass _JavaObject = JavaObject.createJavaObjectClass(runtime, Java);
        JavaArray.createJavaArrayClass(runtime, Java, _JavaObject);
        JavaClass.createJavaClassClass(runtime, Java, _JavaObject);
        JavaMethod.createJavaMethodClass(runtime, Java);
        JavaConstructor.createJavaConstructorClass(runtime, Java);
        JavaField.createJavaFieldClass(runtime, Java);

        // set of utility methods for Java-based proxy objects
        JavaProxyMethods.createJavaProxyMethods(context);

        // the proxy (wrapper) type hierarchy
        JavaProxy.createJavaProxy(context);
        ArrayJavaProxyCreator.createArrayJavaProxyCreator(context);
        ConcreteJavaProxy.createConcreteJavaProxy(context);
        InterfaceJavaProxy.createInterfaceJavaProxy(context);
        ArrayJavaProxy.createArrayJavaProxy(context);

        // creates ruby's hash methods' proxy for Map interface
        MapJavaProxy.createMapJavaProxy(runtime);

        // also create the JavaProxy* classes
        JavaProxyClass.createJavaProxyClasses(runtime, Java);

        // The template for interface modules
        JavaInterfaceTemplate.createJavaInterfaceTemplateModule(context);

        runtime.defineModule("JavaUtilities").defineAnnotatedMethods(JavaUtilities.class);

        JavaArrayUtilities.createJavaArrayUtilitiesModule(runtime);

        // Now attach Java-related extras to core classes
        runtime.getArray().defineAnnotatedMethods(ArrayJavaAddons.class);
        runtime.getKernel().defineAnnotatedMethods(KernelJavaAddons.class);
        runtime.getString().defineAnnotatedMethods(StringJavaAddons.class);
        runtime.getIO().defineAnnotatedMethods(IOJavaAddons.class);
        runtime.getClassClass().defineAnnotatedMethods(ClassJavaAddons.class);

        if ( runtime.getObject().isConstantDefined("StringIO") ) {
            ((RubyClass) runtime.getObject().getConstant("StringIO")).defineAnnotatedMethods(IOJavaAddons.AnyIO.class);
        }

        // add all name-to-class mappings
        addNameClassMappings(runtime, runtime.getJavaSupport().getNameClassMap());

        // add some base Java classes everyone will need
        runtime.getJavaSupport().setObjectJavaClass( JavaClass.get(runtime, Object.class) );

        return Java;
    }

    public static class OldStyleExtensionInherited {
        @Deprecated
        public static IRubyObject inherited(IRubyObject self, IRubyObject subclass) {
            return inherited(self.getRuntime().getCurrentContext(), self, subclass);
        }

        @JRubyMethod
        public static IRubyObject inherited(ThreadContext context, IRubyObject self, IRubyObject subclass) {
            return invokeProxyClassInherited(context, self, subclass);
        }
    };

    public static class NewStyleExtensionInherited {
        @Deprecated
        public static IRubyObject inherited(IRubyObject self, IRubyObject subclass) {
            return inherited(self.getRuntime().getCurrentContext(), self, subclass);
        }

        @JRubyMethod
        public static IRubyObject inherited(ThreadContext context, IRubyObject self, IRubyObject subclass) {
            if ( ! ( subclass instanceof RubyClass ) ) {
                throw context.runtime.newTypeError(subclass, context.runtime.getClassClass());
            }
            JavaInterfaceTemplate.addRealImplClassNew((RubyClass) subclass);
            return context.nil;
        }
    };

    /**
     * This populates the master map from short-cut names to JavaClass instances for
     * a number of core Java types.
     *
     * @param runtime
     * @param nameClassMap
     */
    private static void addNameClassMappings(final Ruby runtime, final Map<String, JavaClass> nameClassMap) {
        JavaClass booleanClass = JavaClass.get(runtime, Boolean.class);
        nameClassMap.put("boolean", JavaClass.get(runtime, Boolean.TYPE));
        nameClassMap.put("Boolean", booleanClass);
        nameClassMap.put("java.lang.Boolean", booleanClass);

        JavaClass byteClass = JavaClass.get(runtime, Byte.class);
        nameClassMap.put("byte", JavaClass.get(runtime, Byte.TYPE));
        nameClassMap.put("Byte", byteClass);
        nameClassMap.put("java.lang.Byte", byteClass);

        JavaClass shortClass = JavaClass.get(runtime, Short.class);
        nameClassMap.put("short", JavaClass.get(runtime, Short.TYPE));
        nameClassMap.put("Short", shortClass);
        nameClassMap.put("java.lang.Short", shortClass);

        JavaClass charClass = JavaClass.get(runtime, Character.class);
        nameClassMap.put("char", JavaClass.get(runtime, Character.TYPE));
        nameClassMap.put("Character", charClass);
        nameClassMap.put("Char", charClass);
        nameClassMap.put("java.lang.Character", charClass);

        JavaClass intClass = JavaClass.get(runtime, Integer.class);
        nameClassMap.put("int", JavaClass.get(runtime, Integer.TYPE));
        nameClassMap.put("Integer", intClass);
        nameClassMap.put("Int", intClass);
        nameClassMap.put("java.lang.Integer", intClass);

        JavaClass longClass = JavaClass.get(runtime, Long.class);
        nameClassMap.put("long", JavaClass.get(runtime, Long.TYPE));
        nameClassMap.put("Long", longClass);
        nameClassMap.put("java.lang.Long", longClass);

        JavaClass floatClass = JavaClass.get(runtime, Float.class);
        nameClassMap.put("float", JavaClass.get(runtime, Float.TYPE));
        nameClassMap.put("Float", floatClass);
        nameClassMap.put("java.lang.Float", floatClass);

        JavaClass doubleClass = JavaClass.get(runtime, Double.class);
        nameClassMap.put("double", JavaClass.get(runtime, Double.TYPE));
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

        nameClassMap.put("void", JavaClass.get(runtime, Void.TYPE));
        nameClassMap.put("Void", JavaClass.get(runtime, Void.class));
    }

    public static IRubyObject create_proxy_class(
            IRubyObject self,
            IRubyObject name,
            IRubyObject javaClass,
            IRubyObject module) {
        final Ruby runtime = self.getRuntime();

        if ( ! ( module instanceof RubyModule ) ) {
            throw runtime.newTypeError(module, runtime.getModule());
        }

        final RubyModule proxyClass = get_proxy_class(self, javaClass);
        final String constName = name.asJavaString();
        IRubyObject existing = ((RubyModule) module).getConstantNoConstMissing(constName);

        if ( existing != null && existing != RubyBasicObject.UNDEF && existing != proxyClass ) {
            runtime.getWarnings().warn("replacing " + existing + " with " + proxyClass + " in constant '" + constName + " on class/module " + module);
        }

        ((RubyModule) module).setConstantQuiet(name.asJavaString(), proxyClass);
        return proxyClass;
    }

    public static IRubyObject get_java_class(final IRubyObject self, final IRubyObject name) {
        try {
            return JavaClass.for_name(self, name);
        }
        catch (Exception e) {
            self.getRuntime().getJavaSupport().handleNativeException(e, null);
            return self.getRuntime().getNil();
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
            RubyClass proxyClass = (RubyClass) getProxyClass(runtime, rawJavaObject.getClass());

            if (OBJECT_PROXY_CACHE || forceCache || proxyClass.getCacheProxy()) {
                return runtime.getJavaSupport().getObjectProxyCache().getOrCreate(rawJavaObject, proxyClass);
            }
            return allocateProxy(rawJavaObject, proxyClass);
        }
        return runtime.getNil();
    }

    public static RubyModule getInterfaceModule(final Ruby runtime, final JavaClass javaClass) {
        return getInterfaceModule(runtime, javaClass.javaClass());
    }

    public static RubyModule getInterfaceModule(final Ruby runtime, final Class javaClass) {
        return Java.getProxyClass(runtime, javaClass);
    }

    public static RubyModule get_interface_module(final Ruby runtime, IRubyObject javaClassObject) {
        JavaClass javaClass;
        if ( javaClassObject instanceof RubyString ) {
            javaClass = JavaClass.forNameVerbose(runtime, javaClassObject.asJavaString());
        }
        else if ( javaClassObject instanceof JavaClass ) {
            javaClass = (JavaClass) javaClassObject;
        }
        else {
            throw runtime.newArgumentError("expected JavaClass, got " + javaClassObject);
        }
        return getInterfaceModule(runtime, javaClass);
    }

    public static RubyModule get_proxy_class(final IRubyObject self, final IRubyObject java_class) {
        final Ruby runtime = self.getRuntime();
        final JavaClass javaClass;
        if ( java_class instanceof RubyString ) {
            javaClass = JavaClass.for_name(self, java_class);
        }
        else if ( java_class instanceof JavaClass ) {
            javaClass = (JavaClass) java_class;
        }
        else {
            throw runtime.newTypeError(java_class, runtime.getJavaSupport().getJavaClassClass());
        }
        return getProxyClass(runtime, javaClass);
    }

    public static RubyClass getProxyClassForObject(Ruby runtime, Object object) {
        return (RubyClass) getProxyClass(runtime, object.getClass());
    }

    public static RubyModule getProxyClass(Ruby runtime, JavaClass javaClass) {
        return getProxyClass(runtime, javaClass.javaClass());
    }

    @SuppressWarnings("deprecation")
    public static RubyModule getProxyClass(final Ruby runtime, final Class<?> clazz) {
        RubyModule proxy = runtime.getJavaSupport().getUnfinishedProxy(clazz);
        if (proxy != null) return proxy;
        return runtime.getJavaSupport().getProxyClassFromCache(clazz);
    }

    @SuppressWarnings("deprecation")
    // Only used by proxy ClassValue calculator in JavaSupport
    static RubyModule createProxyClassForClass(final Ruby runtime, final Class<?> clazz) {
        final JavaSupport javaSupport = runtime.getJavaSupport();

        RubyModule proxy;
        RubyClass superClass = null;
        if (clazz.isInterface()) {
            proxy = (RubyModule) runtime.getJavaSupport().getJavaInterfaceTemplate().dup();
        } else {
            if (clazz.isArray()) {
                superClass = javaSupport.getArrayProxyClass();
            } else if (clazz.isPrimitive()) {
                superClass = javaSupport.getConcreteProxyClass();
                // NOTE: but the class methods such as 'new' will be removed (Initializer.setupProxyClass)
            } else if (clazz == Object.class) {
                superClass = javaSupport.getConcreteProxyClass();
            } else {
                // other java proxy classes added under their superclass' java proxy
                superClass = (RubyClass) getProxyClass(runtime, clazz.getSuperclass());
            }
            proxy = RubyClass.newClass(runtime, superClass);
        }

        // ensure proxy is visible down-thread
        javaSupport.beginProxy(clazz, proxy);
        try {
            if (clazz.isInterface()) {
                generateInterfaceProxy(runtime, clazz, proxy);
            } else {
                generateClassProxy(runtime, clazz, (RubyClass) proxy, superClass);
            }
        } finally {
            javaSupport.endProxy(clazz);
        }

        return proxy;
    }

    private static void generateInterfaceProxy(final Ruby runtime, final Class javaClass, final RubyModule proxy) {
        assert javaClass.isInterface();

        // include any interfaces we extend
        final Class<?>[] extended = javaClass.getInterfaces();
        for (int i = extended.length; --i >= 0; ) {
            RubyModule extModule = getInterfaceModule(runtime, extended[i]);
            proxy.includeModule(extModule);
        }
        Initializer.setupProxyModule(runtime, javaClass, proxy);
        addToJavaPackageModule(proxy);
    }

    private static void generateClassProxy(Ruby runtime, Class<?> clazz, RubyClass proxy, RubyClass superClass) {
        if ( clazz.isArray() ) {
            createProxyClass(runtime, proxy, clazz, superClass, true);

            if ( clazz.getComponentType() == byte.class ) {
                proxy.defineAnnotatedMethods( ByteArrayProxyMethods.class ); // to_s
            }
        }
        else if ( clazz.isPrimitive() ) {
            createProxyClass(runtime, proxy, clazz, superClass, true);
        }
        else if ( clazz == Object.class ) {
            // java.lang.Object is added at root of java proxy classes
            createProxyClass(runtime, proxy, clazz, superClass, true);
            if (NEW_STYLE_EXTENSION) {
                proxy.getMetaClass().defineAnnotatedMethods(NewStyleExtensionInherited.class);
            } else {
                proxy.getMetaClass().defineAnnotatedMethods(OldStyleExtensionInherited.class);
            }
            addToJavaPackageModule(proxy);
        }
        else {
            createProxyClass(runtime, proxy, clazz, superClass, false);
            // include interface modules into the proxy class
            final Class<?>[] interfaces = clazz.getInterfaces();
            for ( int i = interfaces.length; --i >= 0; ) {
                proxy.includeModule(getInterfaceModule(runtime, interfaces[i]));
            }
            if ( Modifier.isPublic(clazz.getModifiers()) ) {
                addToJavaPackageModule(proxy);
            }
        }

        // JRUBY-1000, fail early when attempting to subclass a final Java class;
        // solved here by adding an exception-throwing "inherited"
        if ( Modifier.isFinal(clazz.getModifiers()) ) {
            final String clazzName = clazz.getCanonicalName();
            proxy.getMetaClass().addMethod("inherited", new org.jruby.internal.runtime.methods.JavaMethod(proxy, PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    throw context.runtime.newTypeError("can not extend final Java class: " + clazzName);
                }
            });
        }
    }

    private static RubyClass createProxyClass(final Ruby runtime,
        final RubyClass proxyClass, final Class<?> javaClass,
        final RubyClass superClass, boolean invokeInherited) {

        proxyClass.makeMetaClass( superClass.getMetaClass() );

        if ( Map.class.isAssignableFrom( javaClass ) ) {
            proxyClass.setAllocator( runtime.getJavaSupport().getMapJavaProxyClass().getAllocator() );
            proxyClass.defineAnnotatedMethods( MapJavaProxy.class );
            proxyClass.includeModule( runtime.getEnumerable() );
        }
        else {
            proxyClass.setAllocator( superClass.getAllocator() );
        }
        proxyClass.defineAnnotatedMethods( JavaProxy.ClassMethods.class );

        if ( invokeInherited ) proxyClass.inherit(superClass);

        Initializer.setupProxyClass(runtime, javaClass, proxyClass);

        return proxyClass;
    }

    public static class ByteArrayProxyMethods {

        @JRubyMethod
        public static IRubyObject to_s(ThreadContext context, IRubyObject self) {
            final Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();

            // All bytes can be considered raw strings and forced to particular codings if not 8bitascii
            ByteList bytes = new ByteList((byte[]) ((ArrayJavaProxy) self).getObject(), ascii8bit);
            return RubyString.newStringLight(context.runtime, bytes);
        }

    }

    @Deprecated
    public static IRubyObject concrete_proxy_inherited(final IRubyObject clazz, final IRubyObject subclazz) {
        return invokeProxyClassInherited(clazz.getRuntime().getCurrentContext(), clazz, subclazz);
    }

    private static IRubyObject invokeProxyClassInherited(final ThreadContext context,
        final IRubyObject clazz, final IRubyObject subclazz) {
        final JavaSupport javaSupport = context.runtime.getJavaSupport();
        RubyClass javaProxyClass = javaSupport.getJavaProxyClass().getMetaClass();
        Helpers.invokeAs(context, javaProxyClass, clazz, "inherited", subclazz, Block.NULL_BLOCK);
        if ( ! ( subclazz instanceof RubyClass ) ) {
            throw context.runtime.newTypeError(subclazz, context.runtime.getClassClass());
        }
        setupJavaSubclass(context, (RubyClass) subclazz);
        return context.nil;
    }

    // called for Ruby sub-classes of a Java class
    private static void setupJavaSubclass(final ThreadContext context, final RubyClass subclass) {

        subclass.setInstanceVariable("@java_proxy_class", context.nil);

        // Subclasses of Java classes can safely use ivars, so we set this to silence warnings
        subclass.setCacheProxy(true);

        final RubyClass subclassSingleton = subclass.getSingletonClass();
        subclassSingleton.addReadWriteAttribute(context, "java_proxy_class");
        subclassSingleton.addMethod("java_interfaces", new JavaMethodZero(subclassSingleton, PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                IRubyObject javaInterfaces = self.getInstanceVariables().getInstanceVariable("@java_interfaces");
                if (javaInterfaces != null) return javaInterfaces.dup();
                return context.nil;
            }
        });

        subclass.addMethod("__jcreate!", new JCreateMethod(subclassSingleton));
    }

    public static class JCreateMethod extends JavaMethodN implements CallableSelector.CallableCache<JavaProxyConstructor> {

        private final NonBlockingHashMapLong<JavaProxyConstructor> cache = new NonBlockingHashMapLong<>(8);

        JCreateMethod(RubyModule cls) {
            super(cls, PUBLIC);
        }

        private static JavaProxyClass getProxyClass(final IRubyObject self) {
            final RubyClass metaClass = self.getMetaClass();
            IRubyObject proxyClass = metaClass.getInstanceVariable("@java_proxy_class");
            if (proxyClass == null || proxyClass.isNil()) { // lazy (proxy) class generation ... on JavaSubClass.new
                proxyClass = JavaProxyClass.getProxyClass(self.getRuntime(), metaClass);
                metaClass.setInstanceVariable("@java_proxy_class", proxyClass);
            }
            return (JavaProxyClass) proxyClass;
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return call(context, self, clazz, name, IRubyObject.NULL_ARRAY);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            final JavaProxyConstructor[] constructors = getProxyClass(self).getConstructors();

            final JavaProxyConstructor matching;
            switch (constructors.length) {
                case 1: matching = matchConstructor0ArityOne(context, constructors, arg0); break;
                default: matching = matchConstructorArityOne(context, constructors, arg0);
            }

            JavaObject newObject = matching.newInstance(self, arg0);
            return JavaUtilities.set_java_object(self, self, newObject);
        }

        @Override
        public final IRubyObject call(final ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            final int arity = args.length;
            final JavaProxyConstructor[] constructors = getProxyClass(self).getConstructors();

            final JavaProxyConstructor matching;
            switch (constructors.length) {
                case 1: matching = matchConstructor0(context, constructors, arity, args); break;
                default: matching = matchConstructor(context, constructors, arity, args);
            }

            JavaObject newObject = matching.newInstance(self, args);
            return JavaUtilities.set_java_object(self, self, newObject);
        }

        // assumes only 1 constructor exists!
        private JavaProxyConstructor matchConstructor0ArityOne(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final IRubyObject arg0) {
            JavaProxyConstructor forArity = checkCallableForArity(1, constructors, 0);

            if ( forArity == null ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityOne(
                context.runtime, this, new JavaProxyConstructor[] { forArity }, arg0
            );

            if ( matching == null ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }
            return matching;
        }

        // assumes only 1 constructor exists!
        private JavaProxyConstructor matchConstructor0(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final int arity, final IRubyObject[] args) {
            JavaProxyConstructor forArity = checkCallableForArity(arity, constructors, 0);

            if ( forArity == null ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityN(
                context.runtime, this, new JavaProxyConstructor[] { forArity }, args
            );

            if ( matching == null ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }
            return matching;
        }

        private JavaProxyConstructor matchConstructorArityOne(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final IRubyObject arg0) {
            ArrayList<JavaProxyConstructor> forArity = findCallablesForArity(1, constructors);

            if ( forArity.size() == 0 ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityOne(
                    context.runtime, this, forArity.toArray(new JavaProxyConstructor[forArity.size()]), arg0
            );

            if ( matching == null ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }
            return matching;
        }

        // generic (slowest) path
        private JavaProxyConstructor matchConstructor(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final int arity, final IRubyObject... args) {
            ArrayList<JavaProxyConstructor> forArity = findCallablesForArity(arity, constructors);

            if ( forArity.size() == 0 ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityN(
                context.runtime, this, forArity.toArray(new JavaProxyConstructor[forArity.size()]), args
            );

            if ( matching == null ) {
                throw context.runtime.newArgumentError("wrong number of arguments for constructor");
            }
            return matching;
        }

        public final JavaProxyConstructor getSignature(int signatureCode) {
            return cache.get(signatureCode);
        }

        public final void putSignature(int signatureCode, JavaProxyConstructor callable) {
            cache.put(signatureCode, callable);
        }
    }

    static <T extends ParameterTypes> ArrayList<T> findCallablesForArity(final int arity, final T[] callables) {
        final ArrayList<T> forArity = new ArrayList<>(callables.length);
        for ( int i = 0; i < callables.length; i++ ) {
            final T found = checkCallableForArity(arity, callables, i);
            if ( found != null ) forArity.add(found);
        }
        return forArity;
    }

    private static <T extends ParameterTypes> T checkCallableForArity(final int arity, final T[] callables, final int index) {
        final T callable = callables[index];
        final int callableArity = callable.getArity();

        if ( callableArity == arity ) return callable;
        // for arity 2 :
        // - callable arity 1 ([]...) is OK
        // - callable arity 2 (arg1, []...) is OK
        // - callable arity 3 (arg1, arg2, []...) is OK
        if ( callable.isVarArgs() && callableArity - 1 <= arity ) {
            return callable;
        }
        return null;
    }

    // package scheme 2: separate module for each full package name, constructed
    // from the camel-cased package segments: Java::JavaLang::Object,
    private static void addToJavaPackageModule(RubyModule proxyClass) {
        final Ruby runtime = proxyClass.getRuntime();
        final Class<?> clazz = (Class<?>)proxyClass.dataGetStruct();
        final String fullName;
        if ( ( fullName = clazz.getName() ) == null ) return;

        final RubyModule parentModule; final String className;

        if ( fullName.indexOf('$') != -1 ) { // inner classes must be nested
            Class<?> declaringClass = clazz.getDeclaringClass();
            if ( declaringClass == null ) {
                // no containing class for a $ class; treat it as internal and don't define a constant
                return;
            }
            parentModule = getProxyClass(runtime, JavaClass.get(runtime, clazz));
            className = clazz.getSimpleName();
        }
        else {
            final int endPackage = fullName.lastIndexOf('.');
            String packageString = endPackage < 0 ? "" : fullName.substring(0, endPackage);
            parentModule = getJavaPackageModule(runtime, packageString);
            className = parentModule == null ? fullName : fullName.substring(endPackage + 1);
        }

        if ( parentModule != null && // TODO a Java Ruby class should not validate (as well)
            ( IdUtil.isConstant(className) || parentModule instanceof JavaPackage ) ) {
            if (parentModule.getConstantAt(className) == null) {
                parentModule.setConstant(className, proxyClass);
            }
        }
    }

    public static RubyModule getJavaPackageModule(final Ruby runtime, final Package pkg) {
        return getJavaPackageModule(runtime, pkg == null ? "" : pkg.getName());
    }

    public static RubyModule getJavaPackageModule(final Ruby runtime, final String packageString) {
        final String packageName; final int length;
        if ( ( length = packageString.length() ) == 0 ) {
            packageName = "Default";
        }
        else {
            StringBuilder name = new StringBuilder(length);
            for (int start = 0, offset; start < length; start = offset + 1) {
                offset = packageString.indexOf('.', start);
                if ( offset == -1 ) offset = length;
                name.append( Character.toUpperCase(packageString.charAt(start)) )
                    .append( packageString.substring(start + 1, offset) );
            }
            packageName = name.toString();
        }

        final RubyModule javaModule = runtime.getJavaSupport().getJavaModule();
        final IRubyObject packageModule = javaModule.getConstantAt(packageName);

        if ( packageModule == null ) {
            return createPackageModule(runtime, javaModule, packageName, packageString);
        }
        if ( packageModule instanceof RubyModule ) {
            return (RubyModule) packageModule;
        }
        return null;
    }

    private static RubyModule createPackageModule(final Ruby runtime,
        final RubyModule parentModule, final String name, final String packageString) {

        final RubyModule packageModule = JavaPackage.newPackage(runtime, packageString, parentModule);

        synchronized (parentModule) { // guard initializing in multiple threads
            final IRubyObject packageAlreadySet = parentModule.fetchConstant(name);
            if ( packageAlreadySet != null ) {
                return (RubyModule) packageAlreadySet;
            }
            parentModule.setConstant(name.intern(), packageModule);
            //MetaClass metaClass = (MetaClass) packageModule.getMetaClass();
            //metaClass.setAttached(packageModule);
        }
        return packageModule;
    }

    private static final Pattern CAMEL_CASE_PACKAGE_SPLITTER = Pattern.compile("([a-z0-9_]+)([A-Z])");

    private static RubyModule getPackageModule(final Ruby runtime, final String name) {
        final RubyModule javaModule = runtime.getJavaSupport().getJavaModule();
        final IRubyObject packageModule = javaModule.getConstantAt(name);
        if ( packageModule instanceof RubyModule ) return (RubyModule) packageModule;

        final String packageName;
        if ( "Default".equals(name) ) packageName = "";
        else {
            Matcher match = CAMEL_CASE_PACKAGE_SPLITTER.matcher(name);
            packageName = match.replaceAll("$1.$2").toLowerCase();
        }
        return createPackageModule(runtime, javaModule, name, packageName);
    }

    public static RubyModule get_package_module(final IRubyObject self, final IRubyObject name) {
        return getPackageModule(self.getRuntime(), name.asJavaString());
    }

    public static IRubyObject get_package_module_dot_format(final IRubyObject self,
        final IRubyObject dottedName) {
        final Ruby runtime = self.getRuntime();
        RubyModule module = getJavaPackageModule(runtime, dottedName.asJavaString());
        return module == null ? runtime.getNil() : module;
    }

    static RubyModule getProxyOrPackageUnderPackage(final ThreadContext context,
        final RubyModule parentPackage, final String name, final boolean cacheMethod) {
        final Ruby runtime = context.runtime;

        if ( name.length() == 0 ) {
            throw runtime.newArgumentError("empty class or package name");
        }

        final String fullName = JavaPackage.buildPackageName(parentPackage, name).toString();

        final RubyModule result;

        if ( ! Character.isUpperCase( name.charAt(0) ) ) {
            checkJavaReservedNames(runtime, name, false); // fails on primitives

            // this covers the rare case of lower-case class names (and thus will
            // fail 99.999% of the time). fortunately, we'll only do this once per
            // package name. (and seriously, folks, look into best practices...)
            RubyModule proxyClass = getProxyClassOrNull(runtime, fullName);
            if ( proxyClass != null ) result = proxyClass; /* else not primitive or l-c class */
            else {
                // Haven't found a class, continue on as though it were a package
                final RubyModule packageModule = getJavaPackageModule(runtime, fullName);
                // TODO: decompose getJavaPackageModule so we don't parse fullName
                if ( packageModule == null ) return null;
                result = packageModule;
            }
        }
        else {
            try { // First char is upper case, so assume it's a class name
                final RubyModule javaClass = getProxyClassOrNull(runtime, fullName);
                if ( javaClass != null ) result = javaClass;
                else {
                    if ( allowUppercasePackageNames(runtime) ) {
                        // for those not hip to conventions and best practices,
                        // we'll try as a package
                        result = getJavaPackageModule(runtime, fullName);
                        // NOTE result = getPackageModule(runtime, name);
                        if ( result == null ) {
                            throw runtime.newNameError("missing class (or package) name (`" + fullName + "')", fullName);
                        }
                    }
                    else {
                        throw runtime.newNameError("missing class name (`" + fullName + "')", fullName);
                    }
                }
            }
            catch (RuntimeException e) {
                if ( e instanceof RaiseException ) throw e;
                throw runtime.newNameError("missing class or uppercase package name (`" + fullName + "'), caused by " + e.getMessage(), fullName);
            }
        }

        // saves class in singletonized parent, so we don't come back here :
        if ( cacheMethod ) bindJavaPackageOrClassMethod(parentPackage, name, result);

        return result;
    }

    private static boolean allowUppercasePackageNames(final Ruby runtime) {
        return runtime.getInstanceConfig().getAllowUppercasePackageNames();
    }

    private static void checkJavaReservedNames(final Ruby runtime, final String name,
        final boolean allowPrimitives) {
        // TODO: should check against all Java reserved names here, not just primitives
        if ( ! allowPrimitives && JavaClass.isPrimitiveName(name) ) {
            throw runtime.newArgumentError("illegal package name component: " + name);
        }
    }

    private static RubyModule getProxyClassOrNull(final Ruby runtime, final String className) {
        return getProxyClassOrNull(runtime, className, true);
    }

    private static RubyModule getProxyClassOrNull(final Ruby runtime, final String className,
        final boolean initJavaClass) {
        final Class<?> clazz;
        try { // loadJavaClass here to handle things like LinkageError through
            synchronized (Java.class) {
                // a circular load might potentially dead-lock when loading concurrently
                // this path is reached from JavaPackage#relativeJavaClassOrPackage ...
                // another part preventing concurrent proxy initialization dead-locks is :
                // JavaSupportImpl's proxyClassCache = ClassValue.newInstance( ... )
                // ... having synchronized RubyModule computeValue(Class<?>)
                clazz = runtime.getJavaSupport().loadJavaClass(className);
            }
        }
        catch (ExceptionInInitializerError ex) {
            throw runtime.newNameError("cannot initialize Java class " + className + ' ' + '(' + ex + ')', className, ex, false);
        }
        catch (UnsupportedClassVersionError ex) { // LinkageError
            String type = ex.getClass().getName();
            String msg = ex.getLocalizedMessage();
            if ( msg != null ) {
                final String unMajorMinorVersion = "unsupported major.minor version";
                // e.g. "com/sample/FooBar : Unsupported major.minor version 52.0"
                int idx = msg.indexOf(unMajorMinorVersion);
                if (idx > 0) {
                    idx += unMajorMinorVersion.length();
                    idx = mapMajorMinorClassVersionToJavaVersion(msg, idx);
                    if ( idx > 0 ) msg = "needs Java " + idx + " (" + type + ": " + msg + ')';
                    else msg = '(' + type + ": " + msg + ')';
                }
            }
            else msg = '(' + type + ')';
            // cannot link Java class com.sample.FooBar needs Java 8 (java.lang.UnsupportedClassVersionError: com/sample/FooBar : Unsupported major.minor version 52.0)
            throw runtime.newNameError("cannot link Java class " + className + ' ' + msg, className, ex, false);
        }
        catch (LinkageError ex) {
            throw runtime.newNameError("cannot link Java class " + className + ' ' + '(' + ex + ')', className, ex, false);
        }
        catch (SecurityException ex) {
            throw runtime.newSecurityError(ex.getLocalizedMessage());
        }
        catch (ClassNotFoundException ex) { return null; }

        if ( initJavaClass ) {
            return getProxyClass(runtime, JavaClass.get(runtime, clazz));
        }
        return getProxyClass(runtime, clazz);
    }

    private static int mapMajorMinorClassVersionToJavaVersion(String msg, final int offset) {
        int end;
        if ( ( end = msg.indexOf('.', offset) ) == -1 ) end = msg.length();
        msg = msg.substring(offset, end).trim(); // handle " 52.0"
        try { // Java SE 6.0 = 50, Java SE 7 = 51, Java SE 8 = 52
            return Integer.parseInt(msg) - 50 + 6;
        }
        catch (RuntimeException ignore) { return 0; }
    }

    public static IRubyObject get_proxy_or_package_under_package(final ThreadContext context,
        final IRubyObject self, final IRubyObject parentPackage, final IRubyObject name) {
        final Ruby runtime = context.runtime;
        if ( ! ( parentPackage instanceof RubyModule ) ) {
            throw runtime.newTypeError(parentPackage, runtime.getModule());
        }
        final RubyModule result = getProxyOrPackageUnderPackage(context, (RubyModule) parentPackage, name.asJavaString(), true);
        return result != null ? result : context.nil;
    }

    private static RubyModule getTopLevelProxyOrPackage(final Ruby runtime,
        final String name, final boolean cacheMethod) {

        if ( name.length() == 0 ) {
            throw runtime.newArgumentError("empty class or package name");
        }

        final RubyModule result;

        if ( Character.isLowerCase( name.charAt(0) ) ) {
            // this covers primitives and (unlikely) lower-case class names
            RubyModule proxyClass = getProxyClassOrNull(runtime, name);
            if ( proxyClass != null ) result = proxyClass; /* else not primitive or l-c class */
            else {
                checkJavaReservedNames(runtime, name, true);

                final RubyModule packageModule = getJavaPackageModule(runtime, name);
                // TODO: decompose getJavaPackageModule so we don't parse fullName
                if ( packageModule == null ) return null;

                result = packageModule;
            }
        }
        else {
            RubyModule javaClass = getProxyClassOrNull(runtime, name);
            if ( javaClass != null ) result = javaClass;
            else {
                // upper-case package name
                // TODO: top-level upper-case package was supported in the previous (Ruby-based)
                // implementation, so leaving as is.  see note at #getProxyOrPackageUnderPackage
                // re: future approach below the top-level.
                result = getPackageModule(runtime, name);
            }
        }

        if ( cacheMethod ) bindJavaPackageOrClassMethod(runtime, name, result);

        return result;
    }

    private static boolean bindJavaPackageOrClassMethod(final Ruby runtime, final String name,
        final RubyModule packageOrClass) {
        final RubyModule javaPackage = runtime.getJavaSupport().getJavaModule();
        return bindJavaPackageOrClassMethod(javaPackage, name, packageOrClass);
    }

    private static boolean bindJavaPackageOrClassMethod(final RubyModule parentPackage,
        final String name, final RubyModule packageOrClass) {

        if ( parentPackage.getMetaClass().isMethodBound(name, false) ) {
            return false;
        }

        final RubyClass singleton = parentPackage.getSingletonClass();
        singleton.addMethod(name.intern(), new JavaAccessor(singleton, packageOrClass, parentPackage));
        return true;
    }

    private static class JavaAccessor extends org.jruby.internal.runtime.methods.JavaMethod {

        private final RubyModule packageOrClass;
        private final RubyModule parentPackage;

        JavaAccessor(final RubyClass singleton, final RubyModule packageOrClass, final RubyModule parentPackage) {
            super(singleton, PUBLIC);
            this.parentPackage = parentPackage; this.packageOrClass = packageOrClass;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if ( args.length != 0 ) {
                throw JavaPackage.packageMethodArgumentMismatch(context.runtime, parentPackage, name, args.length);
            }
            return call(context, self, clazz, name);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return this.packageOrClass;
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return this.packageOrClass;
        }

        @Override
        public Arity getArity() { return Arity.noArguments(); }

    }

    static final class ProcToInterface extends org.jruby.internal.runtime.methods.DynamicMethod {

        ProcToInterface(final RubyClass singletonClass) {
            super(singletonClass, PUBLIC);
        }

        @Override // method_missing impl :
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            final IRubyObject[] newArgs;
            switch( args.length ) {
                case 1 :  newArgs = IRubyObject.NULL_ARRAY; break;
                case 2 :  newArgs = new IRubyObject[] { args[1] }; break;
                case 3 :  newArgs = new IRubyObject[] { args[1], args[2] }; break;
                default : newArgs = new IRubyObject[ args.length - 1 ];
                    System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            }
            return callProc(context, self, newArgs);
        }

        private static IRubyObject callProc(ThreadContext context, IRubyObject self, IRubyObject[] procArgs) {
            if ( ! ( self instanceof RubyProc ) ) {
                throw context.runtime.newTypeError("interface impl method_missing for block used with non-Proc object");
            }
            return ((RubyProc) self).call(context, procArgs);
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }

        final ConcreteMethod getConcreteMethod() { return new ConcreteMethod(); }

        final class ConcreteMethod extends org.jruby.internal.runtime.methods.JavaMethod {

            ConcreteMethod() {
                super(ProcToInterface.this.implementationClass, Visibility.PUBLIC);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
                return ProcToInterface.this.callProc(context, self, IRubyObject.NULL_ARRAY);
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, Block block) {
                return ProcToInterface.this.callProc(context, self, new IRubyObject[]{arg0});
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
                return ProcToInterface.this.callProc(context, self, new IRubyObject[]{arg0, arg1});
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
                return ProcToInterface.this.callProc(context, self, new IRubyObject[]{arg0, arg1, arg2});
            }

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
                return ProcToInterface.this.callProc(context, self, args);
            }

        }

    }

    private static RubyModule getProxyUnderClass(final ThreadContext context,
        final RubyModule enclosingClass, final String name) {
        final Ruby runtime = context.runtime;

        if ( name.length() == 0 ) throw runtime.newArgumentError("empty class name");

        Class<?> enclosing = JavaClass.getJavaClass(context, enclosingClass);

        final String fullName = enclosing.getName() + '$' + name;

        final RubyModule result = getProxyClassOrNull(runtime, fullName);
        //if ( result != null && cacheMethod ) bindJavaPackageOrClassMethod(enclosingClass, name, result);
        return result;
    }

    public static IRubyObject get_inner_class(final ThreadContext context,
        final RubyModule self, final IRubyObject name) { // const_missing delegate
        final String constName = name.asJavaString();

        final RubyModule innerClass = getProxyUnderClass(context, self, constName);
        if ( innerClass == null ) {
            return Helpers.invokeSuper(context, self, name, Block.NULL_BLOCK);
        }
        return cacheConstant(self, constName, innerClass, true); // hidden == true (private_constant)
    }

    @JRubyMethod(meta = true)
    public static IRubyObject const_missing(final ThreadContext context,
        final IRubyObject self, final IRubyObject name) {
        final Ruby runtime = context.runtime;
        final String constName = name.asJavaString();
        // it's fine to not add the "cached" method here - when users sticking to
        // constant access won't pay the "penalty" for adding dynamic methods ...
        final RubyModule packageOrClass = getTopLevelProxyOrPackage(runtime, constName, false);
        if ( packageOrClass == null ) return context.nil; // compatibility (with packages)
        return cacheConstant((RubyModule) self, constName, packageOrClass, false);
    }

    private static RubyModule cacheConstant(final RubyModule owner, // e.g. ::Java
        final String constName, final RubyModule packageOrClass, final boolean hidden) {
        if ( packageOrClass != null ) {
            // NOTE: if it's a package createPackageModule already set the constant
            // ... but in case it's a (top-level) Java class name we still need to:
            synchronized (owner) {
                final IRubyObject alreadySet = owner.fetchConstant(constName);
                if ( alreadySet != null ) return (RubyModule) alreadySet;
                owner.setConstant(constName, packageOrClass, hidden);
            }
            return packageOrClass;
        }
        return null;
    }

    @JRubyMethod(name = "method_missing", meta = true, required = 1)
    public static IRubyObject method_missing(ThreadContext context, final IRubyObject self,
        final IRubyObject name) { // JavaUtilities.get_top_level_proxy_or_package(name)
        // NOTE: getTopLevelProxyOrPackage will bind the (cached) method for us :
        final RubyModule result = getTopLevelProxyOrPackage(context.runtime, name.asJavaString(), true);
        if ( result != null ) return result;
        return context.nil;
    }

    @JRubyMethod(name = "method_missing", meta = true, rest = true)
    public static IRubyObject method_missing(ThreadContext context, final IRubyObject self,
        final IRubyObject[] args) {
        final IRubyObject name = args[0];
        if ( args.length > 1 ) {
            final int count = args.length - 1;
            throw context.runtime.newArgumentError("Java does not have a method `"+ name +"' with " + count + " arguments");
        }
        return method_missing(context, self, name);
    }

    public static IRubyObject get_top_level_proxy_or_package(final ThreadContext context,
        final IRubyObject self, final IRubyObject name) {
        final RubyModule result = getTopLevelProxyOrPackage(context.runtime, name.asJavaString(), true);
        return result != null ? result : context.nil;
    }

    public static IRubyObject wrap(Ruby runtime, IRubyObject java_object) {
        return getInstance(runtime, ((JavaObject) java_object).getValue());
    }

    /**
     * High-level object conversion utility function 'java_to_primitive' is the low-level version
     */
    @Deprecated
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject java_to_ruby(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        try {
            return JavaUtil.java_to_ruby(recv.getRuntime(), object);
        } catch (RuntimeException e) {
            recv.getRuntime().getJavaSupport().handleNativeException(e, null);
            // This point is only reached if there was an exception handler installed.
            return recv.getRuntime().getNil();
        }
    }

    /**
     * High-level object conversion utility.
     */
    @Deprecated
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject ruby_to_java(final IRubyObject recv, IRubyObject object, Block unusedBlock) {
        return JavaUtil.ruby_to_java(recv, object, unusedBlock);
    }

    @Deprecated
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        return JavaUtil.java_to_primitive(recv, object, unusedBlock);
    }

    // TODO: Formalize conversion mechanisms between Java and Ruby
    @JRubyMethod(required = 2, module = true, visibility = PRIVATE)
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

    public static JavaObject newInterfaceImpl(final IRubyObject wrapper, Class[] interfaces) {
        final Ruby runtime = wrapper.getRuntime();

        final int length = interfaces.length;
        switch ( length ) {
            case 1 :
                interfaces = new Class[] { interfaces[0], RubyObjectHolderProxy.class };
            case 2 :
                interfaces = new Class[] { interfaces[0], interfaces[1], RubyObjectHolderProxy.class };
            default :
                interfaces = ArraySupport.newCopy(interfaces, length + 1);
                interfaces[length] = RubyObjectHolderProxy.class;
        }

        final RubyClass wrapperClass = wrapper.getMetaClass();
        final boolean isProc = wrapperClass.isSingleton() && wrapperClass.getRealClass() == runtime.getProc();

        final JRubyClassLoader jrubyClassLoader = runtime.getJRubyClassLoader();

        if ( RubyInstanceConfig.INTERFACES_USE_PROXY ) {
            return JavaObject.wrap(runtime, newProxyInterfaceImpl(wrapper, interfaces, jrubyClassLoader));
        }

        final ClassDefiningClassLoader classLoader;
        // hashcode is a combination of the interfaces and the Ruby class we're using to implement them
        int interfacesHashCode = interfacesHashCode(interfaces);
        // if it's a singleton class and the real class is proc, we're doing closure conversion
        // so just use Proc's hashcode
        if ( isProc ) {
            interfacesHashCode = 31 * interfacesHashCode + runtime.getProc().hashCode();
            classLoader = jrubyClassLoader;
        }
        else { // normal new class implementing interfaces
            interfacesHashCode = 31 * interfacesHashCode + wrapperClass.getRealClass().hashCode();
            classLoader = new OneShotClassLoader(jrubyClassLoader);
        }
        final String implClassName = "org.jruby.gen.InterfaceImpl" + Math.abs(interfacesHashCode);
        Class<?> proxyImplClass;
        try {
            proxyImplClass = Class.forName(implClassName, true, jrubyClassLoader);
        }
        catch (ClassNotFoundException ex) {
            proxyImplClass = RealClassGenerator.createOldStyleImplClass(interfaces, wrapperClass, runtime, implClassName, classLoader);
        }

        try {
            Constructor<?> proxyConstructor = proxyImplClass.getConstructor(IRubyObject.class);
            return JavaObject.wrap(runtime, proxyConstructor.newInstance(wrapper));
        }
        catch (InvocationTargetException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
        catch (ReflectiveOperationException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
    }

    // NOTE: only used when java.lang.reflect.Proxy is to be used for interface impls (by default its not)
    private static Object newProxyInterfaceImpl(final IRubyObject wrapper, final Class[] interfaces, final ClassLoader loader) {
        return Proxy.newProxyInstance(loader, interfaces, new InterfaceProxyHandler(wrapper, interfaces));
    }

    private static final class InterfaceProxyHandler implements InvocationHandler {

        final IRubyObject wrapper;

        private final String[] ifaceNames; // interface names (sorted)

        InterfaceProxyHandler(final IRubyObject wrapper, final Class[] interfaces) {
            this.wrapper = wrapper;
            this.ifaceNames = new String[interfaces.length];
            for ( int i = 0; i < interfaces.length; i++ ) {
                ifaceNames[i] = interfaces[i].getName();
            }
            Arrays.sort(ifaceNames);
        }

        public Object invoke(Object proxy, Method method, Object[] nargs) throws Throwable {
            final String methodName = method.getName();
            final int length = nargs == null ? 0 : nargs.length;

            switch ( methodName ) {
                case "toString" :
                    if ( length == 0 && ! wrapper.respondsTo("toString") ) {
                        return proxyToString(proxy);
                    }
                    break;
                case "hashCode" :
                    if ( length == 0 && ! wrapper.respondsTo("hashCode") ) {
                        return proxyHashCode(proxy);
                    }
                    break;
                case "equals" :
                    if ( length == 1 && ! wrapper.respondsTo("equals") ) {
                        Class[] parameterTypes = getParameterTypes(method);
                        if ( parameterTypes[0] == Object.class ) return proxyEquals(proxy, nargs[0]);
                    }
                    break;
                case "__ruby_object" :
                    if ( length == 0 ) return wrapper;
                    break;
            }

            final Ruby runtime = wrapper.getRuntime();
            final ThreadContext context = runtime.getCurrentContext();

            //try {
                switch ( length ) {
                    case 0 :
                        return Helpers.invoke(context, wrapper, methodName).toJava(method.getReturnType());
                    case 1 :
                        IRubyObject arg = JavaUtil.convertJavaToUsableRubyObject(runtime, nargs[0]);
                        return Helpers.invoke(context, wrapper, methodName, arg).toJava(method.getReturnType());
                    default :
                        IRubyObject[] args = JavaUtil.convertJavaArrayToRuby(runtime, nargs);
                        return Helpers.invoke(context, wrapper, methodName, args).toJava(method.getReturnType());
                }
            //}
            //catch (RuntimeException e) {
            //    e.printStackTrace(); throw e;
            //}
        }

        final String proxyToString(final Object proxy) {
            // com.sun.proxy.$Proxy24{org.jruby.javasupport.Java$InterfaceProxyHandler@71ad51e9}
            return proxy.getClass().getName() + '{' + this + '}';
        }

        final boolean proxyEquals(final Object proxy, final Object otherProxy) {
            if ( proxy == otherProxy ) return true;
            if ( otherProxy == null ) return false;
            if ( Proxy.isProxyClass(otherProxy.getClass()) ) {
                InvocationHandler other = Proxy.getInvocationHandler(otherProxy);
                if ( other instanceof InterfaceProxyHandler ) {
                    InterfaceProxyHandler that = (InterfaceProxyHandler) other;
                    if ( this.wrapper != that.wrapper ) return false;
                    return Arrays.equals(this.ifaceNames, that.ifaceNames);
                }
            }
            return false;
        }

        final int proxyHashCode(final Object proxy) {
            int hash = 11 * this.wrapper.hashCode();
            for ( String iface : this.ifaceNames ) {
                hash = 31 * hash + iface.hashCode();
            }
            return hash;
        }

        private Map<Method, Class[]> parameterTypeCache;

        private Class[] getParameterTypes(final Method method) {
            Map<Method, Class[]> parameterTypeCache = this.parameterTypeCache;
            if (parameterTypeCache == null) {
                parameterTypeCache = new ConcurrentHashMap<Method, Class[]>(4);
                this.parameterTypeCache = parameterTypeCache;
            }

            Class[] parameterTypes = parameterTypeCache.get(method);
            if (parameterTypes == null) {
                parameterTypes = method.getParameterTypes();
                parameterTypeCache.put(method, parameterTypes);
            }
            return parameterTypes;
        }

    }

    @SuppressWarnings("unchecked")
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
            implClassName = "anon_class" + Math.abs(System.identityHashCode(clazz)) + '_' + Math.abs(interfacesHashCode);
        } else {
            implClassName = clazz.getName().replaceAll("::", "\\$\\$") + '_' + Math.abs(interfacesHashCode);
        }
        Class<? extends IRubyObject> proxyImplClass;
        try {
            proxyImplClass = (Class<? extends IRubyObject>) Class.forName(implClassName, true, runtime.getJRubyClassLoader());
        }
        catch (ClassNotFoundException ex) {
            // try to use super's reified class; otherwise, RubyObject (for now)
            Class<? extends IRubyObject> superClass = clazz.getSuperClass().getRealClass().getReifiedClass();
            if ( superClass == null ) superClass = RubyObject.class;
            proxyImplClass = RealClassGenerator.createRealImplClass(superClass, interfaces, clazz, runtime, implClassName);

            // add a default initialize if one does not already exist and this is a Java-hierarchy class
            if ( NEW_STYLE_EXTENSION &&
                ! ( RubyBasicObject.class.isAssignableFrom(proxyImplClass) || clazz.getMethods().containsKey("initialize") ) ) {
                clazz.addMethod("initialize", new DummyInitialize(clazz));
            }
        }
        clazz.setReifiedClass(proxyImplClass);
        clazz.setRubyClassAllocator(proxyImplClass);

        return proxyImplClass;
    }

    private static final class DummyInitialize extends JavaMethodZero {

        DummyInitialize(final RubyClass clazz) { super(clazz, PRIVATE); }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return context.nil;
        }

    }

    public static Constructor getRealClassConstructor(final Ruby runtime, Class<?> proxyImplClass) {
        try {
            return proxyImplClass.getConstructor(Ruby.class, RubyClass.class);
        }
        catch (NoSuchMethodException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
    }

    public static IRubyObject constructProxy(Ruby runtime, Constructor proxyConstructor, RubyClass clazz) {
        try {
            return (IRubyObject) proxyConstructor.newInstance(runtime, clazz);
        }
        catch (InvocationTargetException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
        catch (ReflectiveOperationException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
    }

    private static RaiseException mapGeneratedProxyException(final Ruby runtime, final ReflectiveOperationException e) {
        RaiseException ex = runtime.newTypeError("Exception instantiating generated interface impl:\n" + e);
        ex.initCause(e);
        return ex;
    }

    private static RaiseException mapGeneratedProxyException(final Ruby runtime, final InvocationTargetException e) {
        RaiseException ex = runtime.newTypeError("Exception instantiating generated interface impl:\n" + e.getTargetException());
        ex.initCause(e);
        return ex;
    }

    public static IRubyObject allocateProxy(Object javaObject, RubyClass clazz) {
        final Ruby runtime = clazz.getRuntime();
        // Arrays are never stored in OPC
        if ( clazz.getSuperClass() == runtime.getJavaSupport().getArrayProxyClass() ) {
            return new ArrayJavaProxy(runtime, clazz, javaObject, JavaUtil.getJavaConverter(javaObject.getClass().getComponentType()));
        }

        final IRubyObject proxy = clazz.allocate();
        if ( proxy instanceof JavaProxy ) {
            ((JavaProxy) proxy).setObject(javaObject);
        }
        else {
            JavaObject wrappedObject = JavaObject.wrap(runtime, javaObject);
            proxy.dataWrapStruct(wrappedObject);
        }
        return proxy;
    }

    public static IRubyObject wrapJavaObject(Ruby runtime, Object object) {
        return allocateProxy(object, getProxyClassForObject(runtime, object));
    }

    @SuppressWarnings("unchecked")
    public static Class[] getInterfacesFromRubyClass(RubyClass klass) {
        Set<Class> interfaces = new HashSet<Class>();
        // walk all superclasses aggregating interfaces
        while (klass != null) {
            IRubyObject maybeInterfaces = klass.getInstanceVariables().getInstanceVariable("@java_interfaces");
            if (maybeInterfaces instanceof RubyArray) {
                final RubyArray moreInterfaces = (RubyArray) maybeInterfaces;
                if ( ! moreInterfaces.isFrozen() ) moreInterfaces.setFrozen(true);
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

    /**
     * @param iface
     * @return the sole un-implemented method for a functional-style interface or null
     * @note This method is internal and might be subject to change, do not assume its part of JRuby's API!
     */
    public static Method getFunctionalInterfaceMethod(final Class<?> iface) {
        assert iface.isInterface();
        Method single = null;
        for ( final Method method : iface.getMethods() ) {
            final int mod = method.getModifiers();
            if ( Modifier.isStatic(mod) ) continue;
            if ( Modifier.isAbstract(mod) ) {
                try { // check if it's equals, hashCode etc. :
                    Object.class.getMethod(method.getName(), method.getParameterTypes());
                    continue; // abstract but implemented by java.lang.Object
                }
                catch (NoSuchMethodException e) { /* fall-thorough */ }
                catch (SecurityException e) {
                    // NOTE: we could try check for FunctionalInterface on Java 8
                }
            }
            else continue; // not-abstract ... default method
            if ( single == null ) single = method;
            else return null; // not a functional iface
        }
        return single;
    }

    static final boolean JAVA8;
    static {
        boolean java8 = false;
        final String version = SafePropertyAccessor.getProperty("java.version", "0.0");
        if ( version.length() > 2 ) {
            int v = Character.getNumericValue( version.charAt(0) );
            if ( v > 8 ) java8 = true; // 9.0
            else if ( v == 1 ) {
                v = Character.getNumericValue( version.charAt(2) ); // 1.8
                if ( v < 10 && v >= 8 ) java8 = true;
            }
            // seems as no Java 10 support ... yet :)
        }
        JAVA8 = java8;
    }

    // TODO if about to compile against Java 8 this does not need to be reflective
    static boolean isDefaultMethod(final Method method) {
        if ( JAVA8 ) {
            try {
                return (Boolean) Method.class.getMethod("isDefault").invoke(method);
            }
            catch (NoSuchMethodException ex) { throw new RuntimeException(ex); }
            catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
            catch (Exception ex) { /* noop */ }
        }
        return false;
    }

    /**
     * @see JavaUtil#CAN_SET_ACCESSIBLE
     */
    @SuppressWarnings("unused") private static final byte HIDDEN_STATIC_FIELD = 72;
    public static final String HIDDEN_STATIC_FIELD_NAME = "HIDDEN_STATIC_FIELD";

}
