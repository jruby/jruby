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
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.util.ArrayUtils;
import org.jruby.java.util.ClassUtils;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.jruby.api.Create.newString;

/**
 * Java::JavaClass wrapping is no longer used with JRuby.
 * The (automatic) Java proxy wrapping works with Java classes, use the <code>java.lang.Class</code> with JRuby's
 * Java scripting capabilities.
 *
 * @deprecated since 9.3
 * @author  jpetersen
 */
@Deprecated
public class JavaClass extends JavaObject {

    public static final Class[] EMPTY_CLASS_ARRAY = ClassUtils.EMPTY_CLASS_ARRAY;

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
        return (RubyClass) getProxyModule();
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

    public final Class javaClass() {
        return (Class<?>) getValue();
    }

    /**
     * @see JavaUtil#getJavaClass(ThreadContext, RubyModule)
     */
    @Deprecated // no longer used
    public static Class<?> getJavaClass(final ThreadContext context, final RubyModule proxy) {
        return JavaUtil.getJavaClass(proxy, null);
    }

    /**
     * @see JavaUtil#getJavaClass(RubyModule, Supplier)
     */
    @Deprecated // no longer used
    public static Class<?> getJavaClassIfProxy(final ThreadContext context, final RubyModule type) {
        return JavaUtil.getJavaClass(type, null);
    }

    /**
     * <p>Note: Interal API</p>
     * @see Java#isProxyType(RubyModule)
     */
    public static boolean isProxyType(final ThreadContext context, final RubyModule proxy) {
        return JavaUtil.getJavaClass(proxy, null) != null;
    }

    /**
     * Returns the (reified or proxied) Java class if the passed Ruby module/class has one.
     * @param context
     * @param type
     * @return Java proxy class, Java reified class or nil
     */
    @Deprecated // not used
    public static IRubyObject java_class(final ThreadContext context, final RubyModule type) {
        IRubyObject java_class = type.getInstanceVariable("@java_class");
        if (java_class == null) {
            if ( type.respondsTo("java_class") ) { // NOTE: quite bad since built-in Ruby classes will return
                // a Ruby Java proxy for java.lang.Class while Java proxies will return a JavaClass instance !
                java_class = Helpers.invoke(context, type, "java_class");
            } else {
                java_class = JavaProxy.getJavaClass(type);
                if (java_class == null) java_class = context.nil;
            }
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
        return proxyClass == null ? null : get(context.runtime, JavaUtil.getJavaClass(proxyClass, null));
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

    @Deprecated
    @JRubyMethod(name = "for_name", meta = true)
    public static JavaClass for_name(IRubyObject recv, IRubyObject name) {
        return for_name(recv, name.asJavaString());
    }

    static JavaClass for_name(IRubyObject recv, String name) {
        return forNameVerbose(recv.getRuntime(), name);
    }

    @Override
    public RubyString inspect(ThreadContext context) {
        return newString(context, "class " + javaClass().getName());
    }

    public static String getSimpleName(Class<?> clazz) {
        return ClassUtils.getSimpleName(clazz);
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
        return ClassUtils.getArgumentTypes(context, args, offset);
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
    @Deprecated
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
    @Deprecated
    public IRubyObject concatArrays(ThreadContext context, JavaArray original, IRubyObject additional) {
        return ArrayUtils.concatArraysDirect(context, original.getValue(), additional);
    }

    public final boolean isAssignableFrom(final Class<?> clazz) {
        return ClassUtils.assignable(javaClass(), clazz);
    }

    public static boolean assignable(Class<?> target, Class<?> from) {
        return ClassUtils.assignable(target, from);
    }

    public static Constructor[] getConstructors(final Class<?> clazz) {
        return ClassUtils.getConstructors(clazz);
    }

    public static Class<?>[] getDeclaredClasses(final Class<?> clazz) {
        return ClassUtils.getDeclaredClasses(clazz);
    }

    public static Field[] getDeclaredFields(final Class<?> clazz) {
        return ClassUtils.getDeclaredFields(clazz);
    }

    public static Field[] getFields(final Class<?> clazz) {
        return ClassUtils.getFields(clazz);
    }

}
