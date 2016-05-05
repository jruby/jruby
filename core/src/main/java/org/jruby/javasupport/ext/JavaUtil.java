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
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.jruby.javasupport.JavaUtil.CAN_SET_ACCESSIBLE;
import static org.jruby.javasupport.JavaUtil.convertJavaArrayToRuby;
import static org.jruby.javasupport.JavaUtil.convertJavaToUsableRubyObject;
import static org.jruby.javasupport.JavaUtil.unwrapJavaObject;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;

/**
 * Java::JavaUtil package extensions.
 *
 * @author kares
 */
public abstract class JavaUtil {

    public static void define(final Ruby runtime) {
        Enumeration.define(runtime);
        Iterator.define(runtime);
        Collection.define(runtime);
        List.define(runtime);
    }

    @JRubyModule(name = "Java::JavaUtil::Enumeration", include = "Enumerable")
    public static class Enumeration {

        static RubyModule define(final Ruby runtime) {
            final RubyModule Enumeration = //Java.getProxyClass(runtime, java.util.Enumeration.class);
                JavaClass.get(runtime, java.util.Enumeration.class).getProxyModule();
            Enumeration.includeModule( runtime.getEnumerable() ); // include Enumerable
            Enumeration.defineAnnotatedMethods(Enumeration.class);
            return Enumeration;
        }

        @JRubyMethod
        public static IRubyObject each(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            java.util.Enumeration enumeration = unwrapJavaObject(self);
            while ( enumeration.hasMoreElements() ) {
                final Object value = enumeration.nextElement();
                block.yield(context, convertJavaToUsableRubyObject(runtime, value));
            }
            return context.nil;
        }

    }

    @JRubyModule(name = "Java::JavaUtil::Iterator", include = "Enumerable")
    public static class Iterator {

        static RubyModule define(final Ruby runtime) {
            final RubyModule Iterator = //Java.getProxyClass(runtime, java.util.Iterator.class);
                JavaClass.get(runtime, java.util.Iterator.class).getProxyModule();
            Iterator.includeModule( runtime.getEnumerable() ); // include Enumerable
            Iterator.defineAnnotatedMethods(Iterator.class);
            return Iterator;
        }

        @JRubyMethod
        public static IRubyObject each(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            java.util.Iterator iterator = unwrapJavaObject(self);
            while ( iterator.hasNext() ) {
                final Object value = iterator.next();
                block.yield(context, convertJavaToUsableRubyObject(runtime, value));
            }
            return context.nil;
        }

    }

    @JRubyModule(name = "Java::JavaUtil::Collection", include = "Enumerable")
    public static class Collection {

        static RubyModule define(final Ruby runtime) {
            final RubyModule Collection = //Java.getProxyClass(runtime, java.util.Collection.class);
                    JavaClass.get(runtime, java.util.Collection.class).getProxyModule();
            Collection.includeModule( runtime.getEnumerable() ); // include Enumerable
            Collection.defineAnnotatedMethods(Collection.class);
            return Collection;
        }

        @JRubyMethod(name = { "length", "size" })
        public static RubyNumeric length(final ThreadContext context, final IRubyObject self) {
            return RubyFixnum.int2fix(context.runtime, ((java.util.Collection) unwrapJavaObject(self)).size());
        }

        @JRubyMethod
        public static IRubyObject each(final ThreadContext context, final IRubyObject self, final Block block) {
            return JavaLang.Iterable.each(context, self, block);
        }

        @JRubyMethod
        public static IRubyObject each_with_index(final ThreadContext context, final IRubyObject self, final Block block) {
            return JavaLang.Iterable.each_with_index(context, self, block);
        }

        // NOTE: first might conflict with some Java types (e.g. java.util.Deque) thus providing a ruby_ alias
        @JRubyMethod(name = { "first", "ruby_first" }) // re-def Enumerable#first
        public static IRubyObject first(final ThreadContext context, final IRubyObject self) {
            final java.util.Collection coll = unwrapJavaObject(self);
            return coll.isEmpty() ? context.nil : convertJavaToUsableRubyObject(context.runtime, coll.iterator().next());
        }

        @JRubyMethod(name = { "first", "ruby_first" }) // re-def Enumerable#first(n)
        public static IRubyObject first(final ThreadContext context, final IRubyObject self, final IRubyObject count) {
            final java.util.Collection coll = unwrapJavaObject(self);
            int len = count.convertToInteger().getIntValue();
            int size = coll.size(); if ( len > size ) len = size;
            final Ruby runtime = context.runtime;
            if ( len == 0 ) return RubyArray.newEmptyArray(runtime);
            final RubyArray arr = RubyArray.newArray(runtime, len);
            int i = 0; for ( java.util.Iterator it = coll.iterator(); i < len; i++ ) {
                arr.append( convertJavaToUsableRubyObject(runtime, it.next() ) );
            }
            return arr;
        }

        @JRubyMethod(name = { "<<" })
        public static IRubyObject append(final IRubyObject self, final IRubyObject item) {
            java.util.Collection coll = unwrapJavaObject(self);
            coll.add( item.toJava(java.lang.Object.class) );
            return self;
        }

        @JRubyMethod
        public static RubyArray to_a(final ThreadContext context, final IRubyObject self) {
            final Object[] array = ((java.util.Collection) unwrapJavaObject(self)).toArray();
            if ( IRubyObject.class.isAssignableFrom(array.getClass().getComponentType()) ) {
                return RubyArray.newArrayNoCopy(context.runtime, (IRubyObject[]) array);
            }
            return RubyArray.newArrayNoCopy(context.runtime, convertJavaArrayToRuby(context.runtime, array));
        }

        @JRubyMethod(name = "+", required = 1)
        public static IRubyObject op_plus(final ThreadContext context, final IRubyObject self, final IRubyObject coll) {
            final IRubyObject dup = self.callMethod(context, "dup");
            java.util.Collection javaDup = unwrapJavaObject(dup);
            if ( coll instanceof java.util.Collection ) { // e.g. RubyArray
                javaDup.addAll((java.util.Collection) coll);
            }
            else {
                javaDup.addAll((java.util.Collection) unwrapJavaObject(coll));
            }
            return dup;
        }

        @JRubyMethod(name = "-", required = 1)
        public static IRubyObject op_minus(final ThreadContext context, final IRubyObject self, final IRubyObject coll) {
            final IRubyObject dup = self.callMethod(context, "dup");
            java.util.Collection javaDup = unwrapJavaObject(dup);
            if ( coll instanceof java.util.Collection ) { // e.g. RubyArray
                javaDup.removeAll((java.util.Collection) coll);
            }
            else {
                javaDup.removeAll((java.util.Collection) unwrapJavaObject(coll));
            }
            return dup;
        }

        @JRubyMethod
        public static IRubyObject dup(final ThreadContext context, final IRubyObject self) {
            java.util.Collection coll = unwrapJavaObject(self);
            final JavaProxy dup = (JavaProxy) self.dup();
            if ( coll == dup.getObject() ) { // not Cloneable
                dup.setObject( tryNewEqualInstance(coll) );
            }
            return dup;
        }

        @JRubyMethod
        public static IRubyObject clone(final ThreadContext context, final IRubyObject self) {
            java.util.Collection coll = unwrapJavaObject(self);
            final JavaProxy dup = (JavaProxy) self.rbClone();
            if ( coll == dup.getObject() ) { // not Cloneable
                dup.setObject( tryNewEqualInstance(coll) );
            }
            return dup;
        }

        // NOTE: join could be implemented natively iterating (without to_a) - but is it actually used much?!?

        @JRubyMethod
        public static IRubyObject join(final ThreadContext context, final IRubyObject self) {
            return to_a(context, self).join(context);
        }

        @JRubyMethod
        public static IRubyObject join(final ThreadContext context, final IRubyObject self, final IRubyObject sep) {
            return to_a(context, self).join(context, sep);
        }

    }

    @JRubyModule(name = "Java::JavaUtil::List")
    public static class List {

        static RubyModule define(final Ruby runtime) {
            final RubyModule List = //Java.getProxyClass(runtime, java.util.List.class);
                    JavaClass.get(runtime, java.util.List.class).getProxyModule();
            List.defineAnnotatedMethods(List.class);
            return List;
        }

        @JRubyMethod(name = "[]") // act safe on indexes compared to get(idx) throwing IndexOutOfBoundsException
        public static IRubyObject aref(final ThreadContext context, final IRubyObject self, final IRubyObject idx) {
            final java.util.List list = unwrapJavaObject(self);
            final int size = list.size();

            if ( idx instanceof RubyRange ) {
                int first = idx.callMethod(context, "first").convertToInteger().getIntValue();
                int last = idx.callMethod(context, "last").convertToInteger().getIntValue();
                if ( last < 0 ) last += size;
                if ( first < 0 ) first += size;
                if ( first < 0 || first >= size ) return context.nil;
                if ( ! ((RubyRange) idx).isExcludeEnd() ) last++;
                if ( last > size ) last = size;
                // NOTE we intentionally do not return an empty list for first >= last as some list might comply!?
                return Java.getInstance(context.runtime, list.subList(first, last));
            }

            int i = idx.convertToInteger().getIntValue();
            if ( i < 0 ) i = size + i; // -1 ... size - 1
            if ( i >= size || i < 0 ) return context.nil;
            return convertJavaToUsableRubyObject(context.runtime, list.get(i));
        }

        @JRubyMethod(name = "[]") // list[-1, 1] like a RubyArray
        public static IRubyObject aref(final ThreadContext context, final IRubyObject self,
            final IRubyObject idx, final IRubyObject len) {

            if ( len.isNil() ) return aref(context, self, idx);

            final java.util.List list = unwrapJavaObject(self);

            int i = idx.convertToInteger().getIntValue();
            final int size = list.size();
            if ( i < 0 ) i = size + i; // -1 ... size - 1
            if ( i >= size || i < 0 ) return context.nil;

            int last = len.convertToInteger().getIntValue();
            if ( last < 0 ) return context.nil;
            last += i; if ( last > size ) last = size;

            return Java.getInstance(context.runtime, list.subList(i, last));
        }

        @JRubyMethod(name = "[]=") // list[-1] = val like a RubyArray
        public static IRubyObject aset(final ThreadContext context, final IRubyObject self,
            final IRubyObject idx, final IRubyObject val) {

            final java.util.List list = unwrapJavaObject(self);
            final int size = list.size();

            if ( idx instanceof RubyRange ) {
                int first = idx.callMethod(context, "first").convertToInteger().getIntValue();
                int last = idx.callMethod(context, "last").convertToInteger().getIntValue();
                if ( last < 0 ) last += size;
                if ( first < 0 ) first += size;
                if ( ((RubyRange) idx).isExcludeEnd() ) last--;
                for ( int i = last; i >= first; i-- ) {
                    if ( i < size ) list.remove(i);
                    else list.add(null);
                }
                list.add(last, val.toJava(java.lang.Object.class));
                return val;
            }

            int i = idx.convertToInteger().getIntValue();
            if ( i < 0 ) i = size + i; // -1 ... size - 1
            if ( i >= size ) {
                for ( int t = 0; t < i - size; t++ ) list.add(null);
                list.add(val.toJava(java.lang.Object.class));
            }
            else {
                list.set(i, val.toJava(java.lang.Object.class));
            }
            return val;
        }

        // NOTE: first conflicts with some Java types e.g. with java.util.LinkedList#getFirst
        @JRubyMethod(name = { "first", "ruby_first" }) // re-def Enumerable#first (to skip iterator)
        public static IRubyObject first(final ThreadContext context, final IRubyObject self) {
            final java.util.List list = unwrapJavaObject(self);
            return list.isEmpty() ? context.nil : convertJavaToUsableRubyObject(context.runtime, list.get(0));
        }

        @JRubyMethod(name = { "first", "ruby_first" }) // #first ext like with array: [1, 2, 3].first(2) == [1, 2]
        public static IRubyObject first(final ThreadContext context, final IRubyObject self, final IRubyObject count) {
            final java.util.List list = unwrapJavaObject(self);
            int len = count.convertToInteger().getIntValue();
            int size = list.size(); if ( len > size ) len = size;
            return Java.getInstance(context.runtime, list.subList(0, len));
        }

        // NOTE: first conflicts with some Java types e.g. with java.util.LinkedList#getLast
        @JRubyMethod(name = { "last", "ruby_last" }) // like with [].last
        public static IRubyObject last(final ThreadContext context, final IRubyObject self) {
            final java.util.List list = unwrapJavaObject(self);
            return list.isEmpty() ? context.nil : convertJavaToUsableRubyObject(context.runtime, list.get(list.size() - 1));
        }

        @JRubyMethod(name = { "last", "ruby_last" }) // #last ext like with array: [1, 2, 3].last(2) == [2, 3]
        public static IRubyObject last(final ThreadContext context, final IRubyObject self, final IRubyObject count) {
            final java.util.List list = unwrapJavaObject(self);
            int len = count.convertToInteger().getIntValue();
            int size = list.size();
            int start = size - len; if ( start < 0 ) start = 0;
            int end = start + len; if ( end > size ) end = size;
            return Java.getInstance(context.runtime, list.subList(start, end));
        }

        @JRubyMethod(name = "index", required = 0) // list.index { |val| val > 0 }
        public static IRubyObject index(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            if ( ! block.isGiven() ) { // list.index ... Enumerator.new(self, :index)
                return runtime.getEnumerator().callMethod("new", self, runtime.newSymbol("index"));
            }

            final java.util.List list = unwrapJavaObject(self);
            if ( list instanceof java.util.RandomAccess ) {
                for ( int i = 0; i < list.size(); i++ ) {
                    IRubyObject ret = block.yield(context, convertJavaToUsableRubyObject(runtime, list.get(i)));
                    if ( ret.isTrue() ) return runtime.newFixnum(i);
                }
            }
            else {
                int i = 0;
                for ( Object elem : list ) {
                    IRubyObject ret = block.yield(context, convertJavaToUsableRubyObject(runtime, elem));
                    if ( ret.isTrue() ) return runtime.newFixnum(i);
                    i++;
                }
            }
            return context.nil;
        }

        @JRubyMethod(name = "index", required = 1) // list.index '42'
        public static IRubyObject index(final ThreadContext context, final IRubyObject self, final IRubyObject val,
            final Block ignoredBlock) {

            final Ruby runtime = context.runtime;
            final java.util.List list = unwrapJavaObject(self);
            if ( list instanceof java.util.RandomAccess ) {
                for ( int i = 0; i < list.size(); i++ ) {
                    final Object elem = list.get(i);
                    if ( val == elem ||
                        invokedynamic(context, val, OP_EQUAL, convertJavaToUsableRubyObject(runtime, elem)).isTrue() ) {
                        return runtime.newFixnum(i);
                    }
                }
            }
            else {
                int i = 0;
                for ( Object elem : list ) {
                    if ( val == elem ||
                        invokedynamic(context, val, OP_EQUAL, convertJavaToUsableRubyObject(runtime, elem)).isTrue() ) {
                        return runtime.newFixnum(i);
                    }
                    i++;
                }
            }
            return context.nil;
        }

        @JRubyMethod(name = "rindex", required = 0) // list.rindex { |val| val > 0 }
        public static IRubyObject rindex(final ThreadContext context, final IRubyObject self, final Block block) {
            final Ruby runtime = context.runtime;
            if ( ! block.isGiven() ) { // list.rindex ... Enumerator.new(self, :rindex)
                return runtime.getEnumerator().callMethod("new", self, runtime.newSymbol("rindex"));
            }

            final java.util.List list = unwrapJavaObject(self);
            if ( list instanceof java.util.RandomAccess ) {
                for ( int i = list.size() - 1; i >= 0; i-- ) {
                    IRubyObject ret = block.yield(context, convertJavaToUsableRubyObject(runtime, list.get(i)));
                    if ( ret.isTrue() ) return runtime.newFixnum(i);
                }
            }
            else {
                int i = list.size() - 1;
                for ( java.util.ListIterator it = list.listIterator(i); it.hasPrevious(); ) {
                    final Object elem = it.previous();
                    IRubyObject ret = block.yield(context, convertJavaToUsableRubyObject(runtime, elem));
                    if ( ret.isTrue() ) return runtime.newFixnum(i);
                    i--;
                }
            }
            return context.nil;
        }

        @JRubyMethod(name = "rindex", required = 1) // list.rindex '42'
        public static IRubyObject rindex(final ThreadContext context, final IRubyObject self, final IRubyObject val,
            final Block ignoredBlock) {

            final Ruby runtime = context.runtime;
            final java.util.List list = unwrapJavaObject(self);
            if ( list instanceof java.util.RandomAccess ) {
                for ( int i = list.size() - 1; i >= 0; i-- ) {
                    final Object elem = list.get(i);
                    if ( val == elem ||
                        invokedynamic(context, val, OP_EQUAL, convertJavaToUsableRubyObject(runtime, elem)).isTrue() ) {
                        return runtime.newFixnum(i);
                    }
                }
            }
            else {
                int i = list.size() - 1;
                for ( java.util.ListIterator it = list.listIterator(i); it.hasPrevious(); ) {
                    final Object elem = it.previous();
                    if ( val == elem ||
                        invokedynamic(context, val, OP_EQUAL, convertJavaToUsableRubyObject(runtime, elem)).isTrue() ) {
                        return runtime.newFixnum(i);
                    }
                    i--;
                }
            }
            return context.nil;
        }

        @JRubyMethod(name = { "to_a", "to_ary" })
        public static RubyArray to_a(final ThreadContext context, final IRubyObject self) {
            // re-implemented to skip an intermediate toArray() conversion :
            final Ruby runtime = context.runtime;
            final java.util.List list = unwrapJavaObject(self);
            final IRubyObject[] array = new IRubyObject[ list.size() ];
            int i = 0; for ( Object elem : list ) {
                array[i++] = convertJavaToUsableRubyObject(runtime, elem);;
            }
            return RubyArray.newArrayNoCopy(runtime, array);
        }

        @SuppressWarnings("unchecked")
        @JRubyMethod(name = { "sort", "ruby_sort" }) // name conflict on Java 8, but users can alias if they want
        public static IRubyObject sort(final ThreadContext context, final IRubyObject self, final Block block) {
            java.util.List dupList = unwrapJavaObject(self.callMethod(context, "dup"));
            if ( dupList == unwrapJavaObject(self) ) {
                // just in case dup failed - make sure we do not use the same list :
                dupList = new java.util.ArrayList(dupList);
                // NOTE: prior to JRuby 9.1 this method always returned an ArrayList
            }
            sortImpl(context, dupList, block);
            return Java.getInstance(context.runtime, dupList);
        }

        @SuppressWarnings("unchecked")
        @JRubyMethod(name = "sort!")
        public static IRubyObject sort_bang(final ThreadContext context, final IRubyObject self, final Block block) {
            final java.util.List list = unwrapJavaObject(self);
            sortImpl(context, list, block);
            return self;
        }

        private static void sortImpl(final ThreadContext context, final java.util.List list, final Block block) {
            final java.util.Comparator comparator = block.isGiven() ?
                    new BlockComparator(context, block) :
                    new SpaceshipComparator(context);

            java.util.Collections.sort(list, comparator);
        }

        private static final class BlockComparator implements java.util.Comparator {

            final ThreadContext context;
            private final Block block;

            BlockComparator(final ThreadContext context, final Block block) {
                this.context = context; this.block = block;
            }

            @Override
            public int compare(final Object o1, final Object o2) {
                final IRubyObject r1, r2;
                if ( o1 instanceof IRubyObject ) r1 = (IRubyObject) o1;
                else r1 = convertJavaToUsableRubyObject(context.runtime, o1);
                if ( o2 instanceof IRubyObject ) r2 = (IRubyObject) o2;
                else r2 = convertJavaToUsableRubyObject(context.runtime, o2);

                return RubyInteger.fix2int( compare(context, r1, r2) );
            }

            // @JRubyMethod
            public final IRubyObject compare(final ThreadContext context,
                final IRubyObject o1, final IRubyObject o2) {
                return block.call(context, o1, o2);
            }

        }

        private static final class SpaceshipComparator implements java.util.Comparator {

            final ThreadContext context;

            SpaceshipComparator(final ThreadContext context) {
                this.context = context;
            }

            @Override
            @SuppressWarnings("unchecked")
            public int compare(final Object o1, final Object o2) {

                if ( o1 instanceof Comparable && o2 instanceof Comparable ) {
                    // IRubyObjects are comparable and compareTo uses <=>
                    return ((Comparable) o1).compareTo(o2);
                }

                final IRubyObject r1, r2;
                if ( o1 instanceof IRubyObject ) r1 = (IRubyObject) o1;
                else r1 = convertJavaToUsableRubyObject(context.runtime, o1);
                if ( o2 instanceof IRubyObject ) r2 = (IRubyObject) o2;
                else r2 = convertJavaToUsableRubyObject(context.runtime, o2);

                return RubyInteger.fix2int( compare(context, r1, r2) );
            }

            // @JRubyMethod
            public final IRubyObject compare(final ThreadContext context,
                final IRubyObject o1, final IRubyObject o2) {
                return o1.callMethod(context, "<=>", o2);
            }

        }

    }

    private static java.util.Collection tryNewEqualInstance(final java.util.Collection coll) {
        final Class<? extends java.util.Collection> klass = coll.getClass();
        // most collections provide a <init>(Collection<? extends E> coll)
        try {
            // most collections provide a <init>(Collection<? extends E> coll)
            // look for it or any matching e.g. <init>(List<? extends E> coll)
            Constructor best = null;
            for ( Constructor ctor : klass.getDeclaredConstructors() ) {
                final Class[] params = ctor.getParameterTypes();
                if ( params.length == 1 && params[0].isAssignableFrom(klass) ) {
                    if ( best == null ) best = ctor;
                    else {
                        // prefer (List param) over (Collection param)
                        if ( best.getParameterTypes()[0].isAssignableFrom(params[0]) ) {
                            best = ctor;
                        }
                    }
                }
            }
            if ( CAN_SET_ACCESSIBLE ) best.setAccessible(true);
            return (java.util.Collection) best.newInstance(coll);
        }
        catch (IllegalAccessException e) {
            // fallback on getConstructor();
        }
        catch (InstantiationException e) {
            Helpers.throwException(e); return null; // should not happen
        }
        catch (InvocationTargetException e) {
            Helpers.throwException(e.getTargetException()); return null;
        }

        try {
            java.util.Collection clone = klass.newInstance();
            clone.addAll(coll);
            return clone;
        }
        catch (IllegalAccessException e) {
            // can not clone - most of Collections. returned types (e.g. EMPTY_LIST)
            return coll;
        }
        catch (InstantiationException e) {
            return coll;
        }
    }

}
