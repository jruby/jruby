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
import org.jruby.anno.JRubyModule;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.JavaInternalBlockBody;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Modifier;

import static org.jruby.javasupport.JavaUtil.convertJavaToUsableRubyObject;
import static org.jruby.javasupport.JavaUtil.isJavaObject;
import static org.jruby.javasupport.JavaUtil.unwrapIfJavaObject;
import static org.jruby.javasupport.JavaUtil.unwrapJavaObject;
import static org.jruby.runtime.Visibility.PUBLIC;

/**
 * Java::JavaLang package extensions.
 *
 * @author kares
 */
public abstract class JavaLang {

    public static void define(final Ruby runtime) {
        Iterable.define(runtime);
        Comparable.define(runtime);
        Throwable.define(runtime);
        Runnable.define(runtime);
        Character.define(runtime);
        Number.define(runtime);
        Class.define(runtime);
        ClassLoader.define(runtime);
        // Java::byte[].class_eval ...
        final RubyModule byteArray = Java.getProxyClass(runtime, new byte[0].getClass());
        byteArray.addMethod("ubyte_get", new UByteGet(byteArray));
        byteArray.addMethod("ubyte_set", new UByteSet(byteArray));
    }

    @JRubyModule(name = "Java::JavaLang::Iterable", include = "Enumerable")
    public static class Iterable {

        static RubyModule define(final Ruby runtime) {
            final RubyModule Iterable = Java.getProxyClass(runtime, java.lang.Iterable.class);
            Iterable.includeModule( runtime.getEnumerable() ); // include Enumerable
            Iterable.defineAnnotatedMethods(Iterable.class);
            return Iterable;
        }

        @JRubyMethod
        public static IRubyObject each(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            if ( ! block.isGiven() ) { // ... Enumerator.new(self, :each)
                return runtime.getEnumerator().callMethod("new", self, runtime.newSymbol("each"));
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
            final Ruby runtime = context.runtime;
            if ( ! block.isGiven() ) { // ... Enumerator.new(self, :each)
                return runtime.getEnumerator().callMethod("new", self, runtime.newSymbol("each_with_index"));
            }
            java.lang.Iterable iterable = unwrapIfJavaObject(self);
            java.util.Iterator iterator = iterable.iterator();
            final boolean arity2 = block.getSignature().arity() == Arity.TWO_ARGUMENTS;
            int i = 0; while ( iterator.hasNext() ) {
                final RubyInteger index = RubyFixnum.newFixnum(runtime, i++);
                final Object value = iterator.next();
                final IRubyObject rValue = convertJavaToUsableRubyObject(runtime, value);
                if ( arity2 ) {
                    block.yieldSpecific(context, rValue, index);
                } else {
                    block.yield(context, RubyArray.newArray(runtime, rValue, index));
                }
            }
            return self;
        }

        @JRubyMethod(name = { "to_a", "entries" }) // @override Enumerable#to_a
        public static IRubyObject to_a(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            final RubyArray ary = runtime.newArray();
            java.lang.Iterable iterable = unwrapIfJavaObject(self);
            java.util.Iterator iterator = iterable.iterator();
            while ( iterator.hasNext() ) {
                final Object value = iterator.next();
                ary.append( convertJavaToUsableRubyObject(runtime, value) );
            }
            return ary;
        }

        @JRubyMethod(name = "count") // @override Enumerable#count
        public static IRubyObject count(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            java.lang.Iterable iterable = unwrapIfJavaObject(self);
            if ( block.isGiven() ) {
                return countBlock(context, iterable.iterator(), block);
            }
            if ( iterable instanceof java.util.Collection ) {
                return RubyFixnum.newFixnum(runtime, ((java.util.Collection) iterable).size());
            }
            int count = 0;
            for( java.util.Iterator it = iterable.iterator(); it.hasNext(); ) { it.next(); count++; }
            return RubyFixnum.newFixnum(runtime, count);
        }

        static RubyFixnum countBlock(final ThreadContext context, final java.util.Iterator it, final Block block) {
            final Ruby runtime = context.runtime;
            int count = 0; while ( it.hasNext() ) {
                IRubyObject next = convertJavaToUsableRubyObject( runtime, it.next() );
                if ( block.yield( context, next ).isTrue() ) count++;
            }
            return RubyFixnum.newFixnum(runtime, count);
        }

        @JRubyMethod(name = "count") // @override Enumerable#count
        public static IRubyObject count(final ThreadContext context, final IRubyObject self, final IRubyObject obj, final Block unused) {
            // unused block due DescriptorInfo not (yet) supporting if a method receives block and an override doesn't
            final Ruby runtime = context.runtime;
            java.lang.Iterable iterable = unwrapIfJavaObject(self);
            int count = 0; for ( java.util.Iterator it = iterable.iterator(); it.hasNext(); ) {
                IRubyObject next = convertJavaToUsableRubyObject( runtime, it.next() );
                if ( RubyObject.equalInternal(context, next, obj) ) count++;
            }
            return RubyFixnum.newFixnum(runtime, count);
        }

    }

    @JRubyClass(name = "Java::JavaLang::Comparable", include = "Comparable")
    public static class Comparable {

        static RubyModule define(final Ruby runtime) {
            final RubyModule Comparable = Java.getProxyClass(runtime, java.lang.Comparable.class);
            Comparable.includeModule( runtime.getComparable() ); // include Comparable
            Comparable.defineAnnotatedMethods(Comparable.class);
            return Comparable;
        }

        @JRubyMethod(name = "<=>")
        public static IRubyObject cmp(final ThreadContext context, final IRubyObject self, final IRubyObject other) {
            java.lang.Comparable comparable = unwrapIfJavaObject(self);
            if ( other.isNil() ) return context.nil;

            final java.lang.Object otherComp = unwrapIfJavaObject(other);

            final int cmp;
            try {
                cmp = comparable.compareTo(otherComp);
            }
            catch (ClassCastException ex) {
                throw context.runtime.newTypeError(ex.getMessage());
            }
            return RubyFixnum.newFixnum(context.runtime, cmp);
        }

    }

    @JRubyClass(name = "Java::JavaLang::Throwable")
    public static class Throwable {

        static RubyModule define(final Ruby runtime) {
            final RubyModule Throwable = Java.getProxyClass(runtime, java.lang.Throwable.class);
            Throwable.defineAnnotatedMethods(Throwable.class);
            return Throwable;
        }

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
                backtrace[i] = RubyString.newString(runtime, stackTrace[i].toString());
            }
            return RubyArray.newArrayMayCopy(runtime, backtrace);
        }

        @JRubyMethod // can not set backtrace for a java.lang.Throwable
        public static IRubyObject set_backtrace(final IRubyObject self, final IRubyObject backtrace) {
            return self.getRuntime().getNil();
        }

        @JRubyMethod
        public static IRubyObject message(final ThreadContext context, final IRubyObject self) {
            java.lang.Throwable throwable = unwrapIfJavaObject(self);
            final String msg = throwable.getLocalizedMessage(); // does getMessage
            return msg == null ? RubyString.newEmptyString(context.runtime) : RubyString.newString(context.runtime, msg);
        }

        @JRubyMethod // Ruby exception to_s is the same as message
        public static IRubyObject to_s(final ThreadContext context, final IRubyObject self) {
            return message(context, self);
        }

        @JRubyMethod
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            java.lang.Throwable throwable = unwrapIfJavaObject(self);
            return RubyString.newString(context.runtime, throwable.toString());
        }

        @JRubyMethod(name = "===", meta = true)
        public static IRubyObject eqq(final ThreadContext context, final IRubyObject self, IRubyObject other) {
            if ( other instanceof NativeException ) {
                final java.lang.Class java_class = (java.lang.Class) self.dataGetStruct();
                if ( java_class.isAssignableFrom( ((NativeException) other).getCause().getClass() ) ) {
                    return context.runtime.getTrue();
                }
            }
            return self.op_eqq(context, other);
        }

    }

    @JRubyModule(name = "Java::JavaLang::Runnable")
    public static class Runnable {

        static RubyModule define(final Ruby runtime) {
            final RubyModule Runnable = Java.getProxyClass(runtime, java.lang.Runnable.class);
            Runnable.defineAnnotatedMethods(Runnable.class);
            return Runnable;
        }

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

        static RubyClass define(final Ruby runtime) {
            final RubyModule Number = Java.getProxyClass(runtime, java.lang.Number.class);
            Number.defineAnnotatedMethods(Number.class);
            return (RubyClass) Number;
        }

        @JRubyMethod(name = "to_f")
        public static IRubyObject to_f(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = (java.lang.Number) self.toJava(java.lang.Number.class);
            return context.runtime.newFloat(val.doubleValue());
        }

        @JRubyMethod(name = { "to_i", "to_int" })
        public static IRubyObject to_i(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = (java.lang.Number) self.toJava(java.lang.Number.class);
            if (val instanceof java.math.BigInteger) { // NOTE: should be moved into its own?
                return RubyBignum.newBignum(context.runtime, (java.math.BigInteger) val);
            }
            if (val instanceof java.math.BigDecimal) { // NOTE: should be moved into its own?
                return RubyBignum.newBignum(context.runtime, ((java.math.BigDecimal) val).toBigInteger());
            }
            return context.runtime.newFixnum(val.longValue());
        }

        @JRubyMethod(name = "integer?")
        public static IRubyObject integer_p(final ThreadContext context, final IRubyObject self) {
            java.lang.Number val = (java.lang.Number) self.toJava(java.lang.Number.class);
            return context.runtime.newBoolean(val instanceof Integer || val instanceof Long ||
                                                    val instanceof Short || val instanceof Byte ||
                                                    val instanceof java.math.BigInteger);
        }

        @JRubyMethod(name = "coerce")
        public static IRubyObject coerce(final ThreadContext context, final IRubyObject self, final IRubyObject type) {
            java.lang.Number val = (java.lang.Number) self.toJava(java.lang.Number.class);

            // NOTE: a basic stub that always coverts Java numbers to Ruby ones (for simplicity)
            // gist being this is not expected to be used heavily, if so should get special care
            final IRubyObject value;
            if (val instanceof java.math.BigDecimal) {
                final RubyClass klass = context.runtime.getClass("BigDecimal");
                if (klass == null) { // user should require 'bigdecimal'
                    throw context.runtime.newNameError("uninitialized constant BigDecimal", "BigDecimal");
                }
                value = new RubyBigDecimal(context.runtime, klass, (java.math.BigDecimal) val);
            }
            else {
                value = convertJavaToUsableRubyObject(context.runtime, val);
            }
            return context.runtime.newArray(type, value);
        }

    }

    @JRubyClass(name = "Java::JavaLang::Character")
    public static class Character {

        static RubyClass define(final Ruby runtime) {
            final RubyModule Character = Java.getProxyClass(runtime, java.lang.Character.class);
            Character.defineAnnotatedMethods(Character.class);
            return (RubyClass) Character;
        }

        @JRubyMethod(name = "java_identifier_start?", meta = true)
        public static IRubyObject java_identifier_start_p(final IRubyObject self, final IRubyObject num) {
            return self.getRuntime().newBoolean( java.lang.Character.isJavaIdentifierStart(to_char(num)) );
        }

        @JRubyMethod(name = "java_identifier_part?", meta = true)
        public static IRubyObject java_identifier_part_p(final IRubyObject self, final IRubyObject num) {
            return self.getRuntime().newBoolean( java.lang.Character.isJavaIdentifierPart(to_char(num)) );
        }

        private static char to_char(final IRubyObject num) {
            return (java.lang.Character) num.toJava(java.lang.Character.TYPE);
        }

        @JRubyMethod(name = "to_i")
        public static IRubyObject to_i(final ThreadContext context, final IRubyObject self) {
            java.lang.Character c = (java.lang.Character) self.toJava(java.lang.Character.class);
            return context.runtime.newFixnum(c);
        }

    }

    @JRubyClass(name = "Java::JavaLang::Class")
    public static class Class {

        static RubyClass define(final Ruby runtime) {
            final RubyModule Class = Java.getProxyClass(runtime, java.lang.Class.class);
            Class.includeModule( runtime.getComparable() ); // include Comparable
            Class.defineAnnotatedMethods(Class.class);
            return (RubyClass) Class;
        }

        @JRubyMethod(name = "ruby_class")
        public static IRubyObject proxy_class(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return context.runtime.getJavaSupport().getProxyClassFromCache(klass);
        }

        @JRubyMethod
        public static IRubyObject resource_as_stream(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final String resName = name.convertToString().toString();
            return convertJavaToUsableRubyObject(context.runtime, klass.getResourceAsStream(resName));
        }

        @JRubyMethod
        public static IRubyObject resource_as_string(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final String resName = name.convertToString().toString();
            return new RubyIO(context.runtime, klass.getResourceAsStream(resName)).read(context);
        }

        @JRubyMethod // alias to_s name
        public static IRubyObject to_s(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return RubyString.newString(context.runtime, klass.getName());
        }

        @JRubyMethod
        public static IRubyObject inspect(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return RubyString.newString(context.runtime, klass.toString());
        }

        @JRubyMethod(name = "annotations?")
        public static IRubyObject annotations_p(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return context.runtime.newBoolean(klass.getAnnotations().length > 0);
        }

        @JRubyMethod(name = "declared_annotations?")
        public static IRubyObject declared_annotations_p(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return context.runtime.newBoolean(klass.getDeclaredAnnotations().length > 0);
        }

        @JRubyMethod
        public static IRubyObject java_instance_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final RubyArray methods = RubyArray.newArray(context.runtime);
            for ( java.lang.reflect.Method method : klass.getMethods() ) {
                if ( ! Modifier.isStatic(method.getModifiers()) ) methods.add(method);
            }
            return methods;
        }

        @JRubyMethod
        public static IRubyObject declared_instance_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final RubyArray methods = RubyArray.newArray(context.runtime);
            for ( java.lang.reflect.Method method : klass.getDeclaredMethods() ) {
                if ( ! Modifier.isStatic(method.getModifiers()) ) methods.add(method);
            }
            return methods;
        }

        @JRubyMethod
        public static IRubyObject java_class_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final RubyArray methods = RubyArray.newArray(context.runtime);
            for ( java.lang.reflect.Method method : klass.getMethods() ) {
                if ( Modifier.isStatic(method.getModifiers()) ) methods.add(method);
            }
            return methods;
        }

        @JRubyMethod
        public static IRubyObject declared_class_methods(final ThreadContext context, final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            final RubyArray methods = RubyArray.newArray(context.runtime);
            for ( java.lang.reflect.Method method : klass.getDeclaredMethods() ) {
                if ( Modifier.isStatic(method.getModifiers()) ) methods.add(method);
            }
            return methods;
        }

        @JRubyMethod(name = "<=>") // Ruby Comparable
        public static IRubyObject cmp(final ThreadContext context, final IRubyObject self, final IRubyObject other) {
            final java.lang.Class that;
            if ( other instanceof JavaClass ) {
                that = ((JavaClass) other).getJavaClass();
            }
            else if ( isJavaObject(other) ) {
                that = unwrapJavaObject(other);
            }
            else {
                return context.nil;
            }

            final java.lang.Class thiz = unwrapJavaObject(self);

            if ( thiz == that ) return context.runtime.newFixnum(0);
            if ( thiz.isAssignableFrom(that) ) return context.runtime.newFixnum(+1);
            if ( that.isAssignableFrom(thiz) ) return context.runtime.newFixnum(-1);

            return context.nil;
        }

        @JRubyMethod(name = "anonymous?")
        public static IRubyObject anonymous_p(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return self.getRuntime().newBoolean( klass.isAnonymousClass() );
        }

        @JRubyMethod(name = "abstract?")
        public static IRubyObject abstract_p(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isAbstract( self, klass.getModifiers() );
        }

        // JavaUtilities::ModifiedShortcuts :

        @JRubyMethod(name = "public?")
        public static IRubyObject public_p(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isPublic( self, klass.getModifiers() );
        }

        @JRubyMethod(name = "protected?")
        public static IRubyObject protected_p(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isProtected(self, klass.getModifiers());
        }

        @JRubyMethod(name = "private?")
        public static IRubyObject private_p(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isPrivate(self, klass.getModifiers());
        }

        @JRubyMethod(name = "final?")
        public static IRubyObject final_p(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isFinal(self, klass.getModifiers());
        }

        @JRubyMethod(name = "static?")
        public static IRubyObject static_p(final IRubyObject self) {
            final java.lang.Class klass = unwrapJavaObject(self);
            return JavaLangReflect.isStatic(self, klass.getModifiers());
        }

    }

    @JRubyClass(name = "Java::JavaLang::ClassLoader")
    public static class ClassLoader {

        static RubyModule define(final Ruby runtime) {
            final RubyModule ClassLoader = Java.getProxyClass(runtime, java.lang.ClassLoader.class);
            ClassLoader.defineAnnotatedMethods(ClassLoader.class);
            return ClassLoader;
        }

        @JRubyMethod
        public static IRubyObject resource_as_url(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.ClassLoader loader = unwrapIfJavaObject(self);
            final String resName = name.convertToString().toString();
            return convertJavaToUsableRubyObject(context.runtime, loader.getResource(resName));
        }

        @JRubyMethod
        public static IRubyObject resource_as_stream(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.ClassLoader loader = unwrapIfJavaObject(self);
            final String resName = name.convertToString().toString();
            return convertJavaToUsableRubyObject(context.runtime, loader.getResourceAsStream(resName));
        }

        @JRubyMethod
        public static IRubyObject resource_as_string(final ThreadContext context, final IRubyObject self, final IRubyObject name) {
            final java.lang.ClassLoader loader = unwrapIfJavaObject(self);
            final String resName = name.convertToString().toString();
            return new RubyIO(context.runtime, loader.getResourceAsStream(resName)).read(context);
        }

    }

    private static final class UByteGet extends JavaMethod.JavaMethodOne {

        UByteGet(RubyModule implClass) {
            super(implClass, PUBLIC);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject idx) {
            final RubyInteger val = (RubyInteger) self.callMethod(context, "[]", idx);
            int byte_val = val.getIntValue();
            if ( byte_val >= 0 ) return val;
            return RubyFixnum.newFixnum(context.runtime, byte_val + 256); // byte += 256 if byte < 0
        }
    }

    private static final class UByteSet extends JavaMethod.JavaMethodTwo {

        UByteSet(RubyModule implClass) {
            super(implClass, PUBLIC);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject idx, IRubyObject val) {
            int byte_val = ((RubyInteger) val).getIntValue();
            if ( byte_val > 127 ) {
                val = RubyFixnum.newFixnum(context.runtime, byte_val - 256); // value -= 256 if value > 127
            }
            return self.callMethod(context, "[]=", new IRubyObject[] { idx, val });
        }
    }

}
