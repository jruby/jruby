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
import org.jruby.anno.JRubyModule;
import org.jruby.api.Convert;
import org.jruby.exceptions.NoMethodError;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.util.ClassUtils;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaInternalBlockBody;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyStringBuilder;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyModule.undefinedMethodMessage;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.nameError;
import static org.jruby.api.Error.typeError;
import static org.jruby.javasupport.JavaUtil.convertJavaToUsableRubyObject;
import static org.jruby.javasupport.JavaUtil.isJavaObject;
import static org.jruby.javasupport.JavaUtil.unwrapIfJavaObject;
import static org.jruby.javasupport.JavaUtil.unwrapJavaObject;
import static org.jruby.runtime.Visibility.PUBLIC;
import static org.jruby.util.Inspector.*;
import static org.jruby.util.RubyStringBuilder.ids;

/**
 * Java::JavaLang package extensions.
 *
 * @author kares
 */
public abstract class JavaLang {

    public static void define(ThreadContext context, RubyModule _Enumerable, RubyModule _Comparable) {
        var runtime = context.runtime;

        JavaExtensions.put(runtime, java.lang.Iterable.class,proxy -> Iterable.define(context, proxy, _Enumerable));
        JavaExtensions.put(runtime, java.lang.Comparable.class,proxy -> Comparable.define(context, proxy, _Comparable));
        JavaExtensions.put(runtime, java.lang.Throwable.class, proxy -> proxy.defineMethods(context, Throwable.class));
        JavaExtensions.put(runtime, java.lang.Runnable.class,proxy -> proxy.defineMethods(context, Runnable.class));
        JavaExtensions.put(runtime, java.lang.Character.class,proxy -> proxy.defineMethods(context, Character.class));
        JavaExtensions.put(runtime, java.lang.Number.class,proxy -> proxy.defineMethods(context, Number.class));
        JavaExtensions.put(runtime, java.lang.Class.class,proxy -> Class.define(context, proxy, _Comparable));
        JavaExtensions.put(runtime, java.lang.ClassLoader.class, proxy -> proxy.defineMethods(context, ClassLoader.class));
        // Java::byte[].class_eval ...
        JavaExtensions.put(runtime, new byte[0].getClass(), (byteArray) -> {
            byteArray.addMethod(context, "ubyte_get", new UByteGet(byteArray));
            byteArray.addMethod(context, "ubyte_set", new UByteSet(byteArray));
        });
        JavaExtensions.put(runtime, java.lang.CharSequence.class, proxy -> proxy.defineMethods(context, CharSequence.class));
        JavaExtensions.put(runtime, java.lang.String.class, proxy -> proxy.defineMethods(context, String.class));
        JavaExtensions.put(runtime, java.lang.Enum.class, proxy -> proxy.defineAlias(context, "inspect", "to_s"));
        JavaExtensions.put(runtime, java.lang.Boolean.class, proxy -> proxy.defineAlias(context, "inspect", "to_s"));
        JavaExtensions.put(runtime, java.lang.Thread.class, proxy -> proxy.addMethod(context, "inspect", new InspectThread(proxy)));
    }

    @JRubyModule(name = "Java::JavaLang::Iterable", include = "Enumerable")
    public static class Iterable {

        static RubyModule define(ThreadContext context, final RubyModule proxy, RubyModule Enumerable) {
            return proxy.
                    include(context, Enumerable).
                    defineMethods(context, Iterable.class);
        }

        @JRubyMethod
        public static IRubyObject each(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            if ( ! block.isGiven() ) { // ... Enumerator.new(self, :each)
                return enumeratorize(context.runtime, self, "each");
            }
            java.lang.Iterable iterable = unwrapIfJavaObject(self);
            java.util.Iterator iterator = iterable.iterator();
            while ( iterator.hasNext() ) {
                final Object value = iterator.next();
                block.yield(context, convertJavaToUsableRubyObject(runtime, value));
            }
            return self;
        }

        @JRubyMethod
        public static IRubyObject each_with_index(final ThreadContext context, final IRubyObject self, final Block block) {
            // ... Enumerator.new(self, :each_with_index)
            if (!block.isGiven()) return enumeratorize(context.runtime, self, "each_with_index");

            java.lang.Iterable<?> iterable = unwrapIfJavaObject(self);
            java.util.Iterator<?> iterator = iterable.iterator();
            final boolean twoArguments = block.getSignature().isTwoArguments();
            int i = 0; while ( iterator.hasNext() ) {
                final RubyInteger index = asFixnum(context, i++);
                final Object value = iterator.next();
                final IRubyObject rValue = convertJavaToUsableRubyObject(context.runtime, value);

                if (twoArguments) {
                    block.yieldSpecific(context, rValue, index);
                } else {
                    block.yield(context, newArray(context, rValue, index));
                }
            }
            return self;
        }

        @JRubyMethod(name = { "to_a", "entries" }) // @override Enumerable#to_a
        public static IRubyObject to_a(final ThreadContext context, final IRubyObject self, final Block block) {
            final var ary = newArray(context);
            java.lang.Iterable<?> iterable = unwrapIfJavaObject(self);
            for (Object o: iterable) {
                ary.append(context, convertJavaToUsableRubyObject(context.runtime, o));
            }
            return ary;
        }

        @JRubyMethod(name = "count") // @override Enumerable#count
        public static IRubyObject count(final ThreadContext context, final IRubyObject self, final Block block) {
            java.lang.Iterable iterable = unwrapIfJavaObject(self);
            if (block.isGiven()) return countBlock(context, iterable.iterator(), block);

            if ( iterable instanceof java.util.Collection ) {
                return asFixnum(context, ((java.util.Collection) iterable).size());
            }
            int count = 0;
            for( java.util.Iterator it = iterable.iterator(); it.hasNext(); ) { it.next(); count++; }
            return asFixnum(context, count);
        }

        static RubyFixnum countBlock(final ThreadContext context, final java.util.Iterator it, final Block block) {
            int count = 0;
            while ( it.hasNext() ) {
                IRubyObject next = convertJavaToUsableRubyObject(context.runtime, it.next());
                if ( block.yield( context, next ).isTrue() ) count++;
            }
            return asFixnum(context, count);
        }

        @JRubyMethod(name = "count") // @override Enumerable#count
        public static IRubyObject count(final ThreadContext context, final IRubyObject self, final IRubyObject obj, final Block unused) {
            // unused block due DescriptorInfo not (yet) supporting if a method receives block and an override doesn't
            java.lang.Iterable iterable = unwrapIfJavaObject(self);
            int count = 0;
            for ( java.util.Iterator it = iterable.iterator(); it.hasNext(); ) {
                IRubyObject next = convertJavaToUsableRubyObject(context.runtime, it.next() );
                if ( RubyObject.equalInternal(context, next, obj) ) count++;
            }
            return asFixnum(context, count);
        }

    }

    @JRubyClass(name = "Java::JavaLang::Comparable", include = "Comparable")
    public static class Comparable {

        static RubyModule define(ThreadContext context, final RubyModule proxy, RubyModule Comparable) {
            return proxy.
                    include(context, Comparable).
                    defineMethods(context, Comparable.class);
        }

        @JRubyMethod(name = "<=>")
        public static IRubyObject cmp(final ThreadContext context, final IRubyObject self, final IRubyObject other) {
            if (other.isNil()) return context.nil;

            java.lang.Comparable comparable = unwrapIfJavaObject(self);
            final java.lang.Object otherComp = unwrapIfJavaObject(other);

            try {
                return asFixnum(context, comparable.compareTo(otherComp));
            } catch (ClassCastException ex) {
                throw typeError(context, ex.getMessage());
            }
        }

    }

    @JRubyClass(name = "Java::JavaLang::Throwable")
    public static class Throwable {
        @JRubyMethod // stackTrace => backtrace
        public static IRubyObject backtrace(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            java.lang.Throwable throwable = unwrapIfJavaObject(self);
            // TODO instead this should get aligned with NativeException !?!
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            if ( stackTrace == null ) return context.nil; // never actually happens
            final int len = stackTrace.length;
            if ( len == 0 ) return RubyArray.newEmptyArray(runtime);
            IRubyObject[] backtrace = new IRubyObject[len];
            for ( int i=0; i < len; i++ ) {
                backtrace[i] = newString(context, stackTrace[i].toString());
            }
            return RubyArray.newArrayMayCopy(runtime, backtrace);
        }

        public static IRubyObject set_backtrace(final IRubyObject self, final IRubyObject backtrace) {
            return set_backtrace(((RubyBasicObject) self).getCurrentContext(), self, backtrace);
        }

        @JRubyMethod // can not set backtrace for a java.lang.Throwable
        public static IRubyObject set_backtrace(ThreadContext context, final IRubyObject self, final IRubyObject backtrace) {
            return context.nil;
        }

        @JRubyMethod
        public static IRubyObject message(final ThreadContext context, final IRubyObject self) {
            java.lang.Throwable throwable = unwrapIfJavaObject(self);
            final java.lang.String msg = throwable.getLocalizedMessage(); // does getMessage
            return msg == null ? newEmptyString(context) : newString(context, msg);
        }

        @JRubyMethod
        public static IRubyObject full_message(final ThreadContext context, final IRubyObject self) {
            return full_message(context, self, null);
        }

        @JRubyMethod
        public static IRubyObject full_message(final ThreadContext context, final IRubyObject self, final IRubyObject opts) {
            return TraceType.printFullMessage(context, self, opts);
        }

        @JRubyMethod
        public static IRubyObject detailed_message(final ThreadContext context, final IRubyObject self) {
            return detailed_message(context, self, (IRubyObject) null);
        }

        @JRubyMethod
        public static IRubyObject detailed_message(final ThreadContext context, final IRubyObject self, final IRubyObject opts) {
            return TraceType.printDetailedMessage(context, self, opts);
        }

        @JRubyMethod(optional = 1)
        public static IRubyObject detailed_message(final ThreadContext context, final IRubyObject self, final IRubyObject[] args) {
            return switch (args.length) {
                case 0 -> detailed_message(context, self);
                case 1 -> detailed_message(context, self, args[0]);
                default -> throw argumentError(context, args.length, 0, 1);
            };
        }

        @JRubyMethod // Ruby exception to_s is the same as message
        public static IRubyObject to_s(final ThreadContext context, final IRubyObject self) {
            return message(context, self);
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            java.lang.Throwable throwable = unwrapIfJavaObject(self);

            RubyString buf = inspectPrefix(context, self.getMetaClass());
            RubyStringBuilder.cat(context.runtime, buf, SPACE);
            final java.lang.String message = throwable.getMessage();
            buf.catString(message == null ? "" : message);
            RubyStringBuilder.cat(context.runtime, buf, GT); // >

            return buf;
        }

        @JRubyMethod(name = "===", meta = true)
        public static IRubyObject eqq(final ThreadContext context, final IRubyObject self, IRubyObject other) {
            if (checkNativeException(self, other)) {
                return context.tru;
            }
            return self.op_eqq(context, other);
        }

        @SuppressWarnings("deprecation")
        private static boolean checkNativeException(IRubyObject self, IRubyObject other) {
            if ( other instanceof NativeException ) {
                final java.lang.Class java_class = (java.lang.Class) self.dataGetStruct();
                if ( java_class.isAssignableFrom( ((NativeException) other).getCause().getClass() ) ) {
                    return true;
                }
            }
            return false;
        }

    }

    @JRubyModule(name = "Java::JavaLang::Runnable")
    public static class Runnable {
        @JRubyMethod
        public static IRubyObject to_proc(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            final java.lang.Runnable runnable = unwrapIfJavaObject(self);
            final Block block = new Block(new RunBody(runtime, runnable));
            return new RubyProc(runtime, runtime.getProc(), block, null, -1);
        }

        private static final class RunBody extends JavaInternalBlockBody {

            private final java.lang.Runnable runnable;

            RunBody(final Ruby runtime, final java.lang.Runnable runnable) {
                super(runtime, Signature.NO_ARGUMENTS);
                this.runnable = runnable;
            }

            @Override
            public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                return yieldImpl(context);
            }

            final IRubyObject yieldImpl(ThreadContext context) {
                runnable.run(); return context.nil;
            }

            @Override
            protected final IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
                return yieldImpl(context);
            }

            @Override
            protected final IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
                return yieldImpl(context); // avoid new IRubyObject[] { value }
            }

        }

    }

    @JRubyClass(name = "Java::JavaLang::Number")
    public static class Number {
        @JRubyMethod(name = "to_f")
        public static IRubyObject to_f(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = self.toJava(java.lang.Number.class);
            return context.runtime.newFloat(val.doubleValue());
        }

        @JRubyMethod(name = "real?")
        public static IRubyObject real_p(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = self.toJava(java.lang.Number.class);
            return asBoolean(context, val instanceof Integer || val instanceof Long || val instanceof Short ||
                    val instanceof Byte || val instanceof Float || val instanceof Double ||
                    val instanceof java.math.BigInteger || val instanceof java.math.BigDecimal);
        }

        @JRubyMethod(name = { "to_i", "to_int" })
        public static IRubyObject to_i(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = self.toJava(java.lang.Number.class);
            if (val instanceof java.math.BigInteger bigint) { // NOTE: should be moved into its own?
                return RubyBignum.newBignum(context.runtime, bigint);
            }
            return asFixnum(context, val.longValue());
        }

        @JRubyMethod(name = "integer?")
        public static IRubyObject integer_p(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = self.toJava(java.lang.Number.class);
            return asBoolean(context, val instanceof Integer || val instanceof Long ||
                    val instanceof Short || val instanceof Byte || val instanceof java.math.BigInteger);
        }

        @JRubyMethod(name = "zero?")
        public static IRubyObject zero_p(final ThreadContext context, final IRubyObject self) {
            return asBoolean(context, isZero(self));
        }

        private static boolean isZero(final IRubyObject self) {
            java.lang.Number val = (java.lang.Number) self.toJava(java.lang.Number.class);
            return Double.compare(val.doubleValue(), 0) == 0;
        }

        @JRubyMethod(name = "nonzero?")
        public static IRubyObject nonzero_p(final ThreadContext context, final IRubyObject self) {
            return isZero(self) ? context.nil : self;
        }

        @JRubyMethod(name = "coerce")
        public static IRubyObject coerce(final ThreadContext context, final IRubyObject self, final IRubyObject type) {
            java.lang.Number val = self.toJava(java.lang.Number.class);

            // NOTE: a basic stub that always coverts Java numbers to Ruby ones (for simplicity)
            // gist being this is not expected to be used heavily, if so should get special care
            return newArray(context, type, convertJavaToUsableRubyObject(context.runtime, val));
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = self.toJava(java.lang.Number.class);
            return newString(context, val.toString());
        }

    }

    @JRubyClass(name = "Java::JavaLang::Character")
    public static class Character {
        @JRubyMethod(name = "java_identifier_start?", meta = true)
        public static IRubyObject java_identifier_start_p(final ThreadContext context, final IRubyObject self,
                                                          final IRubyObject num) {
            return asBoolean(context, java.lang.Character.isJavaIdentifierStart(int_char(num)));
        }

        @JRubyMethod(name = "java_identifier_part?", meta = true)
        public static IRubyObject java_identifier_part_p(final ThreadContext context, final IRubyObject self,
                                                         final IRubyObject num) {
            return asBoolean(context, java.lang.Character.isJavaIdentifierPart(int_char(num)));
        }

        private static int int_char(IRubyObject num) { // str.ord -> Fixnum
            return num.toJava(java.lang.Character.TYPE);
        }

        @JRubyMethod(name = "to_i")
        public static IRubyObject to_i(final ThreadContext context, final IRubyObject self) {
            java.lang.Character c = self.toJava(java.lang.Character.class);
            return asFixnum(context, c);
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            java.lang.Character c = self.toJava(java.lang.Character.class);
            return RubyString.newString(context.runtime, inspectCharValue(new java.lang.StringBuilder(3), c));
        }

        public static java.lang.StringBuilder inspectCharValue(final java.lang.StringBuilder buf, final char c) {
            return buf.append('\'').append(c).append('\'');
        }

    }

    @JRubyClass(name = "Java::JavaLang::Class")
    public static class Class {

        static RubyClass define(ThreadContext context, final RubyModule proxy, RubyModule Comparable) {
            return proxy.include(context, Comparable).
                    defineMethods(context, Class.class).
                    defineAlias(context, "resource", "get_resource").
                    defineAlias(context, "declared_field", "get_declared_field").
                    defineAlias(context, "field", "get_field");
        }

        @JRubyMethod(name = "ruby_class")
        public static IRubyObject proxy_class(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return Java.getProxyClass(context, klass);
        }

        @JRubyMethod
        public static IRubyObject resource_as_stream(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final java.lang.String resName = name.convertToString().toString();
            return convertJavaToUsableRubyObject(context.runtime, klass.getResourceAsStream(resName));
        }

        @JRubyMethod
        public static IRubyObject resource_as_string(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final java.lang.String resName = name.convertToString().toString();
            return new RubyIO(context.runtime, klass.getResourceAsStream(resName)).read(context);
        }

        @JRubyMethod // make to_s an improved class.name version
        public static IRubyObject to_s(final ThreadContext context, final IRubyObject self) {
            return newString(context, getClassName(self));
        }

        @JRubyMethod
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            // with 'type' - to be able to tell Java::JavaLang::Class apart
            RubyString buf = inspectPrefix(context, self.getMetaClass());
            RubyStringBuilder.cat(context.runtime, buf, SPACE);
            buf.catString(getClassName(self));
            RubyStringBuilder.cat(context.runtime, buf, GT); // >
            return buf;
        }

        private static java.lang.String getClassName(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final java.lang.String name = klass.getCanonicalName();
            return name == null ? klass.getName() : name;
        }

        @JRubyMethod(name = "annotations?")
        public static IRubyObject annotations_p(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return asBoolean(context, klass.getAnnotations().length > 0);
        }

        @JRubyMethod(name = "declared_annotations?")
        public static IRubyObject declared_annotations_p(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return asBoolean(context, klass.getDeclaredAnnotations().length > 0);
        }

        @JRubyMethod
        public static IRubyObject java_instance_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class<?> klass = unwrapJavaObject(self);
            Method[] publicMethods = klass.getMethods();

            // quick count for accurate size
            int size = 0;
            for (java.lang.reflect.Method method : publicMethods) if (!Modifier.isStatic(method.getModifiers())) size++;
            final var methods = allocArray(context, size);

            for (java.lang.reflect.Method method : publicMethods) {
                if (!Modifier.isStatic(method.getModifiers())) methods.add(context, method);
            }
            return methods;
        }

        @JRubyMethod
        public static IRubyObject declared_instance_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class<?> klass = unwrapJavaObject(self);
            Method[] declaredMethods = klass.getDeclaredMethods();

            // quick count for accurate size
            int size = 0;
            for (java.lang.reflect.Method method : declaredMethods) if (!Modifier.isStatic(method.getModifiers())) size++;
            final var methods = allocArray(context, size);

            for (java.lang.reflect.Method method : declaredMethods) {
                if (!Modifier.isStatic(method.getModifiers())) methods.add(context, method);
            }
            return methods;
        }

        @JRubyMethod
        public static IRubyObject java_class_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class<?> klass = unwrapJavaObject(self);
            Method[] publicMethods = klass.getMethods();

            // quick count for accurate size
            int size = 0;
            for (java.lang.reflect.Method method : publicMethods) if (Modifier.isStatic(method.getModifiers())) size++;
            final var methods = allocArray(context, size);

            for ( java.lang.reflect.Method method : publicMethods) {
                if (Modifier.isStatic(method.getModifiers())) methods.add(method);
            }
            return methods;
        }

        @JRubyMethod
        public static IRubyObject declared_class_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class<?> klass = unwrapJavaObject(self);
            Method[] declaredMethods = klass.getDeclaredMethods();

            // quick count for accurate size
            int size = 0;
            for (java.lang.reflect.Method method : declaredMethods) if (Modifier.isStatic(method.getModifiers())) size++;
            final var methods = allocArray(context, size);

            for (java.lang.reflect.Method method : declaredMethods) {
                if (Modifier.isStatic(method.getModifiers())) methods.add(method);
            }
            return methods;
        }

        @JRubyMethod(name = "<=>") // Ruby Comparable
        public static IRubyObject cmp(final ThreadContext context, final IRubyObject self, final IRubyObject other) {
            if (!isJavaObject(other)) return context.nil;
            final java.lang.Class that = unwrapJavaObject(other);
            final java.lang.Class thiz = unwrapJavaObject(self);

            if (thiz == that) return asFixnum(context, 0);
            if (thiz.isAssignableFrom(that)) asFixnum(context, +1);
            if (that.isAssignableFrom(thiz)) asFixnum(context, -1);

            return context.nil;
        }

        @JRubyMethod(name = "anonymous?")
        public static IRubyObject anonymous_p(ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return asBoolean(context, klass.isAnonymousClass());
        }

        @Deprecated
        public static IRubyObject anonymous_p(final IRubyObject self) {
            return anonymous_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "abstract?")
        public static IRubyObject abstract_p(ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isAbstract(context, self, klass.getModifiers());
        }

        @Deprecated
        public static IRubyObject abstract_p(final IRubyObject self) {
            return abstract_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isPublic(context, self, klass.getModifiers());
        }

        @Deprecated
        public static IRubyObject public_p(final IRubyObject self) {
            return public_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isProtected(context, self, klass.getModifiers());
        }

        @Deprecated
        public static IRubyObject protected_p(final IRubyObject self) {
            return protected_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isPrivate(context, self, klass.getModifiers());
        }

        @Deprecated
        public static IRubyObject private_p(final IRubyObject self) {
            return private_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isFinal(context, self, klass.getModifiers());
        }

        @Deprecated
        public static IRubyObject final_p(final IRubyObject self) {
            return final_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isStatic(context, self, klass.getModifiers());
        }

        @Deprecated
        public static IRubyObject static_p(final IRubyObject self) {
            return static_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        // JavaClass facade (compatibility) :

        @JRubyMethod
        public static IRubyObject extend_proxy(final ThreadContext context, IRubyObject self, IRubyObject extender) {
            java.lang.Class<?> klass = Java.unwrapClassProxy(self);
            RubyModule proxy = Java.getProxyClass(context, klass);
            try {
                return extender.callMethod(context, "extend_proxy", proxy);
            } catch (NoMethodError ex) {
                throw typeError(context, "proxy extender must have an extend_proxy method");
            }
        }

        //@JRubyMethod(name = "simple_name") // (legacy) compatibility with JavaClass
        //public static IRubyObject simple_name(final ThreadContext context, final IRubyObject self) {
        //    final java.lang.Class klass = unwrapJavaObject(self);
        //    return context.runtime.newString(JavaClass.getSimpleName(klass));
        //}

        @SuppressWarnings("deprecation")
        @JRubyMethod(required = 1, rest = true, checkArity = false)
        public static IRubyObject java_method(ThreadContext context, IRubyObject self, final IRubyObject[] args) {
            Arity.checkArgumentCount(context, args, 1, -1);

            final java.lang.Class klass = unwrapJavaObject(self);

            final java.lang.String methodName = args[0].asJavaString();
            try {
                java.lang.Class<?>[] argumentTypes = ClassUtils.getArgumentTypes(context, args, 1);
                final Method method = klass.getMethod(methodName, argumentTypes);
                return Java.getInstance(context.runtime, method); // a JavaMethod like
            } catch (NoSuchMethodException e) {
                final Ruby runtime = context.runtime;
                throw nameError(context, undefinedMethodMessage(runtime, ids(runtime, methodName), ids(runtime, klass.getName()), false), methodName);
            }
        }

        @SuppressWarnings("deprecation")
        @JRubyMethod(required = 1, rest = true, checkArity = false)
        public static IRubyObject declared_method(ThreadContext context, IRubyObject self, final IRubyObject[] args) {
            Arity.checkArgumentCount(context, args, 1, -1);

            final java.lang.Class klass = unwrapJavaObject(self);

            final java.lang.String methodName = args[0].asJavaString();
            try {
                java.lang.Class<?>[] argumentTypes = ClassUtils.getArgumentTypes(context, args, 1);
                final Method method = klass.getDeclaredMethod(methodName, argumentTypes);
                return Java.getInstance(context.runtime, method); // a JavaMethod like
            } catch (NoSuchMethodException e) {
                Helpers.throwException(e); // NOTE: this is mostly a get_declared_method alias
                return null; // never reached
            }
        }

        @JRubyMethod(required = 1, rest = true, checkArity = false)
        public static IRubyObject declared_method_smart(ThreadContext context, IRubyObject self, final IRubyObject[] args) {
            Arity.checkArgumentCount(context, args, 1, -1);

            final java.lang.Class klass = unwrapJavaObject(self);
            final java.lang.String methodName = args[0].asJavaString();
            java.lang.Class<?>[] argumentTypes = ClassUtils.getArgumentTypes(context, args, 1);
            AccessibleObject callable = ClassUtils.getMatchingCallable(klass, methodName, argumentTypes);

            if (callable == null) {
                final Ruby runtime = context.runtime;
                throw nameError(context, undefinedMethodMessage(runtime, ids(runtime, methodName), ids(runtime, klass.getName()), false), methodName);
            }

            return Java.getInstance(context.runtime, callable); // a JavaMethod or JavaConstructor like
        }

        @SuppressWarnings("deprecation")
        @JRubyMethod(rest = true)
        public static IRubyObject constructor(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            final java.lang.Class klass = unwrapJavaObject(self);
            try {
                java.lang.Class<?>[] parameterTypes = ClassUtils.getArgumentTypes(context, args, 0);
                Constructor<?> constructor = klass.getConstructor(parameterTypes);
                return Java.getInstance(context.runtime, constructor); // JavaConstructor like
            } catch (NoSuchMethodException e) {
                Helpers.throwException(e); // NOTE: this is mostly a get_constructor alias
                return null; // never reached
            }
        }

        @SuppressWarnings("deprecation")
        @JRubyMethod(rest = true)
        public static IRubyObject declared_constructor(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            final java.lang.Class klass = unwrapJavaObject(self);
            try {
                java.lang.Class<?>[] parameterTypes = ClassUtils.getArgumentTypes(context, args, 0);
                Constructor<?> constructor = klass.getDeclaredConstructor(parameterTypes);
                return Java.getInstance(context.runtime, constructor); // JavaConstructor like
            } catch (NoSuchMethodException e) {
                Helpers.throwException(e); // NOTE: this is mostly a get_declared_constructor alias
                return null; // never reached
            }
        }

        @JRubyMethod
        public static IRubyObject array_class(ThreadContext context, IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final java.lang.Class<?> arrayClass = Array.newInstance(klass, 0).getClass();
            return Java.getInstance(context.runtime, arrayClass);
        }

        @JRubyMethod
        public static IRubyObject new_array(ThreadContext context, IRubyObject self, IRubyObject length) {
            final java.lang.Class klass = unwrapJavaObject(self);

            if (length instanceof RubyInteger lenint) { // one-dimensional array
                return ArrayJavaProxy.newArray(context.runtime, klass, lenint.asInt(context));
            }
            if (length instanceof RubyArray ary) { // n-dimensional array
                IRubyObject[] aryLengths = ary.toJavaArrayMaybeUnsafe();
                final int len = aryLengths.length;
                if (len == 0) throw argumentError(context, "empty dimensions specifier for java array");

                final int[] dimensions = new int[len];
                for (int i = len; --i >= 0; ) {
                    dimensions[i] = Convert.castAsInteger(context, aryLengths[i]).asInt(context);
                }
                return ArrayJavaProxy.newArray(context, klass, dimensions);
            }

            throw argumentError(context, "invalid length or dimensions specifier for java array - must be Integer or Array of Integer");
        }

    }

    @JRubyClass(name = "Java::JavaLang::ClassLoader")
    public static class ClassLoader {
        @JRubyMethod
        public static IRubyObject resource_as_url(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.ClassLoader loader = unwrapIfJavaObject(self);
            final java.lang.String resName = name.convertToString().toString();
            return convertJavaToUsableRubyObject(context.runtime, loader.getResource(resName));
        }

        @JRubyMethod
        public static IRubyObject resource_as_stream(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.ClassLoader loader = unwrapIfJavaObject(self);
            final java.lang.String resName = name.convertToString().toString();
            return convertJavaToUsableRubyObject(context.runtime, loader.getResourceAsStream(resName));
        }

        @JRubyMethod
        public static IRubyObject resource_as_string(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.ClassLoader loader = unwrapIfJavaObject(self);
            final java.lang.String resName = name.convertToString().toString();
            return new RubyIO(context.runtime, loader.getResourceAsStream(resName)).read(context);
        }

    }

    @JRubyClass(name = "Java::JavaLang::CharSequence")
    public static class CharSequence {
        @JRubyMethod(name = "inspect")
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            // we re-define java.lang.String#inspect thus this is for StringBuilder etc.
            java.lang.CharSequence str = unwrapIfJavaObject(self);

            RubyString buf = inspectPrefix(context, self.getMetaClass());
            RubyStringBuilder.cat(context.runtime, buf, SPACE);
            buf.catWithCodeRange((RubyString) RubyString.newString(context.runtime, str).inspect(context));
            RubyStringBuilder.cat(context.runtime, buf, GT); // >

            return buf;
        }

    }

    @JRubyClass(name = "Java::JavaLang::String")
    public static class String {
        @JRubyMethod(name = "to_s", alias = "to_str")
        public static IRubyObject to_s(final ThreadContext context, final IRubyObject self) {
            return newString(context, self.toJava(java.lang.String.class));
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            return newString(context, self.toJava(java.lang.String.class)).inspect(context);
        }

    }

    static final class InspectThread extends JavaMethod.JavaMethodZero {

        InspectThread(RubyModule implClass) {
            super(implClass, PUBLIC, "inspect");
        }

        @Override
        public IRubyObject call(final ThreadContext context, final IRubyObject self, final RubyModule clazz, final java.lang.String name) {
            java.lang.Thread thread = unwrapIfJavaObject(self);

            // 0.03 % of cpu usage, state: timed_waiting, thread name: '[foo] bar', thread id: 760
            // <#Java::JavaLang::Thread:760 [foo] bar TIMED_WAITING>

            RubyString buf = inspectPrefix(context, self.getMetaClass());
            RubyStringBuilder.cat(context.runtime, buf, Long.toString(thread.threadId()));
            RubyStringBuilder.cat(context.runtime, buf, ' ');
            RubyStringBuilder.cat(context.runtime, buf, thread.getName());
            RubyStringBuilder.cat(context.runtime, buf, ' ');
            RubyStringBuilder.cat(context.runtime, buf, thread.getState().toString());
            RubyStringBuilder.cat(context.runtime, buf, GT); // >

            return buf;
        }
    }

    static RubyString inspectValueWithTypePrefix(final ThreadContext context, final IRubyObject self) {
        java.lang.Object obj = unwrapIfJavaObject(self);

        RubyString buf = inspectPrefix(context, self.getMetaClass());
        RubyStringBuilder.cat(context.runtime, buf, SPACE);
        RubyStringBuilder.cat(context.runtime, buf, obj.toString());
        RubyStringBuilder.cat(context.runtime, buf, GT); // >

        return buf;
    }

    static final class InspectRawValue extends JavaMethod.JavaMethodZero {

        InspectRawValue(RubyModule implClass) {
            super(implClass, PUBLIC, "inspect");
        }

        @Override
        public IRubyObject call(final ThreadContext context, final IRubyObject self, final RubyModule clazz, final java.lang.String name) {
            java.lang.Object val = unwrapIfJavaObject(self);
            return newString(context, val.toString());
        }

    }

    static final class InspectValueWithTypePrefix extends JavaMethod.JavaMethodZero {

        InspectValueWithTypePrefix(RubyModule implClass) {
            super(implClass, PUBLIC, "inspect");
        }

        @Override
        public IRubyObject call(final ThreadContext context, final IRubyObject self, final RubyModule clazz, final java.lang.String name) {
            return inspectValueWithTypePrefix(context, self);
        }
    }

    private static final class UByteGet extends JavaMethod.JavaMethodOne {

        UByteGet(RubyModule implClass) {
            super(implClass, PUBLIC, "ubyte_get");
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, java.lang.String name, IRubyObject idx) {
            final RubyInteger val = (RubyInteger) self.callMethod(context, "[]", idx);
            int byte_val = val.asInt(context);
            if ( byte_val >= 0 ) return val;
            return asFixnum(context, byte_val + 256); // byte += 256 if byte < 0
        }
    }

    private static final class UByteSet extends JavaMethod.JavaMethodTwo {

        UByteSet(RubyModule implClass) {
            super(implClass, PUBLIC, "ubyte_set");
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, java.lang.String name, IRubyObject idx, IRubyObject val) {
            int byte_val = ((RubyInteger) val).asInt(context);
            if ( byte_val > 127 ) {
                val = asFixnum(context, byte_val - 256); // value -= 256 if value > 127
            }
            return self.callMethod(context, "[]=", new IRubyObject[] { idx, val });
        }
    }

}
