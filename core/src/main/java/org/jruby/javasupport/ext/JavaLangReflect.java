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
 * Copyright (C) 2016 The JRuby Team
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

package org.jruby.javasupport.ext;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyStringBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.javasupport.JavaUtil.convertArguments;
import static org.jruby.javasupport.JavaUtil.convertJavaToUsableRubyObject;
import static org.jruby.util.Inspector.GT;
import static org.jruby.util.Inspector.inspectPrefix;

/**
 * Java::JavaLangReflect package extensions.
 *
 * @author kares
 */
public abstract class JavaLangReflect {

    public static void define(ThreadContext context) {
        var runtime = context.runtime;
        JavaExtensions.put(runtime, java.lang.reflect.AccessibleObject.class, proxy -> proxy.defineMethods(context, AccessibleObject.class));
        JavaExtensions.put(runtime, java.lang.reflect.Constructor.class, proxy -> proxy.defineMethods(context, Constructor.class));
        JavaExtensions.put(runtime, java.lang.reflect.Field.class, proxy -> proxy.defineMethods(context, Field.class));
        JavaExtensions.put(runtime, java.lang.reflect.Method.class, proxy -> proxy.defineMethods(context, Method.class));
    }

    @JRubyClass(name = "Java::JavaLangReflect::AccessibleObject")
    public static class AccessibleObject {
        @JRubyMethod
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.AccessibleObject obj = JavaUtil.unwrapJavaObject(self);

            RubyString buf = inspectPrefix(context, self.getMetaClass());
            RubyStringBuilder.cat(context.runtime, buf, ' ');
            RubyStringBuilder.cat(context.runtime, buf, obj.toString());
            RubyStringBuilder.cat(context.runtime, buf, GT); // >

            return buf;
        }
    }

    @JRubyClass(name = "Java::JavaLangReflect::Constructor")
    public static class Constructor {
        @JRubyMethod
        public static IRubyObject return_type(final ThreadContext context, final IRubyObject self) {
            return context.nil;
        }

        @JRubyMethod // alias argument_types parameter_types
        public static IRubyObject argument_types(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Constructor<?> thiz = JavaUtil.unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, thiz.getParameterTypes());
        }

        // NOTE: (legacy) JavaConstructor compat - converting arguments
        @JRubyMethod(name = {"newInstance", "new_instance"}, rest = true)
        public static IRubyObject new_instance(final ThreadContext context, final IRubyObject self, final IRubyObject[] args) {
            final java.lang.reflect.Constructor<?> thiz = JavaUtil.unwrapJavaObject(self);
            final Object[] javaArgs;
            if (args.length == 0) {
                javaArgs = NO_ARGS;
            } else {
                javaArgs = convertArguments(args, thiz.getParameterTypes(), 0);
            }
            try {
                return convertJavaToUsableRubyObject(context.runtime, thiz.newInstance(javaArgs));
            }
            catch (IllegalAccessException|InvocationTargetException|InstantiationException e) {
                Helpers.throwException(e); return null;
            }
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Constructor<?> thiz = JavaUtil.unwrapJavaObject(self);
            return isPublic(context, self, thiz.getModifiers());
        }

        @Deprecated(since = "9.4")
        public static IRubyObject public_p(final IRubyObject self) {
            return public_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Constructor<?> thiz = JavaUtil.unwrapJavaObject(self);
            return isProtected(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject protected_p(final IRubyObject self) {
            return protected_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Constructor<?> thiz = JavaUtil.unwrapJavaObject(self);
            return isPrivate(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject private_p(final IRubyObject self) {
            return private_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Constructor<?> thiz = JavaUtil.unwrapJavaObject(self);
            return isFinal(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject final_p(final IRubyObject self) {
            return final_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Constructor<?> thiz = JavaUtil.unwrapJavaObject(self);
            return isStatic(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject static_p(final IRubyObject self) {
            return static_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

    }

    private static final Object[] NO_ARGS = new Object[0];

    @JRubyClass(name = "Java::JavaLangReflect::Method")
    public static class Method {
        @JRubyMethod
        public static IRubyObject return_type(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = JavaUtil.unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, thiz.getReturnType());
        }

        @JRubyMethod // alias argument_types parameter_types
        public static IRubyObject argument_types(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = JavaUtil.unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, thiz.getParameterTypes());
        }

        @JRubyMethod(rest = true)
        public static IRubyObject invoke(final ThreadContext context, final IRubyObject self, final IRubyObject[] args) {
            final java.lang.reflect.Method method = JavaUtil.unwrapJavaObject(self);
            // NOTE: (legacy) JavaMethod compat - also worked with no arguments
            final Object target;
            final Object[] javaArgs;
            if (args.length == 0) {
                target = null;
                javaArgs = NO_ARGS;
            } else {
                target = unwrapJavaObject(args[0]);
                javaArgs = convertArguments(args, method.getParameterTypes(), 1);
            }
            try {
                return convertJavaToUsableRubyObject(context.runtime, method.invoke(target, javaArgs));
            }
            catch (IllegalAccessException|InvocationTargetException e) {
                Helpers.throwException(e); return null;
            }
        }

        @JRubyMethod(rest = true)
        public static IRubyObject invoke_static(final ThreadContext context, final IRubyObject self, final IRubyObject[] args) {
            final java.lang.reflect.Method method = JavaUtil.unwrapJavaObject(self);
            final Object[] javaArgs = convertArguments(args, method.getParameterTypes());
            try {
                return convertJavaToUsableRubyObject(context.runtime, method.invoke(null, javaArgs));
            }
            catch (IllegalAccessException|InvocationTargetException e) {
                Helpers.throwException(e); return null;
            }
        }

        //

        @JRubyMethod(name = "abstract?")
        public static IRubyObject abstract_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field thiz = JavaUtil.unwrapJavaObject(self);
            return isAbstract(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject abstract_p(final IRubyObject self) {
            return abstract_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = JavaUtil.unwrapJavaObject(self);
            return isPublic(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject public_p(final IRubyObject self) {
            return public_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = JavaUtil.unwrapJavaObject(self);
            return isProtected(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject protected_p(final IRubyObject self) {
            return protected_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = JavaUtil.unwrapJavaObject(self);
            return isPrivate(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject private_p(final IRubyObject self) {
            return private_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = JavaUtil.unwrapJavaObject(self);
            return isFinal(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject final_p(final IRubyObject self) {
            return final_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = JavaUtil.unwrapJavaObject(self);
            return isStatic(context, self, thiz.getModifiers());
        }

        @Deprecated(since = "10.0")
        public static IRubyObject static_p(final IRubyObject self) {
            return static_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

    }

    @JRubyClass(name = "Java::JavaLangReflect::Field")
    public static class Field {
        @JRubyMethod
        public static IRubyObject value_type(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field field = JavaUtil.unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, field.getType().getName());
        }

        @JRubyMethod // alias value get
        public static IRubyObject value(final ThreadContext context, final IRubyObject self, final IRubyObject obj) {
            final java.lang.reflect.Field field = JavaUtil.unwrapJavaObject(self);
            // NOTE: (legacy) JavaField compat - also worked when setting a static value
            final Object target = Modifier.isStatic(field.getModifiers()) ? null : unwrapJavaObject(obj);
            try {
                return convertJavaToUsableRubyObject(context.runtime, field.get(target));
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
        }

        @JRubyMethod // alias set_value set
        public static IRubyObject set_value(final ThreadContext context, final IRubyObject self, final IRubyObject obj,
            final IRubyObject value) {
            final java.lang.reflect.Field field = JavaUtil.unwrapJavaObject(self);
            // NOTE: (legacy) JavaField compat - also worked when setting a static value
            final Object target = Modifier.isStatic(field.getModifiers()) ? null : unwrapJavaObject(obj);
            final Object javaValue = convertValueToJava(field, value);
            try {
                field.set(target, javaValue);
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
            return context.nil;
        }

        @JRubyMethod
        public static IRubyObject static_value(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field field = JavaUtil.unwrapJavaObject(self);
            try {
                return convertJavaToUsableRubyObject(context.runtime, field.get(null));
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
        }

        @JRubyMethod
        public static IRubyObject set_static_value(final ThreadContext context, final IRubyObject self, final IRubyObject value) {
            final java.lang.reflect.Field field = JavaUtil.unwrapJavaObject(self);
            final Object javaValue = convertValueToJava(field, value);
            try {
                field.set(null, javaValue);
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
            return context.nil;
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field thiz = JavaUtil.unwrapJavaObject(self);
            return isPublic(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject public_p(final IRubyObject self) {
            return public_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field thiz = JavaUtil.unwrapJavaObject(self);
            return isProtected(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject protected_p(final IRubyObject self) {
            return protected_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field thiz = JavaUtil.unwrapJavaObject(self);
            return isPrivate(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject private_p(final IRubyObject self) {
            return private_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field thiz = JavaUtil.unwrapJavaObject(self);
            return isFinal(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject final_p(final IRubyObject self) {

            return final_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field thiz = JavaUtil.unwrapJavaObject(self);
            return isStatic(context, self, thiz.getModifiers());
        }

        @Deprecated
        public static IRubyObject static_p(final IRubyObject self) {
            return static_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

    }

    private static Object unwrapJavaObject(final IRubyObject object) {
        return JavaUtil.unwrapJavaValue(object);
    }

    private static Object convertValueToJava(final java.lang.reflect.Field field, IRubyObject value) {
        return value.toJava(field.getType());
    }

    static RubyBoolean isAbstract(ThreadContext context, final IRubyObject self, final int mod) {
        return asBoolean(context, java.lang.reflect.Modifier.isAbstract(mod));
    }

    static RubyBoolean isPublic(ThreadContext context, final IRubyObject self, final int mod) {
        return asBoolean(context, java.lang.reflect.Modifier.isPublic(mod));
    }

    static RubyBoolean isProtected(ThreadContext context, final IRubyObject self, final int mod) {
        return asBoolean(context, java.lang.reflect.Modifier.isProtected(mod));
    }

    static RubyBoolean isPrivate(ThreadContext context, final IRubyObject self, final int mod) {
        return asBoolean(context, java.lang.reflect.Modifier.isPrivate(mod));
    }

    static RubyBoolean isFinal(ThreadContext context, final IRubyObject self, final int mod) {
        return asBoolean(context, java.lang.reflect.Modifier.isFinal(mod));
    }

    static RubyBoolean isStatic(ThreadContext context, final IRubyObject self, final int mod) {
        return asBoolean(context, java.lang.reflect.Modifier.isStatic(mod));
    }

}
