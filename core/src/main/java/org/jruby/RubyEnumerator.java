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
 * Copyright (C) 2006 Michael Studman <me@michaelstudman.com>
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

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.StopIteration;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;

import java.util.Spliterator;
import java.util.stream.Stream;

import static org.jruby.api.Convert.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD;
import static org.jruby.runtime.Visibility.PRIVATE;

/**
 * Implementation of Ruby's Enumerator module.
 */
@JRubyModule(name="Enumerator", include="Enumerable")
public class RubyEnumerator extends RubyObject implements java.util.Iterator<Object> {
    public static final String GENERATOR = "@__generator__";
    public static final String LOOKAHEAD = "@__lookahead__";
    public static final String FEEDVALUE = "@__feedvalue__";
    public static final String OBJECT = "@__object__";
    public static final String METHOD = "@__method__";
    public static final String ARGS = "@__args__";
    /** target for each operation */
    private IRubyObject object;

    /** method to invoke for each operation */
    private String method;

    /** args to each method */
    private IRubyObject[] methodArgs;

    /** args contains a keywords argument */
    private boolean methodArgsHasKeywords;

    /** A value or proc to provide the size of the Enumerator contents*/
    private IRubyObject size;

    /** Function object for lazily computing size (used for internally created enumerators) */
    private SizeFn sizeFn;

    private FeedValue feedValue;

    public static RubyClass defineEnumerator(ThreadContext context, RubyClass Object, RubyModule Enumerable) {
        RubyClass Enumerator = defineClass(context, "Enumerator", Object, RubyEnumerator::new).
                include(Enumerable).
                defineMethods(context, RubyEnumerator.class);

        Enumerator.defineClassUnder(context, "FeedValue", Object, NOT_ALLOCATABLE_ALLOCATOR).defineMethods(context, FeedValue.class);
        Enumerator.setConstantVisibility(context.runtime, "FeedValue", true);

        return Enumerator;
    }

    /**
     * Internal Enumerator::FeedValue class to be shared between enumerator and its next-er Fiber.
     * In essence, a `Struct.new(:value)` like container but for finer control implemented as native.
     */
    public static class FeedValue extends RubyObject {

        private volatile IRubyObject value;

        private FeedValue(Ruby runtime, RubyClass type) {
            super(runtime, type);
            value = runtime.getNil();
        }

        FeedValue(Ruby runtime) {
            this(runtime, (RubyClass) runtime.getEnumerator().getConstantAt("FeedValue", true));
        }

        @JRubyMethod
        public IRubyObject value() { return value; }

        @JRubyMethod(name = "value=")
        public IRubyObject set_value(IRubyObject value) {
            return this.value = value;
        }

        @JRubyMethod
        public IRubyObject use_value(ThreadContext context) {
            IRubyObject value = this.value;
            this.value = context.nil;
            return value;
        }

    }

    private RubyEnumerator(Ruby runtime, RubyClass type) {
        super(runtime, type);
        initialize(runtime, runtime.getNil(), RubyString.newEmptyString(runtime), NULL_ARRAY);
    }

    private RubyEnumerator(Ruby runtime, RubyClass type, IRubyObject object, RubySymbol method, IRubyObject[] args,
                           IRubyObject size, SizeFn sizeFn, boolean keywords) {
        super(runtime, type);
        initialize(runtime, object, method, args, size, sizeFn, keywords);
    }

    private RubyEnumerator(Ruby runtime, RubyClass type, IRubyObject object, IRubyObject method, IRubyObject[] args) {
        super(runtime, type);
        initialize(runtime, object, method, args);
    }

    /**
     * Transform object into an Enumerator with the given size
     */
    public static <T extends IRubyObject> RubyEnumerator enumWithSize(ThreadContext context, final T object, String method, IRubyObject[] args, SizeFn<T> sizeFn) {
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), args, null, sizeFn, false);
    }

    public static <T extends IRubyObject> IRubyObject enumeratorizeWithSize(ThreadContext context, final T object, String method, IRubyObject[] args, SizeFn<T> sizeFn, boolean keywords) {
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), args, null, sizeFn, keywords);
    }

    public static <T extends IRubyObject> IRubyObject enumeratorizeWithSize(ThreadContext context, final T object, String method, IRubyObject[] args, SizeFn<T> sizeFn) {
        return enumeratorizeWithSize(context, object, method, args, sizeFn, false);
    }

    public static <T extends IRubyObject> IRubyObject enumeratorizeWithSize(ThreadContext context, T object, String method, SizeFn<T> sizeFn) {
        return enumeratorizeWithSize(context, object, method, NULL_ARRAY, sizeFn);
    }

    public static <T extends IRubyObject> RubyEnumerator enumWithSize(ThreadContext context, T object, String method, SizeFn<T> sizeFn) {
        return enumWithSize(context, object, method, NULL_ARRAY, sizeFn);
    }

    public static IRubyObject enumeratorizeWithSize(ThreadContext context, IRubyObject object, String method, IRubyObject arg, IRubyObject size) {
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), new IRubyObject[] { arg }, size, null, false);
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method) {
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), NULL_ARRAY);
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method, IRubyObject arg) {
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), new IRubyObject[] { arg });
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method, IRubyObject... args) {
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), args);
    }

    public static IRubyObject enumeratorize(Ruby runtime, RubyClass type, IRubyObject object, String method) {
        return new RubyEnumerator(runtime, type, object, runtime.fastNewSymbol(method), NULL_ARRAY);
    }

    public static IRubyObject enumeratorize(Ruby runtime, RubyClass type, IRubyObject object, String method, IRubyObject arg) {
        return new RubyEnumerator(runtime, type, object, runtime.fastNewSymbol(method), new IRubyObject[] {arg});
    }

    public static IRubyObject enumeratorize(Ruby runtime, RubyClass type, IRubyObject object, String method, IRubyObject[] args) {
        return new RubyEnumerator(runtime, type, object, runtime.fastNewSymbol(method), args);
    }

    // used internally to create lazy without block (from Enumerator/Enumerable)
    // and used internally to create enum from Enumerator::Lazy#eager
    @JRubyMethod(name = "__from", meta = true, required = 2, optional = 2, checkArity = false, visibility = PRIVATE, keywords = true)
    public static IRubyObject __from(ThreadContext context, IRubyObject klass, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 2, 4);

        boolean keywords = (context.callInfo & CALL_KEYWORD) != 0 && (context.callInfo & ThreadContext.CALL_KEYWORD_EMPTY) == 0;
        ThreadContext.resetCallInfo(context);

        // Lazy.__from(enum, method, *args, size)
        IRubyObject object = args[0];
        IRubyObject method = args[1];
        IRubyObject[] methodArgs;
        IRubyObject size = null; SizeFn sizeFn = null;
        if (argc > 2) {
            methodArgs = ((RubyArray) args[2]).toJavaArrayMaybeUnsafe();
            if (argc > 3) size = args[3];
        } else {
            methodArgs = NULL_ARRAY;
        }

        RubyEnumerator instance = (RubyEnumerator) ((RubyClass) klass).allocate();

        if (size == null) {
            sizeFn = RubyEnumerable::size;
        }

        instance.initialize(context.runtime, object, method, methodArgs, size, sizeFn, keywords);
        // set: @receiver = obj.object, @method = obj.method || :each, *@args = obj.args || []
        // (for Lazy#inspect)
        instance.setInstanceVariable("@receiver", object);
        instance.setInstanceVariable("@method", method);
        instance.setInstanceVariable("@args", RubyArray.newArrayNoCopyLight(context.runtime, methodArgs));

        return instance;
    }

    @Override
    public IRubyObject initialize(ThreadContext context) {
        return initialize(context, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        return initializeWithSize(context, null, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject size, Block block) {
        return initializeWithSize(context, size, block);
    }

    private static void checkSize(ThreadContext context, IRubyObject size) {
        if (size != null &&
                !(size.isNil() || size.respondsTo("call")) &&
                !(size instanceof RubyFloat && ((RubyFloat) size).value == Float.POSITIVE_INFINITY) &&
                !(size instanceof RubyInteger)) {
            throw typeError(context, size, "Integer");
        }
    }

    private IRubyObject initializeWithSize(ThreadContext context, IRubyObject size, Block block) {
        checkSize(context, size);

        IRubyObject object = context.runtime.getGenerator().newInstance(context, NULL_ARRAY, block);
        IRubyObject method = asSymbol(context, "each");

        return initialize(context.runtime, object, method, NULL_ARRAY, size, null, false);
    }

    private IRubyObject initialize(Ruby runtime, IRubyObject object, IRubyObject method, IRubyObject[] methodArgs) {
        return initialize(runtime, object, method, methodArgs, null, null, false);
    }

    private IRubyObject initialize(Ruby runtime, IRubyObject object, IRubyObject method, IRubyObject[] methodArgs,
                                   IRubyObject size, SizeFn sizeFn, boolean keywords) {
        this.object = object;
        this.method = method.asJavaString();
        this.methodArgs = methodArgs;
        this.methodArgsHasKeywords = keywords;
        this.size = size;
        this.sizeFn = sizeFn;

        setInstanceVariable(OBJECT, object);
        setInstanceVariable(METHOD, method);
        setInstanceVariable(ARGS, RubyArray.newArrayMayCopy(runtime, methodArgs));
        setInstanceVariable(GENERATOR, runtime.getNil());
        setInstanceVariable(LOOKAHEAD, RubyArray.newArray(runtime, 4));
        setInstanceVariable(FEEDVALUE, this.feedValue = new FeedValue(runtime));

        return this;
    }

    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        // JRUBY-5013: Enumerator needs to copy private fields in order to have a valid structure
        RubyEnumerator copy = (RubyEnumerator) super.dup();
        copy.object     = this.object;
        copy.method     = this.method;
        copy.methodArgs = this.methodArgs;
        copy.methodArgsHasKeywords = this.methodArgsHasKeywords;
        copy.size       = this.size;
        copy.sizeFn     = this.sizeFn;
        copy.feedValue  = new FeedValue(getRuntime());

        return copy;
    }

    /**
     * Send current block and supplied args to method on target. According to MRI
     * Block may not be given and "each" should just ignore it and call on through to
     * underlying method.
     */
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        if (!block.isGiven()) return this;
        return __each__(context, block);
    }

    private IRubyObject __each__(ThreadContext context, final Block block) {
        if (methodArgsHasKeywords) context.callInfo = CALL_KEYWORD;
        return object.callMethod(context, method, methodArgs,
                CallBlock.newCallClosure(context, this, Signature.OPTIONAL, (ctx, args, blk) -> {
                    IRubyObject ret = block.yieldValues(ctx, args);
                    IRubyObject val = feedValue.use_value(ctx);
                    return val.isNil() ? ret : val;
                })
        );
    }

    @JRubyMethod(rest = true)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        if (args.length == 0) return each(context, block);

        final int mlen = methodArgs.length;
        IRubyObject[] newArgs = new IRubyObject[mlen + args.length];
        ArraySupport.copy(methodArgs, newArgs, 0, mlen);
        ArraySupport.copy(args, newArgs, mlen, args.length);

        return new RubyEnumerator(context.runtime, getType(), object, asSymbol(context, method), newArgs,
                size, sizeFn, methodArgsHasKeywords).each(context, block);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isInspecting(this)) return inspect(context, true);

        try {
            runtime.registerInspecting(this);
            return inspect(context, false);
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    private IRubyObject inspect(ThreadContext context, boolean recurse) {
        Ruby runtime = context.runtime;
        ByteList bytes = new ByteList(new byte[] {'#', '<'});
        bytes.append(getMetaClass().getName().getBytes());
        bytes.append((byte)':').append((byte)' ');
        RubyString result = RubyString.newStringNoCopy(runtime, bytes);

        if (recurse) {
            return result.catString("...>");
        } else {
            result.catWithCodeRange(RubyObject.inspect(context, object));
            result.cat(':');
            result.catWithCodeRange(getMethod().asString());
            if (methodArgs.length > 0) {
                result.cat('(');
                for (int i= 0; i < methodArgs.length; i++) {
                    if (methodArgsHasKeywords && i == 0) break;
                    result.catWithCodeRange(RubyObject.inspect(context, methodArgs[i]));
                    if (i < methodArgs.length - 1) {
                        result.catString(", ");
                    } else {
                        result.cat(')');
                    }
                }
            }
            result.cat('>');
            return result;
        }
    }

    @JRubyMethod
    public IRubyObject each_with_object(final ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? RubyEnumerable.each_with_objectCommon(context, this, block, arg) :
                enumeratorizeWithSize(context, this, "each_with_object", new IRubyObject[]{arg}, RubyEnumerator::size);
    }

    @JRubyMethod
    public IRubyObject with_object(ThreadContext context, final IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_with_objectCommon(context, this, block, arg) : enumeratorizeWithSize(context, this, "with_object", new IRubyObject[]{arg}, RubyEnumerator::size);
    }

    @JRubyMethod()
    public IRubyObject each_entry(ThreadContext context, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_entryCommon(context, this, NULL_ARRAY, block) : enumeratorize(context.runtime, getType(), this, "each_entry", NULL_ARRAY);
    }

    @JRubyMethod()
    public IRubyObject each_entry(ThreadContext context, final IRubyObject arg0, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_entryCommon(context, this, arg0, block) : enumeratorize(context.runtime, getType(), this, "each_entry", arrayOf(arg0));
    }

    @JRubyMethod(rest = true)
    public IRubyObject each_entry(ThreadContext context, final IRubyObject[] args, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_entryCommon(context, this, args, block) : enumeratorize(context.runtime, getType(), this, "each_entry", args);
    }

    @JRubyMethod(name = "each_slice")
    public IRubyObject each_slice(ThreadContext context, IRubyObject arg, final Block block) {
        int size = (int) numericToLong(context, arg);
        if (size <= 0) throw argumentError(context, "invalid size");

        return block.isGiven() ? RubyEnumerable.each_sliceCommon(context, this, size, block) :
                enumeratorize(context.runtime, getType(), this, "each_slice", arg);
    }

    @JRubyMethod(name = "each_cons")
    public IRubyObject each_cons(ThreadContext context, IRubyObject arg, final Block block) {
        int size = (int) numericToLong(context, arg);
        if (size <= 0) throw argumentError(context, "invalid size");
        return block.isGiven() ? RubyEnumerable.each_consCommon(context, this, size, block) :
                enumeratorize(context.runtime, getType(), this, "each_cons", arg);
    }

    @JRubyMethod
    public final IRubyObject size(ThreadContext context) {
        if (sizeFn != null) {
            IRubyObject[] args = methodArgs;

            if (context == null) context = metaClass.runtime.getCurrentContext();
            return sizeFn.size(context, object, args);
        }

        IRubyObject size = this.size;
        if (size != null) {
            if (size.respondsTo("call")) {
                if (context == null) context = metaClass.runtime.getCurrentContext();
                return size.callMethod(context, "call");
            }

            return size;
        }

        return metaClass.runtime.getNil();
    }

    public long size() {
        final IRubyObject size = size(null);
        if ( size instanceof RubyNumeric ) {
            return ((RubyNumeric) size).getLongValue();
        }
        return -1;
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject size(ThreadContext context, RubyEnumerator recv, IRubyObject[] args) {
        return recv.size(context);
    }

    private IRubyObject with_index_common(ThreadContext context, final Block block, final String rubyMethodName, IRubyObject arg) {
        final int index = arg.isNil() ? 0 : RubyNumeric.num2int(arg);
        if ( ! block.isGiven() ) {
            return arg.isNil() ?
                    enumeratorizeWithSize(context, this, rubyMethodName, RubyEnumerator::size) :
                        enumeratorizeWithSize(context, this, rubyMethodName, new IRubyObject[]{asFixnum(context, index)}, RubyEnumerator::size);
        }

        return RubyEnumerable.callEach(context, sites(context).each, this, new RubyEnumerable.EachWithIndex(block, index));
    }

    @JRubyMethod
    public IRubyObject each_with_index(ThreadContext context, final Block block) {
        return with_index_common(context, block, "each_with_index", context.nil);
    }

    @JRubyMethod(name = "with_index")
    public IRubyObject with_index(ThreadContext context, final Block block) {
        return with_index_common(context, block, "with_index", context.nil);
    }

    @JRubyMethod(name = "with_index")
    public IRubyObject with_index(ThreadContext context, IRubyObject arg, final Block block) {
        return with_index_common(context, block, "with_index", arg);
    }

    // java.util.Iterator :

    @Override
    public synchronized boolean hasNext() {
        ThreadContext context = metaClass.runtime.getCurrentContext();
        try {
            // We don't care about the result, just whether it succeeds.
            sites(context).peek.call(context, this, this);
            return true;
        } catch (StopIteration si) {
            return false;
        }
    }

    private IRubyObject getMethod() {
        return getInstanceVariable(METHOD);
    }

    @Override
    public Object next() {
        ThreadContext context = metaClass.runtime.getCurrentContext();
        return sites(context).next.call(context, this, this).toJava(java.lang.Object.class);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    // Java 8 stream support :

    public Stream<Object> stream() {
        return stream(false);
    }

    public Stream<Object> stream(final boolean parallel) {
        return java.util.stream.StreamSupport.stream(spliterator(), parallel);
    }

    public Spliterator<Object> spliterator() {
        final long size = size();
        // we do not have Array detection - assume immutable
        int mod = java.util.Spliterator.IMMUTABLE;
        if (size >= 0) mod |= java.util.Spliterator.SIZED;
        return java.util.Spliterators.spliterator(this, size, mod);
    }

    public Spliterator<Object> spliterator(final int mod) {
        return java.util.Spliterators.spliterator(this, size(), mod);
    }

    /**
     * "Function" type for java-created enumerators with size.  Should be implemented so that calls to
     * SizeFn#size are kept in sync with the size of the created enum (i.e. if the object underlying an enumerator
     * changes, calls to SizeFn#size should reflect that change).
     *
     * @param <T> the enumerated object's type
     */
    public interface SizeFn<T extends IRubyObject> {
        IRubyObject size(ThreadContext context, T self, IRubyObject[] args);
    }

    private static JavaSites.FiberSites sites(ThreadContext context) {
        return context.sites.Fiber;
    }

    /** MRI: enumerator_s_produce
     *
     */
    @JRubyMethod(meta = true, optional = 1, checkArity = false)
    public static IRubyObject produce(ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        if (!block.isGiven()) throw argumentError(context, "no block given");

        IRubyObject init = argc == 0 ? null : args[0];
        RubyProducer producer = RubyProducer.newProducer(context, init, block);
        return enumeratorizeWithSize(context, producer, "each", RubyProducer::size);
    }

    @Deprecated
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject size = Arity.checkArgumentCount(context, args, 0, 1) == 1 ? args[0] : null;

        return initializeWithSize(context, size, block);
    }

    @Deprecated
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        return initialize(context, args, Block.NULL_BLOCK);
    }
}
