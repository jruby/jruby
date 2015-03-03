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
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.util.ArrayUtils;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@JRubyClass(name="Java::JavaClass", parent="Java::JavaObject")
public class JavaClass extends JavaObject {

    // caching constructors, as they're accessed for each new instance
    private volatile RubyArray constructors;

    public RubyModule getProxyModule() {
        return Java.getProxyClass(getRuntime(), (Class)getValue());
    }

    public RubyClass getProxyClass() {
        return (RubyClass)Java.getProxyClass(getRuntime(), (Class)getValue());
    }

    public JavaClass(final Ruby runtime, final Class<?> javaClass) {
        super(runtime, runtime.getJavaSupport().getJavaClassClass(), javaClass);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaClass &&
            this.getValue() == ((JavaClass) other).getValue();
    }

    @Override
    public int hashCode() {
        return javaClass().hashCode();
    }

    public void addProxyExtender(final IRubyObject extender) {
        Ruby runtime = getRuntime();

        if (!extender.respondsTo("extend_proxy")) {
            throw runtime.newTypeError("proxy extender must have an extend_proxy method");
        }

        ThreadContext context = runtime.getCurrentContext();
        RubyModule proxy = Java.getProxyClass(runtime, javaClass());
        extendProxy(context, extender, proxy);
    }

    private void extendProxy(final ThreadContext context, final IRubyObject extender, final RubyModule proxy) {
        extender.callMethod(context, "extend_proxy", proxy);
    }

    @JRubyMethod(required = 1)
    public IRubyObject extend_proxy(final ThreadContext context, IRubyObject extender) {
        addProxyExtender(extender);
        return getRuntime().getNil();
    }

    public static JavaClass get(final Ruby runtime, final Class<?> klass) {
        return runtime.getJavaSupport().getJavaClassFromCache(klass);
    }

    @Deprecated // only been used package internally - a bit poorly named
    public static RubyArray getRubyArray(Ruby runtime, Class<?>[] classes) {
        return toRubyArray(runtime, classes);
    }

    static RubyArray toRubyArray(final Ruby runtime, final Class<?>[] classes) {
        IRubyObject[] javaClasses = new IRubyObject[classes.length];
        for ( int i = classes.length; --i >= 0; ) {
            javaClasses[i] = get(runtime, classes[i]);
        }
        return RubyArray.newArrayNoCopy(runtime, javaClasses);
    }

    public static RubyClass createJavaClassClass(Ruby runtime, RubyModule javaModule) {
        // FIXME: Determine if a real allocator is needed here. Do people want to extend
        // JavaClass? Do we want them to do that? Can you Class.new(JavaClass)? Should
        // you be able to?
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result = javaModule.defineClassUnder("JavaClass", javaModule.getClass("JavaObject"), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        result.includeModule(runtime.getModule("Comparable"));

        result.defineAnnotatedMethods(JavaClass.class);

        result.getMetaClass().undefineMethod("new");
        result.getMetaClass().undefineMethod("allocate");

        return result;
    }

    public Class javaClass() {
        return (Class) getValue();
    }

    public static Class<?> getJavaClass(final ThreadContext context, final RubyModule proxy) {
        final IRubyObject javaClass = Helpers.invoke(context, proxy, "java_class");
        return ((JavaClass) javaClass).javaClass();
    }

    public static Class<?> getJavaClassIfProxy(final ThreadContext context, final RubyModule proxy) {
        final IRubyObject javaClass;
        try {
            javaClass = Helpers.invoke(context, proxy, "java_class");
        }
        catch (RuntimeException e) {
            // clear $! since our "java_class" invoke above may have failed and set it
            context.setErrorInfo(context.nil);
            return null;
        }
        return ( javaClass instanceof JavaClass ) ? ((JavaClass) javaClass).javaClass() : null;
    }

    static boolean isPrimitiveName(final String name) {
        return JavaUtil.PRIMITIVE_CLASSES.containsKey(name);
    }

    public static synchronized JavaClass forNameVerbose(Ruby runtime, String className) {
        Class <?> klass = null;
        if (className.indexOf('.') == -1 && Character.isLowerCase(className.charAt(0))) {
            // one word type name that starts lower-case...it may be a primitive type
            klass = JavaUtil.PRIMITIVE_CLASSES.get(className);
        }

        if (klass == null) {
            klass = runtime.getJavaSupport().loadJavaClassVerbose(className);
        }
        return JavaClass.get(runtime, klass);
    }

    public static synchronized JavaClass forNameQuiet(Ruby runtime, String className) {
        Class klass = runtime.getJavaSupport().loadJavaClassQuiet(className);
        return JavaClass.get(runtime, klass);
    }

    @JRubyMethod(name = "for_name", required = 1, meta = true)
    public static JavaClass for_name(IRubyObject recv, IRubyObject name) {
        return forNameVerbose(recv.getRuntime(), name.asJavaString());
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

    @JRubyMethod
    @Override
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
    public IRubyObject annotation(IRubyObject annoClass) {
        if (!(annoClass instanceof JavaClass)) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        return Java.getInstance(getRuntime(), javaClass().getAnnotation(((JavaClass) annoClass).javaClass()));
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
    public IRubyObject annotation_present_p(IRubyObject annoClass) {
        if (!(annoClass instanceof JavaClass)) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        return getRuntime().newBoolean(javaClass().isAnnotationPresent(((JavaClass)annoClass).javaClass()));
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
            return new JavaConstructor(getRuntime(), ctor);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject enclosing_method() {
        Method meth = javaClass().getEnclosingMethod();
        if (meth != null) {
            return new JavaMethod(getRuntime(), meth);
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
        Class me = javaClass();
        Class them = null;

        // dig out the other class
        if (other instanceof JavaClass) {
            JavaClass otherClass = (JavaClass) other;
            them = otherClass.javaClass();
        } else if (other instanceof ConcreteJavaProxy) {
            ConcreteJavaProxy proxy = (ConcreteJavaProxy)other;
            if (proxy.getObject() instanceof Class) {
                them = (Class)proxy.getObject();
            }
        }

        if (them != null) {
            if (this.javaClass() == them) {
                return getRuntime().newFixnum(0);
            }
            if (them.isAssignableFrom(me)) {
                return getRuntime().newFixnum(-1);
            }
            if (me.isAssignableFrom(them)) {
                return getRuntime().newFixnum(1);
            }
        }

        // can't do a comparison
        return getRuntime().getNil();
    }

    @JRubyMethod
    public RubyArray java_instance_methods() {
        return java_methods(javaClass().getMethods(), false);
    }

    @JRubyMethod
    public RubyArray declared_instance_methods() {
        return java_methods(javaClass().getDeclaredMethods(), false);
    }

    private RubyArray java_methods(final Method[] methods, final boolean isStatic) {
        final Ruby runtime = getRuntime();
        final RubyArray result = runtime.newArray(methods.length);
        for ( int i = 0; i < methods.length; i++ ) {
            final Method method = methods[i];
            if ( isStatic == Modifier.isStatic(method.getModifiers()) ) {
                result.append( JavaMethod.create(runtime, method) );
            }
        }
        return result;
    }

    @JRubyMethod
    public RubyArray java_class_methods() {
        return java_methods(javaClass().getMethods(), true);
    }

    @JRubyMethod
    public RubyArray declared_class_methods() {
        return java_methods(javaClass().getDeclaredMethods(), true);
    }

    @JRubyMethod(required = 1, rest = true)
    public JavaMethod java_method(IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = buildArgumentTypes(runtime, args);
            return JavaMethod.create(runtime, javaClass(), methodName, argumentTypes);
        }
        catch (ClassNotFoundException cnfe) {
            throw runtime.newNameError("undefined method '" + methodName +
                "' for class '" + javaClass().getName() + "'", methodName);
        }

    }

    @JRubyMethod(required = 1, rest = true)
    public JavaMethod declared_method(final IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = buildArgumentTypes(runtime, args);
            return JavaMethod.createDeclared(runtime, javaClass(), methodName, argumentTypes);
        }
        catch (ClassNotFoundException cnfe) {
            throw runtime.newNameError("undefined method '" + methodName +
                "' for class '" + javaClass().getName() + "'", methodName);
        }
    }

    @JRubyMethod(required = 1, rest = true)
    public JavaCallable declared_method_smart(final IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = buildArgumentTypes(runtime, args);

            JavaCallable callable = getMatchingCallable(runtime, javaClass(), methodName, argumentTypes);

            if ( callable != null ) return callable;
        }
        catch (ClassNotFoundException cnfe) {
            /* fall through to error below */
        }

        throw runtime.newNameError("undefined method '" + methodName +
            "' for class '" + javaClass().getName() + "'", methodName);
    }

    public static JavaCallable getMatchingCallable(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        if ( "<init>".equals(methodName) ) {
            return JavaConstructor.getMatchingConstructor(runtime, javaClass, argumentTypes);
        }
        // FIXME: do we really want 'declared' methods?  includes private/protected, and does _not_
        // include superclass methods
        return JavaMethod.getMatchingDeclaredMethod(runtime, javaClass, methodName, argumentTypes);
    }

    private static Class<?>[] buildArgumentTypes(final Ruby runtime,
        final IRubyObject[] args) throws ClassNotFoundException {
        if ( args.length < 1 ) throw runtime.newArgumentError(args.length, 1);

        Class<?>[] argumentTypes = new Class[args.length - 1];
        for ( int i = 1; i < args.length; i++ ) {
            final IRubyObject arg = args[i];
            final JavaClass type;
            if ( arg instanceof JavaClass ) {
                type = (JavaClass) arg;
            } else if ( arg.respondsTo("java_class") ) {
                type = (JavaClass) arg.callMethod(runtime.getCurrentContext(), "java_class");
            } else {
                type = forNameVerbose(runtime, arg.asJavaString());
            }
            argumentTypes[ i - 1 ] = type.javaClass();
        }
        return argumentTypes;
    }

    @JRubyMethod
    public RubyArray constructors() {
        final RubyArray constructors = this.constructors;
        if ( constructors != null) return constructors;
        return this.constructors = buildConstructors(getRuntime(), javaClass().getConstructors());
    }

    @JRubyMethod
    public RubyArray classes() {
        return toRubyArray(getRuntime(), javaClass().getClasses());
    }

    @JRubyMethod
    public RubyArray declared_classes() {
        final Ruby runtime = getRuntime();
        RubyArray result = runtime.newArray();
        Class<?> javaClass = javaClass();
        try {
            Class<?>[] classes = javaClass.getDeclaredClasses();
            for (int i = 0; i < classes.length; i++) {
                if (Modifier.isPublic(classes[i].getModifiers())) {
                    result.append(get(runtime, classes[i]));
                }
            }
        } catch (SecurityException e) {
            // restrictive security policy; no matter, we only want public
            // classes anyway
            try {
                Class<?>[] classes = javaClass.getClasses();
                for (int i = 0; i < classes.length; i++) {
                    if (javaClass == classes[i].getDeclaringClass()) {
                        result.append(get(runtime, classes[i]));
                    }
                }
            } catch (SecurityException e2) {
                // very restrictive policy (disallows Member.PUBLIC)
                // we'd never actually get this far in that case
            }
        }
        return result;
    }

    @JRubyMethod
    public RubyArray declared_constructors() {
        return buildConstructors(getRuntime(), javaClass().getDeclaredConstructors());
    }

    private static RubyArray buildConstructors(final Ruby runtime, Constructor<?>[] constructors) {
        RubyArray result = RubyArray.newArray(runtime, constructors.length);
        for ( int i = 0; i < constructors.length; i++ ) {
            result.append( new JavaConstructor(runtime, constructors[i]) );
        }
        return result;
    }

    @JRubyMethod(rest = true)
    public JavaConstructor constructor(IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        try {
            Class<?>[] parameterTypes = buildClassArgs(runtime, args);
            @SuppressWarnings("unchecked")
            Constructor<?> constructor = javaClass().getConstructor(parameterTypes);
            return new JavaConstructor(runtime, constructor);
        }
        catch (NoSuchMethodException nsme) {
            throw runtime.newNameError("no matching java constructor", null);
        }
    }

    @JRubyMethod(rest = true)
    public JavaConstructor declared_constructor(IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        try {
            Class<?>[] parameterTypes = buildClassArgs(runtime, args);
            @SuppressWarnings("unchecked")
            Constructor<?> constructor = javaClass().getDeclaredConstructor(parameterTypes);
            return new JavaConstructor(runtime, constructor);
        }
        catch (NoSuchMethodException nsme) {
            throw runtime.newNameError("no matching java constructor", null);
        }
    }

    private static Class<?>[] buildClassArgs(final Ruby runtime, IRubyObject[] args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for ( int i = 0; i < args.length; i++ ) {
            final IRubyObject arg = args[i];
            final JavaClass type;
            if ( arg instanceof JavaClass ) {
                type = (JavaClass) arg;
            } else if ( arg.respondsTo("java_class") ) {
                type = (JavaClass) arg.callMethod(runtime.getCurrentContext(), "java_class");
            } else {
                type = forNameVerbose(runtime, arg.asJavaString());
            }
            parameterTypes[i] = type.javaClass();
        }
        return parameterTypes;
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
            int length = (int) ((RubyInteger) lengthArgument).getLongValue();
            return new JavaArray(getRuntime(), Array.newInstance(javaClass(), length));
        } else if (lengthArgument instanceof RubyArray) {
            // n-dimensional array
            List list = ((RubyArray)lengthArgument).getList();
            int length = list.size();
            if (length == 0) {
                throw getRuntime().newArgumentError("empty dimensions specifier for java array");
            }
            int[] dimensions = new int[length];
            for (int i = length; --i >= 0; ) {
                IRubyObject dimensionLength = (IRubyObject)list.get(i);
                if ( !(dimensionLength instanceof RubyInteger) ) {
                    throw getRuntime()
                    .newTypeError(dimensionLength, getRuntime().getInteger());
                }
                dimensions[i] = (int) ((RubyInteger) dimensionLength).getLongValue();
            }
            return new JavaArray(getRuntime(), Array.newInstance(javaClass(), dimensions));
        } else {
            throw getRuntime().newArgumentError(
                    "invalid length or dimensions specifier for java array" +
            " - must be Integer or Array of Integer");
        }
    }

    public IRubyObject emptyJavaArray(ThreadContext context) {
        return ArrayUtils.emptyJavaArrayDirect(context, javaClass());
    }

    public IRubyObject javaArraySubarray(ThreadContext context, JavaArray fromArray, int index, int size) {
        return ArrayUtils.javaArraySubarrayDirect(context, getValue(), index, size);
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

    public IRubyObject javaArrayFromRubyArray(ThreadContext context, IRubyObject fromArray) {
        Ruby runtime = context.runtime;
        if (!(fromArray instanceof RubyArray)) {
            throw runtime.newTypeError(fromArray, runtime.getArray());
        }

        Object newArray = javaArrayFromRubyArrayDirect(context, fromArray);

        return new ArrayJavaProxy(runtime, Java.getProxyClassForObject(runtime, newArray), newArray, JavaUtil.getJavaConverter(javaClass()));
    }

    public Object javaArrayFromRubyArrayDirect(ThreadContext context, IRubyObject fromArray) {
        Ruby runtime = context.runtime;
        if (!(fromArray instanceof RubyArray)) {
            throw runtime.newTypeError(fromArray, runtime.getArray());
        }
        RubyArray rubyArray = (RubyArray)fromArray;
        Object newArray = Array.newInstance(javaClass(), rubyArray.size());

        if (javaClass().isArray()) {
            // if it's an array of arrays, recurse with the component type
            for (int i = 0; i < rubyArray.size(); i++) {
                JavaClass componentType = component_type();
                Object componentArray = componentType.javaArrayFromRubyArrayDirect(context, rubyArray.eltInternal(i));
                ArrayUtils.setWithExceptionHandlingDirect(runtime, newArray, i, componentArray);
            }
        } else {
            ArrayUtils.copyDataToJavaArrayDirect(context, rubyArray, newArray);
        }

        return newArray;
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
            result.append( new JavaField(runtime, fields[i]) );
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
    public RubyArray interfaces() {
        return toRubyArray(getRuntime(), javaClass().getInterfaces());
    }

    @JRubyMethod(name = "primitive?")
    public RubyBoolean primitive_p() {
        return getRuntime().newBoolean(isPrimitive());
    }

    boolean isPrimitive() { return javaClass().isPrimitive(); }

    @JRubyMethod(name = "assignable_from?", required = 1)
    public RubyBoolean assignable_from_p(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw getRuntime().newTypeError("assignable_from requires JavaClass (" + other.getType() + " given)");
        }

        Class<?> otherClass = ((JavaClass) other).javaClass();
        return assignable(javaClass(), otherClass) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public static boolean assignable(Class<?> thisClass, Class<?> otherClass) {
        if(!thisClass.isPrimitive() && otherClass == Void.TYPE ||
            thisClass.isAssignableFrom(otherClass)) {
            return true;
        }

        otherClass = JavaUtil.primitiveToWrapper(otherClass);
        thisClass = JavaUtil.primitiveToWrapper(thisClass);

        if(thisClass.isAssignableFrom(otherClass)) {
            return true;
        }
        if(Number.class.isAssignableFrom(thisClass)) {
            if(Number.class.isAssignableFrom(otherClass)) {
                return true;
            }
            if(otherClass.equals(Character.class)) {
                return true;
            }
        }
        if(thisClass.equals(Character.class)) {
            if(Number.class.isAssignableFrom(otherClass)) {
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

    public static Constructor[] getConstructors(Class<?> javaClass) {
        try {
            return javaClass.getConstructors();
        }
        catch (SecurityException e) { return new Constructor[0]; }
    }

    public static Class<?>[] getDeclaredClasses(Class<?> javaClass) {
        try {
            return javaClass.getDeclaredClasses();
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

    public static Field[] getDeclaredFields(Class<?> javaClass) {
        try {
            return javaClass.getDeclaredFields();
        }
        catch (SecurityException e) {
            return getFields(javaClass);
        }
    }

    public static Field[] getFields(Class<?> javaClass) {
        try {
            return javaClass.getFields();
        }
        catch (SecurityException e) { return new Field[0]; }
    }

    @Deprecated
    public static final boolean CAN_SET_ACCESSIBLE = JavaUtil.CAN_SET_ACCESSIBLE;
}
