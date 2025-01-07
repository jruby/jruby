/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.headius.backport9.modules.Modules;
import org.jcodings.Encoding;

import org.jruby.*;
import org.jruby.api.Access;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.TypeError;
import org.jruby.javasupport.binding.Initializer;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
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
import org.jruby.java.dispatch.CallableSelector.CallableCache;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.ArrayJavaProxyCreator;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.MapJavaProxy;
import org.jruby.java.proxies.InterfaceJavaProxy;
import org.jruby.java.proxies.JavaInterfaceTemplate;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.proxies.RubyObjectHolderProxy;
import org.jruby.java.util.SystemPropertiesMap;
import org.jruby.util.*;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.NonBlockingHashMapLong;

import static org.jruby.api.Access.*;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.*;
import static org.jruby.runtime.Visibility.*;

@JRubyModule(name = "Java")
public class Java implements Library {
    public static final boolean NEW_STYLE_EXTENSION = Options.JI_NEWSTYLEEXTENSION.load();
    public static final boolean OBJECT_PROXY_CACHE = Options.JI_OBJECTPROXYCACHE.load();

    @Override
    public void load(Ruby runtime, boolean wrap) {
        var context = runtime.getCurrentContext();
        var Module = moduleClass(context);
        var Kernel = kernelModule(context);
        var Enumerable = enumerableModule(context);
        var Comparable = comparableModule(context);
        var Object = objectClass(context);
        final RubyModule Java = createJavaModule(context);

        runtime.getJavaSupport().setJavaPackageClass(JavaPackage.createJavaPackageClass(context, Java, Module, Kernel));

        org.jruby.javasupport.ext.Kernel.define(context, Kernel);
        org.jruby.javasupport.ext.Module.define(context, Module);

        org.jruby.javasupport.ext.JavaLang.define(context, Enumerable, Comparable);
        org.jruby.javasupport.ext.JavaLangReflect.define(context);
        org.jruby.javasupport.ext.JavaUtil.define(context, Enumerable);
        org.jruby.javasupport.ext.JavaUtilRegex.define(context);
        org.jruby.javasupport.ext.JavaIo.define(context);
        org.jruby.javasupport.ext.JavaNio.define(context);
        org.jruby.javasupport.ext.JavaNet.define(context);
        org.jruby.javasupport.ext.JavaMath.define(context);
        org.jruby.javasupport.ext.JavaTime.define(context);

        // initialize java.lang.Object proxy early
        RubyClass objectClass = (RubyClass) getProxyClass(context, java.lang.Object.class);

        // load Ruby parts of the 'java' library
        loadService(context).load("jruby/java.rb", false);

        // rewire ArrayJavaProxy superclass to point at Object, so it inherits Object behaviors
        Access.getClass(context, "ArrayJavaProxy").
                superClass(objectClass).
                include(context, Enumerable);

        RubyClassPathVariable.createClassPathVariable(context, Enumerable, Object);

        // (legacy) JavaClass compatibility:
        Java.defineConstant(context, "JavaClass", getProxyClass(context, java.lang.Class.class)).
                defineConstant(context, "JavaField", getProxyClass(context, java.lang.reflect.Field.class)).
                defineConstant(context, "JavaMethod", getProxyClass(context, java.lang.reflect.Method.class)).
                defineConstant(context, "JavaConstructor", getProxyClass(context, java.lang.reflect.Constructor.class));
        Java.deprecateConstant(context, "JavaClass");
        Java.deprecateConstant(context, "JavaField");
        Java.deprecateConstant(context, "JavaMethod");
        Java.deprecateConstant(context, "JavaConstructor");

        // modify ENV_JAVA to be a read/write version
        final Map systemProperties = new SystemPropertiesMap();
        RubyClass proxyClass = (RubyClass) getProxyClass(context, SystemPropertiesMap.class);
        Object.setConstantQuiet(context, "ENV_JAVA", new MapJavaProxy(runtime, proxyClass, systemProperties));
    }

    @SuppressWarnings("deprecation")
    public static RubyModule createJavaModule(ThreadContext context) {
        var Object = objectClass(context);
        var Enumerable = enumerableModule(context);

        var Java = defineModule(context, "Java").
                defineMethods(context, Java.class);

        //final RubyClass _JavaObject = JavaObject.createJavaObjectClass(runtime, Java);
        //JavaArray.createJavaArrayClass(runtime, Java, _JavaObject);

        // set of utility methods for Java-based proxy objects
        var _JavaProxyMethods = JavaProxyMethods.createJavaProxyMethods(context);

        // the proxy (wrapper) type hierarchy
        RubyClass javaProxyClass = JavaProxy.createJavaProxy(context, Object, _JavaProxyMethods);
        ArrayJavaProxyCreator.createArrayJavaProxyCreator(context, Object);
        RubyClass _ConcreteJavaProxy = ConcreteJavaProxy.createConcreteJavaProxy(context, javaProxyClass);
        InterfaceJavaProxy.createInterfaceJavaProxy(context, Object, javaProxyClass);
        RubyClass _ArrayJavaProxy = ArrayJavaProxy.createArrayJavaProxy(context, javaProxyClass, Enumerable);

        // creates ruby's hash methods' proxy for Map interface
        MapJavaProxy.createMapJavaProxy(context, _ConcreteJavaProxy);

        // also create the JavaProxy* classes
        JavaProxyClass.createJavaProxyClasses(context, Java, Object);

        // The template for interface modules
        JavaInterfaceTemplate.createJavaInterfaceTemplateModule(context);

        defineModule(context, "JavaUtilities").defineMethods(context, JavaUtilities.class);

        JavaArrayUtilities.createJavaArrayUtilitiesModule(context);

        // Now attach Java-related extras to core classes
        arrayClass(context).defineMethods(context, ArrayJavaAddons.class);
        kernelModule(context).defineMethods(context, KernelJavaAddons.class);
        stringClass(context).defineMethods(context, StringJavaAddons.class);
        ioClass(context).defineMethods(context, IOJavaAddons.class);
        classClass(context).defineMethods(context, ClassJavaAddons.class);

        if (Object.isConstantDefined(context, "StringIO")) {
            ((RubyClass) Object.getConstant(context, "StringIO")).defineMethods(context, IOJavaAddons.AnyIO.class);
        }

        Java.defineConstant(context, "JavaObject", _ConcreteJavaProxy); // obj.is_a?(Java::JavaObject) still works
        Java.deprecateConstant(context, "JavaObject");
        Java.defineConstant(context, "JavaArray", _ArrayJavaProxy);
        Java.deprecateConstant(context, "JavaArray"); // obj.is_a?(Java::JavaArray) still works

        return Java;
    }

    public static class OldStyleExtensionInherited {
        @Deprecated
        public static IRubyObject inherited(IRubyObject self, IRubyObject subclass) {
            return inherited(((RubyBasicObject) self).getCurrentContext(), self, subclass);
        }

        @JRubyMethod
        public static IRubyObject inherited(ThreadContext context, IRubyObject self, IRubyObject subclass) {
            return invokeProxyClassInherited(context, self, subclass);
        }
    };

    public static class NewStyleExtensionInherited {
        @Deprecated
        public static IRubyObject inherited(IRubyObject self, IRubyObject subclass) {
            return inherited(((RubyBasicObject) self).getCurrentContext(), self, subclass);
        }

        @JRubyMethod
        public static IRubyObject inherited(ThreadContext context, IRubyObject self, IRubyObject subclass) {
            JavaInterfaceTemplate.addRealImplClassNew(castAsClass(context, subclass));
            return context.nil;
        }
    }

    @Deprecated(since = "9.4")
    public static IRubyObject create_proxy_class(IRubyObject self, IRubyObject name, IRubyObject javaClass, IRubyObject mod) {
        var context = ((RubyBasicObject) self).getCurrentContext();
        RubyModule module = castAsModule(context, mod);

        return setProxyClass(context, module, name.asJavaString(), resolveJavaClassArgument(context, javaClass));
    }

    @Deprecated(since = "10.0")
    public static RubyModule setProxyClass(final Ruby runtime, final RubyModule target, final String constName,
                                           final Class<?> javaClass) throws NameError {
        return setProxyClass(runtime.getCurrentContext(), target, constName, javaClass);
    }

        public static RubyModule setProxyClass(ThreadContext context, final RubyModule target, final String constName,
                                           final Class<?> javaClass) throws NameError {
        final RubyModule proxyClass = getProxyClass(context, javaClass);
        setProxyClass(context, target, constName, proxyClass, true);
        return proxyClass;
    }

    private static void setProxyClass(ThreadContext context, final RubyModule target, final String constName, final RubyModule proxyClass, final boolean validateConstant) {
        if (constantNotSetOrDifferent(context, target, constName, proxyClass)) {
            synchronized (target) { // synchronize to prevent "already initialized constant" warnings with multiple threads
                if (constantNotSetOrDifferent(context, target, constName, proxyClass)) {
                    if (validateConstant) {
                        target.defineConstant(context, constName, proxyClass); // setConstant would not validate const-name
                    } else {
                        target.setConstant(context, constName, proxyClass);
                    }
                }
            }
        }
    }

    private static boolean constantNotSetOrDifferent(ThreadContext context, final RubyModule target,
                                                     final String constName, final RubyModule proxyClass) {
        return !target.constDefinedAt(context, constName) ||
                !proxyClass.equals(target.getConstant(context, constName, false));
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
            RubyClass proxyClass = (RubyClass) getProxyClass(runtime.getCurrentContext(), rawJavaObject.getClass());

            if (OBJECT_PROXY_CACHE || forceCache || proxyClass.getCacheProxy()) {
                return runtime.getJavaSupport().getObjectProxyCache().getOrCreate(rawJavaObject, proxyClass);
            }
            return allocateProxy(rawJavaObject, proxyClass);
        }
        return runtime.getNil();
    }

    @Deprecated(since = "9.4-")
    public static RubyModule getInterfaceModule(final Ruby runtime, final JavaClass javaClass) {
        return getInterfaceModule(runtime.getCurrentContext(), javaClass.javaClass());
    }

    @Deprecated(since = "10.0")
    public static RubyModule getInterfaceModule(final Ruby runtime, final Class javaClass) {
        return getInterfaceModule(runtime.getCurrentContext(), javaClass);
    }

    public static RubyModule getInterfaceModule(ThreadContext context, final Class javaClass) {
        return Java.getProxyClass(context, javaClass);
    }

    @Deprecated(since = "10.0")
    public static RubyModule get_interface_module(final Ruby runtime, final IRubyObject java_class) {
        return get_interface_module(runtime.getCurrentContext(), java_class);
    }

    public static RubyModule get_interface_module(ThreadContext context, final IRubyObject java_class) {
        return getInterfaceModule(context, resolveJavaClassArgument(context, java_class));
    }

    @Deprecated(since = "10.0")
    public static RubyModule get_proxy_class(final IRubyObject self, final IRubyObject java_class) {
        return get_proxy_class(((RubyBasicObject) self).getCurrentContext(), self, java_class);
    }

    public static RubyModule get_proxy_class(ThreadContext context, IRubyObject self, final IRubyObject java_class) {
        return getProxyClass(context, resolveJavaClassArgument(context, java_class));
    }

    @SuppressWarnings("deprecation")
    private static Class<?> resolveJavaClassArgument(ThreadContext context, final IRubyObject java_class) {
        if (java_class instanceof RubyString) return getJavaClass(context, java_class.asJavaString());

        if (java_class instanceof JavaProxy proxy) {
            Object obj = proxy.getObject();
            if (obj instanceof Class cls) return cls;
            if (obj instanceof String str) return getJavaClass(context, str); // j.l.String proxy

            throw argumentError(context, "expected a Java class, got " + java_class.inspect(context));
        }

        throw argumentError(context, "expected a Java class (or String), got " + java_class.inspect(context));
    }

    public static Class<?> unwrapClassProxy(final IRubyObject self) {
        return (Class) ((JavaProxy) self).getObject();
    }

    @Deprecated(since = "10.0")
    public static RubyClass getProxyClassForObject(Ruby runtime, Object object) {
        return getProxyClassForObject(runtime.getCurrentContext(), object);
    }

    public static RubyClass getProxyClassForObject(ThreadContext context, Object object) {
        return (RubyClass) getProxyClass(context, object.getClass());
    }

    public static Class<?> resolveClassType(final ThreadContext context, final IRubyObject type) throws TypeError {
        RubyModule proxyClass = Java.resolveType(context, type);
        if (proxyClass == null) throw typeError(context, "unable to convert to type: " + type);
        return JavaUtil.getJavaClass(context, proxyClass);
    }

    @Deprecated(since = "10.0")
    public static RubyModule resolveType(final Ruby runtime, final IRubyObject type) {
        return resolveType(runtime.getCurrentContext(), type);
    }

    public static RubyModule resolveType(ThreadContext context, final IRubyObject type) {
        Class<?> klass;
        if (type instanceof RubyString || type instanceof RubySymbol) {
            final String className = type.toString();
            klass = resolveShortClassName(className);
            if (klass == null) klass = getJavaClass(context, className);
        } else {
            klass = resolveClassType(type);
            if (klass == null) throw typeError(context, "expected a Java class, got: " + type);
        }
        return getProxyClass(context, klass);
    }

    // this should handle the type returned from Class#java_class
    static Class<?> resolveClassType(final IRubyObject type) {
        if (type instanceof JavaProxy) { // due Class#java_class wrapping
            final Object wrapped = ((JavaProxy) type).getObject();
            if (wrapped instanceof Class) return (Class) wrapped;
            return null;
        }

        if (type instanceof RubyModule) { // assuming a proxy module/class e.g. to_java(java.lang.String)
            return JavaUtil.getJavaClass((RubyModule) type, null);
        }
        return null;
    }

    private static Class resolveShortClassName(final String name) {
        switch (name) {
            case "boolean" : return Boolean.TYPE;
            case "Boolean" : case "java.lang.Boolean" : return Boolean.class;

            case "byte" : return Byte.TYPE;
            case "Byte" : case "java.lang.Byte" : return Byte.class;

            case "short" : return Short.TYPE;
            case "Short" : case "java.lang.Short" : return Short.class;

            case "int" : return Integer.TYPE;
            case "Int" : case "Integer" : case "java.lang.Integer" : return Integer.class;

            case "long" : return Long.TYPE;
            case "Long" : case "java.lang.Long" : return Long.class;

            case "float" : return Float.TYPE;
            case "Float" : case "java.lang.Float" : return Float.class;

            case "double" : return Double.TYPE;
            case "Double" : case "java.lang.Double" : return Double.class;

            case "char" : return Character.TYPE;
            case "Char" : case "Character" : case "java.lang.Character" : return Character.class;

            case "object" : case "Object" : case "java.lang.Object" : return Object.class;

            case "string" : case "String" : case "java.lang.String" : return String.class;

            case "big_int" : case "big_integer" : case "BigInteger" : return BigInteger.class;

            case "big_decimal" : case "BigDecimal" : return BigDecimal.class;

            case "void" : return Void.TYPE;
            case "Void" : return Void.class;
        }
        return null;
    }

    @Deprecated
    public static RubyModule getProxyClass(Ruby runtime, JavaClass javaClass) {
        return getProxyClass(runtime.getCurrentContext(), javaClass.javaClass());
    }

    @Deprecated(since = "10.0")
    public static RubyModule getProxyClass(final Ruby runtime, final Class<?> clazz) {
        return getProxyClass(runtime.getCurrentContext(), clazz);
    }

    @SuppressWarnings("deprecation")
    public static RubyModule getProxyClass(ThreadContext context, final Class<?> clazz) {
        var javaSupport = context.runtime.getJavaSupport();
        RubyModule proxy = javaSupport.getUnfinishedProxy(clazz);

        return proxy != null ? proxy : javaSupport.getProxyClassFromCache(clazz);
    }

    // expected to handle Java proxy (Ruby) sub-classes as well
    public static boolean isProxyType(final RubyModule proxy) {
        return JavaUtil.getJavaClass(proxy, null) != null;
    }

    @SuppressWarnings("deprecation")
    // Only used by proxy ClassValue calculator in JavaSupport
    static RubyModule createProxyClassForClass(final Ruby runtime, final Class<?> clazz) {
        var context = runtime.getCurrentContext();
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
                superClass = (RubyClass) getProxyClass(context, clazz.getSuperclass());
            }
            proxy = RubyClass.newClass(context, superClass, null);
        }

        // ensure proxy is visible down-thread
        javaSupport.beginProxy(clazz, proxy);
        try {
            if (clazz.isInterface()) {
                generateInterfaceProxy(context, clazz, proxy);
            } else {
                generateClassProxy(context, clazz, (RubyClass) proxy, superClass);
            }
        } finally {
            javaSupport.endProxy(clazz);
        }

        return proxy;
    }

    private static void generateInterfaceProxy(ThreadContext context, final Class javaClass, final RubyModule proxy) {
        assert javaClass.isInterface();

        // include any interfaces we extend
        final Class<?>[] extended = javaClass.getInterfaces();
        for (int i = extended.length; --i >= 0; ) {
            proxy.include(context, getInterfaceModule(context, extended[i]));
        }
        Initializer.setupProxyModule(context, javaClass, proxy);
        addToJavaPackageModule(context, proxy);
    }

    private static void generateClassProxy(ThreadContext context, Class<?> clazz, RubyClass proxy, RubyClass superClass) {
        if ( clazz.isArray() ) {
            createProxyClass(context, proxy, clazz, superClass, true);

            if ( clazz.getComponentType() == byte.class ) {
                proxy.defineMethods(context, ByteArrayProxyMethods.class ); // to_s
            }
        }
        else if ( clazz.isPrimitive() ) {
            createProxyClass(context, proxy, clazz, superClass, true);
        }
        else if ( clazz == Object.class ) {
            // java.lang.Object is added at root of java proxy classes
            createProxyClass(context, proxy, clazz, superClass, true);
            if (NEW_STYLE_EXTENSION) {
                proxy.getMetaClass().defineMethods(context, NewStyleExtensionInherited.class);
            } else {
                proxy.getMetaClass().defineMethods(context, OldStyleExtensionInherited.class);
            }
            addToJavaPackageModule(context, proxy);
        }
        else {
            createProxyClass(context, proxy, clazz, superClass, false);
            // include interface modules into the proxy class
            final Class<?>[] interfaces = clazz.getInterfaces();
            for ( int i = interfaces.length; --i >= 0; ) {
                proxy.include(context, getInterfaceModule(context, interfaces[i]));
            }
            // we don't want to expose any synthetic classes into Ruby constants,
            // JRuby generated classes such as interface impls (org.jruby.gen.InterfaceImpl) are marked synthetic
            if (Modifier.isPublic(clazz.getModifiers()) && !clazz.isSynthetic()) {
                addToJavaPackageModule(context, proxy);
            }
        }

        // JRUBY-1000, fail early when attempting to subclass a final Java class;
        // solved here by adding an exception-throwing "inherited"
        if ( Modifier.isFinal(clazz.getModifiers()) ) {
            final String clazzName = clazz.getCanonicalName();
            proxy.getMetaClass().addMethod(context, "inherited", new org.jruby.internal.runtime.methods.JavaMethod(proxy, PUBLIC, "inherited") {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    throw typeError(context, "can not extend final Java class: " + clazzName);
                }
            });
        }
    }

    private static RubyClass createProxyClass(ThreadContext context, final RubyClass proxyClass,
                                              final Class<?> javaClass, final RubyClass superClass, boolean invokeInherited) {

        proxyClass.makeMetaClass(context, superClass.getMetaClass());

        if ( Map.class.isAssignableFrom( javaClass ) ) {
            proxyClass.allocator(context.runtime.getJavaSupport().getMapJavaProxyClass().getAllocator()).
                    defineMethods(context, MapJavaProxy.class).
                    include(context, enumerableModule(context));
        } else {
            proxyClass.allocator(superClass.getAllocator());
        }
        proxyClass.defineMethods(context, JavaProxy.ClassMethods.class);

        if (invokeInherited) superClass.invokeInherited(context, superClass, proxyClass);

        Initializer.setupProxyClass(context, javaClass, proxyClass);

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
        return invokeProxyClassInherited(((RubyBasicObject) clazz).getCurrentContext(), clazz, subclazz);
    }

    private static IRubyObject invokeProxyClassInherited(final ThreadContext context,
        final IRubyObject clazz, final IRubyObject subclazz) {
        final JavaSupport javaSupport = context.runtime.getJavaSupport();
        RubyClass javaProxyClass = javaSupport.getJavaProxyClass().getMetaClass();
        Helpers.invokeAs(context, javaProxyClass, clazz, "inherited", subclazz, Block.NULL_BLOCK);

        setupJavaSubclass(context, castAsClass(context, subclazz));

        return context.nil;
    }

    // called for Ruby sub-classes of a Java class
    private static void setupJavaSubclass(final ThreadContext context, final RubyClass subclass) {

        subclass.setInstanceVariable("@java_proxy_class", context.nil);

        // Subclasses of Java classes can safely use ivars, so we set this to silence warnings
        subclass.setCacheProxy(true);

        final RubyClass subclassSingleton = subclass.singletonClass(context);
        subclassSingleton.addReadAttribute(context, "java_proxy_class");
        subclassSingleton.addMethod(context, "java_interfaces", new JavaMethodZero(subclassSingleton, PUBLIC, "java_interfaces") {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                IRubyObject javaInterfaces = self.getInstanceVariables().getInstanceVariable("@java_interfaces");
                if (javaInterfaces != null) return javaInterfaces.dup();
                return context.nil;
            }
        });
        ///TODO: investigate this, should jcreate! still exist?

        subclass.addMethod(context, "__jcreate!", new JCreateMethod(subclassSingleton));
    }

    /**
     * Used for concrete reified classes. Constructed in generated code (RubyClass)
     */
    public static class JCtorCache implements CallableSelector.CallableCache<ParameterTypes> {

        private final NonBlockingHashMapLong<ParameterTypes> cache = new NonBlockingHashMapLong<>(8);
        public final JavaConstructor[] constructors;
        private final List<JavaConstructor> constructorList;

        public JCtorCache(JavaConstructor[] constructors) {
            this.constructors = constructors;
            constructorList = Arrays.asList(constructors);
        }

        public int indexOf(JavaConstructor ctor) {
            return constructorList.indexOf(ctor);
        }

        public final ParameterTypes getSignature(int signatureCode) {
            return cache.get(signatureCode);
        }

        public final void putSignature(int signatureCode, ParameterTypes callable) {
            cache.put(signatureCode, callable);
        }
    }

    public static class JCreateMethod extends JavaMethodN implements CallableSelector.CallableCache<JavaProxyConstructor> {

        private final NonBlockingHashMapLong<JavaProxyConstructor> cache = new NonBlockingHashMapLong<>(8);

        JCreateMethod(RubyModule cls) {
            super(cls, PUBLIC, "__jcreate!");
        }

        /**
         * Disambiguate which ctor index to call from the given cache
         * @param args argument list for the ctors
         * @param cache cache of ctors
         * @param runtime
         * @return Index of ctor in cache to call, or throws a new exception
         */
        public static int forTypes(Ruby runtime, IRubyObject[] args, JCtorCache cache) {
            JavaConstructor ctor = matchConstructorIndex(runtime.getCurrentContext(), cache.constructors, cache,
                    args.length, args);
            int index = cache.indexOf(ctor);
            if (index < 0) {
                // use our error otherwise
                throw argumentError(runtime.getCurrentContext(), "index error finding superconstructor");
            }
            return index;
        }

        private static JavaProxyClass getProxyClass(final ThreadContext context, final IRubyObject self) {
            final RubyClass metaClass = self.getMetaClass();
            IRubyObject proxyClass = metaClass.getInstanceVariable("@java_proxy_class");
            if (proxyClass == null || proxyClass.isNil()) { // lazy (proxy) class generation ... on JavaSubClass.new
                proxyClass = JavaProxyClass.getProxyClass(context, metaClass);
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
            final JavaProxyConstructor[] constructors = getProxyClass(context, self).getConstructors(context);

            final JavaProxyConstructor matching;
            switch (constructors.length) {
                case 1: matching = matchConstructor0ArityOne(context, constructors, arg0); break;
                default: matching = matchConstructorArityOne(context, constructors, arg0);
            }
            if (self instanceof JavaProxy) {
                return context.nil;
            }
            IRubyObject newObject = matching.newInstance(context.runtime, self, arg0);
            return JavaUtilities.set_java_object(self, self, newObject);
        }
        
        @Override
        public final IRubyObject call(final ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            final int arity = args.length;
            final JavaProxyConstructor[] constructors = getProxyClass(context, self).getConstructors(context);

            final JavaProxyConstructor matching;
            switch (constructors.length) {
                case 1: matching = matchConstructor0(context, constructors, arity, args); break;
                default: matching = matchConstructor(context, constructors, arity, args);
            }

            IRubyObject newObject = matching.newInstance(context.runtime, self, args);
            return JavaUtilities.set_java_object(self, self, newObject);
        }

        // assumes only 1 *Ruby* constructor exists! (Filters out nonruby)
        private JavaProxyConstructor matchConstructor0ArityOne(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final IRubyObject arg0) {
            JavaProxyConstructor forArity = checkCallableForArity(1, constructors, 0);

            if (forArity == null) throw argumentError(context, "wrong number of arguments for constructor");

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityOne(
                context.runtime, this, new JavaProxyConstructor[] { forArity }, arg0
            );

            if ( matching == null ) throw argumentError(context, "wrong number of arguments for constructor");

            return matching;
        }

        // assumes only 1 constructor exists!
        private JavaProxyConstructor matchConstructor0(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final int arity, final IRubyObject[] args) {
            JavaProxyConstructor forArity = checkCallableForArity(arity, constructors, 0);

            if ( forArity == null ) throw argumentError(context, "wrong number of arguments for constructor");

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityN(
                context.runtime, this, new JavaProxyConstructor[] { forArity }, args
            );

            if ( matching == null ) throw argumentError(context, "wrong number of arguments for constructor");

            return matching;
        }

        private JavaProxyConstructor matchConstructorArityOne(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final IRubyObject arg0) {
            ArrayList<JavaProxyConstructor> forArity = findCallablesForArity(1, constructors);

            if (forArity.isEmpty()) throw argumentError(context, "wrong number of arguments for constructor");

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityOne(
                    context.runtime, this, forArity.toArray(new JavaProxyConstructor[forArity.size()]), arg0
            );

            if (matching == null) throw argumentError(context, "wrong number of arguments for constructor");

            return matching;
        }

        // generic (slowest) path
        public JavaProxyConstructor matchConstructor(final ThreadContext context,
            final JavaProxyConstructor[] constructors, final int arity, final IRubyObject... args) {
            ArrayList<JavaProxyConstructor> forArity = findCallablesForArity(arity, constructors);

            if (forArity.isEmpty()) throw argumentError(context, "wrong number of arguments for constructor");

            final JavaProxyConstructor matching = CallableSelector.matchingCallableArityN(
                context.runtime, this, forArity.toArray(new JavaProxyConstructor[forArity.size()]), args
            );

            if (matching == null) throw argumentError(context, "wrong number of arguments for constructor");

            return matching;
        }

        // generic (slowest) path
        public static <T extends ParameterTypes> T matchConstructorIndex(final ThreadContext context,
            final T[] constructors, final CallableCache<ParameterTypes> cache, final int arity, final IRubyObject... args) {
            ArrayList<T> forArity = findCallablesForArity(arity, constructors);

            if (forArity.isEmpty()) throw argumentError(context, "wrong number of arguments for constructor");

            final ParameterTypes matching = CallableSelector.matchingCallableArityN(
                context.runtime, cache, forArity.toArray(new ParameterTypes[forArity.size()]), args
            );

            if (matching == null) throw argumentError(context, "wrong number of arguments for constructor");

            return (T) matching;
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
    private static void addToJavaPackageModule(ThreadContext context, RubyModule proxyClass) {
        final Class<?> clazz = (Class<?>)proxyClass.dataGetStruct();
        final String fullName;
        if ( ( fullName = clazz.getName() ) == null ) return;

        final RubyModule parentModule; final String className;

        if ( fullName.indexOf('$') != -1 ) {
            /*
             We don't want to define an inner class constant here, because it may conflict with static fields.
             Instead, we defer that constant definition to the declaring class's proxy initialization, which will deal
             with naming conflicts appropriately. See GH-6196.
             */
            return;
        }
        else {
            final int endPackage = fullName.lastIndexOf('.');
            String packageString = endPackage < 0 ? "" : fullName.substring(0, endPackage);
            parentModule = getJavaPackageModule(context, packageString);
            className = parentModule == null ? fullName : fullName.substring(endPackage + 1);
        }

        if ( parentModule != null && // TODO a Java Ruby class should not validate (as well)
            ( IdUtil.isConstant(className) || parentModule instanceof JavaPackage ) ) {
            // setConstant without validation since Java class name might be lower-case
            setProxyClass(context, parentModule, className, proxyClass, false);
        }
    }

    public static RubyModule getJavaPackageModule(final Ruby runtime, final Package pkg) {
        return getJavaPackageModule(runtime.getCurrentContext(), pkg == null ? "" : pkg.getName());
    }

    @Deprecated(since = "10.0")
    public static RubyModule getJavaPackageModule(final Ruby runtime, final String packageString) {
        return getJavaPackageModule(runtime.getCurrentContext(), packageString);
    }

    public static RubyModule getJavaPackageModule(ThreadContext context, final String packageString) {
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

        final RubyModule javaModule = context.runtime.getJavaSupport().getJavaModule(context);
        final IRubyObject packageModule = javaModule.getConstantAt(context, packageName);

        if (packageModule == null) return createPackageModule(context, javaModule, packageName, packageString);

        return packageModule instanceof RubyModule pkg ? pkg : null;
    }

    private static RubyModule createPackageModule(ThreadContext context,
        final RubyModule parentModule, final String name, final String packageString) {

        final RubyModule packageModule = JavaPackage.newPackage(context.runtime, packageString, parentModule);

        synchronized (parentModule) { // guard initializing in multiple threads
            final IRubyObject packageAlreadySet = parentModule.fetchConstant(context, name);
            if (packageAlreadySet != null) return (RubyModule) packageAlreadySet;

            parentModule.setConstant(context, name.intern(), packageModule);
            //MetaClass metaClass = (MetaClass) packageModule.getMetaClass();
            //metaClass.setAttached(packageModule);
        }
        return packageModule;
    }

    private static final Pattern CAMEL_CASE_PACKAGE_SPLITTER = Pattern.compile("([a-z0-9_]+)([A-Z])");

    private static RubyModule getPackageModule(ThreadContext context, final String name) {
        final RubyModule javaModule = context.runtime.getJavaSupport().getJavaModule(context);
        final IRubyObject packageModule = javaModule.getConstantAt(context, name);
        if ( packageModule instanceof RubyModule pkg) return pkg;

        final String packageName;
        if ( "Default".equals(name) ) packageName = "";
        else {
            Matcher match = CAMEL_CASE_PACKAGE_SPLITTER.matcher(name);
            packageName = match.replaceAll("$1.$2").toLowerCase();
        }
        return createPackageModule(context, javaModule, name, packageName);
    }

    @Deprecated(since = "10.0")
    public static RubyModule get_package_module(final IRubyObject self, final IRubyObject name) {
        return get_package_module(((RubyBasicObject) self).getCurrentContext(), self, name);
    }

    public static RubyModule get_package_module(ThreadContext context, final IRubyObject self, final IRubyObject name) {
        return getPackageModule(context, name.asJavaString());
    }

    public static IRubyObject get_package_module_dot_format(ThreadContext context, final IRubyObject self,
        final IRubyObject dottedName) {
        RubyModule module = getJavaPackageModule(context, dottedName.asJavaString());
        return module == null ? context.nil : module;
    }

    static RubyModule getProxyOrPackageUnderPackage(final ThreadContext context,
        final RubyModule parentPackage, final String name, final boolean cacheMethod) {
        if (name.isEmpty()) throw argumentError(context, "empty class or package name");

        final String fullName = JavaPackage.buildPackageName(parentPackage, name).toString();

        final RubyModule result;

        if ( ! Character.isUpperCase( name.charAt(0) ) ) {
            checkJavaReservedNames(context, name, false); // fails on primitives

            // this covers the rare case of lower-case class names (and thus will
            // fail 99.999% of the time). fortunately, we'll only do this once per
            // package name. (and seriously, folks, look into best practices...)
            RubyModule proxyClass = getProxyClassOrNull(context, fullName);
            if ( proxyClass != null ) {
                result = proxyClass; /* else not primitive or l-c class */
            } else {
                // Haven't found a class, continue on as though it were a package
                final RubyModule packageModule = getJavaPackageModule(context, fullName);
                // TODO: decompose getJavaPackageModule so we don't parse fullName
                if ( packageModule == null ) return null;
                result = packageModule;
            }
        }
        else {
            try { // First char is upper case, so assume it's a class name
                final RubyModule javaClass = getProxyClassOrNull(context, fullName);
                if ( javaClass != null ) {
                    result = javaClass;
                } else {
                    if (!allowUppercasePackageNames(context)) throw nameError(context, "missing class name " + fullName, fullName);

                    // for those not hip to conventions and best practices, we'll try as a package
                    result = getJavaPackageModule(context, fullName); // NOTE result = getPackageModule(runtime, name);
                    if (result == null) throw nameError(context, "missing class (or package) name " + fullName, fullName);
                }
            }
            catch (RuntimeException e) {
                if ( e instanceof RaiseException ) throw e;
                throw initCause(nameError(context, "missing class or uppercase package name " + fullName + " (" + e + ')', fullName, e), e);
            }
        }

        // saves class in singletonized parent, so we don't come back here :
        if ( cacheMethod )  bindJavaPackageOrClassMethod(context, parentPackage, name, result);

        return result;
    }

    private static boolean allowUppercasePackageNames(ThreadContext context) {
        return instanceConfig(context).getAllowUppercasePackageNames();
    }

    private static void checkJavaReservedNames(ThreadContext context, final String name, final boolean allowPrimitives) {
        // TODO: should check against all Java reserved names here, not just primitives
        if (!allowPrimitives && isPrimitiveClassName(name)) {
            throw argumentError(context, "illegal package name component: " + name);
        }
    }

    private static boolean isPrimitiveClassName(final String name) {
        return JavaUtil.getPrimitiveClass(name) != null;
    }

    @Deprecated(since = "10.0")
    public static Class getJavaClass(final Ruby runtime, final String className) throws RaiseException {
        return getJavaClass(runtime.getCurrentContext(), className);
    }

    public static Class getJavaClass(ThreadContext context, final String className) throws RaiseException {
        return getJavaClass(context, className, true);
    }

    @Deprecated(since = "10.0")
    public static Class getJavaClass(final Ruby runtime, final String className, boolean initialize) throws RaiseException {
        return getJavaClass(runtime.getCurrentContext(), className, initialize);
    }

    public static Class getJavaClass(ThreadContext context, final String className, boolean initialize) throws RaiseException {
        try {
            return loadJavaClass(context.runtime, className, initialize);
        } catch (ClassNotFoundException ex) {
            throw initCause(nameError(context, "Java class " + className + " not found", className, ex), ex);
        }
    }

    @Deprecated(since = "10.0")
    public static Class loadJavaClass(final Ruby runtime, final String className) throws ClassNotFoundException, RaiseException {
        return loadJavaClass(runtime.getCurrentContext(), className);
    }

    public static Class loadJavaClass(ThreadContext context, final String className) throws ClassNotFoundException, RaiseException {
        return loadJavaClass(context.runtime, className, true);
    }

    static Class loadJavaClass(final Ruby runtime, final String className, boolean initialize) throws ClassNotFoundException, RaiseException {
        try { // loadJavaClass here to handle things like LinkageError through
            synchronized (Java.class) {
                // a circular load might potentially dead-lock when loading concurrently
                // this path is reached from JavaPackage#relativeJavaClassOrPackage ...
                // another part preventing concurrent proxy initialization dead-locks is :
                // JavaSupportImpl's proxyClassCache = ClassValue.newInstance( ... )
                // ... having synchronized RubyModule computeValue(Class<?>)
                return runtime.getJavaSupport().loadJavaClass(className, initialize);
            }
        } catch (ExceptionInInitializerError ex) {
            throw initCause(nameError(runtime.getCurrentContext(), "cannot initialize Java class " + className + ' ' + '(' + ex + ')', className, ex), ex);
        } catch (UnsupportedClassVersionError ex) { // LinkageError
            String msg = getJavaVersionErrorMessage(ex);
            // cannot link Java class com.sample.FooBar needs Java 8 (java.lang.UnsupportedClassVersionError: com/sample/FooBar : Unsupported major.minor version 52.0)
            throw initCause(nameError(runtime.getCurrentContext(), "cannot link Java class " + className + ' ' + msg, className, ex), ex);
        } catch (LinkageError ex) {
            throw initCause(nameError(runtime.getCurrentContext(), "cannot link Java class " + className + ' ' + '(' + ex + ')', className, ex), ex);
        } catch (SecurityException ex) {
            throw initCause(runtime.newSecurityError(ex.getLocalizedMessage()), ex);
        }
    }

    private static String getJavaVersionErrorMessage(UnsupportedClassVersionError ex) {
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
        return msg;
    }

    static RaiseException initCause(final RaiseException ex, final Throwable cause) {
        ex.initCause(cause); return ex;
    }

    private static RubyModule getProxyClassOrNull(ThreadContext context, final String className) {
        final Class<?> clazz;
        try {
            clazz = loadJavaClass(context, className);
        } catch (ClassNotFoundException ex) { // used to catch NoClassDefFoundError for whatever reason
            return null;
        }
        return getProxyClass(context, clazz);
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

    public static IRubyObject get_proxy_or_package_under_package(final ThreadContext context, final IRubyObject self,
                                                                 final IRubyObject parentPackage, final IRubyObject name) {
        final RubyModule result = getProxyOrPackageUnderPackage(context, castAsModule(context, parentPackage), name.asJavaString(), true);
        return result != null ? result : context.nil;
    }

    private static RubyModule getTopLevelProxyOrPackage(ThreadContext context,
        final String name, final boolean cacheMethod) {

        if (name.isEmpty()) throw argumentError(context,"empty class or package name");

        final RubyModule result;

        if ( Character.isLowerCase( name.charAt(0) ) ) {
            // this covers primitives and (unlikely) lower-case class names
            RubyModule proxyClass = getProxyClassOrNull(context, name);
            if ( proxyClass != null ) result = proxyClass; /* else not primitive or l-c class */
            else {
                checkJavaReservedNames(context, name, true);

                final RubyModule packageModule = getJavaPackageModule(context.runtime, name);
                // TODO: decompose getJavaPackageModule so we don't parse fullName
                if ( packageModule == null ) return null;

                result = packageModule;
            }
        }
        else {
            RubyModule javaClass = getProxyClassOrNull(context, name);
            if ( javaClass != null ) result = javaClass;
            else {
                // upper-case package name
                // TODO: top-level upper-case package was supported in the previous (Ruby-based)
                // implementation, so leaving as is.  see note at #getProxyOrPackageUnderPackage
                // re: future approach below the top-level.
                result = getPackageModule(context, name);
            }
        }

        if ( cacheMethod ) bindJavaPackageOrClassMethod(context, name, result);

        return result;
    }

    private static boolean bindJavaPackageOrClassMethod(ThreadContext context, final String name,
        final RubyModule packageOrClass) {
        final RubyModule javaPackage = context.runtime.getJavaSupport().getJavaModule(context);
        return bindJavaPackageOrClassMethod(context, javaPackage, name, packageOrClass);
    }

    private static boolean bindJavaPackageOrClassMethod(ThreadContext context, final RubyModule parentPackage,
        final String name, final RubyModule packageOrClass) {

        if (parentPackage.getMetaClass().isMethodBound(name, false)) return false;

        final RubyClass singleton = parentPackage.singletonClass(context);
        singleton.addMethod(context, name.intern(), new JavaAccessor(singleton, packageOrClass, parentPackage, name));
        return true;
    }

    private static class JavaAccessor extends org.jruby.internal.runtime.methods.JavaMethod {

        private final RubyModule packageOrClass;
        private final RubyModule parentPackage;

        JavaAccessor(final RubyClass singleton, final RubyModule packageOrClass, final RubyModule parentPackage, final String name) {
            super(singleton, PUBLIC, name);
            this.parentPackage = parentPackage; this.packageOrClass = packageOrClass;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 0) throw JavaPackage.packageMethodArgumentMismatch(context, parentPackage, name, args.length);

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

        @Deprecated @Override
        public Arity getArity() { return Arity.noArguments(); }

    }

    static final class ProcToInterface extends org.jruby.internal.runtime.methods.DynamicMethod {

        ProcToInterface(final RubyClass singletonClass) {
            super(singletonClass, PUBLIC, "call");
        }

        @Override // method_missing impl :
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            final IRubyObject[] newArgs;
            switch (args.length) {
                case 1 -> newArgs = IRubyObject.NULL_ARRAY;
                case 2 -> newArgs = new IRubyObject[]{args[1]};
                case 3 -> newArgs = new IRubyObject[]{args[1], args[2]};
                default -> {
                    newArgs = new IRubyObject[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, newArgs.length);
                }
            }
            return callProc(context, self, newArgs);
        }

        private static IRubyObject callProc(ThreadContext context, IRubyObject self, IRubyObject[] procArgs) {
            RubyProc proc = castAsProc(context, self, "interface impl method_missing for block used with non-Proc object");
            return proc.call(context, procArgs);
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }

        final ConcreteMethod getConcreteMethod(String name) { return new ConcreteMethod(name); }

        final class ConcreteMethod extends org.jruby.internal.runtime.methods.JavaMethod {

            ConcreteMethod(String name) {
                super(ProcToInterface.this.implementationClass, Visibility.PUBLIC, name);
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
        if (name.isEmpty()) throw argumentError(context, "empty class name");

        Class<?> enclosing = JavaUtil.getJavaClass(enclosingClass, null);

        if (enclosing == null) return null;

        final String fullName = enclosing.getName() + '$' + name;
        final RubyModule result = getProxyClassOrNull(context, fullName);
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
        return cacheConstant(context, self, constName, innerClass, true); // hidden == true (private_constant)
    }

    @JRubyMethod(meta = true)
    public static IRubyObject const_missing(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
        final String constName = name.asJavaString();
        // it's fine to not add the "cached" method here - when users sticking to
        // constant access won't pay the "penalty" for adding dynamic methods ...
        final RubyModule packageOrClass = getTopLevelProxyOrPackage(context, constName, false);
        if ( packageOrClass == null ) return context.nil; // compatibility (with packages)
        return cacheConstant(context, (RubyModule) self, constName, packageOrClass, false);
    }

    private static RubyModule cacheConstant(ThreadContext context, final RubyModule owner, // e.g. ::Java
        final String constName, final RubyModule packageOrClass, final boolean hidden) {
        if ( packageOrClass != null ) {
            // NOTE: if it's a package createPackageModule already set the constant
            // ... but in case it's a (top-level) Java class name we still need to:
            synchronized (owner) {
                final IRubyObject alreadySet = owner.fetchConstant(context, constName);
                if ( alreadySet != null ) return (RubyModule) alreadySet;
                owner.setConstant(context, constName, packageOrClass, hidden);
            }
            return packageOrClass;
        }
        return null;
    }

    @JRubyMethod(name = "method_missing", meta = true, required = 1)
    public static IRubyObject method_missing(ThreadContext context, final IRubyObject self,
        final IRubyObject name) { // JavaUtilities.get_top_level_proxy_or_package(name)
        // NOTE: getTopLevelProxyOrPackage will bind the (cached) method for us :
        final RubyModule result = getTopLevelProxyOrPackage(context, name.asJavaString(), true);
        if ( result != null ) return result;
        return context.nil;
    }

    @JRubyMethod(name = "method_missing", meta = true, rest = true)
    public static IRubyObject method_missing(ThreadContext context, final IRubyObject self,
        final IRubyObject[] args) {
        final IRubyObject name = args[0];
        if ( args.length > 1 ) {
            final int count = args.length - 1;
            throw argumentError(context, "Java does not have a method '"+ name +"' with " + count + " arguments");
        }
        return method_missing(context, self, name);
    }

    public static IRubyObject get_top_level_proxy_or_package(final ThreadContext context,
        final IRubyObject self, final IRubyObject name) {
        final RubyModule result = getTopLevelProxyOrPackage(context, name.asJavaString(), true);
        return result != null ? result : context.nil;
    }

    @Deprecated
    public static IRubyObject wrap(Ruby runtime, IRubyObject java_object) {
        return getInstance(runtime, ((JavaObject) java_object).getValue());
    }

    /**
     * High-level object conversion utility function 'java_to_primitive' is the low-level version
     */
    @Deprecated
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject java_to_ruby(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        var context = ((RubyBasicObject) recv).getCurrentContext();
        try {
            return JavaUtil.java_to_ruby(context.runtime, object);
        } catch (RuntimeException e) {
            context.runtime.getJavaSupport().handleNativeException(e, null);
            // This point is only reached if there was an exception handler installed.
            return context.nil;
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

    /**
     * @param recv
     * @param wrapper
     * @param interfaces
     * @param block
     * @return ""
     * @deprecated Use
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject new_proxy_instance2(IRubyObject recv, final IRubyObject wrapper,
                                                  final IRubyObject interfaces, Block block) {
        return new_proxy_instance2(((RubyBasicObject) recv).getCurrentContext(), recv, wrapper, interfaces, block);
    }

        // TODO: Formalize conversion mechanisms between Java and Ruby
    @JRubyMethod(required = 2, module = true, visibility = PRIVATE)
    public static IRubyObject new_proxy_instance2(ThreadContext context, IRubyObject recv, final IRubyObject wrapper,
                                                  final IRubyObject interfaces, Block block) {
        IRubyObject[] javaClasses = ((RubyArray) interfaces).toJavaArray(context);

        // Create list of interface names to proxy (and make sure they really are interfaces)
        Class[] unwrapped = new Class[javaClasses.length];
        for (int i = 0; i < javaClasses.length; i++) {
            final Class<?> klass = JavaUtil.unwrapJava(context, javaClasses[i]); // TypeError if not a Java wrapper

            if (!klass.isInterface()) throw argumentError(context, "Java interface expected, got: " + klass);

            unwrapped[i] = klass;
        }

        return getInstance(context.runtime, newInterfaceImpl(context, wrapper, unwrapped));
    }

    @Deprecated(since = "10.0")
    public static Object newInterfaceImpl(final IRubyObject wrapper, Class[] interfaces) {
        return newInterfaceImpl(((RubyBasicObject) wrapper).getCurrentContext(), wrapper, interfaces);
    }

    public static Object newInterfaceImpl(ThreadContext context, final IRubyObject wrapper, Class[] interfaces) {
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
        final boolean isProc = wrapperClass.isSingleton() && wrapperClass.getRealClass() == context.runtime.getProc();

        final JRubyClassLoader jrubyClassLoader = context.runtime.getJRubyClassLoader();

        if ( RubyInstanceConfig.INTERFACES_USE_PROXY ) {
            return newProxyInterfaceImpl(wrapper, interfaces, jrubyClassLoader);
        }

        final ClassDefiningClassLoader classLoader;
        // hashcode is a combination of the interfaces and the Ruby class we're using to implement them
        int interfacesHashCode = interfacesHashCode(interfaces);
        // if it's a singleton class and the real class is proc, we're doing closure conversion
        // so just use Proc's hashcode
        if ( isProc ) {
            interfacesHashCode = 31 * interfacesHashCode + context.runtime.getProc().hashCode();
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
            proxyImplClass = RealClassGenerator.createOldStyleImplClass(interfaces, wrapperClass, context.runtime, implClassName, classLoader);
        }

        try {
            Constructor<?> proxyConstructor = proxyImplClass.getConstructor(IRubyObject.class);
            return proxyConstructor.newInstance(wrapper);
        }
        catch (InvocationTargetException e) {
            throw mapGeneratedProxyException(context.runtime, e);
        }
        catch (ReflectiveOperationException e) {
            throw mapGeneratedProxyException(context.runtime, e);
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
                if ( other instanceof InterfaceProxyHandler that) {
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

    //TODO: what is this doing?
    @SuppressWarnings("unchecked")
    public static Class generateRealClass(final RubyClass clazz) {
        final Ruby runtime = clazz.getRuntime();
        var context = runtime.getCurrentContext();
        final Class[] interfaces = getInterfacesFromRubyClass(clazz);

        // hashcode is a combination of the interfaces and the Ruby class we're using
        // to implement them
        int interfacesHashCode = interfacesHashCode(interfaces);
        // normal new class implementing interfaces
        interfacesHashCode = 31 * interfacesHashCode + clazz.hashCode();

        String implClassName = Constants.GENERATED_PACKAGE;
        if (clazz.getBaseName() == null) {
            // no-name class, generate a bogus name for it
            implClassName += "Class0x" + Integer.toHexString(System.identityHashCode(clazz)) + '_' + Math.abs(interfacesHashCode);
        } else {
            implClassName += StringSupport.replaceAll(clazz.getName(context), "::", "$$").toString() + '_' + Math.abs(interfacesHashCode);
        }
        Class<? extends IRubyObject> proxyImplClass;
        try {
            proxyImplClass = (Class<? extends IRubyObject>) Class.forName(implClassName, true, runtime.getJRubyClassLoader());
        }
        catch (ClassNotFoundException ex) {
            // try to use super's reified class; otherwise, RubyObject (for now)
        	//TODO: test java reified?
            Class<?> superClass = clazz.getSuperClass().getRealClass().reifiedClass();
            if ( superClass == null ) superClass = RubyObject.class;
            proxyImplClass = RealClassGenerator.createRealImplClass(superClass, interfaces, clazz, runtime, implClassName);

            // add a default initialize if one does not already exist and this is a Java-hierarchy class
            if ( NEW_STYLE_EXTENSION &&
                ! ( RubyBasicObject.class.isAssignableFrom(proxyImplClass) || clazz.getMethods().containsKey("initialize") ) ) {
                clazz.addMethod(context, "initialize", new DummyInitialize(clazz));
            }
        }
        clazz.reifiedClass(proxyImplClass).setRubyClassAllocator(proxyImplClass);

        return proxyImplClass;
    }

    private static final class DummyInitialize extends JavaMethodZero {

        DummyInitialize(final RubyClass clazz) { super(clazz, PRIVATE, "initialize"); }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return context.nil;
        }

    }

    public static Constructor<? extends IRubyObject> getRealClassConstructor(final Ruby runtime, Class<? extends IRubyObject> proxyImplClass) {
        try {
            return proxyImplClass.getConstructor(Ruby.class, RubyClass.class);
        }
        catch (NoSuchMethodException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
    }

    public static IRubyObject constructProxy(Ruby runtime, Constructor<? extends IRubyObject> proxyConstructor, RubyClass clazz) {
        try {
            return proxyConstructor.newInstance(runtime, clazz);
        }
        catch (InvocationTargetException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
        catch (ReflectiveOperationException e) {
            throw mapGeneratedProxyException(runtime, e);
        }
    }

    private static RaiseException mapGeneratedProxyException(final Ruby runtime, final ReflectiveOperationException e) {
        return withException(typeError(runtime.getCurrentContext(),
                "Exception instantiating generated interface impl:\n" + e), e);
    }

    private static RaiseException mapGeneratedProxyException(final Ruby runtime, final InvocationTargetException e) {
        return withException(typeError(runtime.getCurrentContext(),
                "Exception instantiating generated interface impl:\n" + e.getTargetException()), e);
    }

    public static IRubyObject allocateProxy(Object javaObject, RubyClass clazz) {
        var context = clazz.getRuntime().getCurrentContext();
        // Arrays are never stored in OPC
        if ( clazz.getSuperClass() == context.runtime.getJavaSupport().getArrayProxyClass() ) {
            return new ArrayJavaProxy(context.runtime, clazz, javaObject, JavaUtil.getJavaConverter(javaObject.getClass().getComponentType()));
        }

        final IRubyObject proxy = clazz.allocate(context);

        if ( proxy instanceof JavaProxy jproxy) {
            jproxy.setObject(javaObject);
        } else {
            // TODO (JavaObject transition) is this really necessary?
            proxy.dataWrapStruct(new JavaProxy(context.runtime, clazz, javaObject));
        }
        return proxy;
    }

    @Deprecated(since = "10.0")
    public static IRubyObject wrapJavaObject(Ruby runtime, Object object) {
        return wrapJavaObject(runtime.getCurrentContext(), object);
    }

    public static IRubyObject wrapJavaObject(ThreadContext context, Object object) {
        return allocateProxy(object, getProxyClassForObject(context, object));
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
     * <p>Note: This method is internal and might be subject to change, do not assume its part of JRuby's API!</p>
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
                catch (NoSuchMethodException e) { /* fall-through */ }
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

    /**
     * Try to set the given member to be accessible, considering open modules and avoiding the actual setAccessible
     * call when it would produce a JPMS warning. All classes on Java 8 are considered open, allowing setAccessible
     * to proceed.
     *
     * The open check is based on this class, Java.java, which will be in whatever core or dist JRuby module you are
     * using.
     */
    public static <T extends AccessibleObject & Member> boolean trySetAccessible(T member) {
        return Modules.trySetAccessible(member, Java.class);
    }

    /**
     * Check if the given member would be accessible without using the deprecated AccessibleObject.isAccessible.
     *
     * This uses backport9 logic to check if the given class is in a package that has been opened up to us, since
     * under JPMS that is the only way we can do invasive accesses. On Java 8, it continues to use isAccessible.
     *
     * The open check is based on this class, Java.java, which will be in whatever core or dist JRuby module you are
     * using.
     */
    public static <T extends AccessibleObject & Member> boolean isAccessible(T member) {
        return Modules.isAccessible(member, Java.class);
    }

    public static JavaObject castToJavaObject(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof JavaObject)) throw typeError(context, newValue, "a java object");
        return (JavaObject) newValue;
    }
}
