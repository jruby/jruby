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
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaInternalBlockBody;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyObject.equalInternal;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;
import static org.jruby.RubyEnumerator.SizeFn;

/**
 * The implementation of Ruby's Enumerable module.
 */

@JRubyModule(name="Enumerable")
public class RubyEnumerable {

    public static RubyModule createEnumerableModule(Ruby runtime) {
        RubyModule enumModule = runtime.defineModule("Enumerable");
        runtime.setEnumerable(enumModule);
        
        enumModule.defineAnnotatedMethods(RubyEnumerable.class);

        return enumModule;
    }

    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self,
            BlockCallback callback) {
        return Helpers.invoke(context, self, "each", CallBlock.newCallClosure(self, runtime.getEnumerable(),
                Signature.OPTIONAL, callback, context));
    }

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

    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self,
                                       Signature signature, BlockCallback callback) {
        return Helpers.invoke(context, self, "each", CallBlock.newCallClosure(self, runtime.getEnumerable(),
                signature, callback, context));
    }

    @Deprecated
    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self,
            Arity arity, BlockCallback callback) {
        return Helpers.invoke(context, self, "each", CallBlock.newCallClosure(self, runtime.getEnumerable(),
                arity, callback, context));
    }

    public static IRubyObject callEach19(Ruby runtime, ThreadContext context, IRubyObject self,
            Arity arity, BlockCallback callback) {
        return Helpers.invoke(context, self, "each", CallBlock19.newCallClosure(self, runtime.getEnumerable(),
                arity, callback, context));
    }
    
    public static IRubyObject each(ThreadContext context, IRubyObject self, BlockBody body) {
        Block block = new Block(body, context.currentBinding(self, Visibility.PUBLIC));
        return Helpers.invoke(context, self, "each", block);
    }

    @Deprecated
    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject[] args,
            Arity arity, BlockCallback callback) {
        return Helpers.invoke(context, self, "each", args, CallBlock.newCallClosure(self, runtime.getEnumerable(), arity, callback, context));
    }

    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject[] args,
                                       Signature signature, BlockCallback callback) {
        return Helpers.invoke(context, self, "each", args, CallBlock.newCallClosure(self, runtime.getEnumerable(), signature, callback, context));
    }

    private static void checkContext(ThreadContext firstContext, ThreadContext secondContext, String name) {
        if (firstContext != secondContext) {
            throw secondContext.runtime.newThreadError("Enumerable#" + name + " cannot be parallelized");
        }
    }

    public static IRubyObject count18(ThreadContext context, IRubyObject self, final Block block) {
        return count(context, self, block);
    }

    @JRubyMethod(name = "count")
    public static IRubyObject count(ThreadContext context, IRubyObject self, final Block block) {
        return countCommon(context, self, block, block.arity());
    }

    private static IRubyObject countCommon(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        final Ruby runtime = context.runtime;
        final int result[] = new int[] { 0 };
        
        if (block.isGiven()) {
            each(context, self, new JavaInternalBlockBody(runtime, context, "Enumerable#count", block.getSignature()) {
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    IRubyObject packedArg = packEnumValues(context.runtime, args);
                    if (block.yield(context, packedArg).isTrue()) result[0]++;
                    return runtime.getNil();
                }
            });
        } else {
            each(context, self, new JavaInternalBlockBody(runtime, context, "Enumerable#count", Signature.NO_ARGUMENTS) {
                public IRubyObject yield(ThreadContext context, IRubyObject[] unusedValue) {
                    result[0]++;
                    return runtime.getNil();
                }
            });
        }
        return RubyFixnum.newFixnum(runtime, result[0]);
    }

    public static IRubyObject count18(ThreadContext context, IRubyObject self, final IRubyObject methodArg, final Block block) {
        return count(context, self, methodArg, block);
    }

    @JRubyMethod(name = "count")
    public static IRubyObject count(ThreadContext context, IRubyObject self, final IRubyObject methodArg, final Block block) {
        final Ruby runtime = context.runtime;
        final int result[] = new int[] { 0 };
        
        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");
        
        each(context, self, new JavaInternalBlockBody(runtime, context, "Enumerable#count", Signature.ONE_REQUIRED) {
            public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                IRubyObject packedArg = packEnumValues(context.runtime, args);
                if (packedArg.equals(methodArg)) result[0]++;
                
                return runtime.getNil();
            }
        });

        return RubyFixnum.newFixnum(runtime, result[0]);
    }
    
    @JRubyMethod
    public static IRubyObject cycle(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "cycle", cycleSizeFn(context, self));
        }
        
        return cycleCommon(context, self, -1, block);
    }

    @JRubyMethod
    public static IRubyObject cycle(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        if (arg.isNil()) return cycle(context, self, block);
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "cycle", new IRubyObject[] { arg }, cycleSizeFn(context, self));
        }

        long times = RubyNumeric.num2long(arg);
        if (times <= 0) {
            return context.runtime.getNil();
        }

        return cycleCommon(context, self, times, block);
    }

    /*
     * @param nv number of times to cycle or -1 to cycle indefinitely
     */
    private static IRubyObject cycleCommon(ThreadContext context, IRubyObject self, long nv, final Block block) {
        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

        each(context, self, new JavaInternalBlockBody(runtime, Signature.OPTIONAL) {
            public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                IRubyObject packedArg = packEnumValues(context.runtime, args);
                synchronized (result) { result.append(packedArg); }
                block.yield(context, packedArg);
                return runtime.getNil();            
            }
        });

        int length = result.size();
        if (length == 0) return runtime.getNil();

        while (nv < 0 || 0 < --nv) {
            for (int i=0; i < length; i++) {
                block.yield(context, result.eltInternal(i));
            }
        }

        return runtime.getNil();
    }

    private static SizeFn cycleSizeFn(final ThreadContext context, final IRubyObject self) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                Ruby runtime = context.runtime;
                IRubyObject n = runtime.getNil();
                IRubyObject size = enumSizeFn(context, self).size(args);

                if (size == null || size.isNil()) {
                    return runtime.getNil();
                }

                if (args != null && args.length > 0) {
                    n = args[0];
                }

                if (n == null || n.isNil()) {
                    return RubyFloat.newFloat(runtime, RubyFloat.INFINITY);
                }

                long multiple = RubyNumeric.num2long(n);
                if (multiple <= 0) {
                    return RubyFixnum.zero(runtime);
                }

                return size.callMethod(context, "*", RubyFixnum.newFixnum(runtime, multiple));
            }
        };
    }

    @JRubyMethod(name = "take")
    public static IRubyObject take(ThreadContext context, IRubyObject self, IRubyObject n, Block block) {
        final Ruby runtime = context.runtime;
        final long len = RubyNumeric.num2long(n);
        
        if (len < 0) throw runtime.newArgumentError("attempt to take negative size");
        if (len == 0) return runtime.newEmptyArray();

        final RubyArray result = runtime.newArray();

        try {
            each(context, self, new JavaInternalBlockBody(runtime, Signature.ONE_REQUIRED) {
                long i = len; // Atomic ?
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    synchronized (result) {
                        IRubyObject packedArg = packEnumValues(context.runtime, args);
                        result.append(packedArg);
                        if (--i == 0) throw JumpException.SPECIAL_JUMP; 
                    }
                    
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump e) {}
        
        return result;
    }

    @JRubyMethod(name = "take_while")
    public static IRubyObject take_while19(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) {
            return enumeratorize(context.runtime, self, "take_while");
        }

        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

        try {
            callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext context, IRubyObject[] args, Block blk) {
                    // note the we do not want to call the block with packed args, since to match MRI behavior,
                    // the block's test is against the raw args (using block.arity() rather than the Arity.OPTIONAL
                    // we pass to callEach)
                    if (!block.call(context, args).isTrue()) {
                        throw JumpException.SPECIAL_JUMP;
                    }
                    IRubyObject packedArg = packEnumValues(context.runtime, args);
                    synchronized (result) { result.append(packedArg); }
                    return runtime.getNil();
                }
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
            each(context, self, new JavaInternalBlockBody(runtime, Signature.NO_ARGUMENTS) {
                long i = len; // Atomic ?
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    IRubyObject packedArg = packEnumValues(context.runtime, args);
                    synchronized (result) {
                        if (i == 0) {
                            // While iterating over an RubyEnumerator, "arg"
                            // gets overwritten by the new value, leading to JRUBY-6892.
                            // So call .dup() whenever appropriate.
                            result.append(packedArg.isImmediate() ? packedArg : packedArg.dup());
                        } else {
                            --i;
                        }
                    }
                    return runtime.getNil();
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
            each(context, self, new JavaInternalBlockBody(runtime, context, "Enumerable#drop_while", Signature.OPTIONAL) {
                boolean memo = false;
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    IRubyObject packedArg = packEnumValues(context.runtime, args);
                    if (!memo && !block.yield(context, packedArg).isTrue()) memo = true;
                    if (memo) synchronized (result) { result.append(packedArg); }
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {}
        
        return result;
    }    

    @JRubyMethod(name = "first")
    public static IRubyObject first(ThreadContext context, IRubyObject self) {
        final IRubyObject[] holder = new IRubyObject[]{ context.runtime.getNil() };

        try {
            each(context, self, new JavaInternalBlockBody(context.runtime, context, null, Signature.ONE_REQUIRED) {
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    IRubyObject packedArg = packEnumValues(context.runtime, args);
                    holder[0] = packedArg;
                    throw JumpException.SPECIAL_JUMP;
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return holder[0];
    }

    @JRubyMethod(name = "first")
    public static IRubyObject first(ThreadContext context, IRubyObject self, final IRubyObject num) {
        int firstCount = RubyNumeric.fix2int(num);
        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

        if (firstCount < 0) throw runtime.newArgumentError("negative index");
        if (firstCount == 0) return result;

        try {
            each(context, self, new JavaInternalBlockBody(runtime, context, null, Signature.ONE_REQUIRED) {
                private int iter = RubyNumeric.fix2int(num);                
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    IRubyObject packedArg = packEnumValues(context.runtime, args);
                    result.append(packedArg);
                    if (iter-- == 1) throw JumpException.SPECIAL_JUMP;
                    return runtime.getNil();                
                }
            });
        } catch (JumpException.SpecialJump sj) {}

        return result;
    }

    public static IRubyObject to_a(ThreadContext context, IRubyObject self) {
        return to_a19(context, self);
    }

    public static IRubyObject to_a(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return to_a19(context, self, args);
    }

    @JRubyMethod(name = {"to_a", "entries"})
    public static IRubyObject to_a19(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        RubyArray result = runtime.newArray();
        callEach(runtime, context, self, Signature.OPTIONAL, new AppendBlockCallback(runtime, result));
        result.infectBy(self);
        return result;
    }

    @JRubyMethod(name = {"to_a", "entries"}, rest = true)
    public static IRubyObject to_a19(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyArray result = runtime.newArray();
        Helpers.invoke(context, self, "each", args, CallBlock.newCallClosure(self, runtime.getEnumerable(),
                Signature.OPTIONAL, new AppendBlockCallback(runtime, result), context));
        result.infectBy(self);
        return result;
    }

    @JRubyMethod(name = "to_h", rest = true)
    public static IRubyObject to_h(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyHash result = RubyHash.newHash(runtime);
        Helpers.invoke(context, self, "each", args, CallBlock.newCallClosure(self, runtime.getEnumerable(),
                Signature.OPTIONAL, new PutKeyValueCallback(runtime, result), context));
        result.infectBy(self);
        return result;
    }

    @JRubyMethod
    public static IRubyObject sort(ThreadContext context, IRubyObject self, final Block block) {
        Ruby runtime = context.runtime;
        RubyArray result = runtime.newArray();

        callEach(runtime, context, self, Signature.OPTIONAL, new AppendBlockCallback(runtime, result));
        result.sort_bang(context, block);
        
        return result;
    }

    @JRubyMethod
    public static IRubyObject sort_by(final ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context; // MUST NOT be used across threads
        IRubyObject[][] valuesAndCriteria;

        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "sort_by", enumSizeFn(context, self));
        }

        if (self instanceof RubyArray) {
            RubyArray selfArray = (RubyArray) self;
            final IRubyObject[][] valuesAndCriteriaArray = new IRubyObject[selfArray.size()][2];

            each(context, self, new JavaInternalBlockBody(runtime, Signature.OPTIONAL) {
                AtomicInteger i = new AtomicInteger(0);
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    IRubyObject packedArg = packEnumValues(context.runtime, args);
                    IRubyObject[] myVandC = valuesAndCriteriaArray[i.getAndIncrement()];
                    myVandC[0] = packedArg;
                    myVandC[1] = block.yield(context, packedArg);
                    return runtime.getNil();
                }
            });

            valuesAndCriteria = valuesAndCriteriaArray;
        } else {
            final List<IRubyObject[]> valuesAndCriteriaList = new ArrayList<IRubyObject[]>();

            callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    IRubyObject[] myVandC = new IRubyObject[2];
                    myVandC[0] = larg;
                    myVandC[1] = block.yield(ctx, larg);
                    valuesAndCriteriaList.add(myVandC);
                    return runtime.getNil();
                }
            });

            valuesAndCriteria = valuesAndCriteriaList.toArray(new IRubyObject[valuesAndCriteriaList.size()][]);
        }

        Arrays.sort(valuesAndCriteria, new Comparator<IRubyObject[]>() {
            public int compare(IRubyObject[] o1, IRubyObject[] o2) {
                return RubyComparable.cmpint(context, invokedynamic(localContext, o1[1], OP_CMP, o2[1]), o1[1], o2[1]);
            }
        });


        IRubyObject dstArray[] = new IRubyObject[valuesAndCriteria.length];
        for (int i = 0; i < dstArray.length; i++) {
            dstArray[i] = valuesAndCriteria[i][0];
        }

        return runtime.newArrayNoCopy(dstArray);
    }

    @JRubyMethod
    public static IRubyObject grep(ThreadContext context, IRubyObject self, final IRubyObject pattern, final Block block) {
        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

        if (block.isGiven()) {
            callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    if (pattern.callMethod(ctx, "===", larg).isTrue()) {
                        IRubyObject value = block.yield(ctx, larg);
                        synchronized (result) {
                            result.append(value);
                        }
                    }
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    if (pattern.callMethod(ctx, "===", larg).isTrue()) {
                        synchronized (result) {
                            result.append(larg);
                        }
                    }
                    return runtime.getNil();
                }
            });
        }
        
        return result;
    }

    public static IRubyObject detectCommon(ThreadContext context, IRubyObject self, final Block block) {
        return detectCommon(context, self, null, block);
    }

    public static IRubyObject detectCommon(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { null };
        final ThreadContext localContext = context;

        try {
            callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    checkContext(localContext, ctx, "detect/find");
                    if (block.yield(ctx, larg).isTrue()) {
                        result[0] = larg;
                        throw JumpException.SPECIAL_JUMP;
                    }
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return result[0];
        }

        return ifnone != null ? ifnone.callMethod(context, "call") : runtime.getNil();
    }

    @JRubyMethod
    public static IRubyObject detect(ThreadContext context, IRubyObject self, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, null, block);

        return block.isGiven() ? detectCommon(context, self, block) : enumeratorize(context.runtime, self, "detect");
    }

    @JRubyMethod
    public static IRubyObject detect(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, ifnone, block);

        return block.isGiven() ? detectCommon(context, self, ifnone, block) : enumeratorize(context.runtime, self, "detect", ifnone);
    }

    // FIXME: Custom Array enumeratorize should be made for all of these methods which skip Array without a supplied block.
    @JRubyMethod
    public static IRubyObject find(ThreadContext context, IRubyObject self, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, null, block);

        return blockGiven ? detectCommon(context, self, block) : enumeratorize(context.runtime, self, "find");
    }

    @JRubyMethod
    public static IRubyObject find(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find(context, ifnone, block);

        return blockGiven ? detectCommon(context, self, ifnone, block) :
            enumeratorize(context.runtime, self, "find", ifnone);
    }

    @JRubyMethod(name = "find_index")
    public static IRubyObject find_index19(ThreadContext context, IRubyObject self, final Block block) {
        return find_index(context, self, block);
    }

    public static IRubyObject find_index(ThreadContext context, IRubyObject self, final Block block) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find_index(context, block);

        return blockGiven ? find_indexCommon(context, self, block, block.getSignature()) :
                enumeratorize(context.runtime, self, "find_index");
    }

    @Deprecated
    public static IRubyObject find_index(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        boolean blockGiven = block.isGiven();

        if (self instanceof RubyArray && blockGiven) return ((RubyArray) self).find_index(context, block);

        return blockGiven ? find_indexCommon(context, self, block, callbackArity) :
            enumeratorize(context.runtime, self, "find_index");
    }

    @JRubyMethod(name = "find_index")
    public static IRubyObject find_index19(ThreadContext context, IRubyObject self, final IRubyObject cond, final Block block) {
        return find_index(context, self, cond, block);
    }

    public static IRubyObject find_index(ThreadContext context, IRubyObject self, final IRubyObject cond, final Block block) {
        final Ruby runtime = context.runtime;

        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");
        if (self instanceof RubyArray) return ((RubyArray) self).find_index(context, cond);

        return find_indexCommon(context, self, cond);
    }

    public static IRubyObject find_indexCommon(ThreadContext context, IRubyObject self, final Block block, Signature callbackArity) {
        final Ruby runtime = context.runtime;
        final long result[] = new long[] {0};

        try {
            callEach(runtime, context, self, callbackArity, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    if (block.yield(ctx, larg).isTrue()) throw JumpException.SPECIAL_JUMP;
                    result[0]++;
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return RubyFixnum.newFixnum(runtime, result[0]);
        }

        return runtime.getNil();
    }

    @Deprecated
    public static IRubyObject find_indexCommon(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        final Ruby runtime = context.runtime;
        final long result[] = new long[] {0};

        try {
            callEach(runtime, context, self, callbackArity, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    if (block.yield(ctx, larg).isTrue()) throw JumpException.SPECIAL_JUMP;
                    result[0]++;
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return RubyFixnum.newFixnum(runtime, result[0]);
        }

        return runtime.getNil();
    }


    public static IRubyObject find_indexCommon(ThreadContext context, IRubyObject self, final IRubyObject cond) {
        final Ruby runtime = context.runtime;
        final long result[] = new long[] {0};

        try {
            callEach(runtime, context, self, Signature.ONE_ARGUMENT, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    if (equalInternal(ctx, larg, cond)) throw JumpException.SPECIAL_JUMP;
                    result[0]++;
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return RubyFixnum.newFixnum(runtime, result[0]);
        }

        return runtime.getNil();
    }

    public static IRubyObject selectCommon(ThreadContext context, IRubyObject self, final Block block, String methodName) {
        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, methodName, enumSizeFn(context, self));
        }

        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                if (block.yield(ctx, larg).isTrue()) {
                    synchronized (result) {
                        result.append(larg);
                    }
                }
                return runtime.getNil();
            }
        });

        return result;
    }

    @JRubyMethod
    public static IRubyObject select(ThreadContext context, IRubyObject self, final Block block) {
        return selectCommon(context, self, block, "select");
    }

    @JRubyMethod
    public static IRubyObject find_all(ThreadContext context, IRubyObject self, final Block block) {
        return selectCommon(context, self, block, "find_all");
    }

    @JRubyMethod
    public static IRubyObject reject(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final RubyArray result = runtime.newArray();

        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "reject", enumSizeFn(context, self));
        }

        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                if (!block.yield(ctx, larg).isTrue()) {
                    synchronized (result) {
                        result.append(larg);
                    }
                }
                return runtime.getNil();
            }
        });

        return result;
    }

    @JRubyMethod(name = {"collect"})
    public static IRubyObject collect19(ThreadContext context, IRubyObject self, final Block block) {
        return collectCommon19(context, self, block, "collect");
    }

    @JRubyMethod(name = {"map"})
    public static IRubyObject map19(ThreadContext context, IRubyObject self, final Block block) {
        return collectCommon19(context, self, block, "map");
    }

    private static IRubyObject collectCommon19(ThreadContext context, IRubyObject self, final Block block, String methodName) {
        final Ruby runtime = context.runtime;
        if (block.isGiven()) {
            final RubyArray result = runtime.newArray();

            callEach19(runtime, context, self, block.arity(), new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg;
                    boolean newAry = false;
                    if (largs.length == 0) {
                        larg = ctx.nil;
                    } else if (largs.length == 1) {
                        larg = largs[0];
                    } else {
                        newAry = true;
                        larg = RubyArray.newArrayNoCopy(ctx.runtime, largs);
                    }
                    
                    IRubyObject value = newAry ? block.yieldArray(ctx, larg, null) : block.yield(ctx, larg);
                    synchronized (result) {
                        result.append(value);
                    }
                    return runtime.getNil();
                }
            });
            return result;
        } else {
            return enumeratorizeWithSize(context, self, methodName, enumSizeFn(context, self));
        }
    }

    public static IRubyObject collectCommon(ThreadContext context, Ruby runtime, IRubyObject self,
            RubyArray result, final Block block, BlockCallback blockCallback) {
        callEach(runtime, context, self, Signature.ONE_ARGUMENT, blockCallback);
        return result;
    }

    @JRubyMethod(name = {"flat_map"})
    public static IRubyObject flat_map19(ThreadContext context, IRubyObject self, final Block block) {
        return flatMapCommon19(context, self, block, "flat_map");
    }

    @JRubyMethod(name = {"collect_concat"})
    public static IRubyObject collect_concat19(ThreadContext context, IRubyObject self, final Block block) {
        return flatMapCommon19(context, self, block, "collect_concat");
    }

    private static IRubyObject flatMapCommon19(ThreadContext context, IRubyObject self, final Block block, String methodName) {
        final Ruby runtime = context.runtime;
        if(block.isGiven()) {
            final RubyArray ary = runtime.newArray();

            callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    IRubyObject i = block.yield(ctx, larg);
                    IRubyObject tmp = i.checkArrayType();
                    synchronized(ary) {
                        if(tmp.isNil()) {
                            ary.append(i);
                        } else {
                            ary.concat(tmp);
                        }
                    }
                    return runtime.getNil();
                }
            });
            return ary;
        } else {
            return enumeratorizeWithSize(context, self, methodName, enumSizeFn(context, self));
        }
    }

    public static IRubyObject injectCommon(ThreadContext context, IRubyObject self, IRubyObject init, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { init };
        final ThreadContext localContext = context;

        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                checkContext(localContext, ctx, "inject");
                result[0] = result[0] == null ? 
                        larg : block.yieldArray(ctx, runtime.newArray(result[0], larg), null);

                return runtime.getNil();
            }
        });

        return result[0] == null ? runtime.getNil() : result[0];
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

        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                result[0] = result[0] == null ? larg : result[0].callMethod(ctx, methodId, larg);
                return runtime.getNil();
            }
        });
        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod
    public static IRubyObject partition(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final RubyArray arr_true = runtime.newArray();
        final RubyArray arr_false = runtime.newArray();

        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, self, "partition", enumSizeFn(context, self));
        }

        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                if (block.yield(ctx, larg).isTrue()) {
                    synchronized (arr_true) {
                        arr_true.append(larg);
                    }
                } else {
                    synchronized (arr_false) {
                        arr_false.append(larg);
                    }
                }

                return runtime.getNil();
            }
        });

        return runtime.newArray(arr_true, arr_false);
    }

    static class EachWithIndex implements BlockCallback {
        private int index;
        private final Block block;
        private final Ruby runtime;

        public EachWithIndex(ThreadContext ctx, Block block) {
            this(ctx, block, 0);
        }

        public EachWithIndex(ThreadContext ctx, Block block, int index) {
            this.block = block;
            this.runtime = ctx.runtime;
            this.index = index;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] iargs, Block block) {
            return this.block.call(context, packEnumValues(runtime, iargs), runtime.newFixnum(index++));
        }
    }

    /**
     * Package the arguments appropriately depending on how many there are
     * Corresponds to rb_enum_values_pack in MRI
     */
    static IRubyObject packEnumValues(Ruby runtime, IRubyObject[] args) {
        if (args.length < 2) {
            return args.length == 0 ? runtime.getNil() : args[0];
        } else {
            // For more than 1 arg, we pack them as an array
            return runtime.newArrayNoCopy(args);
        }
    }

    public static IRubyObject each_with_indexCommon(ThreadContext context, IRubyObject self, Block block) {
        callEach(context.runtime, context, self, Signature.OPTIONAL, new EachWithIndex(context, block));
        return self;
    }

    public static IRubyObject each_with_indexCommon19(ThreadContext context, IRubyObject self, Block block, IRubyObject[] args) {
        callEach(context.runtime, context, self, args, Signature.OPTIONAL, new EachWithIndex(context, block));
        return self;
    }

    public static IRubyObject each_with_objectCommon19(ThreadContext context, IRubyObject self, final Block block, final IRubyObject arg) {
        final Ruby runtime = context.runtime;
        RubyEnumerable.callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return block.call(ctx, new IRubyObject[]{runtime.newArray(packEnumValues(runtime, largs), arg)});
            }
        });
        return arg;
    }

    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, Block block) {
        return each_with_index19(context, self, IRubyObject.NULL_ARRAY, block);
    }

    @JRubyMethod(name = "each_with_index", rest = true)
    public static IRubyObject each_with_index19(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return block.isGiven() ? each_with_indexCommon19(context, self, block, args) : enumeratorizeWithSize(context, self, "each_with_index", args, enumSizeFn(context, self));
    }

    @JRubyMethod
    public static IRubyObject each_with_object(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        return block.isGiven() ? each_with_objectCommon19(context, self, block, arg) : enumeratorizeWithSize(context, self, "each_with_object", new IRubyObject[] { arg }, enumSizeFn(context, self));
    }

    @JRubyMethod(rest = true)
    public static IRubyObject each_entry(ThreadContext context, final IRubyObject self, final IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_entryCommon(context, self, args, block) : enumeratorizeWithSize(context, self, "each_entry", args, enumSizeFn(context, self));
    }

    public static IRubyObject each_entryCommon(ThreadContext context, final IRubyObject self, final IRubyObject[] args, final Block block) {
        callEach(context.runtime, context, self, args, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                return block.yieldSpecific(ctx, packEnumValues(ctx.runtime, largs));
            }
        });
        return self;
    }
    
    public static IRubyObject each_slice(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        final int size = RubyNumeric.num2int(arg);
        final Ruby runtime = context.runtime;
        if (size <= 0) throw runtime.newArgumentError("invalid slice size");

        final RubyArray result[] = new RubyArray[]{runtime.newArray(size)};

        RubyEnumerable.callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                result[0].append(packEnumValues(runtime, largs));
                if (result[0].size() == size) {
                    block.yield(ctx, result[0]);
                    result[0] = runtime.newArray(size);
                }
                return runtime.getNil();
            }
        });

        if (result[0].size() > 0) block.yield(context, result[0]);
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "each_slice")
    public static IRubyObject each_slice19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return block.isGiven() ? each_slice(context, self, arg, block) : enumeratorizeWithSize(context, self, "each_slice", new IRubyObject[]{arg}, eachSliceSizeFn(context, self));
    }

    private static SizeFn eachSliceSizeFn(final ThreadContext context, final IRubyObject self) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                Ruby runtime = context.runtime;
                assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #each_slice ensures arg[0] is numeric
                long sliceSize = ((RubyNumeric) args[0]).getLongValue();
                if (sliceSize <= 0) {
                    throw runtime.newArgumentError("invalid slice size");
                }

                IRubyObject size = enumSizeFn(context, self).size(args);
                if (size == null || size.isNil()) {
                    return runtime.getNil();
                }

                IRubyObject n = size.callMethod(context, "+", RubyFixnum.newFixnum(runtime, sliceSize - 1));
                return n.callMethod(context, "/", RubyFixnum.newFixnum(runtime, sliceSize));
            }
        };
    }

    public static IRubyObject each_cons(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        final int size = (int)RubyNumeric.num2long(arg);
        final Ruby runtime = context.runtime;
        if (size <= 0) throw runtime.newArgumentError("invalid size");

        final RubyArray result = runtime.newArray(size);

        RubyEnumerable.callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (result.size() == size) result.shift(ctx);
                result.append(packEnumValues(runtime, largs));
                if (result.size() == size) block.yield(ctx, result.aryDup());
                return runtime.getNil();
            }
        });

        return runtime.getNil();        
    }

    @JRubyMethod(name = "each_cons")
    public static IRubyObject each_cons19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return block.isGiven() ? each_cons(context, self, arg, block) : enumeratorizeWithSize(context, self, "each_cons", new IRubyObject[] { arg }, eachConsSizeFn(context, self));
    }

    private static SizeFn eachConsSizeFn(final ThreadContext context, final IRubyObject self) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                Ruby runtime = context.runtime;
                assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #each_cons ensures arg[0] is numeric
                long consSize = ((RubyNumeric) args[0]).getLongValue();
                if (consSize <= 0) {
                    throw runtime.newArgumentError("invalid size");
                }

                IRubyObject size = enumSizeFn(context, self).size(args);
                if (size == null || size.isNil()) {
                    return runtime.getNil();
                }

                IRubyObject n = size.callMethod(context, "+", RubyFixnum.newFixnum(runtime, 1 - consSize));
                RubyFixnum zero = RubyFixnum.zero(runtime);
                return RubyComparable.cmpint(context, n.callMethod(context, "<=>", zero), n, zero) == -1 ? zero : n;
            }
        };
    }

    @JRubyMethod
    public static IRubyObject reverse_each(ThreadContext context, IRubyObject self, Block block) {
        return block.isGiven() ? reverse_eachInternal(context, self, to_a(context, self), block) :
            enumeratorizeWithSize(context, self, "reverse_each", enumSizeFn(context, self));
    }

    @JRubyMethod(rest = true)
    public static IRubyObject reverse_each(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return block.isGiven() ? reverse_eachInternal(context, self, to_a(context, self, args), block) :
            enumeratorizeWithSize(context, self, "reverse_each", args, enumSizeFn(context, self));
    }

    private static IRubyObject reverse_eachInternal(ThreadContext context, IRubyObject self, IRubyObject obj, Block block) { 
        ((RubyArray)obj).reverse_each(context, block);
        return self;
    }

    @JRubyMethod(name = {"include?", "member?"}, required = 1)
    public static IRubyObject include_p(ThreadContext context, IRubyObject self, final IRubyObject arg) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context;

        try {
            callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    checkContext(localContext, ctx, "include?/member?");
                    if (RubyObject.equalInternal(ctx, larg, arg)) {
                        throw JumpException.SPECIAL_JUMP;
                    }
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return runtime.getTrue();
        }

        return runtime.getFalse();
    }

    @JRubyMethod
    public static IRubyObject max(ThreadContext context, IRubyObject self, final Block block) {
        return singleExtent(context, self, "max", SORT_MAX, block);
    }

    @JRubyMethod
    public static IRubyObject max(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        // TODO: Replace with an implementation (quickselect, etc) which requires O(k) memory rather than O(n) memory
        RubyArray sorted = (RubyArray)sort(context, self, block);
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
        return sorted.first(arg);
    }

    @JRubyMethod
    public static IRubyObject max_by(ThreadContext context, IRubyObject self, final Block block) {
        return singleExtentBy(context, self, "max", SORT_MAX, block);
    }

    @JRubyMethod
    public static IRubyObject max_by(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
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
        // TODO: Replace with an implementation (quickselect, etc) which requires O(k) memory rather than O(n) memory
        RubyArray sorted = (RubyArray)sort_by(context, self, block);
        return sorted.first(arg);
    }

    private static final int SORT_MAX =  1;
    private static final int SORT_MIN = -1;
    private static IRubyObject singleExtent(ThreadContext context, IRubyObject self, final String op, final int sortDirection, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { null };
        final ThreadContext localContext = context;

        callEach(runtime, context, self, block.isGiven() ? block.getSignature() : Signature.ONE_REQUIRED, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                checkContext(localContext, ctx, op + "{}");

                if (result[0] == null ||
                        (block.isGiven() &&
                                RubyComparable.cmpint(ctx, block.yieldArray(ctx, runtime.newArray(larg, result[0]), null), larg, result[0]) * sortDirection > 0) ||
                        (!block.isGiven() &&
                                RubyComparable.cmpint(ctx, invokedynamic(ctx, larg, OP_CMP, result[0]), larg, result[0]) * sortDirection > 0)) {
                    result[0] = larg;
                }
                return runtime.getNil();
            }
        });

        return result[0] == null ? runtime.getNil() : result[0];
    }

    private static IRubyObject singleExtentBy(ThreadContext context, IRubyObject self, final String op, final int sortDirection, final Block block) {
        final Ruby runtime = context.runtime;

        if (!block.isGiven()) return enumeratorizeWithSize(context, self, op, enumSizeFn(context, self));

        final IRubyObject result[] = new IRubyObject[] { runtime.getNil() };
        final ThreadContext localContext = context;
        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            IRubyObject memo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                checkContext(localContext, ctx, op);
                IRubyObject v = block.yield(ctx, larg);

                if (memo == null || RubyComparable.cmpint(ctx, invokedynamic(ctx, v, OP_CMP, memo), v, memo) * sortDirection > 0) {
                    memo = v;
                    result[0] = larg;
                }
                return runtime.getNil();
            }
        });
        return result[0];
    }

    @JRubyMethod
    public static IRubyObject minmax(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject result[] = new IRubyObject[] { null, null };
        final ThreadContext localContext = context;

        if (block.isGiven()) {
            callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    checkContext(localContext, ctx, "minmax");
                    IRubyObject arg = packEnumValues(runtime, largs);

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
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject arg = packEnumValues(runtime, largs);
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
                    return runtime.getNil();
                }
            });
        }
        if (result[0] == null) {
            result[0] = result[1] = runtime.getNil();
        }
        return runtime.newArrayNoCopy(result);
    }

    @JRubyMethod
    public static IRubyObject minmax_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;

        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "minmax_by", enumSizeFn(context, self));

        final IRubyObject result[] = new IRubyObject[] { runtime.getNil(), runtime.getNil() };
        final ThreadContext localContext = context;

        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            IRubyObject minMemo = null, maxMemo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                checkContext(localContext, ctx, "minmax_by");
                IRubyObject arg = packEnumValues(runtime, largs);
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
                return runtime.getNil();
            }
        });
        return runtime.newArrayNoCopy(result);
    }

    public static IRubyObject none_p(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context;

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "none?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (block.yield(ctx, larg).isTrue()) throw JumpException.SPECIAL_JUMP;
                        return runtime.getNil();

                    }
                });
            } else {
                callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "none?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (larg.isTrue()) throw JumpException.SPECIAL_JUMP;
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getFalse();
        }
        return runtime.getTrue();
    }

    @JRubyMethod(name = "none?")
    public static IRubyObject none_p19(ThreadContext context, IRubyObject self, final Block block) {
        return none_p(context, self, block);
    }

    @Deprecated
    public static IRubyObject none_p(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context;
        
        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, callbackArity, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "none?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (block.yield(ctx, larg).isTrue()) throw JumpException.SPECIAL_JUMP;
                        return runtime.getNil();

                    }
                });
            } else {
                callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "none?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (larg.isTrue()) throw JumpException.SPECIAL_JUMP;
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getFalse();
        }
        return runtime.getTrue();
    }

    public static IRubyObject one_p(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context;
        final boolean[] result = new boolean[] { false };

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "one?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (block.yield(ctx, larg).isTrue()) {
                            if (result[0]) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result[0] = true;
                            }
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "one?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (larg.isTrue()) {
                            if (result[0]) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result[0] = true;
                            }
                        }
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getFalse();
        }
        return result[0] ? runtime.getTrue() : runtime.getFalse();
    }

    @JRubyMethod(name = "one?")
    public static IRubyObject one_p19(ThreadContext context, IRubyObject self, final Block block) {
        return one_p(context, self, block);
    }

    @Deprecated
    public static IRubyObject one_p(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context;
        final boolean[] result = new boolean[] { false };
        
        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, callbackArity, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "one?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (block.yield(ctx, larg).isTrue()) {
                            if (result[0]) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result[0] = true;
                            }
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "one?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (larg.isTrue()) {
                            if (result[0]) {
                                throw JumpException.SPECIAL_JUMP;
                            } else {
                                result[0] = true;
                            }
                        }
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getFalse();
        }
        return result[0] ? runtime.getTrue() : runtime.getFalse();
    }

    public static IRubyObject all_p(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context;

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "all?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (!block.yield(ctx, larg).isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "all?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (!larg.isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getFalse();
        }

        return runtime.getTrue();
    }

    @JRubyMethod(name = "all?")
    public static IRubyObject all_p19(ThreadContext context, IRubyObject self, final Block block) {
        if (self instanceof RubyArray) return ((RubyArray) self).all_p(context, block);

        return all_p(context, self, block);
    }

    @Deprecated
    public static IRubyObject all_pCommon(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        final Ruby runtime = context.runtime;
        final ThreadContext localContext = context;

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, callbackArity, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "all?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (!block.yield(ctx, larg).isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        checkContext(localContext, ctx, "all?");
                        IRubyObject larg = packEnumValues(runtime, largs);
                        if (!larg.isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getFalse();
        }

        return runtime.getTrue();
    }
    
    @JRubyMethod(name = "any?")
    public static IRubyObject any_p(ThreadContext context, IRubyObject self, final Block block) {
        return any_pCommon(context, self, block, block.arity());
    }
    
    public static IRubyObject any_pCommon(ThreadContext context, IRubyObject self, final Block block, Arity callbackArity) {
        final Ruby runtime = context.runtime;

        try {
            if (block.isGiven()) {
                each(context, self, new JavaInternalBlockBody(runtime, context, "Enumerable#any?", block.getSignature()) {
                    public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                        IRubyObject packedArg = packEnumValues(context.runtime, args);
                        if (block.yield(context, packedArg).isTrue()) throw JumpException.SPECIAL_JUMP;
                        return runtime.getNil();
                    }
                });
            } else {
                each(context, self, new JavaInternalBlockBody(runtime, context, "Enumerable#any?", Signature.ONE_REQUIRED) {
                    public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                        IRubyObject packedArg = packEnumValues(context.runtime, args);
                        if (packedArg.isTrue()) throw JumpException.SPECIAL_JUMP;
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getTrue();
        }

        return runtime.getFalse();
    }

    public static IRubyObject zip(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        return zip19(context, self, args, block);
    }
    
    @JRubyMethod(name = "zip", rest = true)
    public static IRubyObject zip19(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        return zipCommon19(context, self, args, block);
    }

    public static IRubyObject[] zipCommonConvert(Ruby runtime, IRubyObject[] args) {
        return zipCommonConvert(runtime, args, "to_a");
    }

    public static IRubyObject[] zipCommonConvert(Ruby runtime, IRubyObject[] args, String method) {
        RubyClass array = runtime.getArray();
        ThreadContext context = runtime.getCurrentContext();
        
        // 1.9 tries to convert, and failing that tries to "each" elements into a new array
        for (int i = 0; i < args.length; i++) {
            IRubyObject result = TypeConverter.convertToTypeWithCheck19(args[i], array, method);
            if (result.isNil()) {
                result = takeItems(context, args[i]);
            }
            args[i] = result;
        }

        return args;
    }

    public static IRubyObject zipCommon(ThreadContext context, IRubyObject self,
            IRubyObject[] aArgs, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject[] args = zipCommonConvert(runtime, aArgs);

        return zipCommonAry(context, self, args, block);
    }

    public static IRubyObject zipCommon19(ThreadContext context, IRubyObject self,
            IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        final int aLen = args.length + 1;
        RubyClass array = runtime.getArray();
        
        final IRubyObject[] newArgs = new IRubyObject[args.length];

        boolean hasUncoercible = false;
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = TypeConverter.convertToType(args[i], array, "to_ary", false);
            if (newArgs[i].isNil()) {
                hasUncoercible = true;
            }
        }
        
        // Handle uncoercibles by trying to_enum conversion
        if (hasUncoercible) {
            RubySymbol each = runtime.newSymbol("each");
            for (int i = 0; i < args.length; i++) {
                newArgs[i] = args[i].callMethod(context, "to_enum", each);
            }
        }
        
        if (hasUncoercible) {
            return zipCommonEnum(context, self, newArgs, block);
        } else {
            return zipCommonAry(context, self, newArgs, block);
        }
    }

    // TODO: Eliminate duplication here and zipCommonEnum
    // See enum_zip + zip_ary in Ruby source (1.9, anyway)
    public static IRubyObject zipCommonAry(ThreadContext context, IRubyObject self,
            final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        final int len = args.length + 1;

        if (block.isGiven()) {
            callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    RubyArray array = runtime.newArray(len);
                    int myIx = ix.getAndIncrement();
                    array.append(larg);
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.append(((RubyArray) args[i]).entry(myIx));
                    }
                    block.yield(ctx, array);
                    return runtime.getNil();
                }
            });
            return runtime.getNil();
        } else {
            final RubyArray zip = runtime.newArray();
            callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    RubyArray array = runtime.newArray(len);
                    array.append(larg);
                    int myIx = ix.getAndIncrement();
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.append(((RubyArray) args[i]).entry(myIx));
                    }
                    synchronized (zip) {
                        zip.append(array);
                    }
                    return runtime.getNil();
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
            callEach(runtime, context, self, block.getSignature(), new BlockCallback() {
                AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    RubyArray array = runtime.newArray(len);
                    int myIx = ix.getAndIncrement();
                    array.append(larg);
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.append(zipEnumNext(ctx, args[i]));
                    }
                    block.yield(ctx, array);
                    return runtime.getNil();
                }
            });
            return runtime.getNil();
        } else {
            final RubyArray zip = runtime.newArray();
            callEach(runtime, context, self, Signature.ONE_REQUIRED, new BlockCallback() {
                AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(runtime, largs);
                    RubyArray array = runtime.newArray(len);
                    array.append(larg);
                    int myIx = ix.getAndIncrement();
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.append(zipEnumNext(ctx, args[i]));
                    }
                    synchronized (zip) {
                        zip.append(array);
                    }
                    return runtime.getNil();
                }
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
            callEach(context.runtime, context, enumerable, Signature.ONE_ARGUMENT, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    IRubyObject larg = packEnumValues(ctx.runtime, largs);
                    array.append(larg);
                    return larg;
                }
            });
        }
        
        return array;
    }
    
    public static IRubyObject zipEnumNext(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        
        if (arg.isNil()) {
            return context.nil;
        } else {
            IRubyObject oldExc = runtime.getGlobalVariables().get("$!");
            try {
                return arg.callMethod(context, "next");
            } catch (RaiseException re) {
                if (re.getException().getMetaClass() == runtime.getStopIteration()) {
                    runtime.getGlobalVariables().set("$!", oldExc);
                    return context.nil;
                } else {
                    throw re;
                }
            }
        }
    }

    @JRubyMethod
    public static IRubyObject group_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.runtime;
        
        if (!block.isGiven()) return enumeratorizeWithSize(context, self, "group_by", enumSizeFn(context, self));
        
        final RubyHash result = new RubyHash(runtime);

        callEach(runtime, context, self, Signature.OPTIONAL, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject larg = packEnumValues(runtime, largs);
                IRubyObject key = block.yield(ctx, larg);
                synchronized (result) {
                    RubyArray curr = (RubyArray)result.fastARef(key);

                    if (curr == null) {
                        curr = runtime.newArray();
                        result.fastASet(key, curr);
                    }
                    curr.append(larg);
                }
                return runtime.getNil();
            }
        });

        return result;
    }

    @Deprecated
    public static IRubyObject chunk(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        switch (Arity.checkArgumentCount(context.runtime, args, 0, 1)) {
        case 0:
            return chunk(context, self, block);
        case 1:
            return chunk(context, self, args[0], block);
        default:
            // should never be reached
            throw context.runtime.newArgumentError(args.length, 0);
        }
    }
    
    @JRubyMethod
    public static IRubyObject chunk(ThreadContext context, IRubyObject self, final Block block) {
        return chunk(context, self, context.nil, block);
    }
    
    @JRubyMethod
    public static IRubyObject chunk(ThreadContext context, IRubyObject self, final IRubyObject initialState, final Block block) {
        if(!block.isGiven()) {
            throw context.runtime.newArgumentError("no block given");
        }

        IRubyObject enumerator = context.runtime.getEnumerator().allocate();
        enumerator.getInternalVariables().setInternalVariable("chunk_enumerable", self);
        enumerator.getInternalVariables().setInternalVariable("chunk_categorize", RubyProc.newProc(context.runtime, block, block.type));
        enumerator.getInternalVariables().setInternalVariable("chunk_initial_state", initialState);

        Helpers.invoke(context, enumerator, "initialize",
                CallBlock.newCallClosure(self, context.runtime.getEnumerable(), Signature.ONE_ARGUMENT,
                        new ChunkedBlockCallback(context.runtime, enumerator), context));
        return enumerator;
    }

    private static SizeFn enumSizeFn(final ThreadContext context, final IRubyObject self) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.checkCallMethod(context, "size");
            }
        };
    }

    static class ChunkArg {
        public RubyProc categorize;
        public IRubyObject state;
        public IRubyObject prev_value;
        public IRubyObject prev_elts;
        public IRubyObject yielder;
    }

    // chunk_i
    public static final class ChunkedBlockCallback implements BlockCallback {
        private final Ruby runtime;
        private final IRubyObject enumerator;

        public ChunkedBlockCallback(Ruby runtime, IRubyObject enumerator) {
            this.runtime = runtime;
            this.enumerator = enumerator;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
            IRubyObject args = packEnumValues(runtime, largs);
            final ChunkArg arg = new ChunkArg();
            IRubyObject enumerable = (IRubyObject)enumerator.getInternalVariables().getInternalVariable("chunk_enumerable");
            arg.categorize = (RubyProc)enumerator.getInternalVariables().getInternalVariable("chunk_categorize");
            arg.state = (IRubyObject)enumerator.getInternalVariables().getInternalVariable("chunk_initial_state");
            arg.prev_value = runtime.getNil();
            arg.prev_elts = runtime.getNil();
            arg.yielder = args;

            if(!arg.state.isNil()) {
                arg.state = arg.state.dup();
            }

            final IRubyObject alone = runtime.newSymbol("_alone");
            final IRubyObject separator = runtime.newSymbol("_separator");

            callEach(runtime, context, enumerable, Signature.OPTIONAL, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        IRubyObject packedArgs = packEnumValues(runtime, largs);
                        IRubyObject v;
                        if(arg.state.isNil()) {
                            if (arg.categorize.getBlock().arity().getValue() == 1) {
                                // if chunk's categorize block has arity one, we pass it the packed args
                                v = arg.categorize.callMethod(ctx, "call", packedArgs);
                            } else {
                                // else we let it spread the args as it sees fit for its arity
                                v = arg.categorize.callMethod(ctx, "call", largs);
                            }
                        } else {
                            v = arg.categorize.callMethod(ctx, "call", new IRubyObject[]{packedArgs, arg.state});
                        }

                        if(v == alone) {
                            if(!arg.prev_value.isNil()) {
                                arg.yielder.callMethod(ctx, "<<", runtime.newArray(arg.prev_value, arg.prev_elts));
                                arg.prev_value = arg.prev_elts = runtime.getNil();
                            }
                            arg.yielder.callMethod(ctx, "<<", runtime.newArray(v, runtime.newArray(packedArgs)));
                        } else if(v.isNil() || v == separator) {
                            if(!arg.prev_value.isNil()) {
                                arg.yielder.callMethod(ctx, "<<", runtime.newArray(arg.prev_value, arg.prev_elts));
                                arg.prev_value = arg.prev_elts = runtime.getNil();
                            }
                        } else if((v instanceof RubySymbol) && v.toString().charAt(0) == '_') {
                            throw runtime.newRuntimeError("symbol begins with an underscore is reserved");
                        } else {
                            if(arg.prev_value.isNil()) {
                                arg.prev_value = v;
                                arg.prev_elts = runtime.newArray(packedArgs);
                            } else {
                                if(arg.prev_value.equals(v)) {
                                    ((RubyArray)arg.prev_elts).append(packedArgs);
                                } else {
                                    arg.yielder.callMethod(ctx, "<<", runtime.newArray(arg.prev_value, arg.prev_elts));
                                    arg.prev_value = v;
                                    arg.prev_elts = runtime.newArray(packedArgs);
                                }
                            }
                        }
                        return runtime.getNil();
                    }
                });

            if(!arg.prev_elts.isNil()) {
                arg.yielder.callMethod(context, "<<", runtime.newArray(arg.prev_value, arg.prev_elts));
            }

            return runtime.getNil();
        }
    }

    public static final class AppendBlockCallback implements BlockCallback {
        private Ruby runtime;
        private RubyArray result;

        public AppendBlockCallback(Ruby runtime, RubyArray result) {
            this.runtime = runtime;
            this.result = result;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
            result.append(packEnumValues(runtime, largs));
            return runtime.getNil();
        }
    }

    public static final class PutKeyValueCallback implements BlockCallback {
        private Ruby runtime;
        private RubyHash result;

        public PutKeyValueCallback(Ruby runtime, RubyHash result) {
            this.runtime = runtime;
            this.result = result;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
            IRubyObject value;

            switch (largs.length) {
                case 0:
                    value = context.nil;
                    break;
                case 1:
                    value = largs[0];
                    break;
                default:
                    value = RubyArray.newArrayNoCopy(runtime, largs);
                    break;
            }

            IRubyObject ary = TypeConverter.checkArrayType(runtime, value);
            if (ary.isNil()) throw runtime.newTypeError("wrong element type " + value.getMetaClass().getName() + " (expected array)");
            int size;
            if ((size = ((RubyArray)ary).size()) != 2) {
                throw runtime.newArgumentError("element has wrong array length (expected 2, was " + size + ")");
            }
            result.op_aset(context, ((RubyArray)ary).eltOk(0), ((RubyArray)ary).eltOk(1));
            return context.nil;
        }
    }
}
