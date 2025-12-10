/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <Ola.Bini@ki.se>
 * Copyright (C) 2006 Daniel Steer <damian.steer@hp.com>
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

package org.jruby;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import java.util.stream.Stream;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.api.JRubyAPI;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.specialized.RubyArrayOneObject;
import org.jruby.specialized.RubyArrayTwoObject;
import org.jruby.util.TypeConverter;
import org.jruby.util.collections.StringArraySet;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.api.Access.fixnumClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Visibility.PRIVATE;

/**
 * The implementation of the built-in class Array in Ruby.
 *
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 * Note: elt(long) is based on notion if we exceed precision by passing in a long larger
 * than int (actually Integer.MAX - 8) then it will just return nil.  Anything using eltOk
 * must know this already to avoid an AIOOBE.  We do catch that in eltOk but only as an
 * attempt at detecting concurrent modifications to the length.  So if you improperly
 * use eltOk you will get a conc error and not AIOOBE.
 *
 * Guidance for elt(long) is that if the Ruby method should raise if it is too large
 * then you need to do that before you call this (toLong vs getValue/asLong).
 *
 * @param <T> What array holds
 *
 */
@JRubyClass(name="Array", include = { "Enumerable" },
        overrides = {RubyArrayNative.class, RubyArrayOneObject.class, RubyArrayTwoObject.class, StringArraySet.class})
public abstract class RubyArray<T extends IRubyObject> extends RubyObject implements List, RandomAccess {
    public static final int DEFAULT_INSPECT_STR_SIZE = 10;

    public static RubyClass createArrayClass(ThreadContext context, RubyClass Object, RubyModule Enumerable) {
        return defineClass(context, "Array", Object, RubyArray::newEmptyArray).
                reifiedClass(RubyArray.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyArray.class)).
                classIndex(ClassIndex.ARRAY).
                include(context, Enumerable).
                defineMethods(context, RubyArray.class);
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.ARRAY;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject create(IRubyObject klass, IRubyObject[] args, Block block) {
        return create(klass.getRuntime().getCurrentContext(), klass, args, block);
    }

     @JRubyMethod(name = "[]", rest = true, meta = true)
     public static IRubyObject create(ThreadContext context, IRubyObject klass, IRubyObject[] args, Block block) {
         return RubyArrayNative.create(context, klass, args, block);
     }

    protected RubyArray(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    protected RubyArray(RubyClass klass) {
        super(klass.runtime, klass);
    }

    protected RubyArray(Ruby runtime, RubyClass klass, boolean objectSpace) {
        super(runtime, klass, objectSpace);
    }

    public static RubyArray<?> newArray(final Ruby runtime, final int len) {
        return RubyArrayNative.newArray(runtime, len);
    }

    public static RubyArray<?> newArray(ThreadContext context, final int len) {
        return RubyArrayNative.newArray(context, len);
    }

    public static RubyArray<?> newArrayLight(final Ruby runtime, final int len) {
        return RubyArrayNative.newArrayLight(runtime, len);
    }

    @Deprecated(since = "10.0.0.0")
    public static final RubyArray<?> newArray(final Ruby runtime) {
        return newArray(runtime.getCurrentContext());
    }

    public static RubyArray<?> newArray(ThreadContext context) {
        return RubyArrayNative.newArray(context);
    }

    public static RubyArray<?> newArrayLight(final Ruby runtime) {
        return RubyArrayNative.newArrayLight(runtime);
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject obj) {
        return RubyArrayNative.newArray(runtime, obj);
    }

    public static RubyArray<?> newArrayLight(Ruby runtime, IRubyObject obj) {
        return RubyArrayNative.newArrayLight(runtime, obj);
    }

    public static RubyArray<?> newArrayLight(RubyClass arrayClass, IRubyObject obj) {
        return RubyArrayNative.newArrayLight(arrayClass, obj);
    }

    public static RubyArray<?> newArrayLight(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return RubyArrayNative.newArrayLight(runtime, car, cdr);
    }

    public static RubyArray<?> newArrayLight(RubyClass arrayClass, IRubyObject car, IRubyObject cdr) {
        return RubyArrayNative.newArrayLight(arrayClass, car, cdr);
    }

    public static RubyArray<?> newArrayLight(Ruby runtime, IRubyObject... objs) {
        return RubyArrayNative.newArrayLight(runtime, objs);
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return RubyArrayNative.newArray(runtime, car, cdr);
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject first, IRubyObject second, IRubyObject third) {
        return RubyArrayNative.newArray(runtime, first, second, third);
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject first, IRubyObject second, IRubyObject third, IRubyObject fourth) {
        return RubyArrayNative.newArray(runtime, first, second, third, fourth);
    }

    public static RubyArray<?> newEmptyArray(Ruby runtime) {
        return RubyArrayNative.newEmptyArray(runtime);
    }

    public static RubyArray<?> newEmptyArray(Ruby runtime, RubyClass klass) {
        return RubyArrayNative.newEmptyArray(runtime, klass);
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject[] args) {
        return RubyArrayNative.newArray(runtime, args);
    }

    public static RubyArray<?> newArray(Ruby runtime, Collection<? extends IRubyObject> collection) {
        return RubyArrayNative.newArray(runtime, collection);
    }

    public static RubyArray<?> newArray(Ruby runtime, List<? extends IRubyObject> list) {
        return RubyArrayNative.newArray(runtime, list);
    }

    public static RubyArray<?> newSharedArray(RubyClass arrayClass, IRubyObject[] shared) {
        return RubyArrayNative.newSharedArray(arrayClass, shared);
    }

    public static RubyArray newArrayMayCopy(Ruby runtime, IRubyObject... args) {
        return RubyArrayNative.newArrayMayCopy(runtime, args);
    }

    public static RubyArray newArrayMayCopy(Ruby runtime, IRubyObject[] args, int start) {
        return RubyArrayNative.newArrayMayCopy(runtime, args, start);
    }

    /**
     * Construct a new RubyArray given the specified range of elements in the source array. The elements
     * <i>may</i> be copied into a new backing store, and therefore you should not expect future changes to the
     * source array to be reflected. Conversely, you should not modify the array after passing it, since
     * the contents <i>may not</i> be copied.
     *
     * @param runtime the runtime
     * @param args the args
     * @param start start index
     * @param length number of elements
     * @return an array referencing the given elements
     */
    public static RubyArray newArrayMayCopy(Ruby runtime, IRubyObject[] args, int start, int length) {
        return RubyArrayNative.newArrayMayCopy(runtime, args, start, length);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject... args) {
        return RubyArrayNative.newArrayNoCopy(runtime, args);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args, int begin) {
        return RubyArrayNative.newArrayNoCopy(runtime, args, begin);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args, int begin, int length) {
        return RubyArrayNative.newArrayNoCopy(runtime, args, begin, length);
    }

    public static RubyArray newArrayNoCopyLight(Ruby runtime, IRubyObject[] args) {
        return RubyArrayNative.newArrayNoCopyLight(runtime, args);
    }

    @Deprecated(since = "10.0.0.0")
    protected static final void checkLength(Ruby runtime, long length) {
        checkLength(runtime.getCurrentContext(), length);
    }

    public static final int checkLength(ThreadContext context, long length) {
        if (length < 0) throw argumentError(context, "negative array size (or size too big)");
        if (length >= Integer.MAX_VALUE) throw argumentError(context, "array size too big");
        return (int) length;
    }

    /**
     * @deprecated RubyArray implements List, use it directly
     * @return a read-only copy of this list
     */
    @Deprecated(since = "9.4-")
    public final List<IRubyObject> getList() {
        return Arrays.asList(toJavaArray());
    }

    public abstract int getLength();

    /**
     * @return ""
     * @deprecated Use {@link RubyArray#toJavaArray(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public IRubyObject[] toJavaArray() {
        return toJavaArray(getCurrentContext());
    }

    /**
     * Return a Java array copy of the elements contained in this Array.
     *
     * This version always creates a new Java array that is exactly the length of the Array's elements.
     *
     * @return a Java array with exactly the size and contents of this RubyArray's elements
     */
    public abstract IRubyObject[] toJavaArray(ThreadContext context);

    /**
     * Return a reference to this RubyArray's underlying Java array, if it is not shared with another RubyArray, or
     * an exact copy of the relevant range otherwise.
     *
     * This method is typically used to work with the underlying array directly, knowing that it is not shared and that
     * all accesses must consider the begin offset.
     *
     * @return The underlying Java array for this RubyArray, or a copy if that array is shared.
     */

    public abstract IRubyObject[] toJavaArrayUnsafe();

    /**
     * Return a Java array of the elements contained in this array, possibly a new array object.
     *
     * Use this method to potentially avoid making a new array and copying elements when the Array does not view a
     * subset of the underlying Java array.
     *
     * @return a Java array with exactly the size and contents of this RubyArray's elements, possibly the actual
     *         underlying array.
     */
    public abstract IRubyObject[] toJavaArrayMaybeUnsafe();

    public abstract boolean isSharedJavaArray(RubyArray other);

    @JRubyMethod(visibility = PRIVATE)
    public abstract IRubyObject initialize(ThreadContext context, Block block);

    @JRubyMethod(visibility = PRIVATE)
    public abstract IRubyObject initialize(ThreadContext context, IRubyObject arg0, Block block);

    @JRubyMethod(visibility = PRIVATE)
    public abstract IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block);

    @JRubyMethod(name = {"initialize_copy"}, visibility=PRIVATE)
    public abstract IRubyObject initialize_copy(ThreadContext context, IRubyObject orig);

    public abstract RubyArray aryDup();

    @Deprecated(since = "10.0.0.0")
    public IRubyObject replace(IRubyObject orig) {
        return replace(getCurrentContext(), orig);
    }

    @JRubyMethod(name = {"replace"})
    public abstract IRubyObject replace(ThreadContext context, IRubyObject orig);

    @Override
    public abstract RubyString to_s(ThreadContext context);

    public abstract boolean includes(ThreadContext context, IRubyObject item);

    protected abstract boolean includesByEql(ThreadContext context, IRubyObject item);

    @JRubyMethod(name = "hash")
    public abstract RubyFixnum hash(ThreadContext context);

    public abstract IRubyObject store(long index, IRubyObject value);

    /**
     * Store an element at the specified index or throw if the index is invalid.
     * @param context the current thread context
     * @param index the offset to store the value
     * @param value the value to be stored
     * @return the value set
     */
    @JRubyAPI
    public abstract IRubyObject store(ThreadContext context, long index, IRubyObject value);

    // note: packed arrays will unpack in overridden version of this
    protected abstract void storeInternal(ThreadContext context, int index, IRubyObject value);

    protected abstract IRubyObject elt(long offset);

    public abstract T eltOk(long offset);

    public abstract T eltSetOk(long offset, T value);

    public abstract T eltSetOk(int offset, T value);

    public abstract IRubyObject entry(long offset);

    public abstract IRubyObject entry(int offset);

    public abstract T eltInternal(int offset);

    public abstract T eltInternalSet(int offset, T item);

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 1:
                return fetch(context, args[0], block);
            case 2:
                return fetch(context, args[0], args[1], block);
            default:
                Arity.raiseArgumentError(context, args.length, 1, 2);
                return null; // not reached
        }
    }

    @JRubyMethod(rest = true)
    public abstract IRubyObject fetch_values(ThreadContext context, IRubyObject[] args, Block block);

    @JRubyMethod
    public abstract IRubyObject fetch(ThreadContext context, IRubyObject arg0, Block block);

    @JRubyMethod
    public abstract IRubyObject fetch(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block);

    public static RubyArray aryToAry(ThreadContext context, IRubyObject obj) {
        IRubyObject tmp = TypeConverter.checkArrayType(context, obj);
        return tmp != context.nil ? (RubyArray) tmp : RubyArray.newArray(context.runtime, obj);
    }

    @Deprecated(since = "9.2.5.0")
    public static RubyArray aryToAry(IRubyObject obj) {
        return aryToAry(obj.getRuntime().getCurrentContext(), obj);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject insert(IRubyObject arg) {
        return insert(getCurrentContext(), arg);
    }

    @JRubyMethod(name = "insert")
    public abstract IRubyObject insert(ThreadContext context, IRubyObject arg);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject insert(IRubyObject arg1, IRubyObject arg2) {
        return insert(getCurrentContext(), arg1, arg2);
    }

    @JRubyMethod(name = "insert")
    public abstract IRubyObject insert(ThreadContext context, IRubyObject arg1, IRubyObject arg2);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject insert(IRubyObject[] args) {
        return insert(getCurrentContext(), args);
    }

    @JRubyMethod(name = "insert", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject insert(ThreadContext context, IRubyObject[] args);

    @Deprecated(since = "10.0.0.0")
    public RubyArray transpose() {
        return transpose(getCurrentContext());
    }

    @JRubyMethod(name = "transpose")
    public abstract RubyArray transpose(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject values_at(IRubyObject[] args) {
        return values_at(getCurrentContext(), args);
    }

    @JRubyMethod(name = "values_at", rest = true)
    public abstract IRubyObject values_at(ThreadContext context, IRubyObject[] args);

    public abstract IRubyObject subseqLight(long beg, long len);

    public abstract IRubyObject subseq(RubyClass metaClass, long beg, long len, boolean light);

    @JRubyMethod(name = "length", alias = "size")
    public abstract RubyFixnum length(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public RubyFixnum length() {
        return length(getCurrentContext());
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    protected static IRubyObject size(ThreadContext context, RubyArray self, IRubyObject[] args) {
        return self.length(context);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray<?> append(IRubyObject item) {
        return append(getCurrentContext(), item);
    }

    @JRubyMethod(name = "<<")
    public abstract RubyArray append(ThreadContext context, IRubyObject item);

    @Deprecated // not-used
    public RubyArray<?> push_m(IRubyObject[] items) {
        return push(items);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray push(IRubyObject item) {
        append(item);

        return this;
    }

    @JRubyMethod(name = "push", alias = "append")
    public abstract RubyArray push(ThreadContext context, IRubyObject item);

    @Deprecated(since = "10.0.0.0")
    public RubyArray<?> push(IRubyObject[] items) {
        return push(getCurrentContext(), items);
    }

    @JRubyMethod(name = "push", alias = "append", rest = true)
    public abstract RubyArray push(ThreadContext context, IRubyObject[] items);

    @JRubyMethod
    public abstract IRubyObject pop(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject pop(ThreadContext context, IRubyObject num);

    @JRubyMethod(name = "shift")
    public abstract IRubyObject shift(ThreadContext context);

    @JRubyMethod(name = "shift")
    public abstract IRubyObject shift(ThreadContext context, IRubyObject num);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject unshift() {
        return unshift(getCurrentContext());
    }

    @JRubyMethod(name = "unshift", alias = "prepend")
    public abstract IRubyObject unshift(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject unshift(IRubyObject item) {
        return unshift(getCurrentContext(), item);
    }

    @JRubyMethod(name = "unshift", alias = "prepend")
    public abstract IRubyObject unshift(ThreadContext context, IRubyObject item);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject unshift(IRubyObject[] items) {
        return unshift(getCurrentContext(), items);
    }

    @JRubyMethod(name = "unshift", alias = "prepend",  rest = true)
    public abstract IRubyObject unshift(ThreadContext context, IRubyObject[] items);

    @JRubyMethod(name = "include?")
    public abstract RubyBoolean include_p(ThreadContext context, IRubyObject item);

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "10.0.0.0")
    public IRubyObject aref(IRubyObject[] args) {
        ThreadContext context = getCurrentContext();
        return switch (args.length) {
            case 1 -> aref(context, args[0]);
            case 2 -> aref(context, args[0], args[1]);
            default -> {
                Arity.raiseArgumentError(context, args.length, 1, 2);
                yield null;
            }
        };
    }

    @Deprecated(since = "9.4.0.0")
    public IRubyObject aref(IRubyObject arg0) {
        return aref(getCurrentContext(), arg0);
    }

    @JRubyMethod(name = {"[]", "slice"})
    public abstract IRubyObject aref(ThreadContext context, IRubyObject arg0);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject aref(IRubyObject arg0, IRubyObject arg1) {
        return aref(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = {"[]", "slice"})
    public abstract IRubyObject aref(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject aset(IRubyObject[] args) {
        return switch (args.length) {
            case 2 -> aset(args[0], args[1]);
            case 3 -> aset(args[0], args[1], args[2]);
            default -> throw argumentError(getCurrentContext(), "wrong number of arguments (" + args.length + " for 2)");
        };
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1) {
        return aset(getCurrentContext(), arg0, arg1);
    }

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     * @return ""
     * @deprecated Use {@link RubyArray#aset(ThreadContext, IRubyObject, IRubyObject, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return aset(getCurrentContext(), arg0, arg1, arg2);
    }

    @JRubyMethod(name = "[]=")
    public abstract IRubyObject aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "[]=")
    public abstract IRubyObject aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);

    /**
     * @param pos
     * @return ""
     * @deprecated Use {@link RubyArray#at(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0.0.0")
    public IRubyObject at(IRubyObject pos) {
        return at(getCurrentContext(), pos);
    }

    @JRubyMethod(name = "at")
    public abstract IRubyObject at(ThreadContext context, IRubyObject pos);

    @JRubyMethod(name = "concat")
    public abstract RubyArray concat(ThreadContext context, IRubyObject obj);

    @Deprecated(since = "10.0.0.0")
    public RubyArray aryAppend(RubyArray y) {
        return aryAppend(getCurrentContext(), y);
    }

    public abstract RubyArray aryAppend(ThreadContext context, RubyArray<?> y);

    @JRubyMethod(name = "concat", rest = true)
    public abstract RubyArray concat(ThreadContext context, IRubyObject[] objs);

    public abstract RubyArray concat(IRubyObject obj);

    @JRubyMethod(name = "inspect", alias = "to_s")
    public abstract RubyString inspect(ThreadContext context);

    @Deprecated(since = "9.4-")
    public IRubyObject first(IRubyObject[] args) {
        return switch (args.length) {
            case 0 -> first(getCurrentContext());
            case 1 -> first(getCurrentContext(), args[0]);
            default -> {
                Arity.raiseArgumentError(getCurrentContext(), args.length, 0, 1);
                yield null;
            }
        };
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject first() {
        return first(getCurrentContext());
    }

    @JRubyAPI
    @JRubyMethod(name = "first")
    public abstract IRubyObject first(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject first(IRubyObject arg0) {
        return first(getCurrentContext(), arg0);
    }

    @JRubyAPI
    @JRubyMethod(name = "first")
    public abstract IRubyObject first(ThreadContext context, IRubyObject arg0);

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject last(IRubyObject[] args) {
        return switch (args.length) {
            case 0 -> last(getCurrentContext());
            case 1 -> last(getCurrentContext(), args[0]);
            default -> {
                Arity.raiseArgumentError(getCurrentContext(), args.length, 0, 1);
                yield null;
            }
        };
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject last() {
        return last(getCurrentContext());
    }

    @JRubyAPI
    @JRubyMethod(name = "last")
    public abstract IRubyObject last(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject last(IRubyObject arg0) {
        return last(getCurrentContext(), arg0);
    }

    @JRubyMethod(name = "last")
    public abstract IRubyObject last(ThreadContext context, IRubyObject arg0);

    @JRubyMethod
    public abstract IRubyObject each(ThreadContext context, Block block);

    public abstract IRubyObject eachSlice(ThreadContext context, int size, Block block);

    @JRubyMethod
    public abstract IRubyObject each_slice(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod
    public abstract IRubyObject each_index(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject reverse_each(ThreadContext context, Block block);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject join19(final ThreadContext context, IRubyObject sep) {
        return join(context, sep);
    }

    @JRubyMethod(name = "join")
    public abstract IRubyObject join(final ThreadContext context, IRubyObject sep);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject join19(ThreadContext context) {
        return join(context);
    }

    @JRubyMethod(name = "join")
    public abstract IRubyObject join(ThreadContext context);

    @JRubyMethod(name = "to_a")
    @Override
    public abstract RubyArray to_a(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_ary() {
        return this;
    }

    @Deprecated(since = "9.3.0.0")
    public IRubyObject to_h(ThreadContext context) {
        return to_h(context, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "to_ary")
    public final IRubyObject to_ary(ThreadContext context) {
    	return this;
    }

    @JRubyMethod(name = "to_h")
    public abstract IRubyObject to_h(ThreadContext context, Block block);

    @Override
    public final RubyArray convertToArray() {
        return this;
    }

    @Override
    public final IRubyObject checkArrayType(){
        return this;
    }

    @JRubyMethod(name = "==")
    @Override
    public abstract IRubyObject op_equal(ThreadContext context, IRubyObject obj);

    public abstract RubyBoolean compare(ThreadContext context, CallSite site, IRubyObject other);

    @JRubyMethod(name = "eql?")
    public abstract IRubyObject eql(ThreadContext context, IRubyObject obj);

    /**
     * @return ""
     * @deprecated Use {@link RubyArray#compact_bang(ThreadContext)}
     */
    @Deprecated(since = "10.0.0.0")
    public IRubyObject compact_bang() {
        return compact_bang(getCurrentContext());
    }

    @JRubyMethod(name = "compact!")
    public abstract IRubyObject compact_bang(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject compact() {
        return compact(getCurrentContext());
    }

    @JRubyMethod(name = "compact")
    public abstract IRubyObject compact(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject empty_p() {
        return empty_p(getCurrentContext());
    }

    @JRubyMethod(name = "empty?")
    public abstract IRubyObject empty_p(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject rb_clear() {
        return rb_clear(getCurrentContext());
    }

    @JRubyMethod(name = "clear")
    public abstract IRubyObject rb_clear(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject fill(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject fill(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod
    public abstract IRubyObject fill(ThreadContext context, IRubyObject arg1, IRubyObject arg2, Block block);

    @JRubyMethod
    public abstract IRubyObject fill(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);

    public abstract IRubyObject index(ThreadContext context, IRubyObject obj);

    @JRubyMethod(name = {"index", "find_index"})
    public abstract IRubyObject index(ThreadContext context, IRubyObject obj, Block unused);

    @JRubyMethod(name = {"index", "find_index"})
    public abstract IRubyObject index(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject bsearch(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject bsearch_index(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject rindex(ThreadContext context, IRubyObject obj, Block unused);

    @JRubyMethod
    public abstract IRubyObject rindex(ThreadContext context, Block block);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject indexes(IRubyObject[] args) {
        return indexes(getCurrentContext(), args);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject indexes(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        warn(context, "Array#indexes is deprecated; use Array#values_at");

        if (argc == 1) return Create.newArray(context, args[0]);

        RubyArrayNative ary = RubyArrayNative.newBlankArrayInternal(context.runtime, argc);

        for (int i = 0; i < argc; i++) {
            ary.storeInternal(context, i, aref(context, args[i]));
        }
        ary.realLength = argc;

        return ary;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject reverse_bang() {
        return reverse_bang(getCurrentContext());
    }

    @JRubyMethod(name = "reverse!")
    public abstract IRubyObject reverse_bang(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject reverse() {
        return reverse(getCurrentContext());
    }

    @JRubyAPI
    @JRubyMethod(name = "reverse")
    public abstract IRubyObject reverse(ThreadContext context);

    @JRubyMethod(name = {"collect"})
    public abstract IRubyObject rbCollect(ThreadContext context, Block block);

    @JRubyMethod(name = {"map"})
    public abstract IRubyObject map(ThreadContext context, Block block);

    @JRubyMethod(name = "collect!")
    public abstract IRubyObject collect_bang(ThreadContext context, Block block);

    @JRubyMethod(name = "map!")
    public abstract IRubyObject map_bang(ThreadContext context, Block block);

    @JRubyMethod(name = "select", alias = "filter")
    public abstract IRubyObject select(ThreadContext context, Block block);

    @JRubyMethod(name = "select!", alias = "filter!")
    public abstract IRubyObject select_bang(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject keep_if(ThreadContext context, Block block);

    @JRubyMethod
    public final IRubyObject deconstruct(ThreadContext context) {
        return this;
    }

    @JRubyMethod
    public abstract IRubyObject delete(ThreadContext context, IRubyObject item, Block block);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject delete_at(int pos) {
        return delete_at(getCurrentContext(), pos);
    }

    public abstract IRubyObject delete_at(ThreadContext context, int pos);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject delete_at(IRubyObject obj) {
        return delete_at(getCurrentContext(), obj);
    }

    @JRubyMethod(name = "delete_at")
    public abstract IRubyObject delete_at(ThreadContext context, IRubyObject obj);

    @JRubyMethod
    public abstract IRubyObject reject(ThreadContext context, Block block);

    @JRubyMethod(name = "reject!")
    public abstract IRubyObject reject_bang(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject delete_if(ThreadContext context, Block block);

    @JRubyMethod(optional = 1, rest = true, checkArity = false)
    public abstract IRubyObject zip(ThreadContext context, IRubyObject[] args, Block block);

    // This can be shared with RubyEnumerable to clean #zipCommon{Enum,Arg} a little
    public static interface ArgumentVisitor {
        IRubyObject visit(ThreadContext ctx, IRubyObject arg, int i);
    }

    @JRubyMethod(name = "<=>")
    public abstract IRubyObject op_cmp(ThreadContext context, IRubyObject obj);

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject slice_bang(IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return slice_bang(args[0]);
            case 2:
                return slice_bang(args[0], args[1]);
            default:
                Arity.raiseArgumentError(getCurrentContext(), args.length, 1, 2);
                return null; // not reached
        }
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject slice_bang(IRubyObject arg0) {
        return slice_bang(getCurrentContext(), arg0);
    }

    @JRubyMethod(name = "slice!")
    public abstract IRubyObject slice_bang(ThreadContext context, IRubyObject arg0);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject slice_bang(IRubyObject arg0, IRubyObject arg1) {
        return slice_bang(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "slice!")
    public abstract IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "assoc")
    public abstract IRubyObject assoc(ThreadContext context, IRubyObject key);

    @JRubyMethod(name = "rassoc")
    public abstract IRubyObject rassoc(ThreadContext context, IRubyObject value);

    @JRubyMethod(name = "flatten!")
    public abstract IRubyObject flatten_bang(ThreadContext context);

    @JRubyMethod(name = "flatten!")
    public abstract IRubyObject flatten_bang(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "flatten")
    public abstract IRubyObject flatten(ThreadContext context);

    @JRubyMethod(name = "flatten")
    public abstract IRubyObject flatten(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "count")
    public abstract IRubyObject count(ThreadContext context, Block block);

    @JRubyMethod(name = "count")
    public abstract IRubyObject count(ThreadContext context, IRubyObject obj, Block block);

    @JRubyMethod(name = "nitems")
    public abstract IRubyObject nitems(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject nitems() {
        return nitems(getCurrentContext());
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_plus(IRubyObject obj) {
        return op_plus(getCurrentContext(), obj);
    }

    @JRubyMethod(name = "+")
    public abstract IRubyObject op_plus(ThreadContext context, IRubyObject obj);

    @JRubyMethod(name = "*")
    public abstract IRubyObject op_times(ThreadContext context, IRubyObject times);

    protected abstract RubyHash makeHash(Ruby runtime);

    protected abstract RubyHash makeHash(RubyHash hash);

    @JRubyMethod(name = "uniq!")
    public abstract IRubyObject uniq_bang(ThreadContext context, Block block);

    @JRubyMethod(name = "uniq")
    public abstract IRubyObject uniq(ThreadContext context, Block block);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_diff(IRubyObject other) {
        return op_diff(getCurrentContext(), other);
    }

    @JRubyMethod(name = "-")
    public abstract IRubyObject op_diff(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "difference", rest = true)
    public abstract IRubyObject difference(ThreadContext context, IRubyObject[] args);

    @JRubyMethod(rest = true)
    public abstract IRubyObject intersection(ThreadContext context, IRubyObject[] args);

    @JRubyMethod(name = "intersect?")
    public abstract IRubyObject intersect_p(ThreadContext context, IRubyObject other);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_and(IRubyObject other) {
        return op_and(getCurrentContext(), other);
    }

    @JRubyMethod(name = "&")
    public abstract IRubyObject op_and(ThreadContext context, IRubyObject other);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_or(IRubyObject other) {
        return op_or(getCurrentContext(), other);
    }

    @JRubyMethod(name = "|")
    public abstract IRubyObject op_or(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "union", rest = true)
    public abstract IRubyObject union(ThreadContext context, IRubyObject[] args);

    protected abstract void unionInternal(ThreadContext context, RubyArray... args);

    @JRubyMethod(name = "sort")
    public abstract RubyArray sort(ThreadContext context, Block block);

    @JRubyMethod(name = "sort!")
    public abstract IRubyObject sort_bang(ThreadContext context, Block block);

    public static class DefaultComparator implements Comparator<IRubyObject> {

        final ThreadContext context;

        private final boolean fixnumBypass;
        private final boolean stringBypass;

        public DefaultComparator(ThreadContext context) {
            this(context, true);
        }

        DefaultComparator(ThreadContext context, final boolean honorOverride) {
            this.context = context;
            if ( honorOverride && context != null ) {
                this.fixnumBypass = !honorOverride || fixnumClass(context).isMethodBuiltin("<=>");
                this.stringBypass = !honorOverride || stringClass(context).isMethodBuiltin("<=>");
            }
            else { // no-opt
                this.fixnumBypass = false;
                this.stringBypass = false;
            }
        }

        /*
        DefaultComparator(ThreadContext context, final boolean fixnumBypass, final boolean stringBypass) {
            this.context = context;
            this.fixnumBypass = fixnumBypass;
            this.stringBypass = stringBypass;
        } */

        public int compare(IRubyObject obj1, IRubyObject obj2) {
            if (fixnumBypass && obj1 instanceof RubyFixnum fix1 && obj2 instanceof RubyFixnum fix2) {
                return compareInteger(fix1, fix2);
            }
            if (stringBypass && obj1 instanceof RubyString str1 && obj2 instanceof RubyString str2) {
                return compareString(str1, str2);
            }
            return compareGeneric(obj1, obj2);
        }

        protected int compareGeneric(IRubyObject o1, IRubyObject o2) {
            final ThreadContext context = context();
            return compareGeneric(context, sites(context).op_cmp_sort, o1, o2);
        }

        protected ThreadContext context() {
            return context;
        }

        public static int compareInteger(RubyFixnum o1, RubyFixnum o2) {
            long a = o1.getValue();
            long b = o2.getValue();
            return a > b ? 1 : a == b ? 0 : -1;
        }

        public static int compareString(RubyString o1, RubyString o2) {
            return o1.op_cmp(o2);
        }

        public static int compareGeneric(ThreadContext context, IRubyObject o1, IRubyObject o2) {
            return compareGeneric(context, sites(context).op_cmp_sort, o1, o2);
        }

        public static int compareGeneric(ThreadContext context, CallSite op_cmp_sort, IRubyObject o1, IRubyObject o2) {
            IRubyObject ret = op_cmp_sort.call(context, o1, o1, o2);
            return RubyComparable.cmpint(context, ret, o1, o2);
        }

    }

    @JRubyMethod(name = "sort_by!")
    public abstract IRubyObject sort_by_bang(ThreadContext context, Block block);

    @JRubyMethod(name = "take")
    public abstract IRubyObject take(ThreadContext context, IRubyObject n);

    @JRubyMethod(name = "take_while")
    public abstract IRubyObject take_while(ThreadContext context, Block block);

    @JRubyMethod(name = "drop")
    public abstract IRubyObject drop(ThreadContext context, IRubyObject n);

    @JRubyMethod(name = "drop_while")
    public abstract IRubyObject drop_while(ThreadContext context, Block block);

    @JRubyMethod(name = "cycle")
    public abstract IRubyObject cycle(ThreadContext context, Block block);

    @JRubyMethod(name = "cycle")
    public abstract IRubyObject cycle(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod(name = "product", rest = true)
    public abstract IRubyObject product(ThreadContext context, IRubyObject[] args, Block block);

    @JRubyMethod(name = "combination")
    public abstract IRubyObject combination(ThreadContext context, IRubyObject num, Block block);

    @JRubyMethod(name = "repeated_combination")
    public abstract IRubyObject repeatedCombination(ThreadContext context, IRubyObject num, Block block);

    @JRubyMethod(name = "permutation")
    public abstract IRubyObject permutation(ThreadContext context, IRubyObject num, Block block);

    @JRubyMethod(name = "permutation")
    public abstract IRubyObject permutation(ThreadContext context, Block block);

    @JRubyMethod(name = "repeated_permutation")
    public abstract IRubyObject repeated_permutation(ThreadContext context, IRubyObject num, Block block);

    @JRubyMethod(name = "shuffle!")
    public abstract IRubyObject shuffle_bang(ThreadContext context);

    @JRubyMethod(name = "shuffle!")
    public abstract IRubyObject shuffle_bang(ThreadContext context, IRubyObject opts);

    @JRubyMethod(name = "shuffle")
    public abstract IRubyObject shuffle(ThreadContext context);

    @JRubyMethod(name = "shuffle")
    public abstract IRubyObject shuffle(ThreadContext context, IRubyObject opts);

    @JRubyMethod(name = "sample")
    public abstract IRubyObject sample(ThreadContext context);

    @JRubyMethod(name = "sample")
    public abstract IRubyObject sample(ThreadContext context, IRubyObject sampleOrOpts);

    @JRubyMethod(name = "sample")
    public abstract IRubyObject sample(ThreadContext context, IRubyObject sample, IRubyObject opts);

    @JRubyMethod(name = "rotate!")
    public abstract IRubyObject rotate_bang(ThreadContext context);

    @JRubyMethod(name = "rotate!")
    public abstract IRubyObject rotate_bang(ThreadContext context, IRubyObject cnt);

    @JRubyMethod(name = "rotate")
    public abstract IRubyObject rotate(ThreadContext context);

    @JRubyMethod(name = "rotate")
    public abstract IRubyObject rotate(ThreadContext context, IRubyObject cnt);

    @JRubyMethod(name = "all?")
    public abstract IRubyObject all_p(ThreadContext context, Block block);

    @JRubyMethod(name = "all?")
    public abstract IRubyObject all_p(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod(name = "any?")
    public abstract IRubyObject any_p(ThreadContext context, Block block);

    @JRubyMethod(name = "any?")
    public abstract IRubyObject any_p(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod(name = "none?")
    public abstract IRubyObject none_p(ThreadContext context, Block block);

    @JRubyMethod(name = "none?")
    public abstract IRubyObject none_p(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod(name = "one?")
    public abstract IRubyObject one_p(ThreadContext context, Block block);

    @JRubyMethod(name = "one?")
    public abstract IRubyObject one_p(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod
    public abstract IRubyObject sum(final ThreadContext context, final Block block);

    @JRubyMethod
    public abstract IRubyObject sum(final ThreadContext context, IRubyObject init, final Block block);

    public abstract IRubyObject find(ThreadContext context, IRubyObject ifnone, Block block);

    public abstract IRubyObject find_index(ThreadContext context, Block block);

    public abstract IRubyObject find_index(ThreadContext context, IRubyObject cond);

    @Deprecated(since = "10.0.0.0")
    public static RubyArray newBlankArray(Ruby runtime, int size) {
        return RubyArray.newBlankArray(runtime.getCurrentContext(), size);
    }

    /**
     * Construct the most efficient array shape for the given size. This should only be used when you
     * intend to populate all elements, since the packed arrays will be born with a nonzero size and
     * would have to be unpacked to partially populate.
     *
     * We nil-fill all cases, to ensure nulls will never leak out if there's an unpopulated element
     * or an index accessed before assignment.
     *
     * @param context the current thread context
     * @param size the size
     * @return a RubyArray shaped for the given size
     */
    public static RubyArray newBlankArray(ThreadContext context, int size) {
        return RubyArrayNative.newBlankArray(context, size);
    }

    @JRubyMethod(name = "try_convert", meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return arg.checkArrayType();
    }

    @JRubyMethod(name = "pack")
    public abstract RubyString pack(ThreadContext context, IRubyObject obj);

    @JRubyMethod(name = "pack")
    public abstract RubyString pack(ThreadContext context, IRubyObject obj, IRubyObject maybeOpts);

    @JRubyMethod(name = "dig")
    public abstract IRubyObject dig(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(name = "dig")
    public abstract IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "dig")
    public abstract IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);

    @JRubyMethod(name = "dig", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject dig(ThreadContext context, IRubyObject[] args);

    @JRubyMethod(name = "max")
    public abstract IRubyObject max(ThreadContext context, Block block);

    @JRubyMethod(name = "max")
    public abstract IRubyObject max(ThreadContext context, IRubyObject num, Block block);

    @JRubyMethod(name = "min")
    public abstract IRubyObject min(ThreadContext context, Block block);

    @JRubyMethod(name = "min")
    public abstract IRubyObject min(ThreadContext context, IRubyObject num, Block block);

    @JRubyMethod
    public abstract IRubyObject minmax(ThreadContext context, Block block);

    @Override
    public Class getJavaClass() {
        return List.class;
    }

    @Deprecated(since = "10.0.0.0")
    public void copyInto(IRubyObject[] target, int start) {
        copyInto(getCurrentContext(), target, start);
    }

    /**
     * Copy the values contained in this array into the target array at the specified offset.
     * It is expected that the target array is large enough to hold all necessary values.
     */
    public abstract void copyInto(ThreadContext context, IRubyObject[] target, int start);

    @Deprecated(since = "10.0.0.0")
    public void copyInto(IRubyObject[] target, int start, int len) {
        copyInto(getCurrentContext(), target, start, len);
    }

    /**
     * Copy the specified number of values contained in this array into the target array at the specified offset.
     * It is expected that the target array is large enough to hold all necessary values.
     */
    public abstract void copyInto(ThreadContext context, IRubyObject[] target, int start, int len);

    @Override
    public abstract boolean equals(Object other);

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static void marshalTo(RubyArray array, org.jruby.runtime.marshal.MarshalStream output) throws IOException {
        marshalTo(((RubyBasicObject) array).getCurrentContext(), array, output);
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static void marshalTo(ThreadContext context, RubyArray array, org.jruby.runtime.marshal.MarshalStream output) throws IOException {
        output.registerLinkTarget(context, array);

        int length = array.getLength();

        output.writeInt(length);

        for (int i = 0; i < length; i++) {
            output.dumpObject(array.eltOk(i));
        }
    }

    public static void marshalTo(ThreadContext context, RubyOutputStream out, RubyArray array, MarshalDumper output) {
        output.registerLinkTarget(array);

        int length = array.getLength();

        output.writeInt(out, length);

        for (int i = 0; i < length; i++) {
            output.dumpObject(context, out, array.eltOk(i));
        }
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static RubyArray unmarshalFrom(org.jruby.runtime.marshal.UnmarshalStream input) throws IOException {
        int size = input.unmarshalInt();
        var context = input.getRuntime().getCurrentContext();

        // FIXME: We used to use newArrayBlankInternal but this will not hash into a HashSet without an NPE.
        // we create this now with an empty, nulled array so it's available for links in the marshal data
        var result = (RubyArray<?>) input.entry(Create.allocArray(context, size));

        for (int i = 0; i < size; i++) {
            result.append(context, input.unmarshalObject());
        }

        return result;
    }

    public static RubyArray unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        int size = input.unmarshalInt(context, in);

        // FIXME: We used to use newArrayBlankInternal but this will not hash into a HashSet without an NPE.
        // we create this now with an empty, nulled array so it's available for links in the marshal data
        var result = (RubyArray<?>) input.entry(Create.allocArray(context, size));

        for (int i = 0; i < size; i++) {
            result.append(context, input.unmarshalObject(context, in));
        }

        return result;
    }

    private static JavaSites.ArraySites sites(ThreadContext context) {
        return context.sites.Array;
    }

    /**
     * Increases the capacity of this <code>Array</code>, if necessary.
     * @param minCapacity the desired minimum capacity of the internal array
     */
    @Deprecated(since = "9.1.3.0")
    public void ensureCapacity(int minCapacity) {
        throw getRuntime().newNotImplementedError("RubyArray#ensure_capacity has been removed");
    }

    @Deprecated(since = "9.2.10.0")
    @Override
    public RubyArray to_a() {
        throw getRuntime().newNotImplementedError("RubyArray#to_a has been removed");
    }

    @Deprecated(since = "9.2.15.0")
    public IRubyObject shuffle(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return shuffle(context);
            case 1:
                return shuffle(context, args[0]);
            default:
                throw argumentError(context, args.length, 0, 0);
        }
    }

    @Deprecated(since = "9.2.15.0")
    public IRubyObject shuffle_bang(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return shuffle_bang(context, context.nil);
            case 1:
                return shuffle_bang(context, args[0]);
            default:
                throw argumentError(context, args.length, 0, 0);
        }
    }

    @Deprecated(since = "9.2.15.0")
    public IRubyObject sample(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return sample(context);
            case 1:
                return sample(context, args[0]);
            case 2:
                return sample(context, args[0], args[1]);
            default:
                throw argumentError(context, args.length, 0, 1);
        }
    }

    /**
     * Returns a stream of each IRubyObject
     * @return
     */
    public abstract Stream<IRubyObject> rubyStream();
}
