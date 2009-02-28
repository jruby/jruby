/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import static org.jruby.RubyEnumerator.enumeratorize;

import java.util.Comparator;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.common.IRubyWarnings.ID;

import org.jruby.exceptions.JumpException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

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
        return RuntimeHelpers.invoke(context, self, "each", CallBlock.newCallClosure(self, runtime.getEnumerable(), 
                Arity.noArguments(), callback, context));
    }

    private static void checkContext(ThreadContext firstContext, ThreadContext secondContext, String name) {
        if (firstContext != secondContext) {
            throw secondContext.getRuntime().newThreadError("Enumerable#" + name + " cannot be parallelized");
        }
    }

    @JRubyMethod(name = "count", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject count(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;
        final int result[];
        
        if (block.isGiven()) {
            result = new int[] { 0 };
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    checkContext(localContext, ctx, "count");
                    if (block.yield(ctx, largs[0]).isTrue()) {
                        result[0]++; 
                    }
                    return runtime.getNil();
                }
            });
        } else {
            if (self.respondsTo("size")) return self.callMethod(context, "size");

            result = new int[] { 0 };
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    checkContext(localContext, ctx, "count");
                    result[0]++;
                    return runtime.getNil();
                }
            });
        }
        return RubyFixnum.newFixnum(runtime, result[0]);
    }
    
    @JRubyMethod(name = "count", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject count(ThreadContext context, IRubyObject self, final IRubyObject arg, final Block block) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;
        final int result[] = new int[] { 0 };
        
        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");
        
        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block block) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                checkContext(localContext, ctx, "count");
                if (largs[0].equals(arg)) result[0]++;
                return runtime.getNil();
            }
        });
        return RubyFixnum.newFixnum(runtime, result[0]);
    }
    
    @JRubyMethod(name = "cycle", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject cycle(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), self, "cycle");
        
        return cycleCommon(context, self, -1, block);
    }

    @JRubyMethod(name = "cycle", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject cycle(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), self, "cycle");
        long nv = RubyNumeric.num2long(arg);
        return nv <= 0 ? context.getRuntime().getNil(): cycleCommon(context, self, nv, block);
    }
    
    private static IRubyObject cycleCommon(ThreadContext context, IRubyObject self, long nv, final Block block) {
        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();
        
        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                synchronized (result) {
                    result.append(largs[0]);
                }
                block.yield(ctx, largs[0]);
                return runtime.getNil();
            }
        });

        int len = result.size();
        if (len == 0) return runtime.getNil();
        
        while (nv < 0 || 0 < --nv) {
            for (int i=0; i < len; i++) {
                block.yield(context, result.eltInternal(i));
            }
        }
        return runtime.getNil();
    }

    @JRubyMethod(name = "take", compat = CompatVersion.RUBY1_9)
    public static IRubyObject take(ThreadContext context, IRubyObject self, IRubyObject n, final Block block) {
        final Ruby runtime = context.getRuntime();

        final long len = RubyNumeric.num2long(n);
        if (len < 0) throw runtime.newArgumentError("attempt to take negative size");
        if (len == 0) return runtime.newEmptyArray();

        final RubyArray result = runtime.newArray();

        try {
            callEach(runtime, context, self, new BlockCallback() {
                long i = len; // Atomic ?
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    synchronized (result) {
                        result.append(largs[0]);
                        if (--i == 0) throw JumpException.SPECIAL_JUMP; 
                    }
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {}
        return result;
    }

    @JRubyMethod(name = "take_while", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject take_while(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), self, "take_while");

        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();

        try {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    if (!block.yield(ctx, largs[0]).isTrue()) throw JumpException.SPECIAL_JUMP;
                    synchronized (result) {
                        result.append(largs[0]);
                    }
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {}
        return result;
    }    

    @JRubyMethod(name = "drop", compat = CompatVersion.RUBY1_9)
    public static IRubyObject drop(ThreadContext context, IRubyObject self, IRubyObject n, final Block block) {
        final Ruby runtime = context.getRuntime();

        final long len = RubyNumeric.num2long(n);
        if (len < 0) throw runtime.newArgumentError("attempt to drop negative size");

        final RubyArray result = runtime.newArray();

        try {
            callEach(runtime, context, self, new BlockCallback() {
                long i = len; // Atomic ?
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    synchronized (result) {
                        if (i == 0) {
                            result.append(largs[0]);
                        } else {
                            --i;
                        }
                    }
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {}
        return result;
    }

    @JRubyMethod(name = "drop_while", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject drop_while(ThreadContext context, IRubyObject self, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), self, "drop_while");

        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();

        try {
            callEach(runtime, context, self, new BlockCallback() {
                boolean memo = false;
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    if (!memo && !block.yield(ctx, largs[0]).isTrue()) memo = true;
                    if (memo) synchronized (result) {
                        result.append(largs[0]);
                    }
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {}
        return result;
    }    

    @JRubyMethod(name = "first", compat = CompatVersion.RUBY1_9)
    public static IRubyObject first(ThreadContext context, IRubyObject self) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;
        
        final IRubyObject[] holder = new IRubyObject[]{runtime.getNil()};

        try {
            callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "first");
                        holder[0] = largs[0];
                        throw JumpException.SPECIAL_JUMP;
                    }
                });
        } catch (JumpException.SpecialJump sj) {}

        return holder[0];
    }

    @JRubyMethod(name = "first", compat = CompatVersion.RUBY1_9)
    public static IRubyObject first(ThreadContext context, IRubyObject self, final IRubyObject num) {
        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();
        final ThreadContext localContext = context;

        if(RubyNumeric.fix2int(num) < 0) {
            throw runtime.newArgumentError("negative index");
        }

        try {
            callEach(runtime, context, self, new BlockCallback() {
                    private int iter = RubyNumeric.fix2int(num);
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "first");
                        if (iter-- == 0) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        result.append(largs[0]);
                        return runtime.getNil();
                    }
                });
        } catch (JumpException.SpecialJump sj) {}

        return result;
    }

    @JRubyMethod(name = {"to_a", "entries"})
    public static IRubyObject to_a(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.getRuntime();
        RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new AppendBlockCallback(runtime, result));

        return result;
    }

    @JRubyMethod(name = "sort", frame = true)
    public static IRubyObject sort(ThreadContext context, IRubyObject self, final Block block) {
        Ruby runtime = context.getRuntime();
        RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new AppendBlockCallback(runtime, result));
        result.sort_bang(context, block);
        
        return result;
    }

    @JRubyMethod(name = "sort_by", frame = true)
    public static IRubyObject sort_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context; // MUST NOT be used across threads

        if (self instanceof RubyArray) {
            RubyArray selfArray = (RubyArray) self;
            final IRubyObject[][] valuesAndCriteria = new IRubyObject[selfArray.size()][2];

            callEach(runtime, context, self, new BlockCallback() {
                AtomicInteger i = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    IRubyObject[] myVandC = valuesAndCriteria[i.getAndIncrement()];
                    myVandC[0] = largs[0];
                    myVandC[1] = block.yield(ctx, largs[0]);
                    return runtime.getNil();
                }
            });
            
            Arrays.sort(valuesAndCriteria, new Comparator<IRubyObject[]>() {
                public int compare(IRubyObject[] o1, IRubyObject[] o2) {
                    return RubyFixnum.fix2int(o1[1].callMethod(localContext, "<=>", o2[1]));
                }
            });
            
            IRubyObject dstArray[] = new IRubyObject[selfArray.size()];
            for (int i = 0; i < dstArray.length; i++) {
                dstArray[i] = valuesAndCriteria[i][0];
            }

            return runtime.newArrayNoCopy(dstArray);
        } else {
            final RubyArray result = runtime.newArray();
            callEach(runtime, context, self, new AppendBlockCallback(runtime, result));
            
            final IRubyObject[][] valuesAndCriteria = new IRubyObject[result.size()][2];
            for (int i = 0; i < valuesAndCriteria.length; i++) {
                IRubyObject val = result.eltInternal(i);
                valuesAndCriteria[i][0] = val;
                valuesAndCriteria[i][1] = block.yield(context, val);
            }
            
            Arrays.sort(valuesAndCriteria, new Comparator<IRubyObject[]>() {
                public int compare(IRubyObject[] o1, IRubyObject[] o2) {
                    return RubyFixnum.fix2int(o1[1].callMethod(localContext, "<=>", o2[1]));
                }
            });
            
            for (int i = 0; i < valuesAndCriteria.length; i++) {
                result.eltInternalSet(i, valuesAndCriteria[i][0]);
            }

            return result;
        }
    }

    @JRubyMethod(name = "sort_by", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject sort_by19(ThreadContext context, IRubyObject self, final Block block) {
        return block.isGiven() ? sort_by(context, self, block) : enumeratorize(context.getRuntime(), self, "sort_by");
    }

    @JRubyMethod(name = "grep", required = 1, frame = true)
    public static IRubyObject grep(ThreadContext context, IRubyObject self, final IRubyObject pattern, final Block block) {
        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    ctx.setRubyFrameDelta(ctx.getRubyFrameDelta()+2);
                    if (pattern.callMethod(ctx, "===", largs[0]).isTrue()) {
                        IRubyObject value = block.yield(ctx, largs[0]);
                        synchronized (result) {
                            result.append(value);
                        }
                    }
                    ctx.setRubyFrameDelta(ctx.getRubyFrameDelta()-2);
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    if (pattern.callMethod(ctx, "===", largs[0]).isTrue()) {
                        synchronized (result) {
                            result.append(largs[0]);
                        }
                    }
                    return runtime.getNil();
                }
            });
        }
        
        return result;
    }

    @JRubyMethod(name = {"detect", "find"}, frame = true)
    public static IRubyObject detect(ThreadContext context, IRubyObject self, final Block block) {
        return detect(context, self, null, block);
    }

    @JRubyMethod(name = {"detect", "find"}, frame = true)
    public static IRubyObject detect(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject result[] = new IRubyObject[] { null };
        final ThreadContext localContext = context;

        try {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    checkContext(localContext, ctx, "detect/find");
                    if (block.yield(ctx, largs[0]).isTrue()) {
                        result[0] = largs[0];
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

    @JRubyMethod(name = "detect", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject detect19(ThreadContext context, IRubyObject self, final Block block) {
        return block.isGiven() ? detect(context, self, block) : enumeratorize(context.getRuntime(), self, "detect");
    }

    @JRubyMethod(name = "detect", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject detect19(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        return block.isGiven() ? detect(context, self, ifnone, block) : enumeratorize(context.getRuntime(), self, "detect", ifnone);
    }

    @JRubyMethod(name = "find", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject find19(ThreadContext context, IRubyObject self, final Block block) {
        return block.isGiven() ? detect(context, self, block) : enumeratorize(context.getRuntime(), self, "find");
    }

    @JRubyMethod(name = "find", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject find19(ThreadContext context, IRubyObject self, IRubyObject ifnone, final Block block) {
        return block.isGiven() ? detect(context, self, ifnone, block) : enumeratorize(context.getRuntime(), self, "find", ifnone);
    }
    
    @JRubyMethod(name = "find_index", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject find_index(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();

        if (!block.isGiven()) return enumeratorize(runtime, self, "find_index");

        final long result[] = new long[] {0};

        try {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    if (block.yield(ctx, largs[0]).isTrue()) throw JumpException.SPECIAL_JUMP;
                    result[0]++;
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return RubyFixnum.newFixnum(runtime, result[0]);
        }
        return runtime.getNil();
    }

    @JRubyMethod(name = "find_index", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject find_index(ThreadContext context, IRubyObject self, final IRubyObject cond, final Block block) {
        final Ruby runtime = context.getRuntime();

        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");

        final long result[] = new long[] {0};

        try {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    if (largs[0].equals(cond)) throw JumpException.SPECIAL_JUMP;
                    result[0]++;
                    return runtime.getNil();
                }
            });
        } catch (JumpException.SpecialJump sj) {
            return RubyFixnum.newFixnum(runtime, result[0]);
        }
        return runtime.getNil();
    }

    @JRubyMethod(name = {"select", "find_all"}, frame = true)
    public static IRubyObject select(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                if (block.yield(ctx, largs[0]).isTrue()) {
                    synchronized (result) {
                        result.append(largs[0]);
                    }
                }
                return runtime.getNil();
            }
        });

        return result;
    }

    @JRubyMethod(name = "select", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject select19(ThreadContext context, IRubyObject self, final Block block) {
        return block.isGiven() ? select(context, self, block) : enumeratorize(context.getRuntime(), self, "select");
    }

    @JRubyMethod(name = "find_all", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject find_all19(ThreadContext context, IRubyObject self, final Block block) {
        return block.isGiven() ? select(context, self, block) : enumeratorize(context.getRuntime(), self, "find_all");
    }

    @JRubyMethod(name = "reject", frame = true)
    public static IRubyObject reject(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                if (!block.yield(ctx, largs[0]).isTrue()) {
                    synchronized (result) {
                        result.append(largs[0]);
                    }
                }
                return runtime.getNil();
            }
        });

        return result;
    }

    @JRubyMethod(name = "reject", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject reject19(ThreadContext context, IRubyObject self, final Block block) {
        return block.isGiven() ? reject(context, self, block) : enumeratorize(context.getRuntime(), self, "reject");
    }

    @JRubyMethod(name = {"collect", "map"}, frame = true)
    public static IRubyObject collect(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final RubyArray result = runtime.newArray();

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    IRubyObject value = block.yield(ctx, largs[0]);
                    synchronized (result) {
                        result.append(value);
                    }
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new AppendBlockCallback(runtime, result));
        }
        return result;
    }

    @JRubyMethod(name = "inject", frame = true)
    public static IRubyObject inject(ThreadContext context, IRubyObject self, final Block block) {
        return inject(context, self, null, block);
    }

    @JRubyMethod(name = "inject", frame = true)
    public static IRubyObject inject(ThreadContext context, IRubyObject self, IRubyObject init, final Block block) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject result[] = new IRubyObject[] { init };
        final ThreadContext localContext = context;

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                checkContext(localContext, ctx, "inject");
                result[0] = result[0] == null ? 
                        largs[0] : block.yield(ctx, runtime.newArray(result[0], largs[0]), null, null, true);

                return runtime.getNil();
            }
        });

        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod(name = {"inject", "reduce"}, frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject inject19(ThreadContext context, IRubyObject self, final Block block) {
        return inject(context, self, block);
    }
    
    @JRubyMethod(name = {"inject", "reduce"}, frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject inject19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return block.isGiven() ? inject(context, self, arg, block) : inject19(context, self, null, arg, block);
    }

    @JRubyMethod(name = {"inject", "reduce"}, frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject inject19(ThreadContext context, IRubyObject self, IRubyObject init, IRubyObject method, final Block block) {
        final Ruby runtime = context.getRuntime();

        if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED , "given block not used");

        final String methodId = method.asJavaString();
        final IRubyObject result[] = new IRubyObject[] { init }; 

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                result[0] = result[0] == null ? largs[0] : result[0].callMethod(ctx, methodId, largs[0]);
                return runtime.getNil();
            }
        });
        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod(name = "partition", frame = true)
    public static IRubyObject partition(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final RubyArray arr_true = runtime.newArray();
        final RubyArray arr_false = runtime.newArray();

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                if (block.yield(ctx, largs[0]).isTrue()) {
                    synchronized (arr_true) {
                        arr_true.append(largs[0]);
                    }
                } else {
                    synchronized (arr_false) {
                        arr_false.append(largs[0]);
                    }
                }

                return runtime.getNil();
            }
        });

        return runtime.newArray(arr_true, arr_false);
    }

    @JRubyMethod(name = "partition", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject partition19(ThreadContext context, IRubyObject self, final Block block) {
        return block.isGiven() ? partition(context, self, block) : enumeratorize(context.getRuntime(), self, "partition");
    }

    private static class EachWithIndex implements BlockCallback {
        private int index = 0;
        private final Block block;
        private final Ruby runtime;

        public EachWithIndex(ThreadContext ctx, Block block) {
            this.block = block;
            this.runtime = ctx.getRuntime();
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] iargs, Block block) {
            this.block.call(context, new IRubyObject[] { runtime.newArray(iargs[0], runtime.newFixnum(index++)) });
            return runtime.getNil();            
        }
    }

    @JRubyMethod(name = "each_with_index", frame = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, Block block) {
        callEach(context.getRuntime(), context, self, new EachWithIndex(context, block));
        return self;
    }

    @JRubyMethod(name = "each_with_index", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject each_with_index19(ThreadContext context, IRubyObject self, Block block) {
        return block.isGiven() ? each_with_index(context, self, block) : enumeratorize(context.getRuntime(), self, "each_with_index");
    }

    @JRubyMethod(name = {"include?", "member?"}, required = 1, frame = true)
    public static IRubyObject include_p(ThreadContext context, IRubyObject self, final IRubyObject arg) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;

        try {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    checkContext(localContext, ctx, "include?/member?");
                    if (RubyObject.equalInternal(ctx, largs[0], arg)) {
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

    @JRubyMethod(name = "max", frame = true)
    public static IRubyObject max(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject result[] = new IRubyObject[] { null };
        final ThreadContext localContext = context;

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    checkContext(localContext, ctx, "max{}");
                    if (result[0] == null || RubyComparable.cmpint(ctx, block.yield(ctx, 
                            runtime.newArray(largs[0], result[0]), null, null, true), largs[0], result[0]) > 0) {
                        result[0] = largs[0];
                    }
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    synchronized (result) {
                        if (result[0] == null || RubyComparable.cmpint(ctx, largs[0].callMethod(ctx, "<=>", result[0]), largs[0], result[0]) > 0) {
                            result[0] = largs[0];
                        }
                    }
                    return runtime.getNil();
                }
            });
        }
        
        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod(name = "max_by", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject max_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();

        if (!block.isGiven()) return enumeratorize(runtime, self, "max_by");

        final IRubyObject result[] = new IRubyObject[] { runtime.getNil() };
        final ThreadContext localContext = context;

        callEach(runtime, context, self, new BlockCallback() {
            IRubyObject memo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                checkContext(localContext, ctx, "max_by");
                IRubyObject v = block.yield(ctx, largs[0]);

                if (memo == null || RubyComparable.cmpint(ctx, v.callMethod(ctx, "<=>", memo), v, memo) > 0) {
                    memo = v;
                    result[0] = largs[0];
                }
                return runtime.getNil();
            }
        });
        return result[0];
    }

    @JRubyMethod(name = "min", frame = true)
    public static IRubyObject min(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject result[] = new IRubyObject[] { null };
        final ThreadContext localContext = context;

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    checkContext(localContext, ctx, "min{}");
                    if (result[0] == null || RubyComparable.cmpint(ctx, block.yield(ctx, 
                            runtime.newArray(largs[0], result[0])), largs[0], result[0]) < 0) {
                        result[0] = largs[0];
                    }
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    synchronized (result) {
                        if (result[0] == null || RubyComparable.cmpint(ctx, largs[0].callMethod(ctx, "<=>", result[0]), largs[0], result[0]) < 0) {
                            result[0] = largs[0];
                        }
                    }
                    return runtime.getNil();
                }
            });
        }

        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod(name = "min_by", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject min_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();

        if (!block.isGiven()) return enumeratorize(runtime, self, "min_by");

        final IRubyObject result[] = new IRubyObject[] { runtime.getNil() };
        final ThreadContext localContext = context;

        callEach(runtime, context, self, new BlockCallback() {
            IRubyObject memo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                checkContext(localContext, ctx, "min_by");
                IRubyObject v = block.yield(ctx, largs[0]);

                if (memo == null || RubyComparable.cmpint(ctx, v.callMethod(ctx, "<=>", memo), v, memo) < 0) {
                    memo = v;
                    result[0] = largs[0];
                }
                return runtime.getNil();
            }
        });
        return result[0];
    }

    @JRubyMethod(name = "minmax", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject minmax(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject result[] = new IRubyObject[] { null, null };
        final ThreadContext localContext = context;

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    checkContext(localContext, ctx, "minmax");
                    IRubyObject arg = largs[0];

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
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    IRubyObject arg = largs[0];
                    synchronized (result) {
                        if (result[0] == null) {
                            result[0] = result[1] = arg;
                        } else {
                            if (RubyComparable.cmpint(ctx, arg.callMethod(ctx, "<=>", result[0]), arg, result[0]) < 0) {
                                result[0] = arg;
                            }

                            if (RubyComparable.cmpint(ctx, arg.callMethod(ctx, "<=>", result[1]), arg, result[1]) > 0) {
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

    @JRubyMethod(name = "minmax_by", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject minmax_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();

        if (!block.isGiven()) return enumeratorize(runtime, self, "minmax_by");

        final IRubyObject result[] = new IRubyObject[] { runtime.getNil(), runtime.getNil() };
        final ThreadContext localContext = context;

        callEach(runtime, context, self, new BlockCallback() {
            IRubyObject minMemo = null, maxMemo = null;
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                checkContext(localContext, ctx, "minmax_by");
                IRubyObject arg = largs[0];
                IRubyObject v = block.yield(ctx, arg);

                if (minMemo == null) {
                    minMemo = maxMemo = v;
                    result[0] = result[1] = arg;
                } else {
                    if (RubyComparable.cmpint(ctx, v.callMethod(ctx, "<=>", minMemo), v, minMemo) < 0) {
                        minMemo = v;
                        result[0] = arg;
                    }
                    if (RubyComparable.cmpint(ctx, v.callMethod(ctx, "<=>", maxMemo), v, maxMemo) > 0) {
                        maxMemo = v;
                        result[1] = arg;
                    }
                }
                return runtime.getNil();
            }
        });
        return runtime.newArrayNoCopy(result);
    }
    
    @JRubyMethod(name = "none?", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject none_p(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;
        
        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "none?");
                        if (block.yield(ctx, largs[0]).isTrue()) throw JumpException.SPECIAL_JUMP; 
                        return runtime.getNil();
                        
                    }
                });
            } else {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "none?");
                        if (largs[0].isTrue()) throw JumpException.SPECIAL_JUMP;
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getFalse();
        }
        return runtime.getTrue();
    }
    
    @JRubyMethod(name = "one?", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject one_p(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;
        final boolean[] result = new boolean[] { false };
        
        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "one?");
                        if (block.yield(ctx, largs[0]).isTrue()) {
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
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "one?");
                        if (largs[0].isTrue()) {
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
    
    @JRubyMethod(name = "all?", frame = true)
    public static IRubyObject all_p(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "all?");
                        if (!block.yield(ctx, largs[0]).isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "all?");
                        if (!largs[0].isTrue()) {
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

    @JRubyMethod(name = "any?", frame = true)
    public static IRubyObject any_p(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        final ThreadContext localContext = context;

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "any?");
                        if (block.yield(ctx, largs[0]).isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                        checkContext(localContext, ctx, "any?");
                        if (largs[0].isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            }
        } catch (JumpException.SpecialJump sj) {
            return runtime.getTrue();
        }

        return runtime.getFalse();
    }

    @JRubyMethod(name = "zip", rest = true, frame = true)
    public static IRubyObject zip(ThreadContext context, IRubyObject self, final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.getRuntime();

        for (int i = 0; i < args.length; i++) {
            args[i] = TypeConverter.convertToType(args[i], runtime.getArray(), "to_a");
        }
        
        final int aLen = args.length + 1;

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    RubyArray array = runtime.newArray(aLen);
                    int myIx = ix.getAndIncrement();
                    array.append(largs[0]);
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
            callEach(runtime, context, self, new BlockCallback() {
                AtomicInteger ix = new AtomicInteger(0);

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                    RubyArray array = runtime.newArray(aLen);
                    array.append(largs[0]);
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

    @JRubyMethod(name = "group_by", frame = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject group_by(ThreadContext context, IRubyObject self, final Block block) {
        final Ruby runtime = context.getRuntime();
        
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(runtime, self, "group_by");
        
        final RubyHash result = new RubyHash(runtime);

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (largs.length == 0) throw ctx.getRuntime().newArgumentError(0, 1);
                IRubyObject key = block.yield(ctx, largs[0]);
                synchronized (result) {
                    RubyArray curr = (RubyArray)result.fastARef(key);

                    if (curr == null) {
                        curr = runtime.newArray();
                        result.fastASet(key, curr);
                    }
                    curr.append(largs[0]);
                }
                return runtime.getNil();
            }
        });

        return result;
    }
    
    public static final class AppendBlockCallback implements BlockCallback {
        private Ruby runtime;
        private RubyArray result;

        public AppendBlockCallback(Ruby runtime, RubyArray result) {
            this.runtime = runtime;
            this.result = result;
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject[] largs, Block blk) {
            if (largs.length == 0) throw context.getRuntime().newArgumentError(0, 1);
            result.append(largs[0]);
            
            return runtime.getNil();
        }
    }
}
