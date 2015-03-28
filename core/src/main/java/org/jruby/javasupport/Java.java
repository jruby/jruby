/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
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

import org.jruby.java.util.BlankSlateWrapper;
import org.jruby.java.util.SystemPropertiesMap;
import org.jruby.java.proxies.JavaInterfaceTemplate;
import org.jruby.java.addons.KernelJavaAddons;

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

import org.jcodings.Encoding;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyClassPathVariable;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubyUnboundMethod;
import org.jruby.javasupport.binding.Initializer;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
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
import org.jruby.javasupport.proxy.JavaProxyClassFactory;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.ByteList;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.ClassProvider;
import org.jruby.util.CodegenUtils;
import org.jruby.util.IdUtil;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.IntHashMap;
import static org.jruby.java.dispatch.CallableSelector.newCallableCache;

@JRubyModule(name = "Java")
public class Java implements Library {
    public static final boolean NEW_STYLE_EXTENSION = Options.JI_NEWSTYLEEXTENSION.load();
    public static final boolean OBJECT_PROXY_CACHE = Options.JI_OBJECTPROXYCACHE.load();

    @Override
    public void load(Ruby runtime, boolean wrap) {
        createJavaModule(runtime);

        RubyModule jpmt = runtime.defineModule("JavaPackageModuleTemplate");
        jpmt.getSingletonClass().setSuperClass(new BlankSlateWrapper(runtime, jpmt.getMetaClass().getSuperClass(), runtime.getKernel()));

        // load Ruby parts of the 'java' library
        runtime.getLoadService().load("jruby/java.rb", false);
        
        // rewite ArrayJavaProxy superclass to point at Object, so it inherits Object behaviors
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
        @JRubyMethod
        public static IRubyObject inherited(IRubyObject self, IRubyObject arg0) {
            return Java.concrete_proxy_inherited(self, arg0);
        }
    };

    public static class NewStyleExtensionInherited {
        @JRubyMethod
        public static IRubyObject inherited(IRubyObject self, IRubyObject arg0) {
            final Ruby runtime = self.getRuntime();
            if ( ! ( arg0 instanceof RubyClass ) ) {
                throw runtime.newTypeError(arg0, runtime.getClassClass());
            }
            JavaInterfaceTemplate.addRealImplClassNew((RubyClass) arg0);
            return runtime.getNil();
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

    private static class JavaPackageClassProvider implements ClassProvider {

        static final JavaPackageClassProvider INSTANCE = new JavaPackageClassProvider();

        public RubyClass defineClassUnder(RubyModule pkg, String name, RubyClass superClazz) {
            // shouldn't happen, but if a superclass is specified, it's not ours
            if ( superClazz != null ) return null;

            String packageName = getPackageName(pkg);
            // again, shouldn't happen. TODO: might want to throw exception instead.
            if ( packageName == null ) return null;

            final Ruby runtime = pkg.getRuntime();
            JavaClass javaClass = JavaClass.forNameVerbose(runtime, packageName + name);
            return (RubyClass) get_proxy_class(runtime.getJavaSupport().getJavaUtilitiesModule(), javaClass);
        }

        public RubyModule defineModuleUnder(RubyModule pkg, String name) {
            String packageName = getPackageName(pkg);
            // again, shouldn't happen. TODO: might want to throw exception instead.
            if ( packageName == null ) return null;

            final Ruby runtime = pkg.getRuntime();
            JavaClass javaClass = JavaClass.forNameVerbose(runtime, packageName + name);
            return get_interface_module(runtime, javaClass);
        }

        private static String getPackageName(final RubyModule pkg) {
            final IRubyObject package_name = pkg.getInstanceVariables().getInstanceVariable("@package_name");
            return package_name == null ? null : package_name.asJavaString();
        }

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
            } else {
                return allocateProxy(rawJavaObject, proxyClass);
            }
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

    public static RubyModule getProxyClass(final Ruby runtime, final Class<?> clazz) {
        RubyModule unfinished = runtime.getJavaSupport().getUnfinishedProxyClassCache().get(clazz).get();
        if (unfinished != null) return unfinished;
        return runtime.getJavaSupport().getProxyClassFromCache(clazz);
    }

    // Only used by proxy ClassValue calculator in JavaSupport
    static RubyModule createProxyClassForClass(final Ruby runtime, final Class<?> clazz) {
        final JavaSupport javaSupport = runtime.getJavaSupport();

        if (clazz.isInterface()) return generateInterfaceProxy(runtime, clazz);

        return generateClassProxy(runtime, clazz, javaSupport);
    }

    private static RubyModule generateInterfaceProxy(final Ruby runtime, final Class javaClass) {
        if (!javaClass.isInterface()) {
            throw runtime.newArgumentError(javaClass.toString() + " is not an interface");
        }

        RubyModule proxyModule = (RubyModule) runtime.getJavaSupport().getJavaInterfaceTemplate().dup();

        // include any interfaces we extend
        final Class<?>[] extended = javaClass.getInterfaces();
        for ( int i = extended.length; --i >= 0; ) {
            RubyModule extModule = getInterfaceModule(runtime, extended[i]);
            proxyModule.includeModule(extModule);
        }
        Initializer.setupProxyModule(runtime, javaClass, proxyModule);
        addToJavaPackageModule(proxyModule);

        return proxyModule;
    }

    private static RubyModule generateClassProxy(Ruby runtime, Class<?> clazz, JavaSupport javaSupport) {
        RubyModule proxyClass;
        if (clazz.isArray()) {
            proxyClass = createProxyClass(runtime, javaSupport.getArrayProxyClass(), clazz, true);

            // FIXME: Organizationally this might be nicer in a specialized class
            if ( clazz.getComponentType() == byte.class ) {
                final Encoding ascii8bit = runtime.getEncodingService().getAscii8bitEncoding();

                // All bytes can be considered raw strings and forced to particular codings if not 8bitascii
                proxyClass.addMethod("to_s", new JavaMethodZero(proxyClass, PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                        ByteList bytes = new ByteList((byte[]) ((ArrayJavaProxy) self).getObject(), ascii8bit);
                        return RubyString.newStringLight(context.runtime, bytes);
                    }
                });
            }
        }
        else if ( clazz.isPrimitive() ) {
            proxyClass = createProxyClass(runtime, javaSupport.getConcreteProxyClass(), clazz, true);
        }
        else if ( clazz == Object.class ) {
            // java.lang.Object is added at root of java proxy classes
            proxyClass = createProxyClass(runtime, javaSupport.getConcreteProxyClass(), clazz, true);
            if (NEW_STYLE_EXTENSION) {
                proxyClass.getMetaClass().defineAnnotatedMethods(NewStyleExtensionInherited.class);
            } else {
                proxyClass.getMetaClass().defineAnnotatedMethods(OldStyleExtensionInherited.class);
            }
            addToJavaPackageModule(proxyClass);
        }
        else {
            // other java proxy classes added under their superclass' java proxy
            RubyClass superProxyClass = (RubyClass) getProxyClass(runtime, clazz.getSuperclass());
            proxyClass = createProxyClass(runtime, superProxyClass, clazz, false);
            // include interface modules into the proxy class
            final Class<?>[] interfaces = clazz.getInterfaces();
            for ( int i = interfaces.length; --i >= 0; ) {
                proxyClass.includeModule(getInterfaceModule(runtime, interfaces[i]));
            }
            if ( Modifier.isPublic(clazz.getModifiers()) ) {
                addToJavaPackageModule(proxyClass);
            }
        }

        // JRUBY-1000, fail early when attempting to subclass a final Java class;
        // solved here by adding an exception-throwing "inherited"
        if ( Modifier.isFinal(clazz.getModifiers()) ) {
            final String clazzName = clazz.getCanonicalName();
            proxyClass.getMetaClass().addMethod("inherited", new org.jruby.internal.runtime.methods.JavaMethod(proxyClass, PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    throw context.runtime.newTypeError("can not extend final Java class: " + clazzName);
                }
            });
        }

        return proxyClass;
    }

    private static RubyClass createProxyClass(final Ruby runtime,
                                              final RubyClass baseType, final Class<?> javaClass, boolean invokeInherited) {
        RubyClass proxyClass;

        // this needs to be split, since conditional calling #inherited doesn't fit standard ruby semantics

        final RubyClass superClass = baseType;
        proxyClass = RubyClass.newClass(runtime, superClass);
        proxyClass.makeMetaClass( superClass.getMetaClass() );

        if ( Map.class.isAssignableFrom( javaClass ) ) {
            proxyClass.setAllocator( runtime.getJavaSupport().getMapJavaProxyClass().getAllocator() );
            proxyClass.defineAnnotatedMethods( MapJavaProxy.class );
            proxyClass.includeModule( runtime.getEnumerable() );
        }
        else {
            proxyClass.setAllocator( superClass.getAllocator() );
        }
        proxyClass.defineAnnotatedMethods( JavaProxyClassMethods.class );

        if ( invokeInherited ) proxyClass.inherit(superClass);

        Initializer.setupProxyClass(runtime, javaClass, proxyClass);

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
            final Ruby runtime = context.runtime;

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(runtime, recv, name));
            return method.invokeStaticDirect(context);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName, IRubyObject argTypes) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            final Ruby runtime = context.runtime;

            if (argTypesAry.size() != 0) {
                Class[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argTypesAry.size()]);
                throw JavaMethod.newArgSizeMismatchError(runtime, argTypesClasses);
            }

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(runtime, recv, name));
            return method.invokeStaticDirect(context);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject rubyName, IRubyObject argTypes, IRubyObject arg0) {
            String name = rubyName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            final Ruby runtime = context.runtime;

            if (argTypesAry.size() != 1) {
                throw JavaMethod.newArgSizeMismatchError(runtime, (Class) argTypesAry.eltInternal(0).toJava(Class.class));
            }

            Class argTypeClass = (Class) argTypesAry.eltInternal(0).toJava(Class.class);

            JavaMethod method = new JavaMethod(runtime, getMethodFromClass(runtime, recv, name, argTypeClass));
            return method.invokeStaticDirect(context, arg0.toJava(argTypeClass));
        }

        @JRubyMethod(required = 1, rest = true, meta = true)
        public static IRubyObject java_send(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            final Ruby runtime = context.runtime;

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
            return method.invokeStaticDirect(context, argsAry);
        }

        @JRubyMethod(meta = true, visibility = PRIVATE)
        public static IRubyObject java_alias(ThreadContext context, IRubyObject clazz, IRubyObject newName, IRubyObject rubyName) {
            return java_alias(context, clazz, newName, rubyName, context.runtime.newEmptyArray());
        }

        @JRubyMethod(meta = true, visibility = PRIVATE)
        public static IRubyObject java_alias(ThreadContext context, IRubyObject clazz, IRubyObject newName, IRubyObject rubyName, IRubyObject argTypes) {
            final Ruby runtime = context.runtime;
            if ( ! ( clazz instanceof RubyClass ) ) {
                throw runtime.newTypeError(clazz, runtime.getModule());
            }
            final RubyClass proxyClass = (RubyClass) clazz;

            String name = rubyName.asJavaString();
            String newNameStr = newName.asJavaString();
            RubyArray argTypesAry = argTypes.convertToArray();
            Class<?>[] argTypesClasses = (Class[]) argTypesAry.toArray(new Class[argTypesAry.size()]);

            final Method method = getMethodFromClass(context, clazz, name, argTypesClasses);
            final MethodInvoker invoker;

            if ( Modifier.isStatic( method.getModifiers() ) ) {
                invoker = new StaticMethodInvoker(proxyClass.getMetaClass(), method);
                // add alias to meta
                proxyClass.getSingletonClass().addMethod(newNameStr, invoker);
            }
            else {
                invoker = new InstanceMethodInvoker(proxyClass, method);
                proxyClass.addMethod(newNameStr, invoker);
            }

            return context.nil;
        }
    }

    private static IRubyObject getRubyMethod(ThreadContext context, IRubyObject clazz, String name, Class... argTypesClasses) {
        final Ruby runtime = context.runtime;
        if ( ! ( clazz instanceof RubyClass ) ) {
            throw runtime.newTypeError(clazz, runtime.getModule());
        }
        final RubyClass proxyClass = (RubyClass) clazz;

        final Method method = getMethodFromClass(context, clazz, name, argTypesClasses);
        final String prettyName = name + CodegenUtils.prettyParams(argTypesClasses);

        if ( Modifier.isStatic( method.getModifiers() ) ) {
            MethodInvoker invoker = new StaticMethodInvoker(proxyClass, method);
            return RubyMethod.newMethod(proxyClass, prettyName, proxyClass, name, invoker, clazz);
        }

        MethodInvoker invoker = new InstanceMethodInvoker(proxyClass, method);
        return RubyUnboundMethod.newUnboundMethod(proxyClass, prettyName, proxyClass, name, invoker);
    }

    private static Method getMethodFromClass(final ThreadContext context, final IRubyObject proxyClass,
        final String name, final Class... argTypes) {
        final Class<?> clazz = JavaClass.getJavaClass(context, (RubyModule) proxyClass);
        try {
            return clazz.getMethod(name, argTypes);
        }
        catch (NoSuchMethodException nsme) {
            String prettyName = name + CodegenUtils.prettyParams(argTypes);
            String errorName = clazz.getName() + '.' + prettyName;
            throw context.runtime.newNameError("Java method not found: " + errorName, name);
        }
    }

    public static Method getMethodFromClass(final Ruby runtime, final IRubyObject proxyClass,
        final String name, final Class... argTypes) {
        return getMethodFromClass(runtime.getCurrentContext(), proxyClass, name, argTypes);
    }

    public static IRubyObject concrete_proxy_inherited(final IRubyObject clazz, final IRubyObject subclazz) {
        final Ruby runtime = clazz.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        JavaSupport javaSupport = runtime.getJavaSupport();
        RubyClass javaProxyClass = javaSupport.getJavaProxyClass().getMetaClass();
        Helpers.invokeAs(context, javaProxyClass, clazz, "inherited", subclazz, Block.NULL_BLOCK);
        if ( ! ( subclazz instanceof RubyClass ) ) {
            throw runtime.newTypeError(subclazz, runtime.getClassClass());
        }
        setupJavaSubclass(context, (RubyClass) subclazz);
        return context.nil;
    }

    private static void setupJavaSubclass(final ThreadContext context, final RubyClass subclass) {

        subclass.getInstanceVariables().setInstanceVariable("@java_proxy_class", context.nil);

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

        subclass.addMethod("__jcreate!", new JavaMethodN(subclassSingleton, PUBLIC) {

            private final IntHashMap<JavaProxyConstructor> cache = newCallableCache();

            @Override
            public IRubyObject call(final ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                IRubyObject proxyClass = self.getMetaClass().getInstanceVariables().getInstanceVariable("@java_proxy_class");
                if (proxyClass == null || proxyClass.isNil()) {
                    proxyClass = JavaProxyClass.get_with_class(self, self.getMetaClass());
                    self.getMetaClass().getInstanceVariables().setInstanceVariable("@java_proxy_class", proxyClass);
                }

                final int argsLength = args.length;
                final RubyArray constructors = ((JavaProxyClass) proxyClass).constructors();
                ArrayList<JavaProxyConstructor> forArity = new ArrayList<JavaProxyConstructor>(constructors.size());
                for ( int i = 0; i < constructors.size(); i++ ) {
                    JavaProxyConstructor constructor = (JavaProxyConstructor) constructors.eltInternal(i);
                    if ( constructor.getArity() == argsLength ) forArity.add(constructor);
                }

                if ( forArity.size() == 0 ) {
                    throw context.runtime.newArgumentError("wrong number of arguments for constructor");
                }

                final JavaProxyConstructor matching = CallableSelector.matchingCallableArityN(
                        context.runtime, cache,
                        forArity.toArray(new JavaProxyConstructor[forArity.size()]), args
                );

                if ( matching == null ) {
                    throw context.runtime.newArgumentError("wrong number of arguments for constructor");
                }

                final Object[] javaArgs = new Object[argsLength];
                Class[] parameterTypes = matching.getParameterTypes();
                for ( int i = 0; i < argsLength; i++ ) {
                    javaArgs[i] = args[i].toJava(parameterTypes[i]);
                }

                JavaObject newObject = matching.newInstance(self, javaArgs);
                return JavaUtilities.set_java_object(self, self, newObject);
            }
        });
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

        if ( parentModule != null && IdUtil.isConstant(className) ) {
            if (parentModule.getConstantAt(className) == null) {
                parentModule.setConstant(className, proxyClass);
            }
        }
    }

    public static RubyModule getJavaPackageModule(final Ruby runtime, final Package pkg) {
        return getJavaPackageModule(runtime, pkg == null ? "" : pkg.getName());
    }

    private static RubyModule getJavaPackageModule(final Ruby runtime, final String packageString) {
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

        final RubyModule packageModule = (RubyModule) runtime.getJavaSupport().getPackageModuleTemplate().dup();

        final String package_name = packageString.length() > 0 ? packageString + '.' : packageString;
        packageModule.setInstanceVariable( "@package_name", runtime.newString(package_name) );

        // this is where we'll get connected when classes are opened using
        // package module syntax.
        packageModule.addClassProvider( JavaPackageClassProvider.INSTANCE );

        synchronized (parentModule) { // guard initializing in multiple threads
            final IRubyObject packageAlreadySet = parentModule.fetchConstant(name);
            if ( packageAlreadySet != null ) {
                return (RubyModule) packageAlreadySet;
            }
            parentModule.setConstant(name.intern(), packageModule);
            MetaClass metaClass = (MetaClass) packageModule.getMetaClass();
            metaClass.setAttached(packageModule);
        }
        return packageModule;
    }

    private static final Pattern CAMEL_CASE_PACKAGE_SPLITTER = Pattern.compile("([a-z][0-9]*)([A-Z])");

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

    private static RubyModule getProxyOrPackageUnderPackage(final ThreadContext context,
        final RubyModule parentPackage, final String name, final boolean cacheMethod) {
        final Ruby runtime = context.runtime;

        if ( name.length() == 0 ) {
            throw runtime.newArgumentError("empty class or package name");
        }

        IRubyObject package_name = parentPackage.getInstanceVariable("@package_name");
        if ( package_name == null ) throw runtime.newArgumentError("invalid package module");

        final String parentPackageName = package_name.asJavaString();
        final String fullName = parentPackageName + name;

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
            clazz = runtime.getJavaSupport().loadJavaClass(className);
        }
        catch (ExceptionInInitializerError ex) {
            throw runtime.newNameError("cannot initialize Java class " + className, className, ex, false);
        }
        catch (LinkageError ex) {
            throw runtime.newNameError("cannot link Java class " + className, className, ex, false);
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
                IRubyObject packageName = parentPackage.callMethod("package_name");
                throw context.runtime.newArgumentError(
                    "Java package `" + packageName + "' does not have a method `" + name + "'"
                );
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

    final static class ProcToInterface extends org.jruby.internal.runtime.methods.DynamicMethod {

        ProcToInterface(final RubyClass singletonClass) {
            super(singletonClass, PUBLIC, org.jruby.internal.runtime.methods.CallConfiguration.FrameNoneScopeNone);
        }

        @Override // method_missing impl :
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if ( ! ( self instanceof RubyProc ) ) {
                throw context.runtime.newTypeError("interface impl method_missing for block used with non-Proc object");
            }
            final RubyProc proc = (RubyProc) self;
            final IRubyObject[] newArgs;
            if ( args.length == 1 ) newArgs = IRubyObject.NULL_ARRAY;
            else {
                newArgs = new IRubyObject[ args.length - 1 ];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            }
            return proc.call(context, newArgs);
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }

    }

    @JRubyMethod(meta = true)
    public static IRubyObject const_missing(final ThreadContext context,
        final IRubyObject self, final IRubyObject name) {
        final Ruby runtime = context.runtime;
        final String constName = name.asJavaString();
        // it's fine to not add the "cached" method here - when users sticking to
        // constant access won't pay the "penalty" for adding dynamic methods ...
        final RubyModule packageOrClass = getTopLevelProxyOrPackage(runtime, constName, false);
        if ( packageOrClass != null ) {
            final RubyModule Java = (RubyModule) self;
            // NOTE: if it's a package createPackageModule already set the constant
            // ... but in case it's a (top-level) Java class name we still need to:
            synchronized (Java) {
                final IRubyObject alreadySet = Java.fetchConstant(constName);
                if ( alreadySet != null ) return (RubyModule) alreadySet;
                Java.setConstant(constName, packageOrClass);
            }
            return packageOrClass;
        }
        return context.nil; // TODO compatibility - should be throwing instead, right !?
    }

    @JRubyMethod(name = "method_missing", meta = true, required = 1)
    public static IRubyObject method_missing(ThreadContext context, final IRubyObject self,
        final IRubyObject name) { // JavaUtilities.get_top_level_proxy_or_package(name)
        // NOTE: getTopLevelProxyOrPackage will bind the (cached) method for us :
        final RubyModule result = getTopLevelProxyOrPackage(context.runtime, name.asJavaString(), true);
        if ( result != null ) return result;
        return context.nil; // TODO compatibility - should be throwing instead, right !?
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
            Class<?> proxyImplClass;
            try {
                proxyImplClass = Class.forName(implClassName, true, runtime.getJRubyClassLoader());
            } catch (ClassNotFoundException cnfe) {
                proxyImplClass = RealClassGenerator.createOldStyleImplClass(interfaces, wrapper.getMetaClass(), runtime, implClassName, classLoader);
            }

            try {
                Constructor<?> proxyConstructor = proxyImplClass.getConstructor(IRubyObject.class);
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

                private final Map<Method, Class[]> parameterTypeCache = new ConcurrentHashMap<Method, Class[]>();

                public Object invoke(Object proxy, Method method, Object[] nargs) throws Throwable {
                    String methodName = method.getName();
                    int length = nargs == null ? 0 : nargs.length;

                    // FIXME: wtf is this? Why would these use the class?
                    if (methodName.equals("toString") && length == 0) {
                        return proxy.getClass().getName();
                    } else if (methodName.equals("hashCode") && length == 0) {
                        return Integer.valueOf(proxy.getClass().hashCode());
                    } else if (methodName.equals("equals") && length == 1) {
                        Class[] parameterTypes = parameterTypeCache.get(method);
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
                        return Helpers.invoke(runtime.getCurrentContext(), wrapper, methodName, rubyArgs).toJava(method.getReturnType());
                    }
                    catch (RuntimeException e) { e.printStackTrace(); throw e; }
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
                clazz.addMethod("initialize", new JavaMethodZero(clazz, PRIVATE) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                        return context.nil;
                    }
                });
            }
        }
        clazz.setReifiedClass(proxyImplClass);
        clazz.setRubyClassAllocator(proxyImplClass);

        return proxyImplClass;
    }

    public static Constructor getRealClassConstructor(final Ruby runtime, Class<?> proxyImplClass) {
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
        // Arrays are never stored in OPC
        if (clazz.getSuperClass() == clazz.getRuntime().getJavaSupport().getArrayProxyClass()) {
            return new ArrayJavaProxy(clazz.getRuntime(), clazz, javaObject, JavaUtil.getJavaConverter(javaObject.getClass().getComponentType()));
        }

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

    @Deprecated
    private static void addToJavaPackageModule(RubyModule proxyClass, JavaClass javaClass) {
        addToJavaPackageModule(proxyClass);
    }

    @Deprecated
    private static RubyClass createProxyClass(final Ruby runtime,
                                              final RubyClass baseType, final JavaClass javaClass, boolean invokeInherited) {
        return createProxyClass(runtime, baseType, javaClass.javaClass(), invokeInherited);
    }
}
