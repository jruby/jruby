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
import org.jruby.javasupport.Java;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.InvocationTargetException;

import static org.jruby.javasupport.JavaUtil.convertArguments;
import static org.jruby.javasupport.JavaUtil.convertJavaToUsableRubyObject;
import static org.jruby.javasupport.JavaUtil.unwrapJavaObject;

/**
 * Java::JavaLangReflect package extensions.
 *
 * @author kares
 */
public abstract class JavaLangReflect {

    public static void define(final Ruby runtime) {
        Constructor.define(runtime);
        Field.define(runtime);
        Method.define(runtime);
    }

    @JRubyClass(name = "Java::JavaLangReflect::Constructor")
    public static class Constructor {

        static RubyClass define(final Ruby runtime) {
            final RubyModule Constructor = Java.getProxyClass(runtime, java.lang.reflect.Constructor.class);
            Constructor.defineAnnotatedMethods(Constructor.class);
            return (RubyClass) Constructor;
        }

        @JRubyMethod
        public static IRubyObject return_type(final ThreadContext context, final IRubyObject self) {
            return context.nil;
        }

        @JRubyMethod // alias argument_types parameter_types
        public static IRubyObject argument_types(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Constructor thiz = unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, thiz.getParameterTypes());
        }

        //

        @JRubyMethod
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.AccessibleObject thiz = unwrapJavaObject(self);
            return RubyString.newString(context.runtime, thiz.toString());
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(final IRubyObject self) {
            final java.lang.reflect.Constructor thiz = unwrapJavaObject(self);
            return isPublic(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(final IRubyObject self) {
            final java.lang.reflect.Constructor thiz = unwrapJavaObject(self);
            return isProtected(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(final IRubyObject self) {
            final java.lang.reflect.Constructor thiz = unwrapJavaObject(self);
            return isPrivate(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(final IRubyObject self) {
            final java.lang.reflect.Constructor thiz = unwrapJavaObject(self);
            return isFinal(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(final IRubyObject self) {
            final java.lang.reflect.Constructor thiz = unwrapJavaObject(self);
            return isStatic(self, thiz.getModifiers());
        }

    }

    @JRubyClass(name = "Java::JavaLangReflect::Method")
    public static class Method {

        static RubyClass define(final Ruby runtime) {
            final RubyModule Method = Java.getProxyClass(runtime, java.lang.reflect.Method.class);
            Method.defineAnnotatedMethods(Method.class);
            return (RubyClass) Method;
        }

        @JRubyMethod
        public static IRubyObject return_type(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, thiz.getReturnType());
        }

        @JRubyMethod // alias argument_types parameter_types
        public static IRubyObject argument_types(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, thiz.getParameterTypes());
        }

        @JRubyMethod(rest = true)
        public static IRubyObject invoke_static(final ThreadContext context, final IRubyObject self, final IRubyObject[] args) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            final Object[] javaArgs = convertArguments(args, thiz.getParameterTypes());
            try {
                return convertJavaToUsableRubyObject(context.runtime, thiz.invoke(null, javaArgs));
            }
            catch (IllegalAccessException|InvocationTargetException e) {
                Helpers.throwException(e); return null;
            }
        }

        //

        @JRubyMethod
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.AccessibleObject thiz = unwrapJavaObject(self);
            return RubyString.newString(context.runtime, thiz.toString());
        }

        @JRubyMethod(name = "abstract?")
        public static IRubyObject abstract_p(final IRubyObject self) {
            final java.lang.reflect.Field thiz = unwrapJavaObject(self);
            return isAbstract(self, thiz.getModifiers());
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(final IRubyObject self) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            return isPublic(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(final IRubyObject self) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            return isProtected(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(final IRubyObject self) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            return isPrivate(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(final IRubyObject self) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            return isFinal(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(final IRubyObject self) {
            final java.lang.reflect.Method thiz = unwrapJavaObject(self);
            return isStatic(self, thiz.getModifiers());
        }

    }

    @JRubyClass(name = "Java::JavaLangReflect::Field")
    public static class Field {

        static RubyClass define(final Ruby runtime) {
            final RubyModule Field = Java.getProxyClass(runtime, java.lang.reflect.Field.class);
            Field.defineAnnotatedMethods(Field.class);
            return (RubyClass) Field;
        }

        @JRubyMethod // alias value_type name
        public static IRubyObject value_type(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field field = unwrapJavaObject(self);
            return convertJavaToUsableRubyObject(context.runtime, field.getName());
        }

        @JRubyMethod // alias value get
        public static IRubyObject value(final ThreadContext context, final IRubyObject self, final IRubyObject obj) {
            final java.lang.reflect.Field field = unwrapJavaObject(self);
            try {
                return convertJavaToUsableRubyObject(context.runtime, field.get(unwrapJavaObject(obj)));
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
        }

        @JRubyMethod // alias set_value set
        public static IRubyObject set_value(final ThreadContext context, final IRubyObject self, final IRubyObject obj,
            final IRubyObject value) {
            final java.lang.reflect.Field field = unwrapJavaObject(self);
            try {
                final Object val = value.toJava(field.getType());
                field.set(unwrapJavaObject(obj), val);
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
            return context.nil;
        }

        @JRubyMethod
        public static IRubyObject static_value(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.Field field = unwrapJavaObject(self);
            try {
                return convertJavaToUsableRubyObject(context.runtime, field.get(null));
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
        }

        @JRubyMethod
        public static IRubyObject set_static_value(final ThreadContext context, final IRubyObject self, final IRubyObject value) {
            final java.lang.reflect.Field field = unwrapJavaObject(self);
            try {
                final Object val = value.toJava(field.getType());
                field.set(null, val);
            }
            catch (IllegalAccessException e) {
                Helpers.throwException(e); return null;
            }
            return context.nil;
        }

        //

        @JRubyMethod
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            final java.lang.reflect.AccessibleObject thiz = unwrapJavaObject(self);
            return RubyString.newString(context.runtime, thiz.toString());
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(final IRubyObject self) {
            final java.lang.reflect.Field thiz = unwrapJavaObject(self);
            return isPublic(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(final IRubyObject self) {
            final java.lang.reflect.Field thiz = unwrapJavaObject(self);
            return isProtected(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(final IRubyObject self) {
            final java.lang.reflect.Field thiz = unwrapJavaObject(self);
            return isPrivate(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(final IRubyObject self) {
            final java.lang.reflect.Field thiz = unwrapJavaObject(self);
            return isFinal(self, thiz.getModifiers());
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(final IRubyObject self) {
            final java.lang.reflect.Field thiz = unwrapJavaObject(self);
            return isStatic(self, thiz.getModifiers());
        }

    }

    static RubyBoolean isAbstract(final IRubyObject self, final int mod) {
        return self.getRuntime().newBoolean(java.lang.reflect.Modifier.isAbstract(mod));
    }

    static RubyBoolean isPublic(final IRubyObject self, final int mod) {
        return self.getRuntime().newBoolean(java.lang.reflect.Modifier.isPublic(mod));
    }

    static RubyBoolean isProtected(final IRubyObject self, final int mod) {
        return self.getRuntime().newBoolean(java.lang.reflect.Modifier.isProtected(mod));
    }

    static RubyBoolean isPrivate(final IRubyObject self, final int mod) {
        return self.getRuntime().newBoolean(java.lang.reflect.Modifier.isPrivate(mod));
    }

    static RubyBoolean isFinal(final IRubyObject self, final int mod) {
        return self.getRuntime().newBoolean(java.lang.reflect.Modifier.isFinal(mod));
    }

    static RubyBoolean isStatic(final IRubyObject self, final int mod) {
        return self.getRuntime().newBoolean(java.lang.reflect.Modifier.isStatic(mod));
    }

}
