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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 * Copyright (C) 2011 David Pollak <feeder.of.the.bears@gmail.com>
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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.util.ArrayUtils;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.jruby.RubyModule.undefinedMethodMessage;
import static org.jruby.util.RubyStringBuilder.ids;

// @JRubyClass(name="Java::JavaClass", parent="Java::JavaObject", include = "Comparable")
public class JavaClass extends JavaObject {

    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    public JavaClass(final Ruby runtime, final Class<?> klass) {
        this(runtime, runtime.getJavaSupport().getJavaClassClass(), klass);
    }

    JavaClass(final Ruby runtime, final RubyClass javaClassProxy, final Class<?> klass) {
        super(runtime, javaClassProxy, klass);
    }

    @Override
    public final boolean equals(Object other) {
        if ( this == other ) return true;
        return other instanceof JavaClass && this.getValue() == ((JavaClass) other).getValue();
    }

    @Override
    public final int hashCode() {
        return getValue().hashCode();
    }

    public final RubyModule getProxyModule() {
        return Java.getProxyClass(getRuntime(), javaClass());
    }

    public final RubyClass getProxyClass() {
        return (RubyClass) Java.getProxyClass(getRuntime(), javaClass());
    }

    private static IRubyObject addProxyExtender(final ThreadContext context, final Class<?> klass, final IRubyObject extender) {
        if ( ! extender.respondsTo("extend_proxy") ) {
            throw context.runtime.newTypeError("proxy extender must have an extend_proxy method");
        }
        RubyModule proxy = Java.getProxyClass(context.runtime, klass);
        return extender.callMethod(context, "extend_proxy", proxy);
    }

    @JRubyMethod(required = 1)
    public static IRubyObject extend_proxy(final ThreadContext context, IRubyObject self, IRubyObject extender) {
        if (self instanceof JavaClass) {
            addProxyExtender(context, ((JavaClass) self).javaClass(), extender);
        } else {
            // NOTE: used by java.lang.Class as a JavaClass compatiblity layer
            addProxyExtender(context, Java.unwrapClassProxy(self), extender);
        }
        return context.nil;
    }

    @Deprecated
    public static JavaClass get(final Ruby runtime, final Class<?> klass) {
        return runtime.getJavaSupport().getJavaClassFromCache(klass);
    }

    @Deprecated // only been used package internally - a bit poorly named
    public static RubyArray getRubyArray(Ruby runtime, Class<?>[] classes) {
        return toRubyArray(runtime, classes);
    }

    @Deprecated
    public static RubyArray toRubyArray(final Ruby runtime, final Class<?>[] classes) {
        IRubyObject[] javaClasses = new IRubyObject[classes.length];
        for ( int i = classes.length; --i >= 0; ) {
            javaClasses[i] = get(runtime, classes[i]);
        }
        return RubyArray.newArrayMayCopy(runtime, javaClasses);
    }

    static RubyClass createJavaClassClass(final Ruby runtime, final RubyModule Java, final RubyClass JavaObject) {
        // TODO: Determine if a real allocator is needed here. Do people want to extend
        // JavaClass? Do we want them to do that? Can you Class.new(JavaClass)? Should you be able to?
        // NOTE: NOT_ALLOCATABLE_ALLOCATOR is probably OK here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass JavaClass = Java.defineClassUnder("JavaClass", JavaObject, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        JavaClass.includeModule(runtime.getModule("Comparable"));

        JavaClass.defineAnnotatedMethods(JavaClass.class);

        JavaClass.getMetaClass().undefineMethod("new");
        JavaClass.getMetaClass().undefineMethod("allocate");

        return JavaClass;
    }

    public final Class javaClass() {
        return (Class<?>) getValue();
    }

    /**
     * Get the associated JavaClass for a proxy module.
     *
     * The passed module/class is assumed to be a Java proxy module/class!
     * @param context
     * @param proxy
     * @return class
     */
    // NOTE: API still used, need a better place for it after whole class is deprecated
    public static Class<?> getJavaClass(final ThreadContext context, final RubyModule proxy) {
        return getJavaClassIfProxy(context, proxy);
    }

    /**
     * Retieve a JavaClass if the passed module/class is a Java proxy.
     * @param context
     * @param type
     * @return class or null if not a Java proxy
     *
     * @note Class objects have a java_class method but they're not considered Java proxies!
     */
    // NOTE: API still used, need a better place for it after whole class is deprecated
    public static Class<?> getJavaClassIfProxy(final ThreadContext context, final RubyModule type) {
        if (type.getJavaProxy()) return (Class) type.dataGetStruct();

        Object java_class = JavaProxy.getJavaClass(type);
        if (java_class instanceof JavaProxy) {
            return (Class<?>) ((JavaProxy) java_class).getObject();
        }

        if (java_class == null) { // NOTE: old java_class(context, proxy) impl
            if (type.respondsTo("java_class")) { // NOTE: quite bad since built-in Ruby classes will return
                // a Ruby Java proxy for java.lang.Class while Java proxies will return a JavaClass instance !
                java_class = Helpers.invoke(context, type, "java_class");
            }
            if (java_class instanceof JavaClass) { // legacy
                return ((JavaClass) java_class).javaClass();
            }
        }

        return null;
    }

    // expected to handle Java proxy (Ruby) sub-classes as well
    public static boolean isProxyType(final ThreadContext context, final RubyModule proxy) {
        return getJavaClassIfProxy(context, proxy) != null;
    }

    /**
     * Returns the (reified or proxied) Java class if the passed Ruby module/class has one.
     * @param context
     * @param type
     * @return Java proxy class, Java reified class or nil
     */
    @Deprecated
    public static IRubyObject java_class(final ThreadContext context, final RubyModule type) { // TODO avoid callers!
        IRubyObject java_class = type.getInstanceVariable("@java_class");
        if ( java_class == null ) { // || java_class.isNil()
            if ( type.respondsTo("java_class") ) { // NOTE: quite bad since built-in Ruby classes will return
                // a Ruby Java proxy for java.lang.Class while Java proxies will return a JavaClass instance !
                java_class = Helpers.invoke(context, type, "java_class");
            }
            else java_class = context.nil; // we return != null (just like callMethod would)
        }
        return java_class;
    }

    /**
     * Resolves a Java class from a passed type parameter.
     *
     * Uisng the rules accepted by `to_java(type)` in Ruby land.
     * @param context
     * @param type
     * @return resolved type or null if resolution failed
     */
    @Deprecated
    public static JavaClass resolveType(final ThreadContext context, final IRubyObject type) {
        RubyModule proxyClass = Java.resolveType(context.runtime, type);
        return proxyClass == null ? null : get(context.runtime, getJavaClass(context, proxyClass));
    }

    @Deprecated
    public static JavaClass forNameVerbose(Ruby runtime, String className) {
        Class<?> klass = null;
        synchronized (JavaClass.class) {
            if (klass == null) {
                klass = runtime.getJavaSupport().loadJavaClassVerbose(className);
            }
            return JavaClass.get(runtime, klass);
        }
    }

    @Deprecated // no longer used
    public static JavaClass forNameQuiet(Ruby runtime, String className) {
        synchronized (JavaClass.class) {
            Class<?> klass = runtime.getJavaSupport().loadJavaClassQuiet(className);
            return JavaClass.get(runtime, klass);
        }
    }

    @JRubyMethod(name = "for_name", required = 1, meta = true)
    public static JavaClass for_name(IRubyObject recv, IRubyObject name) {
        return for_name(recv, name.asJavaString());
    }

    static JavaClass for_name(IRubyObject recv, String name) {
        return forNameVerbose(recv.getRuntime(), name);
    }

    @JRubyMethod
    public RubyModule ruby_class() {
        // Java.getProxyClass deals with sync issues, so we won't duplicate the logic here
        return Java.getProxyClass(getRuntime(), javaClass());
    }

    @JRubyMethod(name = "public?")
    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "protected?")
    public RubyBoolean protected_p() {
        return getRuntime().newBoolean(Modifier.isProtected(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "private?")
    public RubyBoolean private_p() {
        return getRuntime().newBoolean(Modifier.isPrivate(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "final?")
    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "interface?")
    public RubyBoolean interface_p() {
        return getRuntime().newBoolean(javaClass().isInterface());
    }

    @JRubyMethod(name = "array?")
    public RubyBoolean array_p() {
        return getRuntime().newBoolean(javaClass().isArray());
    }

    @JRubyMethod(name = "enum?")
    public RubyBoolean enum_p() {
        return getRuntime().newBoolean(javaClass().isEnum());
    }

    @JRubyMethod(name = "annotation?")
    public RubyBoolean annotation_p() {
        return getRuntime().newBoolean(javaClass().isAnnotation());
    }

    @JRubyMethod(name = "anonymous_class?")
    public RubyBoolean anonymous_class_p() {
        return getRuntime().newBoolean(javaClass().isAnonymousClass());
    }

    @JRubyMethod(name = "local_class?")
    public RubyBoolean local_class_p() {
        return getRuntime().newBoolean(javaClass().isLocalClass());
    }

    @JRubyMethod(name = "member_class?")
    public RubyBoolean member_class_p() {
        return getRuntime().newBoolean(javaClass().isMemberClass());
    }

    @JRubyMethod(name = "synthetic?")
    public IRubyObject synthetic_p() {
        return getRuntime().newBoolean(javaClass().isSynthetic());
    }

    @JRubyMethod(name = {"name", "to_s"})
    public RubyString name() {
        return getRuntime().newString(javaClass().getName());
    }

    @Override
    @JRubyMethod
    public RubyString inspect() {
        return getRuntime().newString("class " + javaClass().getName());
    }

    @JRubyMethod
    public IRubyObject canonical_name() {
        String canonicalName = javaClass().getCanonicalName();
        if (canonicalName != null) {
            return getRuntime().newString(canonicalName);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "package")
    public IRubyObject get_package() {
        return Java.getInstance(getRuntime(), javaClass().getPackage());
    }

    @JRubyMethod
    public IRubyObject class_loader() {
        return Java.getInstance(getRuntime(), javaClass().getClassLoader());
    }

    @JRubyMethod
    public IRubyObject protection_domain() {
        return Java.getInstance(getRuntime(), javaClass().getProtectionDomain());
    }

    @JRubyMethod(required = 1)
    public IRubyObject resource(IRubyObject name) {
        return Java.getInstance(getRuntime(), javaClass().getResource(name.asJavaString()));
    }

    @JRubyMethod(required = 1)
    public IRubyObject resource_as_stream(IRubyObject name) {
        return Java.getInstance(getRuntime(), javaClass().getResourceAsStream(name.asJavaString()));
    }

    @JRubyMethod(required = 1)
    public IRubyObject resource_as_string(IRubyObject name) {
        InputStream in = javaClass().getResourceAsStream(name.asJavaString());
        if (in == null) return getRuntime().getNil();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            int len;
            byte[] buf = new byte[4096];
            while ((len = in.read(buf)) >= 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw getRuntime().newIOErrorFromException(e);
        } finally {
            try {in.close();} catch (IOException ioe) {}
        }
        return getRuntime().newString(new ByteList(out.toByteArray(), false));
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(required = 1)
    public IRubyObject annotation(final IRubyObject annoClass) {
        final Ruby runtime = getRuntime();
        if ( ! ( annoClass instanceof JavaClass ) ) {
            throw runtime.newTypeError(annoClass, runtime.getJavaSupport().getJavaClassClass());
        }
        final Class annotation = ((JavaClass) annoClass).javaClass();
        return Java.getInstance(runtime, javaClass().getAnnotation(annotation));
    }

    @JRubyMethod
    public IRubyObject annotations() {
        // note: intentionally returning the actual array returned from Java, rather
        // than wrapping it in a RubyArray. wave of the future, when java_class will
        // return the actual class, rather than a JavaClass wrapper.
        return Java.getInstance(getRuntime(), javaClass().getAnnotations());
    }

    @JRubyMethod(name = "annotations?")
    public RubyBoolean annotations_p() {
        return getRuntime().newBoolean(javaClass().getAnnotations().length > 0);
    }

    @JRubyMethod
    public IRubyObject declared_annotations() {
        // see note above re: return type
        return Java.getInstance(getRuntime(), javaClass().getDeclaredAnnotations());
    }

    @JRubyMethod(name = "declared_annotations?")
    public RubyBoolean declared_annotations_p() {
        return getRuntime().newBoolean(javaClass().getDeclaredAnnotations().length > 0);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "annotation_present?", required = 1)
    public IRubyObject annotation_present_p(final IRubyObject annoClass) {
        final Ruby runtime = getRuntime();
        if ( ! ( annoClass instanceof JavaClass ) ) {
            throw runtime.newTypeError(annoClass, runtime.getJavaSupport().getJavaClassClass());
        }
        final Class annotation = ((JavaClass) annoClass).javaClass();
        return runtime.newBoolean( javaClass().isAnnotationPresent(annotation) );
    }

    @JRubyMethod
    public IRubyObject modifiers() {
        return getRuntime().newFixnum(javaClass().getModifiers());
    }

    @JRubyMethod
    public IRubyObject declaring_class() {
        Class<?> clazz = javaClass().getDeclaringClass();
        if (clazz != null) {
            return JavaClass.get(getRuntime(), clazz);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject enclosing_class() {
        return Java.getInstance(getRuntime(), javaClass().getEnclosingClass());
    }

    @JRubyMethod
    public IRubyObject enclosing_constructor() {
        Constructor<?> ctor = javaClass().getEnclosingConstructor();
        if (ctor != null) {
            return Java.getInstance(getRuntime(), ctor);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject enclosing_method() {
        Method meth = javaClass().getEnclosingMethod();
        if (meth != null) {
            return Java.getInstance(getRuntime(), meth);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject enum_constants() {
        return Java.getInstance(getRuntime(), javaClass().getEnumConstants());
    }

    @JRubyMethod
    public IRubyObject generic_interfaces() {
        return Java.getInstance(getRuntime(), javaClass().getGenericInterfaces());
    }

    @JRubyMethod
    public IRubyObject generic_superclass() {
        return Java.getInstance(getRuntime(), javaClass().getGenericSuperclass());
    }

    @JRubyMethod
    public IRubyObject type_parameters() {
        return Java.getInstance(getRuntime(), javaClass().getTypeParameters());
    }

    @JRubyMethod
    public IRubyObject signers() {
        return Java.getInstance(getRuntime(), javaClass().getSigners());
    }

    public static String getSimpleName(Class<?> clazz) {
 		if (clazz.isArray()) {
 			return getSimpleName(clazz.getComponentType()) + "[]";
 		}

 		String className = clazz.getName();
 		int len = className.length();
        int i = className.lastIndexOf('$');
 		if (i != -1) {
            do {
 				i++;
 			} while (i < len && Character.isDigit(className.charAt(i)));
 			return className.substring(i);
 		}

 		return className.substring(className.lastIndexOf('.') + 1);
 	}

    @JRubyMethod
    public RubyString simple_name() {
        return getRuntime().newString(getSimpleName(javaClass()));
    }

    @JRubyMethod
    public IRubyObject superclass() {
        Class<?> superclass = javaClass().getSuperclass();
        if (superclass == null) {
            return getRuntime().getNil();
        }
        return JavaClass.get(getRuntime(), superclass);
    }

    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(IRubyObject other) {
        final Class<?> thisClass = javaClass();
        Class<?> otherClass = null;

        // dig out the other class
        if (other instanceof JavaClass) {
            otherClass = ( (JavaClass) other ).javaClass();
        }
        else if (other instanceof ConcreteJavaProxy) {
            ConcreteJavaProxy proxy = (ConcreteJavaProxy) other;
            final Object wrapped = proxy.getObject();
            if ( wrapped instanceof Class ) {
                otherClass = (Class) wrapped;
            }
        }

        if ( otherClass != null ) {
            if ( thisClass == otherClass ) {
                return getRuntime().newFixnum(0);
            }
            if ( otherClass.isAssignableFrom(thisClass) ) {
                return getRuntime().newFixnum(-1);
            }
            if ( thisClass.isAssignableFrom(otherClass) ) {
                return getRuntime().newFixnum(1);
            }
        }

        // can't do a comparison
        return getRuntime().getNil();
    }

    @JRubyMethod
    public RubyArray java_instance_methods() {
        return toJavaMethods(javaClass().getMethods(), false);
    }

    @JRubyMethod
    public RubyArray declared_instance_methods() {
        return toJavaMethods(javaClass().getDeclaredMethods(), false);
    }

    @JRubyMethod
    public RubyArray java_class_methods() {
        return toJavaMethods(javaClass().getMethods(), true);
    }

    @JRubyMethod
    public RubyArray declared_class_methods() {
        return toJavaMethods(javaClass().getDeclaredMethods(), true);
    }

    private RubyArray toJavaMethods(final Method[] methods, final boolean isStatic) {
        final Ruby runtime = getRuntime();
        final RubyArray result = runtime.newArray(methods.length);
        for ( int i = 0; i < methods.length; i++ ) {
            final Method method = methods[i];
            if ( isStatic == Modifier.isStatic(method.getModifiers()) ) {
                result.append(Java.getInstance(runtime, method));
            }
        }
        return result;
    }

    @JRubyMethod(required = 1, rest = true)
    public JavaMethod java_method(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        if ( args.length < 1 ) throw runtime.newArgumentError(args.length, 1);

        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = getArgumentTypes(context, args, 1);
            @SuppressWarnings("unchecked")
            final Method method = javaClass().getMethod(methodName, argumentTypes);
            return new JavaMethod(runtime, method);
        }
        catch (NoSuchMethodException e) {
            throw runtime.newNameError(undefinedMethodMessage(runtime, ids(runtime, methodName), ids(runtime, javaClass().getName()), false), methodName);
        }
    }

    @JRubyMethod(required = 1, rest = true)
    public JavaMethod declared_method(ThreadContext context, final IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        if ( args.length < 1 ) throw runtime.newArgumentError(args.length, 1);

        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = getArgumentTypes(context, args, 1);
            @SuppressWarnings("unchecked")
            final Method method = javaClass().getDeclaredMethod(methodName, argumentTypes);
            return new JavaMethod(runtime, method);
        }
        catch (NoSuchMethodException e) {
            throw runtime.newNameError(undefinedMethodMessage(runtime, ids(runtime, methodName), ids(runtime, javaClass().getName()), false), methodName);
        }
    }

    @JRubyMethod(required = 1, rest = true)
    public JavaCallable declared_method_smart(ThreadContext context, final IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        if ( args.length < 1 ) throw runtime.newArgumentError(args.length, 1);

        final String methodName = args[0].asJavaString();

        Class<?>[] argumentTypes = getArgumentTypes(context, args, 1);

        JavaCallable callable = getMatchingCallable(runtime, javaClass(), methodName, argumentTypes);

        if ( callable != null ) return callable;

        throw runtime.newNameError(undefinedMethodMessage(runtime, ids(runtime, methodName), ids(runtime, javaClass().getName()), false), methodName);
    }

    public static JavaCallable getMatchingCallable(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        if ( methodName.length() == 6 && "<init>".equals(methodName) ) {
            return JavaConstructor.getMatchingConstructor(runtime, javaClass, argumentTypes);
        }
        // FIXME: do we really want 'declared' methods?  includes private/protected, and does _not_
        // include superclass methods
        return JavaMethod.getMatchingDeclaredMethod(runtime, javaClass, methodName, argumentTypes);
    }

    public static Class<?>[] getArgumentTypes(final ThreadContext context, final IRubyObject[] args, final int offset) {
        final int length = args.length; // offset == 0 || 1
        if ( length == offset ) return EMPTY_CLASS_ARRAY;
        final Class<?>[] argumentTypes = new Class[length - offset];
        for ( int i = offset; i < length; i++ ) {
            final IRubyObject arg = args[i];
            argumentTypes[ i - offset ] = Java.resolveClassType(context, args[i]);
        }
        return argumentTypes;
    }

    // caching constructors, as they're accessed for each new instance
    private RubyArray constructors; // TODO seems not used that often?

    @JRubyMethod
    public RubyArray constructors() {
        final RubyArray constructors = this.constructors;
        if ( constructors != null) return constructors;
        return this.constructors = buildConstructors(getRuntime(), javaClass().getConstructors());
    }

    @JRubyMethod
    @SuppressWarnings("deprecation")
    public RubyArray classes() {
        return toRubyArray(getRuntime(), javaClass().getClasses());
    }

    @JRubyMethod
    public RubyArray declared_classes() {
        final Ruby runtime = getRuntime();
        final Class<?> javaClass = javaClass();
        try {
            Class<?>[] classes = javaClass.getDeclaredClasses();
            final RubyArray result = runtime.newArray(classes.length);
            for (int i = 0; i < classes.length; i++) {
                if (Modifier.isPublic(classes[i].getModifiers())) {
                    result.append( get(runtime, classes[i]) );
                }
            }
            return result;
        }
        catch (SecurityException e) {
            // restrictive security policy; no matter, we only want public
            // classes anyway
            try {
                Class<?>[] classes = javaClass.getClasses();
                final RubyArray result = runtime.newArray(classes.length);
                for (int i = 0; i < classes.length; i++) {
                    if (javaClass == classes[i].getDeclaringClass()) {
                        result.append( get(runtime, classes[i]) );
                    }
                }
                return result;
            }
            catch (SecurityException e2) {
                // very restrictive policy (disallows Member.PUBLIC)
                // we'd never actually get this far in that case
            }
        }
        return RubyArray.newEmptyArray(runtime);
    }

    @JRubyMethod
    public RubyArray declared_constructors() {
        return buildConstructors(getRuntime(), javaClass().getDeclaredConstructors());
    }

    private static RubyArray buildConstructors(final Ruby runtime, Constructor<?>[] constructors) {
        RubyArray result = RubyArray.newArray(runtime, constructors.length);
        for ( int i = 0; i < constructors.length; i++ ) {
            result.append(Java.getInstance(runtime, constructors[i]));
        }
        return result;
    }

    @JRubyMethod(rest = true)
    public JavaConstructor constructor(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        try {
            Class<?>[] parameterTypes = getArgumentTypes(context, args, 0);
            @SuppressWarnings("unchecked")
            Constructor<?> constructor = javaClass().getConstructor(parameterTypes);
            return new JavaConstructor(runtime, constructor);
        }
        catch (NoSuchMethodException nsme) {
            throw runtime.newNameError("no matching java constructor", (String) null);
        }
    }

    @JRubyMethod(rest = true)
    public JavaConstructor declared_constructor(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        try {
            Class<?>[] parameterTypes = getArgumentTypes(context, args, 0);
            @SuppressWarnings("unchecked")
            Constructor<?> constructor = javaClass().getDeclaredConstructor(parameterTypes);
            return new JavaConstructor(runtime, constructor);
        }
        catch (NoSuchMethodException nsme) {
            throw runtime.newNameError("no matching java constructor", (String) null);
        }
    }

    @JRubyMethod
    public JavaClass array_class() {
        final Class<?> arrayClass = Array.newInstance(javaClass(), 0).getClass();
        return JavaClass.get(getRuntime(), arrayClass);
    }

    @JRubyMethod(required = 1)
    public JavaObject new_array(IRubyObject lengthArgument) {
        if (lengthArgument instanceof RubyInteger) {
            // one-dimensional array
            int length = ((RubyInteger) lengthArgument).getIntValue();
            return new JavaArray(getRuntime(), Array.newInstance(javaClass(), length));
        }
        else if (lengthArgument instanceof RubyArray) {
            // n-dimensional array
            IRubyObject[] aryLengths = ((RubyArray)lengthArgument).toJavaArrayMaybeUnsafe();
            final int length = aryLengths.length;
            if (length == 0) {
                throw getRuntime().newArgumentError("empty dimensions specifier for java array");
            }
            final int[] dimensions = new int[length];
            for (int i = length; --i >= 0; ) {
                IRubyObject dimLength = aryLengths[i];
                if ( ! ( dimLength instanceof RubyInteger ) ) {
                    throw getRuntime().newTypeError(dimLength, getRuntime().getInteger());
                }
                dimensions[i] = ((RubyInteger) dimLength).getIntValue();
            }
            return new JavaArray(getRuntime(), Array.newInstance(javaClass(), dimensions));
        }
        else {
            throw getRuntime().newArgumentError(
                "invalid length or dimensions specifier for java array - must be Integer or Array of Integer");
        }
    }

    public IRubyObject emptyJavaArray(ThreadContext context) {
        return ArrayUtils.emptyJavaArrayDirect(context, javaClass());
    }

    /**
     * Contatenate two Java arrays into a new one. The component type of the
     * additional array must be assignable to the component type of the
     * original array.
     *
     * @param context
     * @param original
     * @param additional
     * @return
     */
    public IRubyObject concatArrays(ThreadContext context, JavaArray original, JavaArray additional) {
        return ArrayUtils.concatArraysDirect(context, original.getValue(), additional.getValue());
    }

    /**
     * The slow version for when concatenating a Java array of a different type.
     *
     * @param context
     * @param original
     * @param additional
     * @return
     */
    public IRubyObject concatArrays(ThreadContext context, JavaArray original, IRubyObject additional) {
        return ArrayUtils.concatArraysDirect(context, original.getValue(), additional);
    }

    @JRubyMethod
    public RubyArray fields() {
        return buildFieldResults(getRuntime(), javaClass().getFields());
    }

    @JRubyMethod
    public RubyArray declared_fields() {
        return buildFieldResults(getRuntime(), javaClass().getDeclaredFields());
    }

    private static RubyArray buildFieldResults(final Ruby runtime, Field[] fields) {
        RubyArray result = runtime.newArray( fields.length );
        for ( int i = 0; i < fields.length; i++ ) {
            result.append(Java.getInstance(runtime, fields[i]));
        }
        return result;
    }

    @JRubyMethod(required = 1)
    public JavaField field(ThreadContext context, IRubyObject name) {
        Class<?> javaClass = javaClass();
        Ruby runtime = context.runtime;
        String stringName = name.asJavaString();

        try {
            return new JavaField(runtime, javaClass.getField(stringName));
        } catch (NoSuchFieldException nsfe) {
            String newName = JavaUtil.getJavaCasedName(stringName);
            if(newName != null) {
                try {
                    return new JavaField(runtime, javaClass.getField(newName));
                } catch (NoSuchFieldException nsfe2) {}
            }
            throw undefinedFieldError(runtime, javaClass.getName(), stringName);
         }
    }

    @JRubyMethod(required = 1)
    public JavaField declared_field(ThreadContext context, IRubyObject name) {
        Class<?> javaClass = javaClass();
        Ruby runtime = context.runtime;
        String stringName = name.asJavaString();

        try {
            return new JavaField(runtime, javaClass.getDeclaredField(stringName));
        } catch (NoSuchFieldException nsfe) {
            String newName = JavaUtil.getJavaCasedName(stringName);
            if(newName != null) {
                try {
                    return new JavaField(runtime, javaClass.getDeclaredField(newName));
                } catch (NoSuchFieldException nsfe2) {}
            }
            throw undefinedFieldError(runtime, javaClass.getName(), stringName);
        }
    }

    public static RaiseException undefinedFieldError(Ruby runtime, String javaClassName, String name) {
        return runtime.newNameError("undefined field '" + name + "' for class '" + javaClassName + "'", name);
    }

    @JRubyMethod
    @SuppressWarnings("deprecation")
    public RubyArray interfaces() {
        return toRubyArray(getRuntime(), javaClass().getInterfaces());
    }

    @JRubyMethod(name = "primitive?")
    public RubyBoolean primitive_p() {
        return getRuntime().newBoolean( isPrimitive() );
    }

    boolean isPrimitive() { return javaClass().isPrimitive(); }

    @JRubyMethod(name = "assignable_from?", required = 1)
    public RubyBoolean assignable_from_p(IRubyObject other) {
        if ( ! (other instanceof JavaClass) ) {
            throw getRuntime().newTypeError("assignable_from requires JavaClass (" + other.getType() + " given)");
        }

        Class<?> otherClass = ((JavaClass) other).javaClass();
        return isAssignableFrom(otherClass) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public final boolean isAssignableFrom(final Class<?> clazz) {
        return assignable(javaClass(), clazz);
    }

    public static boolean assignable(Class<?> target, Class<?> from) {
        if ( target.isPrimitive() ) target = CodegenUtils.getBoxType(target);
        else if ( from == Void.TYPE || target.isAssignableFrom(from) ) {
            return true;
        }
        if ( from.isPrimitive() ) from = CodegenUtils.getBoxType(from);

        if ( target.isAssignableFrom(from) ) return true;

        if ( Number.class.isAssignableFrom(target) ) {
            if ( Number.class.isAssignableFrom(from) ) {
                return true;
            }
            if ( from == Character.class ) {
                return true;
            }
        }
        else if ( target == Character.class ) {
            if ( Number.class.isAssignableFrom(from) ) {
                return true;
            }
        }
        return false;
    }

    @JRubyMethod
    public JavaClass component_type() {
        if ( ! javaClass().isArray() ) {
            throw getRuntime().newTypeError("not a java array-class");
        }
        return JavaClass.get(getRuntime(), javaClass().getComponentType());
    }

    public static Constructor[] getConstructors(final Class<?> clazz) {
        try {
            return clazz.getConstructors();
        }
        catch (SecurityException e) { return new Constructor[0]; }
    }

    public static Class<?>[] getDeclaredClasses(final Class<?> clazz) {
        try {
            return clazz.getDeclaredClasses();
        }
        catch (SecurityException e) { return new Class<?>[0]; }
        catch (NoClassDefFoundError cnfe) {
            // This is a Scala-specific hack, since Scala uses peculiar
            // naming conventions and class attributes that confuse Java's
            // reflection logic and cause a blow up in getDeclaredClasses.
            // See http://lampsvn.epfl.ch/trac/scala/ticket/2749
            return new Class<?>[0];
        }
    }

    public static Field[] getDeclaredFields(final Class<?> clazz) {
        try {
            return clazz.getDeclaredFields();
        }
        catch (SecurityException e) {
            return getFields(clazz);
        }
    }

    public static Field[] getFields(final Class<?> clazz) {
        try {
            return clazz.getFields();
        }
        catch (SecurityException e) { return new Field[0]; }
    }

}
