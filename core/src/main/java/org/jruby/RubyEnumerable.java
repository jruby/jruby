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
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyObject.equalInternal;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;

/**
 * The implementation of Ruby's Enumerable module.
 */

@JRubyModule(name="Enumerable")
public class RubyEnumerable {

    public static RubyModule createEnumerableModule(Ruby runtime) {
        RubyModule enumModule = runtime.defineModule("Enumerable");

        enumModule.defineAnnotatedMethods(RubyEnumerable.class);

        return enumModule;
    }

    public static IRubyObject callEach(ThreadContext context, IRubyObject self, Signature signature, BlockCallback callback) {
        return callEach(context, eachSite(context), self, signature, callback);
    }

    public static IRubyObject callEach(ThreadContext context, CallSite each, IRubyObject self, Signature signature, BlockCallback callback) {
        return each.call(context, self, self, CallBlock.newCallClosure(context, self, signature, callback));
    }

    public static IRubyObject callEach(ThreadContext context, IRubyObject self, BlockCallback callback) {
        return callEach(context, eachSite(context), self, callback);
    }

    public static IRubyObject callEach(ThreadContext context, CallSite each, IRubyObject self, BlockCallback callback) {
        return each.call(context, self, self, CallBlock.newCallClosure(context, self, Signature.OPTIONAL, callback));
    }

    public static IRubyObject callEach(ThreadContext context, IRubyObject self, IRubyObject[] args, Signature signature,
                                       BlockCallback callback) {
        return callEach(context, eachSite(context), self, args, signature, callback);
    }

    public static IRubyObject callEach(ThreadContext context, CallSite each, IRubyObject self, IRubyObject[] args, Signature signature,
                                       BlockCallback callback) {
        return each.call(context, self, self, args, CallBlock.newCallClosure(context, self, signature, callback));
    }

    public static IRubyObject each(ThreadContext context, IRubyObject self, BlockBody body) {
        return each(context, eachSite(context), self, body);
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
        final Ruby runtime = context.runtime;
        final int result[] = new int[] { 0 };

        if (block.isGiven()) {
            each(context, each, self, new JavaInternalBlockBody(runtime, context, "Enumerable#count", block.getSignature()) {
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    if (block.yield(context1, value).isTrue()) result[0]++;
                    return context1.nil;
                }
            });
        } else {
            each(context, each, self, new JavaInternalBlockBody(runtime, context, "Enumerable#count", Signature.NO_ARGUMENTS) {
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    result[0]++;
                    return context1.nil;
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    result[0]++;
                    return context1.nil;
                }
            });
        }
        return RubyFixnum.newFixnum(runtime, result[0]);
    }

    @JRubyMethod(name = "count")
    public static IRubyObject count(ThreadContext context, IRubyObject self, final IRubyObject methodArg, final Block block) {
        final Ruby runtime = context.runtime;
        final int result[] = new int[] { 0 };

        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");

        each(context, eachSite(context), self, new JavaInternalBlockBody(runtime, context, "Enumerable#count", Signature.ONE_REQUIRED) {
            public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                return this.yield(context1, packEnumValues(context1, args));
            }
            @Override
            public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                if (value.equals(methodArg)) result[0]++;
                return context1.nil;
            }
        });

        return RubyFixnum.newFixnum(runtime, result[0]);
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

        long times = RubyNumeric.num2long(arg);
        if (times <= 0) {
            return context.nil;
        }

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
        Ruby runtime = context.runtime;
        long mul = 0;
        IRubyObject n = runtime.getNil();

        if (args != null && args.length > 0) {
            n = args[0];
            if (!n.isNil()) mul = n.convertToInteger().getLongValue();
        }

        IRubyObject size = ((SizeFn) RubyEnumerable::size).size(context, self, args);
        if (size == null || size.isNil() || size.equals(RubyFixnum.zero(runtime))) {
            return size;
        }

        if (n == null || n.isNil()) {
            return RubyFloat.newFloat(runtime, RubyFloat.INFINITY);
        }

        if (mul <= 0) {
            return RubyFixnum.zero(runtime);
        }

        return sites(context).cycle_op_mul.call(context, size, size, mul);
    }

    @JRubyMethod(name = "take")
    public static IRubyObject take(ThreadContext context, IRubyObject self, IRubyObject n, Block block) {
        final Ruby runtime = context.runtime;
        final long len = RubyNumeric.num2long(n);

        if (len < 0) throw runtime.newArgumentError("attempt to take negative size");
        if (len == 0) return runtime.newEmptyArray();

        final RubyArray result = runtime.newArray();

        try {
            // Atomic ?
            each(context, eachSite(context), self, new JavaInternalBlockBody(runtime, Signature.OPTIONAL) {
                long i = len; // Atomic ?
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    synchronized (result) {
                        result.append(value);
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
        if (!block.isGiven()) {
            return enumeratorize(context.runtime, self, "take_while");
        }

        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

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
                synchronized (result) { result.append(larg); }
                return ctx.nil;
            });
        } catch (JumpException.SpecialJump sj) {}
        return result;
    }

    @JRubyMethod(name = "drop")
    public static IRubyObject drop(ThreadContext context, IRubyObject self, IRubyObject n, final Block block) {
        final Ruby runtime = context.runtime;
        final long len = RubyNumeric.num2long(n);

        if (len < 0) throw runtime.newArgumentError("attempt to drop negative size");

        final RubyArray result = runtime.newArray();

        try {
            // Atomic ?
            each(context, eachSite(context), self, new JavaInternalBlockBody(runtime, Signature.OPTIONAL) {
                long i = len; // Atomic ?
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    synchronized (result) {
                        if (i == 0) {
                            result.append(value);
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
        if (!block.isGiven()) {
            return enumeratorize(context.runtime, self, "drop_while");
        }

        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

        try {
            each(context, eachSite(context), self, new JavaInternalBlockBody(runtime, context, "Enumerable#drop_while", Signature.OPTIONAL) {
                boolean memo = false;
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    if (!memo && !block.yield(context1, value).isTrue()) memo = true;
                    if (memo) synchronized (result) { result.append(value); }
                    return context1.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return result;
    }

    @JRubyMethod(name = "first")
    public static IRubyObject first(ThreadContext context, IRubyObject self) {
        final IRubyObject[] holder = new IRubyObject[]{ context.nil };

        try {
            each(context, eachSite(context), self, new JavaInternalBlockBody(context.runtime, context, "Enumerable#first", Signature.OPTIONAL) {
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    holder[0] = value;
                    throw JumpException.SPECIAL_JUMP;
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return holder[0];
    }

    @JRubyMethod(name = "first")
    public static IRubyObject first(ThreadContext context, IRubyObject self, final IRubyObject num) {
        final Ruby runtime = context.runtime;
        final long firstCount = RubyNumeric.num2long(num);

        if (firstCount == 0) return runtime.newEmptyArray();
        if (firstCount < 0) throw runtime.newArgumentError("attempt to take negative size");
        final RubyArray result = RubyArray.newArray(runtime, firstCount);

        try {
            each(context, eachSite(context), self, new JavaInternalBlockBody(runtime, context, "Enumerable#first", Signature.OPTIONAL) {
                private long iter = firstCount;
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return this.yield(context1, packEnumValues(context1, args));
                }
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject value) {
                    result.append(value);
                    if (iter-- == 1) throw JumpException.SPECIAL_JUMP;
                    return context1.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return result;
    }

    @JRubyMethod(name = {"to_a", "entries"})
    public static IRubyObject to_a(ThreadContext context, IRubyObject self) {
        RubyArray result = context.runtime.newArray();
        callEach(context, eachSite(context), self, Signature.OPTIONAL, new AppendBlockCallback(result));
        result.infectBy(self);
        return result;
    }

    @JRubyMethod(name = {"to_a", "entries"}, rest = true)
    public static IRubyObject to_a(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();
        Helpers.invoke(context, self, "each", args,
                CallBlock.newCallClosure(context, self, Signature.OPTIONAL, new AppendBlockCallback(result)));
        result.infectBy(self);
        return result;
    }

    @JRubyMethod(name = "to_h", rest = true)
    public static IRubyObject to_h(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;
        final RubyHash result = RubyHash.newHash(runtime);
        Helpers.invoke(context, self, "each", args,
                CallBlock.newCallClosure(context, self, Signature.OPTIONAL, new PutKeyValueCallback(result, block)));
        result.infectBy(self);
        return result;
    }

    @JRubyMethod
    public static IRubyObject sort(ThreadContext context, IRubyObject self, final Block block) {
        final RubyArray result = context.runtime.newArray();

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new AppendBlockCallback(result));
        result.sort_bang(context, block);

        return result;
    }

    @JRubyMethod
    public static IRubyObject sort_by(final ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        IRubyObject[][] valuesAndCriteria;

        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "sort_by", (SizeFn) RubyEnumerable::size);
        }

        final CachingCallSite each = eachSite(context);
        if (self instanceof RubyArray) {
            RubyArray selfArray = (RubyArray) self;
            final IRubyObject[][] valuesAndCriteriaArray = new IRubyObject[selfArray.size()][2];

            each(context, each, self, new JavaInternalBlockBody(runtime, Signature.OPTIONAL) {
                final AtomicInteger i = new AtomicInteger(0);
                @Override
                public IRubyObject yield(ThreadContext context1, IRubyObject[] args) {
                    return doYield(context1, null, packEnumValues(context1, args));
                }
                @Override
                protected IRubyObject doYield(ThreadContext context1, Block unused, IRubyObject value) {
                    IRubyObject[] myVandC = valuesAndCriteriaArray[i.getAndIncrement()];
                    myVandC[0] = value;
                    myVandC[1] = block.yield(context1, value);
                    return context1.nil;
                }
            });

            valuesAndCriteria = valuesAndCriteriaArray;
        } else {
            final ArrayList<IRubyObject[]> valuesAndCriteriaList = new ArrayList<>();

            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext context1, IRubyObject[] args, Block unused) {
                    return call(context1, packEnumValues(context1, args), unused);
                }
                @Override
                public IRubyObject call(ThreadContext context1, IRubyObject arg, Block unused) {
                    valuesAndCriteriaList.add(new IRubyObject[] { arg, block.yield(context1, arg) });
                    return context1.nil;
                }
            });

            valuesAndCriteria = valuesAndCriteriaList.toArray(new IRubyObject[valuesAndCriteriaList.size()][]);
        }

        Arrays.sort(valuesAndCriteria,
                (o1, o2) -> RubyComparable.cmpint(context, invokedynamic(context, o1[1], OP_CMP, o2[1]), o1[1], o2[1]));


        IRubyObject dstArray[] = new IRubyObject[valuesAndCriteria.length];
        for (int i = 0; i < dstArray.length; i++) {
            dstArray[i] = valuesAndCriteria[i][0];
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
        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

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
                        synchronized (result) { result.append(value); }
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
                        synchronized (result) { result.append(arg); }
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
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { null };

        try {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    checkContext(context, ctx, "detect/find");
                    if (block.yield(ctx, larg).isTrue()) {
                        result[0] = larg;
                        throw JumpException.SPECIAL_JUMP;
                    }
                    return ctx.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return result[0];
        }

        return ifnone != null && !ifnone.isNil() ? sites(context).detect_call.call(context, ifnone, ifnone) : runtime.getNil();
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
        final Ruby runtime = context.runtime;

        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");
        if (self instanceof RubyArray) return ((RubyArray) self).find_index(context, cond);

        return find_indexCommon(context, eachSite(context), self, cond);
    }

    public static IRubyObject find_indexCommon(ThreadContext context, IRubyObject self, final Block block, Signature callbackArity) {
        return find_indexCommon(context, eachSite(context), self, block, callbackArity);
    }

    public static IRubyObject find_indexCommon(ThreadContext context, CallSite each, IRubyObject self, final Block block, Signature callbackArity) {
        final Ruby runtime = context.runtime;
        final long result[] = new long[] {0};

        try {
            callEach(context, each, self, callbackArity, (ctx, largs, blk) -> {
                if (block.yieldValues(ctx, largs).isTrue()) throw JumpException.SPECIAL_JUMP;
                result[0]++;
                return ctx.nil;
            });
        } catch (JumpException.SpecialJump sj) {
            return RubyFixnum.newFixnum(runtime, result[0]);
        }

        return context.nil;
    }

    public static IRubyObject find_indexCommon(ThreadContext context, IRubyObject self, final IRubyObject cond) {
        return find_indexCommon(context, eachSite(context), self, cond);
    }

    public static IRubyObject find_indexCommon(ThreadContext context, CallSite each, IRubyObject self, final IRubyObject cond) {
        final Ruby runtime = context.runtime;
        final long result[] = new long[] {0};

        try {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    if (equalInternal(ctx, larg, cond)) throw JumpException.SPECIAL_JUMP;
                    result[0]++;
                    return ctx.nil;
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return RubyFixnum.newFixnum(runtime, result[0]);
        }

        return context.nil;
    }

    public static IRubyObject selectCommon(ThreadContext context, IRubyObject self, final Block block, String methodName) {
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, methodName, (SizeFn) RubyEnumerable::size);
        }

        final RubyArray result = context.runtime.newArray();

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                if (block.yield(ctx, larg).isTrue()) {
                    synchronized (result) { result.append(larg); }
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
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "reject", (SizeFn) RubyEnumerable::size);
        }

        final RubyArray result = context.runtime.newArray();

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                if ( ! block.yield(ctx, larg).isTrue() ) {
                    synchronized (result) { result.append(larg); }
                }
                return ctx.nil;
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg) {
                if ( ! block.yield(ctx, larg).isTrue() ) {
                    synchronized (result) { result.append(larg); }
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
        final Ruby runtime = context.runtime;
        if (block.isGiven()) {
            final RubyArray result = runtime.newArray();

            eachSite(context).call(context, self, self, CallBlock19.newCallClosure(self, runtime.getEnumerable(), block.getSignature(), new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    final IRubyObject larg; boolean ary = false;
                    switch (largs.length) {
                        case 0:  larg = ctx.nil; break;
                        case 1:  larg = largs[0]; break;
                        default: larg = RubyArray.newArrayMayCopy(ctx.runtime, largs); ary = true;
                    }
                    IRubyObject val = ary ? block.yieldArray(ctx, larg, null) : block.yield(ctx, larg);

                    synchronized (result) { result.append(val); }
                    return ctx.nil;
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    IRubyObject val = block.yield(ctx, larg);

                    synchronized (result) { result.append(val); }
                    return ctx.nil;
                }
            }, context));
            return result;
        } else {
            return enumeratorizeWithSize(context, self, methodName, (SizeFn) RubyEnumerable::size);
        }
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
        if (block.isGiven()) {
            final RubyArray ary = context.runtime.newArray();

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
                            ary.append(i);
                        } else {
                            ary.concat(ctx, tmp);
                        }
                    }
                    return ctx.nil;
                }
            });
            return ary;
        } else {
            return enumeratorizeWithSize(context, self, methodName, (SizeFn) RubyEnumerable::size);
        }
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
        final IRubyObject result[] = new IRubyObject[] { init };
        final double memo[] = new double[] { 0.0 };

        if (block.isGiven()) {
            callEach(context, each, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    result[0] = sumAdd(ctx, result[0], block.yieldArray(ctx, larg, null), memo);
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
                    result[0] = sumAdd(ctx, result[0], larg, memo);
                    return ctx.nil;
                }
            });
        }

        if (result[0] instanceof RubyFloat) {
            return ((RubyFloat) result[0]).op_plus(context, memo[0]);
        }
        return result[0];
    }

    /* FIXME: optimise for special types (e.g. Integer)? */
    /* NB: MRI says "Enumerable#sum method may not respect method redefinition of "+" methods such as Integer#+." */
    public static IRubyObject sumAdd(final ThreadContext context, IRubyObject lhs, IRubyObject rhs, final double c[]) {
        boolean floats = false;
        double f = 0.0;
        /*
         * Kahan-Babuska balancing compensated summation algorithm
         * See http://link.springer.com/article/10.1007/s00607-005-0139-x
         */
        double x = 0.0, t;
        if (lhs instanceof RubyFloat) {
            if (rhs instanceof RubyFloat) {
                f = ((RubyFloat) lhs).value;
                x = ((RubyFloat) rhs).value;
                floats = true;
            } else if (rhs instanceof RubyFixnum) {
                f = ((RubyFloat) lhs).value;
                x = ((RubyFixnum) rhs).value;
                floats = true;
            } else if (rhs instanceof RubyBignum) {
                f = ((RubyFloat) lhs).value;
                x = ((RubyBignum) rhs).getDoubleValue();
                floats = true;
            } else if (rhs instanceof RubyRational) {
                f = ((RubyFloat) lhs).getValue();
                x = ((RubyRational) rhs).getDoubleValue(context);
                floats = true;
            }
        } else if (rhs instanceof RubyFloat) {
            if (lhs instanceof RubyFixnum) {
                c[0] = 0.0;
                f = ((RubyFixnum) lhs).value;
                x = ((RubyFloat) rhs).value;
                floats = true;
            } else if (lhs instanceof RubyBignum) {
                c[0] = 0.0;
                f = ((RubyBignum) lhs).getDoubleValue();
                x = ((RubyFloat) rhs).value;
                floats = true;
            } else if (lhs instanceof RubyRational) {
                c[0] = 0.0;
                f = ((RubyRational) lhs).getDoubleValue();
                x = ((RubyFloat) rhs).value;
                floats = true;
            }
        }

        if (!floats) {
            return sites(context).sum_op_plus.call(context, lhs, lhs, rhs);
        }

        Ruby runtime = context.runtime;

        if (Double.isNaN(f)) return lhs;
        if (Double.isNaN(x)) {
            return lhs;
        }
        if (Double.isInfinite(x)) {
            if (Double.isInfinite(f) && Math.signum(x) != Math.signum(f)) {
                return new RubyFloat(runtime, RubyFloat.NAN);
            } else {
                return rhs;
            }
        }
        if (Double.isInfinite(f)) return lhs;

        // Kahan's compensated summation algorithm
        t = f + x;
        if (Math.abs(f) >= Math.abs(x)) {
            c[0] += ((f - t) + x);
        } else {
            c[0] += ((x - t) + f);
        }
        f = t;

        return new RubyFloat(runtime, f);
    }

    public static IRubyObject injectCommon(final ThreadContext context, IRubyObject self, IRubyObject init, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { init };

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            checkContext(context, ctx, "inject");
            result[0] = result[0] == null ?
                    larg : block.yieldArray(ctx, runtime.newArray(result[0], larg), null);

            return ctx.nil;
        });

        return result[0] == null ? context.nil : result[0];
    }

    @JRubyMethod(name = {"inject", "reduce"})
    public static IRubyObject inject(ThreadContext context, IRubyObject self, final Block block) {
        return injectCommon(context, self, null, block);
    }

    @JRubyMethod(name = {"inject", "reduce"})
    public static IRubyObject inject(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return block.isGiven() ? injectCommon(context, self, arg, block) : inject(context, self, null, arg, block);
    }

    @JRubyMethod(name = {"inject", "reduce"})
    public static IRubyObject inject(ThreadContext context, IRubyObject self, IRubyObject init, IRubyObject method, final Block block) {
        final Ruby runtime = context.runtime;

        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");

        final String methodId = method.asJavaString();
        final IRubyObject result[] = new IRubyObject[] { init };

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            final MonomorphicCallSite site = new MonomorphicCallSite(methodId);
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return call(ctx, packEnumValues(ctx, largs), blk);
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                result[0] = result[0] == null ? larg : site.call(ctx, self, result[0], larg);
                return ctx.nil;
            }
            @Override
            public IRubyObject call(ThreadContext ctx, IRubyObject larg) {
                result[0] = result[0] == null ? larg : site.call(ctx, self, result[0], larg);
                return ctx.nil;
            }
        });
        return result[0] == null ? context.nil : result[0];
    }

    @JRubyMethod
    public static IRubyObject partition(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final RubyArray arr_true = runtime.newArray();
        final RubyArray arr_false = runtime.newArray();

        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "partition", (SizeFn) RubyEnumerable::size);
        }

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            if (block.yield(ctx, larg).isTrue()) {
                synchronized (arr_true) {
                    arr_true.append(larg);
                }
            } else {
                synchronized (arr_false) {
                    arr_false.append(larg);
                }
            }

            return ctx.nil;
        });

        return runtime.newArray(arr_true, arr_false);
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
            return this.block.call(context, packEnumValues(context, iargs), context.runtime.newFixnum(index++));
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject iarg, Block block) {
            return this.block.call(context, iarg, context.runtime.newFixnum(index++));
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

    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, Block block) {
        return each_with_index(context, self, IRubyObject.NULL_ARRAY, block);
    }

    @JRubyMethod(name = "each_with_index", rest = true)
    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return block.isGiven() ? each_with_indexCommon(context, self, block, args) : enumeratorizeWithSize(context, self, "each_with_index", args, (SizeFn) RubyEnumerable::size);
    }

    @JRubyMethod(required = 1)
    public static IRubyObject each_with_object(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        return block.isGiven() ? each_with_objectCommon(context, self, block, arg) : enumeratorizeWithSize(context, self, "each_with_object", new IRubyObject[] { arg }, RubyEnumerable::size);
    }

    @JRubyMethod(rest = true)
    public static IRubyObject each_entry(ThreadContext context, final IRubyObject self, final IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_entryCommon(context, self, args, block) : enumeratorizeWithSize(context, self, "each_entry", args, RubyEnumerable::size);
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
        int size = (int) RubyNumeric.num2long(arg);
        if (size <= 0) throw context.runtime.newArgumentError("invalid size");

        return block.isGiven() ? each_sliceCommon(context, self, size, block) :
                enumeratorizeWithSize(context, self, "each_slice", new IRubyObject[]{arg}, RubyEnumerable::eachSliceSize);
    }

    static IRubyObject each_sliceCommon(ThreadContext context, IRubyObject self, final int size, final Block block) {
        final Ruby runtime = context.runtime;
        if (size <= 0) throw runtime.newArgumentError("invalid slice size");

        final RubyArray result[] = new RubyArray[] { runtime.newArray(size) };

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            result[0].append(packEnumValues(ctx, largs));
            if (result[0].size() == size) {
                block.yield(ctx, result[0]);
                result[0] = runtime.newArray(size);
            }
            return ctx.nil;
        });

        if (result[0].size() > 0) block.yield(context, result[0]);
        return context.nil;
    }

    /**
     * A each_slice size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachSliceSize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #each_slice ensures arg[0] is numeric
        long sliceSize = ((RubyNumeric) args[0]).getLongValue();
        if (sliceSize <= 0) {
            throw runtime.newArgumentError("invalid slice size");
        }

        IRubyObject size = RubyEnumerable.size(context, self, args);
        if (size == null || size.isNil()) {
            return runtime.getNil();
        }

        IRubyObject n = sites(context).each_slice_op_plus.call(context, size, size, sliceSize - 1);
        return sites(context).each_slice_op_div.call(context, n, n, sliceSize);
    }

    @JRubyMethod(name = "each_cons")
    public static IRubyObject each_cons(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        int size = (int) RubyNumeric.num2long(arg);
        if (size <= 0) throw context.runtime.newArgumentError("invalid size");
        return block.isGiven() ? each_consCommon(context, self, size, block) : enumeratorizeWithSize(context, self, "each_cons", new IRubyObject[] { arg }, (SizeFn) RubyEnumerable::eachConsSize);
    }

    static IRubyObject each_consCommon(ThreadContext context, IRubyObject self, final int size, final Block block) {
        final RubyArray result = context.runtime.newArray(size);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            if (result.size() == size) result.shift(ctx);
            result.append(packEnumValues(ctx, largs));
            if (result.size() == size) block.yield(ctx, result.aryDup());
            return ctx.nil;
        });

        return context.nil;
    }

    /**
     * A each_cons size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachConsSize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #each_cons ensures arg[0] is numeric
        long consSize = ((RubyNumeric) args[0]).getLongValue();
        if (consSize <= 0) {
            throw runtime.newArgumentError("invalid size");
        }

        IRubyObject size = ((SizeFn) RubyEnumerable::size).size(context, self, args);
        if (size == null || size.isNil()) {
            return runtime.getNil();
        }


        IRubyObject n = sites(context).each_cons_op_plus.call(context, size, size, 1 - consSize);
        RubyFixnum zero = RubyFixnum.zero(runtime);
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

    @JRubyMethod(name = {"include?", "member?"}, required = 1)
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
        RubyArray sorted = (RubyArray)sort(context, self, block);
        if (arg.isNil()) return sorted.last();
        return ((RubyArray) sorted.last(arg)).reverse();
    }

    @JRubyMethod
    public static IRubyObject min(ThreadContext context, IRubyObject self, final Block block) {
        return singleExtent(context, self, "min", SORT_MIN, block);
    }

    @JRubyMethod
    public static IRubyObject min(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        // TODO: Replace with an implementation (quickselect, etc) which requires O(k) memory rather than O(n) memory
        RubyArray sorted = (RubyArray)sort(context, self, block);
        if (arg.isNil()) return sorted.first();
        return sorted.first(arg);
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
        RubyArray sorted = (RubyArray)sort_by(context, self, block);
        return ((RubyArray) sorted.last(arg)).reverse();
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
        RubyArray sorted = (RubyArray)sort_by(context, self, block);
        return sorted.first(arg);
    }

    private static final int SORT_MAX =  1;
    private static final int SORT_MIN = -1;
    private static IRubyObject singleExtent(final ThreadContext context, IRubyObject self, final String op, final int sortDirection, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { null };

        Signature signature = block.isGiven() ? block.getSignature() : Signature.ONE_REQUIRED;
        callEach(context, eachSite(context), self, signature, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            checkContext(context, ctx, op + "{}");

            if (result[0] == null ||
                    (block.isGiven() &&
                            RubyComparable.cmpint(ctx, block.yieldArray(ctx, runtime.newArray(larg, result[0]), null), larg, result[0]) * sortDirection > 0) ||
                    (!block.isGiven() &&
                            RubyComparable.cmpint(ctx, invokedynamic(ctx, larg, OP_CMP, result[0]), larg, result[0]) * sortDirection > 0)) {
                result[0] = larg;
            }
            return ctx.nil;
        });

        return result[0] == null ? context.nil : result[0];
    }

    private static IRubyObject singleExtentBy(final ThreadContext context, IRubyObject self, final String op, final int sortDirection, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, op, RubyEnumerable::size);

        final IRubyObject result[] = new IRubyObject[] { context.nil };

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            IRubyObject memo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(ctx, largs);
                checkContext(context, ctx, op);
                IRubyObject v = block.yield(ctx, larg);

                if (memo == null || RubyComparable.cmpint(ctx, invokedynamic(ctx, v, OP_CMP, memo), v, memo) * sortDirection > 0) {
                    memo = v;
                    result[0] = larg;
                }
                return ctx.nil;
            }
        });
        return result[0];
    }

    @JRubyMethod
    public static IRubyObject minmax(final ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { null, null };

        if (block.isGiven()) {
            callEach(context, eachSite(context), self, block.getSignature(), (ctx, largs, blk) -> {
                checkContext(context, ctx, "minmax");
                IRubyObject arg = packEnumValues(ctx, largs);

                if (result[0] == null) {
                    result[0] = result[1] = arg;
                } else {
                    if (RubyComparable.cmpint(ctx,
                            block.yield(ctx, runtime.newArray(arg, result[0])), arg, result[0]) < 0) {
                        result[0] = arg;
                    }

                    if (RubyComparable.cmpint(ctx,
                            block.yield(ctx, runtime.newArray(arg, result[1])), arg, result[1]) > 0) {
                        result[1] = arg;
                    }
                }
                return ctx.nil;
            });
        } else {
            callEach(context, eachSite(context), self, Signature.ONE_REQUIRED, (ctx, largs, blk) -> {
                IRubyObject arg = packEnumValues(ctx, largs);
                synchronized (result) {
                    if (result[0] == null) {
                        result[0] = result[1] = arg;
                    } else {
                        if (RubyComparable.cmpint(ctx, invokedynamic(ctx, arg, OP_CMP, result[0]), arg, result[0]) < 0) {
                            result[0] = arg;
                        }

                        if (RubyComparable.cmpint(ctx, invokedynamic(ctx, arg, OP_CMP, result[1]), arg, result[1]) > 0) {
                            result[1] = arg;
                        }
                    }
                }
                return ctx.nil;
            });
        }
        if (result[0] == null) {
            result[0] = result[1] = runtime.getNil();
        }
        return RubyArray.newArrayMayCopy(runtime, result);
    }

    @JRubyMethod
    public static IRubyObject minmax_by(final ThreadContext context, IRubyObject self, final Block block) {

        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "minmax_by", RubyEnumerable::size);

        final IRubyObject result[] = new IRubyObject[] { context.nil, context.nil };

        callEach(context, eachSite(context), self, Signature.OPTIONAL, new BlockCallback() {
            IRubyObject minMemo = null, maxMemo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                checkContext(context, ctx, "minmax_by");
                IRubyObject arg = packEnumValues(ctx, largs);
                IRubyObject v = block.yield(ctx, arg);

                if (minMemo == null) {
                    minMemo = maxMemo = v;
                    result[0] = result[1] = arg;
                } else {
                    if (RubyComparable.cmpint(ctx, invokedynamic(ctx, v, OP_CMP, minMemo), v, minMemo) < 0) {
                        minMemo = v;
                        result[0] = arg;
                    }
                    if (RubyComparable.cmpint(ctx, invokedynamic(ctx, v, OP_CMP, maxMemo), v, maxMemo) > 0) {
                        maxMemo = v;
                        result[1] = arg;
                    }
                }
                return ctx.nil;
            }
        });
        return RubyArray.newArrayMayCopy(context.runtime, result);
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

        if (block.isGiven() && patternGiven) {
            context.runtime.getWarnings().warn("given block not used");
        }

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
        final boolean[] result = new boolean[] { false };
        final boolean patternGiven = pattern != null;

        if (block.isGiven() && patternGiven) {
            context.runtime.getWarnings().warn("given block not used");
        }

        try {
            if (block.isGiven() && !patternGiven) {
                callEach(context, each, self, block.getSignature(), (ctx, largs, blk) -> {
                    checkContext(localContext, ctx, "one?");
                    if (block.yieldValues(ctx, largs).isTrue()) {
                        if (result[0]) {
                            throw JumpException.SPECIAL_JUMP;
                        } else {
                            result[0] = true;
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
                            if (result[0]) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result[0] = true;
                            }
                        }
                        return ctx.nil;
                    });
                } else {
                    callEach(context, each, self, Signature.ONE_REQUIRED, (ctx, largs, blk) -> {
                        checkContext(localContext, ctx, "one?");
                        IRubyObject larg = packEnumValues(ctx, largs);
                        if (larg.isTrue()) {
                            if (result[0]) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result[0] = true;
                            }
                        }
                        return ctx.nil;
                    });
                }
            }
        } catch (JumpException.SpecialJump sj) {
            return context.fals;
        }
        return result[0] ? context.tru : context.fals;
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
                throw context.runtime.newArgumentError(args.length, 0, 1);
        }
    }

    public static IRubyObject all_pCommon(ThreadContext context, IRubyObject self, IRubyObject pattern, final Block block) {
        return all_pCommon(context, eachSite(context), self, pattern, block);
    }

    public static IRubyObject all_pCommon(ThreadContext localContext, CallSite each, IRubyObject self, IRubyObject pattern, final Block block) {
        final boolean patternGiven = pattern != null;

        if (block.isGiven() && patternGiven) {
            localContext.runtime.getWarnings().warn("given block not used");
        }

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
                throw context.runtime.newArgumentError(args.length, 0, 1);
        }
    }

    public static IRubyObject any_pCommon(ThreadContext context, IRubyObject self, IRubyObject pattern, final Block block) {
        return any_pCommon(context, eachSite(context), self, pattern, block);
    }

    public static IRubyObject any_pCommon(ThreadContext localContext, CallSite site, IRubyObject self, IRubyObject pattern, final Block block) {
        final boolean patternGiven = pattern != null;

        if (block.isGiven() && patternGiven) {
            localContext.runtime.getWarnings().warn("given block not used");
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

    @JRubyMethod(name = "zip", rest = true)
    public static IRubyObject zip(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        return zipCommon(context, self, args, block);
    }

    public static IRubyObject zipCommon(ThreadContext context, IRubyObject self, IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        final RubyClass Array = runtime.getArray();

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
        if (hasUncoercible) {
            final RubySymbol each = runtime.newSymbol("each");
            for (int i = 0; i < args.length; i++) {
                newArgs[i] = sites(context).to_enum.call(context, args[i], args[i], each); // args[i].to_enum(:each)
            }

            return zipCommonEnum(context, self, newArgs, block);
        }

        return zipCommonAry(context, self, newArgs, block);
    }

    // TODO: Eliminate duplication here and zipCommonEnum
    // See enum_zip + zip_ary in Ruby source (1.9, anyway)
    public static IRubyObject zipCommonAry(ThreadContext context, IRubyObject self,
            final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        final int len = args.length + 1;

        if (block.isGiven()) {
            callEach(context, eachSite(context), self, new BlockCallback() {
                final AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block unused) {
                    RubyArray array = RubyArray.newBlankArrayInternal(runtime, len);
                    int myIx = ix.getAndIncrement();
                    array.eltInternalSet(0, packEnumValues(ctx, largs));
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.eltInternalSet(i + 1, ((RubyArray) args[i]).entry(myIx));
                    }
                    array.realLength = len;
                    block.yield(ctx, array);
                    return ctx.nil;
                }
            });
            return context.nil;
        } else {
            final RubyArray zip = runtime.newArray();
            callEach(context, eachSite(context), self, Signature.ONE_REQUIRED, new BlockCallback() {
                final AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block unused) {
                    RubyArray array = RubyArray.newBlankArrayInternal(runtime, len);
                    array.eltInternalSet(0, packEnumValues(ctx, largs));
                    int myIx = ix.getAndIncrement();
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.eltInternalSet(i + 1, ((RubyArray) args[i]).entry(myIx));
                    }
                    array.realLength = len;
                    synchronized (zip) { zip.append(array); }
                    return ctx.nil;
                }
            });
            return zip;
        }
    }

    // TODO: Eliminate duplication here and zipCommonAry
    // See enum_zip + zip_i in Ruby source
    public static IRubyObject zipCommonEnum(ThreadContext context, IRubyObject self,
            final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        final int len = args.length + 1;

        if (block.isGiven()) {
            callEach(context, eachSite(context), self, (ctx, largs, unused) -> {
                RubyArray array = RubyArray.newBlankArrayInternal(runtime, len);
                array.eltInternalSet(0, packEnumValues(ctx, largs));
                for (int i = 0, j = args.length; i < j; i++) {
                    array.eltInternalSet(i + 1, zipEnumNext(ctx, args[i]));
                }
                array.realLength = len;
                block.yield(ctx, array);
                return ctx.nil;
            });
            return context.nil;
        } else {
            final RubyArray zip = runtime.newArray();
            callEach(context, eachSite(context), self, Signature.ONE_REQUIRED, (ctx, largs, unused) -> {
                RubyArray array = RubyArray.newBlankArrayInternal(runtime, len);
                array.eltInternalSet(0, packEnumValues(ctx, largs));
                for (int i = 0, j = args.length; i < j; i++) {
                    array.eltInternalSet(i + 1, zipEnumNext(ctx, args[i]));
                }
                array.realLength = len;
                synchronized (zip) { zip.append(array); }
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
        final RubyArray array = context.runtime.newArray();
        synchronized (array) {
            callEach(context, eachSite(context), enumerable, Signature.ONE_ARGUMENT, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    return call(ctx, packEnumValues(ctx, largs), blk);
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg, Block blk) {
                    array.append(larg);
                    return larg;
                }
                @Override
                public IRubyObject call(ThreadContext ctx, IRubyObject larg) {
                    array.append(larg);
                    return larg;
                }
            });
        }

        return array;
    }

    public static IRubyObject zipEnumNext(ThreadContext context, IRubyObject arg) {
        if (arg.isNil()) return context.nil;

        final Ruby runtime = context.runtime;
        IRubyObject oldExc = runtime.getGlobalVariables().get("$!");
        try {
            return sites(context).zip_next.call(context, arg, arg);
        } catch (RaiseException re) {
            if (re.getException().getMetaClass() == runtime.getStopIteration()) {
                runtime.getGlobalVariables().set("$!", oldExc);
                return context.nil;
            }
            throw re;
        }
    }

    @JRubyMethod
    public static IRubyObject group_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;

        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "group_by", RubyEnumerable::size);

        final RubyHash result = new RubyHash(runtime);

        callEach(context, eachSite(context), self, Signature.OPTIONAL, (ctx, largs, blk) -> {
            IRubyObject larg = packEnumValues(ctx, largs);
            IRubyObject key = block.yield(ctx, larg);
            synchronized (result) {
                RubyArray curr = (RubyArray)result.fastARef(key);

                if (curr == null) {
                    curr = runtime.newArray();
                    result.fastASet(key, curr);
                }
                curr.append(larg);
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

        IRubyObject enumerator = runtime.getEnumerator().allocate();
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
            InternalVariables variables = enumerator.getInternalVariables();
            final IRubyObject enumerable = (IRubyObject) variables.getInternalVariable("chunk_enumerable");
            final RubyProc categorize = (RubyProc) variables.getInternalVariable("chunk_categorize");
            final IRubyObject yielder = packEnumValues(context, args);
            final ChunkArg arg = new ChunkArg(context);

            final RubySymbol alone = runtime.newSymbol("_alone");
            final RubySymbol separator = runtime.newSymbol("_separator");
            final EnumerableSites sites = sites(context);
            final CallSite chunk_call = sites.chunk_call;
            final CallSite chunk_op_lshift = sites.chunk_op_lshift;

            // if chunk's categorize block has arity one, we pass it the packed args
            // else we let it spread the args as it sees fit for its arity
            callEach(context, eachSite(context), enumerable, Signature.OPTIONAL, (ctx, largs, blk) -> {
                final IRubyObject larg = packEnumValues(ctx, largs);
                final IRubyObject v;
                if ( categorize.getBlock().getSignature().arityValue() == 1 ) {
                    // if chunk's categorize block has arity one, we pass it the packed args
                    v = chunk_call.call(ctx, categorize, categorize, larg);
                } else {
                    // else we let it spread the args as it sees fit for its arity
                    v = chunk_call.call(ctx, categorize, categorize, largs);
                }

                if ( v == alone ) {
                    if ( ! arg.prev_value.isNil() ) {
                        chunk_op_lshift.call(ctx, yielder, yielder, runtime.newArray(arg.prev_value, arg.prev_elts));
                        arg.prev_value = arg.prev_elts = ctx.nil;
                    }
                    chunk_op_lshift.call(ctx, yielder, yielder, runtime.newArray(v, runtime.newArray(larg)));
                }
                else if ( v.isNil() || v == separator ) {
                    if( ! arg.prev_value.isNil() ) {
                        chunk_op_lshift.call(ctx, yielder, yielder, runtime.newArray(arg.prev_value, arg.prev_elts));
                        arg.prev_value = arg.prev_elts = ctx.nil;
                    }
                }
                else if ( (v instanceof RubySymbol) && v.toString().charAt(0) == '_' ) {
                    throw runtime.newRuntimeError("symbol begins with an underscore is reserved");
                }
                else {
                    if ( arg.prev_value.isNil() ) {
                        arg.prev_value = v;
                        arg.prev_elts = runtime.newArray(larg);
                    }
                    else {
                        if ( arg.prev_value.equals(v) ) {
                            ((RubyArray) arg.prev_elts).append(larg);
                        }
                        else {
                            chunk_op_lshift.call(ctx, yielder, yielder, runtime.newArray(arg.prev_value, arg.prev_elts));
                            arg.prev_value = v;
                            arg.prev_elts = runtime.newArray(larg);
                        }
                    }
                }
                return ctx.nil;
            });

            if ( ! arg.prev_elts.isNil() ) {
                chunk_op_lshift.call(context, yielder, yielder, runtime.newArray(arg.prev_value, arg.prev_elts));
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
            result.append(packEnumValues(context, args));
            return context.nil;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject arg, Block block) {
            result.append(arg);
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
            final Ruby runtime = context.runtime;
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
                    IRubyObject v = RubyArray.newArrayMayCopy(runtime, largs);
                    value = blockGiven ? block.yield(context, v) : v;
                    break;
            }

            callImpl(runtime, value);
            return context.nil;
        }

        private void callImpl(final Ruby runtime, IRubyObject value) {
            IRubyObject ary = TypeConverter.checkArrayType(runtime, value);
            if (ary.isNil()) throw runtime.newTypeError("wrong element type " + value.getType().getName() + " (expected array)");
            final RubyArray array = (RubyArray) ary;
            if (array.size() != 2) {
                throw runtime.newArgumentError("element has wrong array length (expected 2, was " + array.size() + ")");
            }
            result.fastASetCheckString(runtime, array.eltOk(0), array.eltOk(1));
        }

    }

    @Deprecated
    public static IRubyObject all_p19(ThreadContext context, IRubyObject self, final Block block) {
        return all_p(context, self, block);
    }

    @Deprecated
    public static IRubyObject all_pCommon(final ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        return all_pCommon(context, eachSite(context), self, null, block);
    }

    @Deprecated
    public static IRubyObject any_pCommon(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        return any_pCommon(context, eachSite(context), self, null, block);
    }

    @Deprecated
    public static IRubyObject none_p19(ThreadContext context, IRubyObject self, final Block block) {
        return none_p(context, self, null, block);
    }

    @Deprecated
    public static IRubyObject one_p19(ThreadContext context, IRubyObject self, final Block block) {
        return one_p(context, self, null, block);
    }

    protected static CachingCallSite eachSite(ThreadContext context) {
        return sites(context).each;
    }

    private static EnumerableSites sites(ThreadContext context) {
        return context.sites.Enumerable;
    }

    @Deprecated
    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self, BlockCallback callback) {
        return Helpers.invoke(context, self, "each", CallBlock.newCallClosure(context, self, Signature.OPTIONAL, callback));
    }

    @Deprecated
    public static IRubyObject callEach19(Ruby runtime, ThreadContext context, IRubyObject self,
                                         BlockCallback callback) {
        return Helpers.invoke(context, self, "each", CallBlock19.newCallClosure(self, runtime.getEnumerable(),
                Signature.OPTIONAL, callback, context));
    }

    @Deprecated
    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject[] args,
                                       BlockCallback callback) {
        return Helpers.invoke(context, self, "each", args, CallBlock.newCallClosure(self, runtime.getEnumerable(), Signature.OPTIONAL, callback, context));
    }

    @Deprecated
    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject[] args,
                                       Signature signature, BlockCallback callback) {
        return Helpers.invoke(context, self, "each", args,
                CallBlock.newCallClosure(context, self, signature, callback));
    }

    @Deprecated
    public static IRubyObject callEach19(Ruby runtime, ThreadContext context, IRubyObject self,
                                         Signature signature, BlockCallback callback) {
        return eachSite(context).call(context, self, self, CallBlock19.newCallClosure(self, runtime.getEnumerable(), signature, callback, context));
    }

    @Deprecated
    public static IRubyObject count18(ThreadContext context, IRubyObject self, final Block block) {
        return count(context, self, block);
    }

    @Deprecated
    public static IRubyObject count18(ThreadContext context, IRubyObject self, final IRubyObject methodArg, final Block block) {
        return count(context, self, methodArg, block);
    }

    @Deprecated
    public static IRubyObject take_while19(ThreadContext context, IRubyObject self, final Block block) {
        return take_while(context, self, block);
    }

    @Deprecated
    public static IRubyObject to_a19(ThreadContext context, IRubyObject self) {
        return to_a(context, self);
    }

    @Deprecated
    public static IRubyObject to_a19(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return to_a(context, self, args);
    }

    @Deprecated
    public static IRubyObject to_h(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return to_h(context, self, args, Block.NULL_BLOCK);
    }

    @Deprecated
    public static IRubyObject find_index19(ThreadContext context, IRubyObject self, final Block block) {
        return find_index(context, self, block);
    }

    @Deprecated
    public static IRubyObject find_index19(ThreadContext context, IRubyObject self, final IRubyObject cond, final Block block) {
        return find_index(context, self, cond, block);
    }

    @Deprecated
    public static IRubyObject collect19(ThreadContext context, IRubyObject self, final Block block) {
        return collect(context, self, block);
    }

    @Deprecated
    public static IRubyObject map19(ThreadContext context, IRubyObject self, final Block block) {
        return map(context, self, block);
    }

    @Deprecated
    public static IRubyObject collectCommon(ThreadContext context, Ruby runtime, IRubyObject self,
                                            RubyArray result, final Block block, BlockCallback blockCallback) {
        callEach(context, eachSite(context), self, Signature.ONE_ARGUMENT, blockCallback);
        return result;
    }

    @Deprecated
    public static IRubyObject flat_map19(ThreadContext context, IRubyObject self, final Block block) {
        return flat_map(context, self, block);
    }

    @Deprecated
    public static IRubyObject collect_concat19(ThreadContext context, IRubyObject self, final Block block) {
        return collect_concat(context, self, block);
    }

    @Deprecated
    public static IRubyObject each_with_indexCommon(ThreadContext context, IRubyObject self, Block block) {
        callEach(context, eachSite(context), self, Signature.OPTIONAL, new EachWithIndex(block));
        return self;
    }

    @Deprecated
    public static IRubyObject each_with_indexCommon19(ThreadContext context, IRubyObject self, Block block, IRubyObject[] args) {
        return each_with_indexCommon(context, self, block, args);
    }

    @Deprecated
    public static IRubyObject each_with_objectCommon19(ThreadContext context, IRubyObject self, final Block block, final IRubyObject arg) {
        return each_with_objectCommon(context, self, block, arg);
    }

    @Deprecated
    public static IRubyObject each_with_index19(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return each_with_index(context, self, args, block);
    }

    @Deprecated
    public static IRubyObject each_slice19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return each_slice(context, self, arg, block);
    }

    @Deprecated
    public static IRubyObject each_cons19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return each_cons(context, self, arg, block);
    }

    @Deprecated
    public static IRubyObject zip19(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        return zip(context, self, args, block);
    }

    @Deprecated
    public static IRubyObject[] zipCommonConvert(Ruby runtime, IRubyObject[] args) {
        return zipCommonConvert(runtime, args, "to_a");
    }

    @Deprecated
    public static IRubyObject[] zipCommonConvert(Ruby runtime, IRubyObject[] args, String method) {
        final RubyClass Array = runtime.getArray();
        ThreadContext context = runtime.getCurrentContext();

        // 1.9 tries to convert, and failing that tries to "each" elements into a new array
        for (int i = 0; i < args.length; i++) {
            IRubyObject result = TypeConverter.convertToTypeWithCheck(args[i], Array, method);
            if (result.isNil()) {
                result = takeItems(context, args[i]);
            }
            args[i] = result;
        }

        return args;
    }

    @Deprecated
    public static IRubyObject zipCommon19(ThreadContext context, IRubyObject self, IRubyObject[] args, final Block block) {
        return zipCommon(context, self, args, block);
    }

    @Deprecated
    public static IRubyObject chunk(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        switch (Arity.checkArgumentCount(context.runtime, args, 0, 1)) {
            case 0:
                return chunk(context, self, block);
            default:
                // should never be reached
                throw context.runtime.newArgumentError(args.length, 0);
        }
    }
}
