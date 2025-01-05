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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
import org.jruby.api.Access;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.CallBlock19;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaInternalBlockBody;
import org.jruby.runtime.JavaSites.EnumerableSites;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InternalVariables;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.util.TypeConverter;
import org.jruby.util.collections.DoubleObject;
import org.jruby.util.collections.SingleBoolean;
import org.jruby.util.collections.SingleDouble;
import org.jruby.util.collections.SingleInt;
import org.jruby.util.collections.SingleLong;
import org.jruby.util.collections.SingleObject;
import org.jruby.util.func.ObjectObjectIntFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyObject.equalInternal;
import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.enumerableModule;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.*;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.builtin.IRubyObject.NULL_ARRAY;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;

/**
 * The implementation of Ruby's Enumerable module.
 */

@JRubyModule(name="Enumerable")
public class RubyEnumerable {
    public static RubyModule createEnumerableModule(ThreadContext context) {
        return defineModule(context, "Enumerable").defineMethods(context, RubyEnumerable.class);
    }

    public static IRubyObject callEach(ThreadContext context, IRubyObject self, Signature signature, BlockCallback callback) {
        return callEach(context, eachSite(context), self, signature, callback);
    }

    public static IRubyObject callEach(ThreadContext context, CallSite each, IRubyObject self, Signature signature, BlockCallback callback) {
        return each.call(context, self, self, CallBlock.newCallClosure(context, self, signature, callback));
    }

    public static IRubyObject callEach(ThreadContext context, CallSite each, IRubyObject self, BlockCallback callback) {
        return each.call(context, self, self, CallBlock.newCallClosure(context, self, Signature.OPTIONAL, callback));
    }

    public static IRubyObject callEach(ThreadContext context, CallSite each, IRubyObject self, IRubyObject[] args, Signature signature,
                                       BlockCallback callback) {
        return each.call(context, self, self, args, CallBlock.newCallClosure(context, self, signature, callback));
    }

    public static IRubyObject callEach(ThreadContext context, CallSite each, IRubyObject self, IRubyObject arg0, Signature signature,
                                       BlockCallback callback) {
        return each.call(context, self, self, arg0, CallBlock.newCallClosure(context, self, signature, callback));
    }

    public static IRubyObject each(ThreadContext context, CallSite site, IRubyObject self, BlockBody body) {
        return site.call(context, self, self, new Block(body, context.currentBinding(self, Visibility.PUBLIC)));
    }

    private static void checkContext(ThreadContext firstContext, ThreadContext secondContext, String name) {
        if (firstContext != secondContext) {
            throw secondContext.runtime.newThreadError("Enumerable#" + name + " cannot be parallelized");
        }
    }

    @JRubyMethod(name = "count")
    public static IRubyObject count(ThreadContext context, IRubyObject self, final Block block) {
        return countCommon(context, eachSite(context), self, block);
    }

    private static IRubyObject countCommon(ThreadContext context, CallSite each, IRubyObject self, final Block block) {
        final SingleInt result = new SingleInt();

        if (block.isGiven()) {
            each(context, each, self, new JavaInternalBlockBody(context.runtime, context, "Enumerable#count", block.getSignature()) {
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    if (block.yield(context1, value).isTrue()) result.i++;
                    return context1.nil;
                }
            });
        } else {
            each(context, each, self, new JavaInternalBlockBody(context.runtime, context, "Enumerable#count", Signature.NO_ARGUMENTS) {
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    result.i++;
                    return context1.nil;
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    result.i++;
                    return context1.nil;
                }
            });
        }
        return asFixnum(context, result.i);
    }

    @JRubyMethod(name = "count")
    public static IRubyObject count(ThreadContext context, IRubyObject self, final IRubyObject methodArg, final Block block) {
        final SingleInt result = new SingleInt();

        if (block.isGiven()) warn(context, "given block not used");

        each(context, eachSite(context), self, new JavaInternalBlockBody(context.runtime, context, "Enumerable#count", Signature.ONE_REQUIRED) {
            public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                return this.yield(context1, packEnumValues(context1, args));
            }
            @Override
            public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                if (value.equals(methodArg)) result.i++;
                return context1.nil;
            }
        });

        return asFixnum(context, result.i);
    }

    @JRubyMethod
    public static IRubyObject cycle(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "cycle", RubyEnumerable::cycleSize);
        }

        return cycleCommon(context, self, -1, block);
    }

    @JRubyMethod
    public static IRubyObject cycle(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        if (arg.isNil()) return cycle(context, self, block);
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "cycle", new IRubyObject[] { arg }, RubyEnumerable::cycleSize);
        }

        long times = toLong(context, arg);
        if (times <= 0) return context.nil;

        return cycleCommon(context, self, times, block);
    }

    /*
     * @param nv number of times to cycle or -1 to cycle indefinitely
     */
    private static IRubyObject cycleCommon(ThreadContext context, IRubyObject self, long nv, final Block block) {
        final Ruby runtime = context.runtime;
        final List<IRubyObject> result = new ArrayList<>();

        each(context, eachSite(context), self, new JavaInternalBlockBody(runtime, Signature.OPTIONAL) {
            @Override
            public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                return doYield(context1, null, packEnumValues(context1, args));
            }
            @Override
            public IRubyObject doYield(ThreadContext context1, Block unused, IRubyObject args) {
                synchronized (result) { result.add(args); }
                block.yield(context1, args);
                return context1.nil;
            }
        });

        int length = result.size();
        if (length == 0) return context.nil;

        while (nv < 0 || 0 < --nv) {
            for (int i = 0; i < length; i++) {
                block.yield(context, result.get(i));
            }
        }

        return context.nil;
    }

    /**
     * A cycle size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject cycleSize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        long mul = 0;
        IRubyObject n = context.nil;

        if (args != null && args.length > 0) {
            n = args[0];
            if (!n.isNil()) mul = toLong(context, n);
        }

        IRubyObject size = size(context, self, args);

        if (size == null || size.isNil() || size.equals(asFixnum(context, 0))) return size;
        if (n == null || n.isNil()) return asFloat(context, RubyFloat.INFINITY);
        if (mul <= 0) return asFixnum(context, 0);

        return sites(context).cycle_op_mul.call(context, size, size, mul);
    }

    @JRubyMethod(name = "take")
    public static IRubyObject take(ThreadContext context, IRubyObject self, IRubyObject n, Block block) {
        final long len = toLong(context, n);

        if (len < 0) throw argumentError(context, "attempt to take negative size");
        if (len == 0) return newEmptyArray(context);

        final var result = newArray(context);

        try {
            // Atomic ?
            each(context, eachSite(context), self, new JavaInternalBlockBody(context.runtime, Signature.OPTIONAL) {
                long i = len; // Atomic ?
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    synchronized (result) {
                        result.append(context, value);
                        if (--i == 0) throw JumpException.SPECIAL_JUMP;
                    }
                    return context1.nil;
                }
            });
        } catch (JumpException.SpecialJump e) {}

        return result;
    }

    @JRubyMethod(name = "take_while")
    public static IRubyObject take_while(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, self, "take_while");

        final var result = newArray(context);

        try {
            callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, unused) -> {
                final IRubyObject larg; boolean ary = false;
                switch (largs.length) {
                    case 0:  larg = ctx.nil; break;
                    case 1:  larg = largs[0]; break;
                    default: larg = RubyArray.newArrayMayCopy(ctx.runtime, largs); ary = true;
                }
                IRubyObject val = ary ? block.yieldArray(ctx, larg, null) : block.yield(ctx, larg);
                if ( ! val.isTrue() ) throw JumpException.SPECIAL_JUMP;
                synchronized (result) { result.append(context, larg); }
                return ctx.nil;
            });
        } catch (JumpException.SpecialJump ignored) {}
        return result;
    }

    @JRubyMethod(name = "drop")
    public static IRubyObject drop(ThreadContext context, IRubyObject self, IRubyObject n, final Block block) {
        final long len = toLong(context, n);
        if (len < 0) throw argumentError(context, "attempt to drop negative size");

        final var result = newArray(context);

        try {
            // Atomic ?
            each(context, eachSite(context), self, new JavaInternalBlockBody(context.runtime, Signature.OPTIONAL) {
                long i = len; // Atomic ?
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    synchronized (result) {
                        if (i == 0) {
                            result.append(context, value);
                        } else {
                            --i;
                        }
                    }
                    return context1.nil;
                }
            });
        } catch (JumpException.SpecialJump e) {}

        return result;
    }

    @JRubyMethod
    public static IRubyObject drop_while(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, self, "drop_while");

        final var result = newArray(context);

        try {
            each(context, eachSite(context), self, new JavaInternalBlockBody(context.runtime, context, "Enumerable#drop_while", Signature.OPTIONAL) {
                boolean memo = false;
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    if (!memo && !block.yield(context1, value).isTrue()) memo = true;
                    if (memo) synchronized (result) { result.append(context, value); }
                    return context1.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return result;
    }

    @JRubyMethod(name = "first")
    public static IRubyObject first(ThreadContext context, IRubyObject self) {
        final SingleObject<IRubyObject> holder = new SingleObject(context.nil);

        try {
            each(context, eachSite(context), self, new JavaInternalBlockBody(context.runtime, context, "Enumerable#first", Signature.OPTIONAL) {
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    holder.object = value;
                    throw JumpException.SPECIAL_JUMP;
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return holder.object;
    }

    @JRubyMethod(name = "first")
    public static IRubyObject first(ThreadContext context, IRubyObject self, final IRubyObject num) {
        final long firstCount = toLong(context, num);
        if (firstCount == 0) return newEmptyArray(context);
        if (firstCount < 0) throw argumentError(context, "attempt to take negative size");

        final RubyArray<?> result = newRawArray(context, firstCount);

        try {
            each(context, eachSite(context), self, new JavaInternalBlockBody(context.runtime, context, "Enumerable#first", Signature.OPTIONAL) {
                private long iter = firstCount;
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    result.append(context, value);
                    if (iter-- == 1) throw JumpException.SPECIAL_JUMP;
                    return context1.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return result.finishRawArray(context);
    }

    @JRubyMethod
    public static IRubyObject tally(ThreadContext context, IRubyObject self) {
        RubyHash result = newHash(context);
        callEach(context, eachSite(context), self, Signature.NO_ARGUMENTS, new TallyCallback(result));
        return result;
    }

    @JRubyMethod
    public static IRubyObject tally(ThreadContext context, IRubyObject self, IRubyObject hashArg) {
        RubyHash result = (RubyHash) TypeConverter.convertToType(hashArg, hashClass(context), "to_hash");
        result.checkFrozen();
        callEach(context, eachSite(context), self, Signature.NO_ARGUMENTS, new TallyCallback(result));
        return result;
    }

    @JRubyMethod(name = {"to_a", "entries"})
    public static IRubyObject to_a(ThreadContext context, IRubyObject self) {
        var result = newArray(context);
        callEach(context, eachSite(context), self, Signature.OPTIONAL, new AppendBlockCallback(result));
        return result;
    }

    @JRubyMethod(name = {"to_a", "entries"}, keywords = true)
    public static IRubyObject to_a(ThreadContext context, IRubyObject self, IRubyObject arg) {
        var result = newArray(context);
        Helpers.invoke(context, self, "each", arg,
                CallBlock.newCallClosure(context, self, Signature.OPTIONAL, new AppendBlockCallback(result)));
        return result;
    }

    @JRubyMethod(name = {"to_a", "entries"}, rest = true, keywords = true)
    public static IRubyObject to_a(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final var result = newArray(context);
        Helpers.invoke(context, self, "each", args,
                CallBlock.newCallClosure(context, self, Signature.OPTIONAL, new AppendBlockCallback(result)));
        return result;
    }

    @JRubyMethod(name = "to_h", rest = true)
    public static IRubyObject to_h(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        final RubyHash result = newHash(context);
        Helpers.invoke(context, self, "each", args,
                CallBlock.newCallClosure(context, self, Signature.OPTIONAL, new PutKeyValueCallback(result, block)));
        return result;
    }

    @JRubyMethod
    public static IRubyObject sort(ThreadContext context, IRubyObject self, final Block block) {
        final var result = newArray(context);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new AppendBlockCallback(result));
        result.sort_bang(context, block);

        return result;
    }

    @JRubyMethod
    public static IRubyObject sort_by(final ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        List<DoubleObject<IRubyObject, IRubyObject>> valuesAndCriteria;

        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "sort_by", (SizeFn) RubyEnumerable::size);
        }

        final CachingCallSite each = eachSite(context);
        final ArrayList<DoubleObject<IRubyObject, IRubyObject>> valuesAndCriteriaList = new ArrayList<>();

        callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext context1, IRubyObject[] args, Block unused) {
                return call(context1, packEnumValues(context1, args), unused);
            }
            @Override
            public IRubyObject call(ThreadContext context1, IRubyObject arg, Block unused) {
                IRubyObject value = block.yield(context1, arg);
                synchronized (valuesAndCriteriaList) {
                    valuesAndCriteriaList.add(new DoubleObject<>(arg, value));
                }
                return context1.nil;
            }
        });

        valuesAndCriteria = valuesAndCriteriaList;

        Collections.sort(valuesAndCriteria,
                (o1, o2) -> RubyComparable.cmpint(context, invokedynamic(context, o1.object2, OP_CMP, o2.object2), o1.object2, o2.object2));

        IRubyObject dstArray[] = new IRubyObject[valuesAndCriteria.size()];
        for (int i = 0; i < dstArray.length; i++) {
            dstArray[i] = valuesAndCriteria.get(i).object1;
        }

        return RubyArray.newArrayMayCopy(runtime, dstArray);
    }

    @JRubyMethod
    public static IRubyObject grep(ThreadContext context, IRubyObject self, final IRubyObject pattern, final Block block) {
        return grep(context, self, pattern, block, true);
    }

    @JRubyMethod(name = "grep_v")
    public static IRubyObject inverseGrep(ThreadContext context, IRubyObject self, final IRubyObject pattern, final Block block) {
        return grep(context, self, pattern, block, false);
    }

    private static IRubyObject grep(ThreadContext context, IRubyObject self, final IRubyObject pattern, final Block block,
                                    final boolean isPresent) {
        final var result = newArray(context);

        final CachingCallSite each = eachSite(context);
        if (block.isGiven()) {
            // pattern === arg
            callEach(context, each, self, Signature.ONE_REQUIRED, new BlockCallback() {
                final MonomorphicCallSite site = new MonomorphicCallSite("===");
                public IRubyObject call(ThreadContext ctx, IRubyObject[] args, Block unused) {
                    return call(ctx, packEnumValues(ctx, args), unused);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject arg, Block unused) {
                    if (site.call(ctx, pattern, pattern, arg).isTrue() == isPresent) { // pattern === arg
                        IRubyObject value = block.yield(ctx, arg);
                        synchronized (result) { result.append(context, value); }
                    }
                    return ctx.nil;
                }
            });
        } else if ((pattern instanceof RubyRegexp) && pattern.getMetaClass().checkMethodBasicDefinition("===")) {
            callEach(context, each, self, Signature.ONE_REQUIRED, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] args, Block unused) {
                    return call(ctx, packEnumValues(ctx, args), unused);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject arg, Block unused) {
                    IRubyObject converted = arg instanceof RubySymbol ? arg : TypeConverter.checkStringType(ctx.runtime, arg);

                    if (((RubyRegexp) pattern).match_p(ctx, converted).isTrue() == isPresent) {
                        synchronized (result) { result.append(context, arg); }
                    }
                    return ctx.nil;
                }
            });
        } else {
            // pattern === arg
            callEach(context, each, self, Signature.ONE_REQUIRED, new BlockCallback() {
                final MonomorphicCallSite site = new MonomorphicCallSite("===");
                public IRubyObject call(ThreadContext ctx, IRubyObject[] args, Block unused) {
                    return call(ctx, packEnumValues(ctx, args), unused);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject arg, Block unused) {
                    if (site.call(ctx, pattern, pattern, arg).isTrue() == isPresent) { // pattern === arg
                        synchronized (result) { result.append(ctx, arg); }
                    }
                    return ctx.nil;
                }
            });
        }

        return result;
    }

    public static IRubyObject detectCommon(ThreadContext context, IRubyObject self, final Block block) {
        return detectCommon(context, eachSite(context), self, null, block);
    }

    public static IRubyObject detectCommon(ThreadContext context, CallSite each, IRubyObject self, final Block block) {
        return detectCommon(context, each, self, null, block);
    }

    public static IRubyObject detectCommon(final ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        return detectCommon(context, eachSite(context), self, ifnone, block);
    }

    public static IRubyObject detectCommon(final ThreadContext context, CallSite each, IRubyObject self, IRubyObject ifnone, final Block block) {
        final SingleObject<IRubyObject> result = new SingleObject<>(null);

        try {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    checkContext(context, ctx, "detect/find");
                    if (block.yield(ctx, larg).isTrue()) {
                        result.object = larg;
                        throw JumpException.SPECIAL_JUMP;
                    }
                    return ctx.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return result.object;
        }

        return ifnone != null && !ifnone.isNil() ?
                sites(context).detect_call.call(context, ifnone, ifnone) : context.nil;
    }

    @JRubyMethod
    public static IRubyObject detect(ThreadContext context, IRubyObject self, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, null, block);

        return block.isGiven() ? detectCommon(context, eachSite(context), self, null, block) : enumeratorize(context.runtime, self, "detect");
    }

    @JRubyMethod
    public static IRubyObject detect(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, ifnone, block);

        return block.isGiven() ? detectCommon(context, eachSite(context), self, ifnone, block) : enumeratorize(context.runtime, self, "detect", ifnone);
    }

    // FIXME: Custom Array enumeratorize should be made for all of these methods which skip Array without a supplied block.
    @JRubyMethod
    public static IRubyObject find(ThreadContext context, IRubyObject self, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, null, block);

        return blockGiven ? detectCommon(context, eachSite(context), self, null, block) : enumeratorize(context.runtime, self, "find");
    }

    @JRubyMethod
    public static IRubyObject find(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, ifnone, block);

        return blockGiven ? detectCommon(context, eachSite(context), self, ifnone, block) :
            enumeratorize(context.runtime, self, "find", ifnone);
    }

    @JRubyMethod(name = "find_index")
    public static IRubyObject find_index(ThreadContext context, IRubyObject self, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find_index(context, block);

        return blockGiven ? find_indexCommon(context, eachSite(context), self, block, block.getSignature()) :
                enumeratorize(context.runtime, self, "find_index");
    }

    @JRubyMethod(name = "find_index")
    public static IRubyObject find_index(ThreadContext context, IRubyObject self, final IRubyObject cond, final Block block) {
        if (block.isGiven()) warn(context, "given block not used");

        return self instanceof RubyArray ary ?
                ary.find_index(context, cond) :
                find_indexCommon(context, eachSite(context), self, cond);
    }

    public static IRubyObject find_indexCommon(ThreadContext context, IRubyObject self, final Block block, Signature callbackArity) {
        return find_indexCommon(context, eachSite(context), self, block, callbackArity);
    }

    public static IRubyObject find_indexCommon(ThreadContext context, CallSite each, IRubyObject self, final Block block, Signature callbackArity) {
        final SingleLong result = new SingleLong();

        try {
            callEach(context, each, self, callbackArity, (ctx, largs, blk) -> {
                if (block.yieldValues(ctx, largs).isTrue()) throw JumpException.SPECIAL_JUMP;
                result.l++;
                return ctx.nil;
            });
        } catch (JumpException.SpecialJump sj) {
            return asFixnum(context, result.l);
        }

        return context.nil;
    }

    public static IRubyObject find_indexCommon(ThreadContext context, IRubyObject self, final IRubyObject cond) {
        return find_indexCommon(context, eachSite(context), self, cond);
    }

    public static IRubyObject find_indexCommon(ThreadContext context, CallSite each, IRubyObject self, final IRubyObject cond) {
        final SingleLong result = new SingleLong(0);

        try {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    if (equalInternal(ctx, larg, cond)) throw JumpException.SPECIAL_JUMP;
                    result.l++;
                    return ctx.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return asFixnum(context, result.l);
        }

        return context.nil;
    }

    public static IRubyObject selectCommon(ThreadContext context, IRubyObject self, final Block block, String methodName) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, methodName, (SizeFn) RubyEnumerable::size);

        final var result = newArray(context);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                if (block.yield(ctx, larg).isTrue()) {
                    synchronized (result) { result.append(ctx, larg); }
                }
                return ctx.nil;
            }
        });

        return result;
    }

    @JRubyMethod
    public static IRubyObject select(ThreadContext context, IRubyObject self, final Block block) {
        return selectCommon(context, self, block, "select");
    }

    @JRubyMethod(alias = "filter")
    public static IRubyObject find_all(ThreadContext context, IRubyObject self, final Block block) {
        return selectCommon(context, self, block, "find_all");
    }

    @JRubyMethod
    public static IRubyObject reject(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "reject", (SizeFn) RubyEnumerable::size);

        final var result = newArray(context);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                if ( ! block.yield(ctx, larg).isTrue() ) {
                    synchronized (result) { result.append(ctx, larg); }
                }
                return ctx.nil;
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg) {
                if ( ! block.yield(ctx, larg).isTrue() ) {
                    synchronized (result) { result.append(ctx, larg); }
                }
                return ctx.nil;
            }
        });

        return result;
    }

    @JRubyMethod(name = "collect")
    public static IRubyObject collect(ThreadContext context, IRubyObject self, final Block block) {
        return collectCommon(context, self, block, "collect");
    }

    @JRubyMethod(name = "map")
    public static IRubyObject map(ThreadContext context, IRubyObject self, final Block block) {
        return collectCommon(context, self, block, "map");
    }

    private static IRubyObject collectCommon(ThreadContext context, IRubyObject self, final Block block, String methodName) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, methodName, (SizeFn) RubyEnumerable::size);

        final var result = newArray(context);

        eachSite(context).call(context, self, self, CallBlock19.newCallClosure(self, enumerableModule(context),
                block.getSignature(), new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                final IRubyObject larg;
                boolean ary = false;
                switch (largs.length) {
                    case 0:  larg = ctx.nil; break;
                    case 1:  larg = largs[0]; break;
                    default: larg = RubyArray.newArrayMayCopy(ctx.runtime, largs); ary = true;
                }
                IRubyObject val = ary ? block.yieldArray(ctx, larg, null) : block.yield(ctx, larg);

                synchronized (result) { result.append(ctx, val); }
                return ctx.nil;
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                IRubyObject val = block.yield(ctx, larg);

                synchronized (result) { result.append(ctx, val); }
                return ctx.nil;
            }
        }, context));

        return result;
    }

    @JRubyMethod
    public static IRubyObject filter_map(ThreadContext context, IRubyObject self, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "filter_map", (SizeFn) RubyEnumerable::size);

        var result = newArray(context);

        eachSite(context).call(context, self, self, CallBlock19.newCallClosure(self, enumerableModule(context),
                block.getSignature(), new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                final IRubyObject larg; boolean ary = false;
                switch (largs.length) {
                    case 0:  larg = ctx.nil; break;
                    case 1:  larg = largs[0]; break;
                    default: larg = RubyArray.newArrayMayCopy(ctx.runtime, largs); ary = true;
                }
                IRubyObject val = ary ? block.yieldArray(ctx, larg, null) : block.yield(ctx, larg);

                if (val.isTrue()) {
                    synchronized (result) { result.append(ctx, val); }
                }
                return ctx.nil;
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                IRubyObject val = block.yield(ctx, larg);

                if (val.isTrue()) {
                    synchronized (result) { result.append(ctx, val); }
                }
                return ctx.nil;
            }
        }, context));

        return result;
    }

    @JRubyMethod(name = "flat_map")
    public static IRubyObject flat_map(ThreadContext context, IRubyObject self, final Block block) {
        return flatMapCommon(context, self, block, "flat_map");
    }

    @JRubyMethod(name = "collect_concat")
    public static IRubyObject collect_concat(ThreadContext context, IRubyObject self, final Block block) {
        return flatMapCommon(context, self, block, "collect_concat");
    }

    private static IRubyObject flatMapCommon(ThreadContext context, IRubyObject self, final Block block, String methodName) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, methodName, (SizeFn) RubyEnumerable::size);

        final var ary = newArray(context);

        callEach(context, eachSite(context), self, block.getSignature(), new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                IRubyObject i = block.yield(ctx, larg);
                IRubyObject tmp = i.checkArrayType();
                synchronized(ary) {
                    if (tmp.isNil()) {
                        ary.append(ctx, i);
                    } else {
                        ary.concat(ctx, tmp);
                    }
                }
                return ctx.nil;
            }
        });

        return ary;
    }

    @JRubyMethod
    public static IRubyObject sum(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        RubyFixnum zero = RubyFixnum.zero(runtime);
        return sumCommon(context, self, zero, block);
    }

    @JRubyMethod
    public static IRubyObject sum(ThreadContext context, IRubyObject self, IRubyObject init, final Block block) {
        return sumCommon(context, self, init, block);
    }

    public static IRubyObject sumCommon(final ThreadContext context, IRubyObject self, IRubyObject init, final Block block) {
        return sumCommon(context, eachSite(context), self, init, block);
    }

    /* TODO: optimise for special types (e.g. Range, Hash, ...) */
    public static IRubyObject sumCommon(final ThreadContext context, CallSite each, IRubyObject self, IRubyObject init, final Block block) {
        final SingleObject<IRubyObject> result = new SingleObject<>(init);
        final SingleDouble memo = new SingleDouble(0.0);

        if (block.isGiven()) {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    result.object = sumAdd(ctx, result.object, block.yield(ctx, larg), memo);
                    return ctx.nil;
                }
            });
        } else {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    result.object = sumAdd(ctx, result.object, larg, memo);
                    return ctx.nil;
                }
            });
        }

        if (result.object instanceof RubyFloat) {
            return ((RubyFloat) result.object).op_plus(context, memo.d);
        }
        return result.object;
    }

    /* FIXME: optimise for special types (e.g. Integer)? */
    /* NB: MRI says "Enumerable#sum method may not respect method redefinition of "+" methods such as Integer#+." */
    public static IRubyObject sumAdd(final ThreadContext context, IRubyObject lhs, IRubyObject rhs, final SingleDouble c) {
        boolean floats = false;
        double f = 0.0;
        /*
         * Kahan-Babuska balancing compensated summation algorithm
         * See http://link.springer.com/article/10.1007/s00607-005-0139-x
         */
        double x = 0.0, t;
        if (lhs instanceof RubyFloat lhsFloat) {
            if (rhs instanceof RubyNumeric num) {
                f = lhsFloat.value;
                x = num.asDouble(context);
                floats = true;
            }
        } else if (rhs instanceof RubyFloat rhsFloat) {
            if (lhs instanceof RubyNumeric num) {
                c.d = 0.0;
                f = num.asDouble(context);
                x = rhsFloat.value;
                floats = true;
            }
        }

        if (!floats) return sites(context).sum_op_plus.call(context, lhs, lhs, rhs);

        if (Double.isNaN(f)) return lhs;
        if (Double.isNaN(x)) return lhs;

        if (Double.isInfinite(x)) {
            if (Double.isInfinite(f) && Math.signum(x) != Math.signum(f)) {
                return asFloat(context, RubyFloat.NAN);
            } else {
                return rhs;
            }
        }
        if (Double.isInfinite(f)) return lhs;

        // Kahan's compensated summation algorithm
        t = f + x;
        if (Math.abs(f) >= Math.abs(x)) {
            c.d += ((f - t) + x);
        } else {
            c.d += ((x - t) + f);
        }
        f = t;

        return asFloat(context, f);
    }

    public static IRubyObject injectCommon(final ThreadContext context, IRubyObject self, IRubyObject init, final Block block) {
        final SingleObject<IRubyObject> result = new SingleObject(init);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            checkContext(context, ctx, "inject");
            result.object = result.object == null ?
                    larg : block.yieldArray(ctx, newArray(ctx, result.object, larg), null);

            return ctx.nil;
        });

        return result.object == null ? context.nil : result.object;
    }

    @JRubyMethod(name = {"inject", "reduce"})
    public static IRubyObject inject(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) throw argumentError(context, "wrong number of arguments (given 0, expected 1..2)");

        return injectCommon(context, self, null, block);
    }

    @JRubyMethod(name = {"inject", "reduce"})
    public static IRubyObject inject(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return block.isGiven() ? injectCommon(context, self, arg, block) : inject(context, self, null, arg, block);
    }

    @JRubyMethod(name = {"inject", "reduce"})
    public static IRubyObject inject(ThreadContext context, IRubyObject self, IRubyObject init, IRubyObject method, final Block block) {
        if (block.isGiven()) warn(context, "given block not used");

        final String methodId = method.asJavaString();
        final SingleObject<IRubyObject> result = new SingleObject<>(init);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            final MonomorphicCallSite site = new MonomorphicCallSite(methodId);
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                result.object = result.object == null ? larg : site.call(ctx, self, result.object, larg);
                return ctx.nil;
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg) {
                result.object = result.object == null ? larg : site.call(ctx, self, result.object, larg);
                return ctx.nil;
            }
        });
        return result.object == null ? context.nil : result.object;
    }

    @JRubyMethod
    public static IRubyObject partition(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "partition", (SizeFn) RubyEnumerable::size);

        final var arr_true = newArray(context);
        final var arr_false = newArray(context);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            if (block.yield(ctx, larg).isTrue()) {
                synchronized (arr_true) { arr_true.append(ctx, larg); }
            } else {
                synchronized (arr_false) { arr_false.append(ctx, larg); }
            }

            return ctx.nil;
        });

        return newArray(context, arr_true, arr_false);
    }

    static class EachWithIndex implements BlockCallback {
        private int index;
        private final Block block;

        EachWithIndex(Block block, int index) {
            this.block = block;
            this.index = index;
        }

        EachWithIndex(Block block) {
            this.block = block;
            this.index = 0;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] iargs, Block block) {
            return this.block.call(context, packEnumValues(context, iargs), asFixnum(context, index++));
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject iarg, Block block) {
            return this.block.call(context, iarg, asFixnum(context, index++));
        }
    }

    /**
     * Package the arguments appropriately depending on how many there are
     * Corresponds to rb_enum_values_pack in MRI
     */
    static IRubyObject packEnumValues(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:  return context.nil;
            case 1:  return args[0];
            default: return RubyArray.newArrayMayCopy(context.runtime, args);
        }
    }

    public static IRubyObject each_with_indexCommon(ThreadContext context, IRubyObject self, Block block, IRubyObject[] args) {
        callEach(context, eachSite(context), self, args, Signature.OPTIONAL, new EachWithIndex(block));
        return self;
    }

    private static IRubyObject each_with_indexCommon(ThreadContext context, IRubyObject self, Block block, IRubyObject arg0) {
        callEach(context, eachSite(context), self, arg0, Signature.OPTIONAL, new EachWithIndex(block));
        return self;
    }

    public static IRubyObject each_with_objectCommon(ThreadContext context, IRubyObject self, final Block block, final IRubyObject arg) {
        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return block.call(ctx, packEnumValues(ctx, largs), arg);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                return block.call(ctx, larg, arg);
            }
        });
        return arg;
    }

    @JRubyMethod(name = "each_with_index")
    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, Block block) {
        return each_with_index(context, self, IRubyObject.NULL_ARRAY, block);
    }

    @JRubyMethod(name = "each_with_index")
    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        return block.isGiven() ? each_with_indexCommon(context, self, block, arg0) : enumeratorizeWithSize(context, self, "each_with_index", arrayOf(arg0), (SizeFn) RubyEnumerable::size);
    }

    @JRubyMethod(name = "each_with_index", rest = true)
    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return block.isGiven() ? each_with_indexCommon(context, self, block, args) : enumeratorizeWithSize(context, self, "each_with_index", args, (SizeFn) RubyEnumerable::size);
    }

    @JRubyMethod
    public static IRubyObject each_with_object(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        return block.isGiven() ? each_with_objectCommon(context, self, block, arg) : enumeratorizeWithSize(context, self, "each_with_object", new IRubyObject[] { arg }, RubyEnumerable::size);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject each_entry(ThreadContext context, final IRubyObject self, final IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_entryCommon(context, self, args, block) : enumeratorizeWithSize(context, self, "each_entry", args, RubyEnumerable::size);
    }

    public static IRubyObject each_entryCommon(ThreadContext context, final IRubyObject self, final IRubyObject arg0, final Block block) {
        callEach(context, eachSite(context), self, arg0, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return block.yield(ctx, packEnumValues(ctx, largs));
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                return block.yield(ctx, larg);
            }
        });
        return self;
    }

    public static IRubyObject each_entryCommon(ThreadContext context, final IRubyObject self, final IRubyObject[] args, final Block block) {
        callEach(context, eachSite(context), self, args, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return block.yield(ctx, packEnumValues(ctx, largs));
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                return block.yield(ctx, larg);
            }
        });
        return self;
    }

    @JRubyMethod(name = "each_slice")
    public static IRubyObject each_slice(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        int size = toInt(context, arg);
        if (size <= 0) throw argumentError(context, "invalid size");

        return block.isGiven() ? each_sliceCommon(context, self, size, block) :
                enumeratorizeWithSize(context, self, "each_slice", new IRubyObject[]{arg}, RubyEnumerable::eachSliceSize);
    }

    static IRubyObject each_sliceCommon(ThreadContext context, IRubyObject self, final int size, final Block block) {
        if (size <= 0) throw argumentError(context, "invalid slice size");

        final SingleObject<RubyArray> result = new SingleObject<>(null);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            RubyArray object = result.object;
            if (object == null) {
                object = result.object = newRawArray(context, size);
            }

            object.append(ctx, packEnumValues(ctx, largs));

            if (object.size() == size) {
                block.yield(ctx, object.finishRawArray(ctx));
                result.object = newRawArray(ctx, size);
            }

            return ctx.nil;
        });

        if (result.object != null && result.object.size() > 0) block.yield(context, result.object.finishRawArray(context));
        return self;
    }

    /**
     * A each_slice size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachSliceSize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #each_slice ensures arg[0] is numeric

        long sliceSize = ((RubyNumeric) args[0]).asLong(context);
        if (sliceSize <= 0) throw argumentError(context, "invalid slice size");

        IRubyObject size = RubyEnumerable.size(context, self, args);
        if (size == null || size.isNil()) return context.nil;

        IRubyObject n = sites(context).each_slice_op_plus.call(context, size, size, sliceSize - 1);
        return sites(context).each_slice_op_div.call(context, n, n, sliceSize);
    }

    @JRubyMethod(name = "each_cons")
    public static IRubyObject each_cons(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        int size = toInt(context, arg);
        if (size <= 0) throw argumentError(context, "invalid size");
        return block.isGiven() ? each_consCommon(context, self, size, block) : enumeratorizeWithSize(context, self, "each_cons", new IRubyObject[] { arg }, (SizeFn) RubyEnumerable::eachConsSize);
    }

    static IRubyObject each_consCommon(ThreadContext context, IRubyObject self, final int size, final Block block) {
        final var result = newRawArray(context, size);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            if (result.size() == size) result.shift(ctx);
            result.append(ctx, packEnumValues(ctx, largs));
            if (result.size() == size) block.yield(ctx, result.finishRawArray(context).aryDup());
            return ctx.nil;
        });

        return self;
    }

    /**
     * A each_cons size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachConsSize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #each_cons ensures arg[0] is numeric
        long consSize = ((RubyNumeric) args[0]).asLong(context);
        if (consSize <= 0) throw argumentError(context, "invalid size");

        IRubyObject size = size(context, self, args);
        if (size == null || size.isNil()) return context.nil;

        IRubyObject n = sites(context).each_cons_op_plus.call(context, size, size, 1 - consSize);
        RubyFixnum zero = asFixnum(context, 0);
        return RubyComparable.cmpint(context, sites(context).each_cons_op_cmp.call(context, n, n, zero), n, zero) == -1 ? zero : n;
    }

    @JRubyMethod
    public static IRubyObject reverse_each(ThreadContext context, IRubyObject self, Block block) {
        return block.isGiven() ? reverse_eachInternal(context, self, to_a(context, self), block) :
            enumeratorizeWithSize(context, self, "reverse_each", RubyEnumerable::size);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject reverse_each(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return block.isGiven() ? reverse_eachInternal(context, self, to_a(context, self, args), block) :
            enumeratorizeWithSize(context, self, "reverse_each", args, RubyEnumerable::size);
    }

    private static IRubyObject reverse_eachInternal(ThreadContext context, IRubyObject self, IRubyObject obj, Block block) {
        ((RubyArray)obj).reverse_each(context, block);
        return self;
    }

    @JRubyMethod(name = {"include?", "member?"})
    public static IRubyObject include_p(final ThreadContext context, IRubyObject self, final IRubyObject arg) {
        try {
            callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    checkContext(context, ctx, "include?/member?");
                    if (RubyObject.equalInternal(ctx, larg, arg)) {
                        throw JumpException.SPECIAL_JUMP;
                    }
                    return ctx.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return context.tru;
        }

        return context.fals;
    }

    @JRubyMethod
    public static IRubyObject max(ThreadContext context, IRubyObject self, final Block block) {
        return singleExtent(context, self, "max", SORT_MAX, block);
    }

    @JRubyMethod
    public static IRubyObject max(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        // TODO: Replace with an implementation (quickselect, etc) which requires O(k) memory rather than O(n) memory
        RubyArray sorted = (RubyArray) sort(context, self, block);
        return arg.isNil() ?
                sorted.last(context) :
                ((RubyArray) sorted.last(context, arg)).reverse(context);
    }

    @JRubyMethod
    public static IRubyObject min(ThreadContext context, IRubyObject self, final Block block) {
        return singleExtent(context, self, "min", SORT_MIN, block);
    }

    @JRubyMethod
    public static IRubyObject min(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        // TODO: Replace with an implementation (quickselect, etc) which requires O(k) memory rather than O(n) memory
        RubyArray sorted = (RubyArray) sort(context, self, block);

        return arg.isNil() ? sorted.first(context) : sorted.first(context, arg);
    }

    @JRubyMethod
    public static IRubyObject max_by(ThreadContext context, IRubyObject self, final Block block) {
        return singleExtentBy(context, self, "max", SORT_MAX, block);
    }

    @JRubyMethod
    public static IRubyObject max_by(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        if (arg == context.nil) return singleExtentBy(context, self, "max", SORT_MAX, block);

        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "max_by", RubyEnumerable::size);

        // TODO: Replace with an implementation (quickselect, etc) which requires O(k) memory rather than O(n) memory
        return ((RubyArray) ((RubyArray) sort_by(context, self, block)).last(context, arg)).reverse(context);
    }

    @JRubyMethod
    public static IRubyObject min_by(ThreadContext context, IRubyObject self, final Block block) {
        return singleExtentBy(context, self, "min", SORT_MIN, block);
    }

    @JRubyMethod
    public static IRubyObject min_by(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        if (arg == context.nil) return singleExtentBy(context, self, "min", SORT_MIN, block);

        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "min_by", RubyEnumerable::size);

        // TODO: Replace with an implementation (quickselect, etc) which requires O(k) memory rather than O(n) memory
        return ((RubyArray) sort_by(context, self, block)).first(context, arg);
    }

    private static final int SORT_MAX =  1;
    private static final int SORT_MIN = -1;
    private static IRubyObject singleExtent(final ThreadContext context, IRubyObject self, final String op, final int sortDirection, final Block block) {
        final SingleObject<IRubyObject> result = new SingleObject<>(null);

        Signature signature = block.isGiven() ? block.getSignature() : Signature.ONE_REQUIRED;
        callEach(context, eachSite(context), self, signature, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            checkContext(context, ctx, op + "{}");

            if (result.object == null ||
                    (block.isGiven() &&
                            RubyComparable.cmpint(ctx, block.yieldArray(ctx, newArray(context, larg, result.object), null), larg, result.object) * sortDirection > 0) ||
                    (!block.isGiven() &&
                            RubyComparable.cmpint(ctx, invokedynamic(ctx, larg, OP_CMP, result.object), larg, result.object) * sortDirection > 0)) {
                result.object = larg;
            }
            return ctx.nil;
        });

        return result.object == null ? context.nil : result.object;
    }

    private static IRubyObject singleExtentBy(final ThreadContext context, IRubyObject self, final String op, final int sortDirection, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, op, RubyEnumerable::size);

        final SingleObject<IRubyObject> result = new SingleObject<>(context.nil);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            IRubyObject memo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(ctx, largs);
                checkContext(context, ctx, op);
                IRubyObject v = block.yield(ctx, larg);

                if (memo == null || RubyComparable.cmpint(ctx, invokedynamic(ctx, v, OP_CMP, memo), v, memo) * sortDirection > 0) {
                    memo = v;
                    result.object = larg;
                }
                return ctx.nil;
            }
        });
        return result.object;
    }

    @JRubyMethod
    public static IRubyObject minmax(final ThreadContext context, IRubyObject self, final Block block) {
        final DoubleObject<IRubyObject, IRubyObject> result = new DoubleObject<>(null, null);

        if (block.isGiven()) {
            callEach(context, eachSite(context), self, block.getSignature(), (ctx, largs, blk) -> {
                checkContext(context, ctx, "minmax");
                IRubyObject arg = packEnumValues(ctx, largs);

                if (result.object1 == null) {
                    result.object1 = result.object2 = arg;
                } else {
                    if (RubyComparable.cmpint(ctx, block.yield(ctx, newArray(context, arg, result.object1)), arg, result.object1) < 0) {
                        result.object1 = arg;
                    }

                    if (RubyComparable.cmpint(ctx, block.yield(ctx, newArray(context, arg, result.object2)), arg, result.object2) > 0) {
                        result.object2 = arg;
                    }
                }
                return ctx.nil;
            });
        } else {
            callEach(context, eachSite(context), self, Signature.ONE_REQUIRED, (ctx, largs, blk) -> {
                IRubyObject arg = packEnumValues(ctx, largs);
                synchronized (result) {
                    if (result.object1 == null) {
                        result.object1 = result.object2 = arg;
                    } else {
                        if (RubyComparable.cmpint(ctx, invokedynamic(ctx, arg, OP_CMP, result.object1), arg, result.object1) < 0) {
                            result.object1 = arg;
                        }

                        if (RubyComparable.cmpint(ctx, invokedynamic(ctx, arg, OP_CMP, result.object2), arg, result.object2) > 0) {
                            result.object2 = arg;
                        }
                    }
                }
                return ctx.nil;
            });
        }

        return result.object1 == null ?
                newArray(context, context.nil, context.nil) : newArray(context, result.object1, result.object2);
    }

    @JRubyMethod
    public static IRubyObject minmax_by(final ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "minmax_by", RubyEnumerable::size);

        final DoubleObject<IRubyObject, IRubyObject> result = new DoubleObject<>(context.nil, context.nil);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            IRubyObject minMemo = null, maxMemo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                checkContext(context, ctx, "minmax_by");
                IRubyObject arg = packEnumValues(ctx, largs);
                IRubyObject v = block.yield(ctx, arg);

                if (minMemo == null) {
                    minMemo = maxMemo = v;
                    result.object1 = result.object2 = arg;
                } else {
                    if (RubyComparable.cmpint(ctx, invokedynamic(ctx, v, OP_CMP, minMemo), v, minMemo) < 0) {
                        minMemo = v;
                        result.object1 = arg;
                    }
                    if (RubyComparable.cmpint(ctx, invokedynamic(ctx, v, OP_CMP, maxMemo), v, maxMemo) > 0) {
                        maxMemo = v;
                        result.object2 = arg;
                    }
                }
                return ctx.nil;
            }
        });
        return newArray(context, result.object1, result.object2);
    }

    @JRubyMethod(name = "none?")
    public static IRubyObject none_p(ThreadContext context, IRubyObject self, final Block block) {
        return none_pCommon(context, eachSite(context), self, null, block);
    }

    @JRubyMethod(name = "none?")
    public static IRubyObject none_p(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return none_pCommon(context, eachSite(context), self, arg, block);
    }

    public static IRubyObject none_pCommon(ThreadContext context, IRubyObject self, IRubyObject pattern, final Block block) {
        return none_pCommon(context, eachSite(context), self, pattern, block);
    }

    public static IRubyObject none_pCommon(ThreadContext context, CallSite each, IRubyObject self, IRubyObject pattern, final Block block) {
        final ThreadContext localContext = context;
        final boolean patternGiven = pattern != null;

        if (block.isGiven() && patternGiven) warn(context, "given block not used");

        try {
            if (block.isGiven() && !patternGiven) {
                callEach(context, each, self, block.getSignature(), (ctx, largs, blk) -> {
                    checkContext(localContext, ctx, "none?");
                    if (block.yieldValues(ctx, largs).isTrue()) throw JumpException.SPECIAL_JUMP;
                    return ctx.nil;

                });
            } else {
                if (patternGiven) {
                    final CallSite none_op_eqq = sites(context).none_op_eqq;
                    callEach(context, each, self, Signature.ONE_REQUIRED, (ctx, largs, blk) -> {
                        checkContext(localContext, ctx, "none?");
                        IRubyObject larg = packEnumValues(ctx, largs);
                        if (none_op_eqq.call(context, pattern, pattern, larg).isTrue()) throw JumpException.SPECIAL_JUMP;
                        return ctx.nil;
                    });
                } else {
                    callEach(context, each, self, Signature.ONE_REQUIRED, (ctx, largs, blk) -> {
                        checkContext(localContext, ctx, "none?");
                        IRubyObject larg = packEnumValues(ctx, largs);
                        if (larg.isTrue()) throw JumpException.SPECIAL_JUMP;
                        return ctx.nil;
                    });
                }
            }
        } catch (JumpException.SpecialJump sj) {
            return context.fals;
        }
        return context.tru;
    }

    @JRubyMethod(name = "one?")
    public static IRubyObject one_p(ThreadContext context, IRubyObject self, final Block block) {
        return one_pCommon(context, eachSite(context), self, null, block);
    }

    @JRubyMethod(name = "one?")
    public static IRubyObject one_p(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return one_pCommon(context, eachSite(context), self, arg, block);
    }

    public static IRubyObject one_pCommon(ThreadContext context, IRubyObject self, IRubyObject pattern, final Block block) {
        return one_pCommon(context, eachSite(context), self, pattern, block);
    }

    public static IRubyObject one_pCommon(ThreadContext context, CallSite each, IRubyObject self, IRubyObject pattern, final Block block) {
        final ThreadContext localContext = context;
        final SingleBoolean result = new SingleBoolean(false);
        final boolean patternGiven = pattern != null;

        if (block.isGiven() && patternGiven) warn(context, "given block not used");

        try {
            if (block.isGiven() && !patternGiven) {
                callEach(context, each, self, block.getSignature(), (ctx, largs, blk) -> {
                    checkContext(localContext, ctx, "one?");
                    if (block.yieldValues(ctx, largs).isTrue()) {
                        if (result.b) {
                            throw JumpException.SPECIAL_JUMP;
                        } else {
                            result.b = true;
                        }
                    }
                    return ctx.nil;
                });
            } else {
                if (patternGiven) {
                    final CallSite one_op_eqq = sites(context).one_op_eqq;
                    callEach(context, each, self, Signature.ONE_REQUIRED, (ctx, largs, blk) -> {
                        checkContext(localContext, ctx, "one?");
                        IRubyObject larg = packEnumValues(ctx, largs);
                        if (one_op_eqq.call(context, pattern, pattern, larg).isTrue()) {
                            if (result.b) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result.b = true;
                            }
                        }
                        return ctx.nil;
                    });
                } else {
                    callEach(context, each, self, Signature.ONE_REQUIRED, (ctx, largs, blk) -> {
                        checkContext(localContext, ctx, "one?");
                        IRubyObject larg = packEnumValues(ctx, largs);
                        if (larg.isTrue()) {
                            if (result.b) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result.b = true;
                            }
                        }
                        return ctx.nil;
                    });
                }
            }
        } catch (JumpException.SpecialJump sj) {
            return context.fals;
        }
        return result.b ? context.tru : context.fals;
    }

    @JRubyMethod(name = "all?")
    public static IRubyObject all_p(ThreadContext context, IRubyObject self, final Block block) {
        if (self instanceof RubyArray) return ((RubyArray) self).all_p(context, null, block);
        return all_pCommon(context, eachSite(context), self, null, block);
    }

    @JRubyMethod(name = "all?")
    public static IRubyObject all_p(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        if (self instanceof RubyArray) return ((RubyArray) self).all_p(context, arg, block);
        return all_pCommon(context, eachSite(context), self, arg, block);
    }

    public static IRubyObject all_p(ThreadContext context, IRubyObject self, IRubyObject[] args, final Block block) {
        switch (args.length) {
            case 0:
                return all_p(context, self, block);
            case 1:
                return all_p(context, self, args[0], block);
            default:
                throw argumentError(context, args.length, 0, 1);
        }
    }

    public static IRubyObject all_pCommon(ThreadContext context, IRubyObject self, IRubyObject pattern, final Block block) {
        return all_pCommon(context, eachSite(context), self, pattern, block);
    }

    public static IRubyObject all_pCommon(ThreadContext localContext, CallSite each, IRubyObject self, IRubyObject pattern, final Block block) {
        final boolean patternGiven = pattern != null;

        if (block.isGiven() && patternGiven) warn(localContext, "given block not used");

        try {
            if (block.isGiven() && !patternGiven) {
                callEach(localContext, each, self, block.getSignature(), (context, largs, blk) -> {
                    checkContext(localContext, context, "all?");
                    if (!block.yieldValues(context, largs).isTrue()) {
                        throw JumpException.SPECIAL_JUMP;
                    }
                    return context.nil;
                });
            } else {
                if (patternGiven) {
                    final CallSite all_op_eqq = sites(localContext).all_op_eqq;
                    callEach(localContext, each, self, Signature.ONE_REQUIRED, new BlockCallback() {
                        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
                            checkContext(localContext, context, "all?");
                            IRubyObject larg = packEnumValues(context, largs);
                            if (!all_op_eqq.call(context, pattern, pattern, larg).isTrue()) {
                                throw JumpException.SPECIAL_JUMP;
                            }
                            return context.nil;
                        }
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject larg) {
                            checkContext(localContext, context, "all?");
                            if (!all_op_eqq.call(context, pattern, pattern, larg).isTrue()) {
                                throw JumpException.SPECIAL_JUMP;
                            }
                            return context.nil;
                        }
                    });
                } else {
                    callEach(localContext, each, self, Signature.ONE_REQUIRED, new BlockCallback() {
                        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
                            checkContext(localContext, context, "all?");
                            IRubyObject larg = packEnumValues(context, largs);
                            if (!larg.isTrue()) {
                                throw JumpException.SPECIAL_JUMP;
                            }
                            return context.nil;
                        }

                        @Override
                        public IRubyObject call(ThreadContext ctx, IRubyObject larg) {
                            checkContext(localContext, localContext, "all?");
                            if (!larg.isTrue()) {
                                throw JumpException.SPECIAL_JUMP;
                            }
                            return localContext.nil;
                        }
                    });
                }
            }
        } catch (JumpException.SpecialJump sj) {
            return localContext.fals;
        }
        return localContext.tru;
    }

    @JRubyMethod(name = "any?")
    public static IRubyObject any_p(ThreadContext context, IRubyObject self, final Block block) {
        return any_pCommon(context, eachSite(context), self, null, block);
    }

    @JRubyMethod(name = "any?")
    public static IRubyObject any_p(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return any_pCommon(context, eachSite(context), self, arg, block);
    }

    public static IRubyObject any_p(ThreadContext context, IRubyObject self, IRubyObject[] args, final Block block) {
        switch (args.length) {
            case 0:
                return any_pCommon(context, eachSite(context), self, null, block);
            case 1:
                return any_pCommon(context, eachSite(context), self, args[0], block);
            default:
                throw argumentError(context, args.length, 0, 1);
        }
    }

    public static IRubyObject any_pCommon(ThreadContext context, IRubyObject self, IRubyObject pattern, final Block block) {
        return any_pCommon(context, eachSite(context), self, pattern, block);
    }

    public static IRubyObject any_pCommon(ThreadContext localContext, CallSite site, IRubyObject self, IRubyObject pattern, final Block block) {
        final boolean patternGiven = pattern != null;

        if (block.isGiven() && patternGiven) {
            warn(localContext, "given block not used");
        }

        try {
            if (block.isGiven() && !patternGiven) {
                callEach(localContext, site, self, block.getSignature(), (context, largs, blk) -> {
                    checkContext(localContext, context, "any?");
                    if (block.yieldValues(context, largs).isTrue()) throw JumpException.SPECIAL_JUMP;
                    return context.nil;
                });
            } else {
                if (patternGiven) {
                    final CallSite any_op_eqq = sites(localContext).any_op_eqq;
                    callEach(localContext, site, self, Signature.ONE_REQUIRED, new BlockCallback() {
                        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
                            checkContext(localContext, context, "any?");
                            IRubyObject larg = packEnumValues(context, largs);
                            if (any_op_eqq.call(context, pattern, pattern, larg).isTrue()) throw JumpException.SPECIAL_JUMP;
                            return context.nil;
                        }
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject larg) {
                            checkContext(localContext, context, "any?");
                            if (any_op_eqq.call(context, pattern, pattern, larg).isTrue()) throw JumpException.SPECIAL_JUMP;
                            return context.nil;
                        }
                    });
                } else {
                    callEach(localContext, site, self, Signature.OPTIONAL, new BlockCallback() {
                        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
                            checkContext(localContext, context, "any?");
                            IRubyObject larg = packEnumValues(context, largs);
                            if (larg.isTrue()) throw JumpException.SPECIAL_JUMP;
                            return context.nil;
                        }
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject larg) {
                            checkContext(localContext, context, "any?");
                            if (larg.isTrue()) throw JumpException.SPECIAL_JUMP;
                            return context.nil;
                        }
                    });
                }
            }
        } catch (JumpException.SpecialJump sj) {
            return localContext.tru;
        }
        return localContext.fals;
    }

    @JRubyMethod(name = "zip")
    public static IRubyObject zip(ThreadContext context, IRubyObject self, final Block block) {
        return zipCommon(context, self, NULL_ARRAY, block);
    }

    @JRubyMethod(name = "zip")
    public static IRubyObject zip(ThreadContext context, IRubyObject self, final IRubyObject arg0, final Block block) {
        return zipCommon(context, self, arg0, block);
    }

    @JRubyMethod(name = "zip", rest = true)
    public static IRubyObject zip(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        return zipCommon(context, self, args, block);
    }

    public static IRubyObject zipCommon(ThreadContext context, IRubyObject self, IRubyObject arg0, final Block block) {
        IRubyObject newArg = TypeConverter.convertToType(arg0, arrayClass(context), "to_ary", false);
        if (!newArg.isNil()) return zipCommonAry(context, self, newArg, block);

        if (!arg0.respondsTo("each")) throw typeError(context, "wrong argument type ", arg0, " (must respond to :each)");
        newArg = sites(context).to_enum.call(context, arg0, arg0, asSymbol(context, "each")); // args[i].to_enum(:each)

        return zipCommonEnum(context, self, newArg, block);
    }

    public static IRubyObject zipCommon(ThreadContext context, IRubyObject self, IRubyObject[] args, final Block block) {
        var Array = arrayClass(context);
        final IRubyObject[] newArgs = new IRubyObject[args.length];

        boolean hasUncoercible = false;
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = TypeConverter.convertToType(args[i], Array, "to_ary", false);
            if (newArgs[i].isNil()) {
                hasUncoercible = true;
                break; // since we will overwrite newArgs[]
            }
        }

        // Handle uncoercibles by trying to_enum conversion
        if (!hasUncoercible) return zipCommonAry(context, self, newArgs, block);

        var each = asSymbol(context, "each");
        for (int i = 0; i < args.length; i++) {
            if (!args[i].respondsTo("each")) throw typeError(context, "wrong argument type ", args[i], " (must respond to :each)");

            newArgs[i] = sites(context).to_enum.call(context, args[i], args[i], each); // args[i].to_enum(:each)
        }

        return zipCommonEnum(context, self, newArgs, block);
    }

    // See enum_zip + zip_ary in Ruby source
    public static IRubyObject zipCommonAry(ThreadContext context, IRubyObject self,
            final IRubyObject[] args, final Block block) {
        return zipCommon(context, self, args, block, (ctx, elt, i) -> ((RubyArray) elt).entry(i));
    }

    // See enum_zip + zip_ary in Ruby source
    public static IRubyObject zipCommonAry(ThreadContext context, IRubyObject self,
                                           final IRubyObject arg0, final Block block) {
        return zipCommon(context, self, arg0, block, (ctx, elt, i) -> ((RubyArray) elt).entry(i));
    }

    // See enum_zip + zip_i in Ruby source
    public static IRubyObject zipCommonEnum(ThreadContext context, IRubyObject self,
            final IRubyObject[] args, final Block block) {
        return zipCommon(context, self, args, block, (ctx, elt, i) -> zipEnumNext(ctx, elt));
    }

    // See enum_zip + zip_i in Ruby source
    public static IRubyObject zipCommonEnum(ThreadContext context, IRubyObject self,
                                            final IRubyObject arg0, final Block block) {
        return zipCommon(context, self, arg0, block, (ctx, elt, i) -> zipEnumNext(ctx, elt));
    }

    // See enum_zip + zip_i in Ruby source
    public static IRubyObject zipCommon(ThreadContext context, IRubyObject self,
                                            final IRubyObject[] args, final Block block, ObjectObjectIntFunction<ThreadContext, IRubyObject, IRubyObject> nextElement) {
        final int len = args.length + 1;
        final AtomicInteger ix = new AtomicInteger(0);

        if (block.isGiven()) {
            callEach(context, eachSite(context), self, (ctx, largs, unused) -> {
                var array = RubyArray.newBlankArrayInternal(ctx.runtime, len);
                int myIx = ix.getAndIncrement();
                array.eltInternalSet(0, packEnumValues(ctx, largs));
                for (int i = 0, j = args.length; i < j; i++) {
                    array.eltInternalSet(i + 1, nextElement.apply(context, args[i], myIx));
                }
                array.realLength = len;
                block.yield(ctx, array);
                return ctx.nil;
            });
            return context.nil;
        } else {
            final var zip = newArray(context);
            callEach(context, eachSite(context), self, Signature.ONE_REQUIRED, (ctx, largs, unused) -> {
                var array = RubyArray.newBlankArrayInternal(ctx.runtime, len);
                int myIx = ix.getAndIncrement();
                array.eltInternalSet(0, packEnumValues(ctx, largs));
                for (int i = 0, j = args.length; i < j; i++) {
                    array.eltInternalSet(i + 1, nextElement.apply(context, args[i], myIx));
                }
                array.realLength = len;
                synchronized (zip) { zip.append(context, array); }
                return ctx.nil;
            });
            return zip;
        }
    }

    // See enum_zip + zip_i in Ruby source
    // This path is specialized for a single companion enumerable/array
    public static IRubyObject zipCommon(ThreadContext context, IRubyObject self,
                                        final IRubyObject arg0, final Block block, ObjectObjectIntFunction<ThreadContext, IRubyObject, IRubyObject> nextElement) {
        final AtomicInteger ix = new AtomicInteger(0);

        if (block.isGiven()) {
            callEach(context, eachSite(context), self, (ctx, largs, unused) -> {
                int myIx = ix.getAndIncrement();
                block.yield(ctx, newArray(ctx, packEnumValues(ctx, largs), nextElement.apply(ctx, arg0, myIx)));
                return ctx.nil;
            });
            return context.nil;
        } else {
            final var zip = newArray(context);
            callEach(context, eachSite(context), self, Signature.ONE_REQUIRED, (ctx, largs, unused) -> {
                int myIx = ix.getAndIncrement();
                var array = newArray(ctx, packEnumValues(ctx, largs), nextElement.apply(ctx, arg0, myIx));
                synchronized (zip) { zip.append(ctx, array); }
                return ctx.nil;
            });
            return zip;
        }
    }

    /**
     * Take all items from the given enumerable and insert them into a new array.
     *
     * See take_items() in array.c.
     *
     * @param context current context
     * @param enumerable object from which to take
     * @return an array of the object's elements
     */
    public static IRubyObject takeItems(ThreadContext context, IRubyObject enumerable) {
        final var array = newArray(context);
        synchronized (array) {
            callEach(context, eachSite(context), enumerable, Signature.ONE_ARGUMENT, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    array.append(ctx, larg);
                    return larg;
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg) {
                    array.append(ctx, larg);
                    return larg;
                }
            });
        }

        return array;
    }

    public static IRubyObject zipEnumNext(ThreadContext context, IRubyObject arg) {
        if (arg.isNil()) return context.nil;

        var globalVariables = Access.globalVariables(context);
        IRubyObject oldExc = globalVariables.get("$!");
        try {
            return sites(context).zip_next.call(context, arg, arg);
        } catch (RaiseException re) {
            if (re.getException().getMetaClass() == context.runtime.getStopIteration()) {
                globalVariables.set("$!", oldExc);
                return context.nil;
            }
            throw re;
        }
    }

    @JRubyMethod
    public static IRubyObject group_by(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "group_by", RubyEnumerable::size);

        final RubyHash result = new RubyHash(context.runtime);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            IRubyObject key = block.yield(ctx, larg);
            synchronized (result) {
                var curr = (RubyArray<?>)result.fastARef(key);

                if (curr == null) {
                    curr = newArray(context);
                    result.fastASet(key, curr);
                }
                curr.append(context, larg);
            }
            return ctx.nil;
        });

        return result;
    }

    @JRubyMethod(rest = true)
    public static IRubyObject chain(ThreadContext context, IRubyObject self, final IRubyObject[] args) {
        IRubyObject [] enums = new IRubyObject[args.length + 1];
        enums[0] = self;
        System.arraycopy(args, 0, enums, 1, args.length);

        return RubyChain.newChain(context, enums);
    }

    @JRubyMethod
    public static IRubyObject chunk(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;

        if(!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "chunk", RubyEnumerable::size);
        }

        IRubyObject enumerator = runtime.getEnumerator().allocate(context);
        enumerator.getInternalVariables().setInternalVariable("chunk_enumerable", self);
        enumerator.getInternalVariables().setInternalVariable("chunk_categorize",
                RubyProc.newProc(runtime, block, block.type == Block.Type.LAMBDA ? block.type : Block.Type.PROC));

        Helpers.invoke(context, enumerator, "initialize",
                CallBlock.newCallClosure(context, self, Signature.ONE_ARGUMENT,
                        new ChunkedBlockCallback(runtime, enumerator)));
        return enumerator;
    }

    @JRubyMethod
    public static IRubyObject uniq(ThreadContext context, IRubyObject self, final Block block) {
        final RubyHash hash = new RubyHash(context.runtime, 12, false);

        final CachingCallSite each = eachSite(context);
        if (block.isGiven()) {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject obj, Block blk) {
                    IRubyObject key = block.yield(ctx, obj);
                    if (hash.getEntry(key) == RubyHash.NO_ENTRY) {
                        hash.internalPut(key, obj);
                    }
                    return obj;
                }
            });
            return hash.values(context);
        } else {
            // relying on Hash order - if obj key existed it stays at the same place
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject obj, Block blk) {
                    // relying on Hash order - if obj key existed it stays at the same place
                    hash.internalPut(obj, obj);
                    return obj;
                }
            });
            return hash.keys(context);
        }
    }

    @JRubyMethod
    public static IRubyObject compact(ThreadContext context, IRubyObject self) {
        RubyArray array = RubyArray.newEmptyArray(context.runtime);

        final CachingCallSite each = eachSite(context);
        callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject obj, Block blk) {
                if (!obj.isNil()) array.append(ctx, obj);
                return obj;
            }
        });
        return array;
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    static IRubyObject size(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        IRubyObject size = self.checkCallMethod(context, sites(context).size_checked);
        return size == null ? context.nil : size;
    }

    private static final class ChunkArg {

        ChunkArg(final ThreadContext context) {
            this.prev_elts = this.prev_value = context.nil;
        }

        IRubyObject prev_value;
        IRubyObject prev_elts;

    }

    // chunk_i
    public static final class ChunkedBlockCallback implements BlockCallback {
        private final Ruby runtime;
        private final IRubyObject enumerator;

        public ChunkedBlockCallback(Ruby runtime, IRubyObject enumerator) {
            this.runtime = runtime;
            this.enumerator = enumerator;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
            ThreadContext.resetCallInfo(context);
            InternalVariables variables = enumerator.getInternalVariables();
            final IRubyObject enumerable = (IRubyObject) variables.getInternalVariable("chunk_enumerable");
            final RubyProc categorize = (RubyProc) variables.getInternalVariable("chunk_categorize");
            final IRubyObject yielder = packEnumValues(context, args);
            final ChunkArg arg = new ChunkArg(context);

            final RubySymbol alone = asSymbol(context, "_alone");
            final RubySymbol separator = asSymbol(context, "_separator");
            final EnumerableSites sites = sites(context);
            final CallSite chunk_call = sites.chunk_call;
            final CallSite chunk_op_lshift = sites.chunk_op_lshift;

            // if chunk's categorize block has arity one, we pass it the packed args
            // else we let it spread the args as it sees fit for its arity
            callEach(context, eachSite(context), enumerable, Signature.OPTIONAL, (ctx, largs, blk) -> {
                final IRubyObject larg = packEnumValues(ctx, largs);
                final IRubyObject v = categorize.getBlock().getSignature().arityValue() == 1 ?
                    chunk_call.call(ctx, categorize, categorize, larg) : // categorize block has arity one, we pass it the packed args
                    chunk_call.call(ctx, categorize, categorize, largs); // else spread the args as it sees fit for its arity

                if ( v == alone ) {
                    if ( ! arg.prev_value.isNil() ) {
                        chunk_op_lshift.call(ctx, yielder, yielder, newArray(context, arg.prev_value, arg.prev_elts));
                        arg.prev_value = arg.prev_elts = ctx.nil;
                    }
                    chunk_op_lshift.call(ctx, yielder, yielder, newArray(ctx, v, newArray(context, larg)));
                } else if ( v.isNil() || v == separator ) {
                    if( ! arg.prev_value.isNil() ) {
                        chunk_op_lshift.call(ctx, yielder, yielder, newArray(ctx, arg.prev_value, arg.prev_elts));
                        arg.prev_value = arg.prev_elts = ctx.nil;
                    }
                } else if ( (v instanceof RubySymbol) && v.toString().charAt(0) == '_' ) {
                    throw runtimeError(context, "symbol begins with an underscore is reserved");
                } else {
                    if ( arg.prev_value.isNil() ) {
                        arg.prev_value = v;
                        arg.prev_elts = newArray(ctx, larg);
                    } else {
                        if ( arg.prev_value.equals(v) ) {
                            ((RubyArray) arg.prev_elts).append(ctx, larg);
                        } else {
                            chunk_op_lshift.call(ctx, yielder, yielder, newArray(ctx, arg.prev_value, arg.prev_elts));
                            arg.prev_value = v;
                            arg.prev_elts = newArray(ctx, larg);
                        }
                    }
                }
                return ctx.nil;
            });

            if ( ! arg.prev_elts.isNil() ) {
                chunk_op_lshift.call(context, yielder, yielder, newArray(context, arg.prev_value, arg.prev_elts));
            }

            return context.nil;
        }
    }

    public static final class AppendBlockCallback implements BlockCallback {

        private final RubyArray result;

        @Deprecated
        public AppendBlockCallback(Ruby runtime, RubyArray result) {
            this.result = result;
        }

        AppendBlockCallback(final RubyArray result) {
            this.result = result;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
            ThreadContext.resetCallInfo(context);
            result.append(context, packEnumValues(context, args));
            return context.nil;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject arg, Block block) {
            ThreadContext.resetCallInfo(context);
            result.append(context, arg);
            return context.nil;
        }

    }

    public static final class PutKeyValueCallback implements BlockCallback {

        private final RubyHash result;
        private final Block block;

        @Deprecated
        public PutKeyValueCallback(Ruby runtime, RubyHash result) {
            this.result = result;
            this.block = Block.NULL_BLOCK;
        }

        @Deprecated
        public PutKeyValueCallback(Ruby runtime, RubyHash result, Block block) {
            this.result = result;
            this.block = block;
        }

        PutKeyValueCallback(RubyHash result) {
            this.result = result;
            this.block = Block.NULL_BLOCK;
        }

        PutKeyValueCallback(RubyHash result, Block block) {
            this.result = result;
            this.block = block;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
            ThreadContext.resetCallInfo(context);
            final boolean blockGiven = block.isGiven();

            IRubyObject value;
            switch (largs.length) {
                case 0:
                    value = blockGiven ? block.yield(context, context.nil) : context.nil;
                    break;
                case 1:
                    value = blockGiven ? block.yield(context, largs[0]) : largs[0];
                    break;
                default:
                    IRubyObject v = RubyArray.newArrayMayCopy(context.runtime, largs);
                    value = blockGiven ? block.yield(context, v) : v;
                    break;
            }

            callImpl(context, value);
            return context.nil;
        }

        private void callImpl(ThreadContext context, IRubyObject value) {
            IRubyObject ary = TypeConverter.checkArrayType(context.runtime, value);
            if (ary.isNil()) throw typeError(context, "wrong element type ", value, " (expected array)");
            final RubyArray array = (RubyArray) ary;
            if (array.size() != 2) {
                throw argumentError(context, "element has wrong array length (expected 2, was " + array.size() + ")");
            }
            result.fastASetCheckString(context.runtime, array.eltOk(0), array.eltOk(1));
        }

    }

    public static final class TallyCallback implements BlockCallback {

        private final RubyHash result;

        TallyCallback(RubyHash result) {
            this.result = result;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
            ThreadContext.resetCallInfo(context);
            IRubyObject value;
            if (largs.length == 0) {
                value = context.nil;
            } else if (largs.length == 1) {
                value = largs[0];
            } else {
                value = newArrayNoCopy(context, largs);
            }

            IRubyObject count = result.fastARef(value);
            if (count == null) {
                result.fastASet(value, RubyFixnum.one(context.runtime));
            } else if (count instanceof RubyFixnum) {
                result.fastASetSmall(value, ((RubyInteger)count).succ(context));
            } else {
                TypeConverter.checkType(context, count, context.runtime.getInteger());
                result.fastASetSmall(value, ((RubyBignum) count).op_plus(context, 1L));
            }
            return context.nil;
        }

    }

    @Deprecated
    public static IRubyObject callEach(ThreadContext context, IRubyObject self, IRubyObject[] args, Signature signature,
                                       BlockCallback callback) {
        return callEach(context, eachSite(context), self, args, signature, callback);
    }

    @Deprecated
    public static IRubyObject callEach(ThreadContext context, IRubyObject self, BlockCallback callback) {
        return callEach(context, eachSite(context), self, callback);
    }

    @Deprecated
    public static IRubyObject each(ThreadContext context, IRubyObject self, BlockBody body) {
        return each(context, eachSite(context), self, body);
    }

    protected static CachingCallSite eachSite(ThreadContext context) {
        return sites(context).each;
    }

    private static EnumerableSites sites(ThreadContext context) {
        return context.sites.Enumerable;
    }
}
