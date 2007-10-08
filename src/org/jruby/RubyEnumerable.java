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

import java.util.Comparator;
import java.util.Arrays;
import org.jruby.anno.JRubyMethod;

import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The implementation of Ruby's Enumerable module.
 */
public class RubyEnumerable {

    public static RubyModule createEnumerableModule(Ruby runtime) {
        RubyModule enumModule = runtime.defineModule("Enumerable");
        runtime.setEnumerable(enumModule);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyEnumerable.class);
        
        enumModule.defineAnnotatedMethods(RubyEnumerable.class, callbackFactory);

        return enumModule;
    }

    public static IRubyObject callEach(Ruby runtime, ThreadContext context, IRubyObject self,
            BlockCallback callback) {
        return self.callMethod(context, "each", new CallBlock(self, runtime.getEnumerable(), 
                Arity.noArguments(), callback, context));
    }

    @JRubyMethod(name = "to_a", name2 = "entries")
    public static IRubyObject to_a(IRubyObject self) {
        Ruby runtime = self.getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new AppendBlockCallback(runtime, result));

        return result;
    }

    @JRubyMethod(name = "sort", frame = true)
    public static IRubyObject sort(IRubyObject self, final Block block) {
        Ruby runtime = self.getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new AppendBlockCallback(runtime, result));
        result.sort_bang(block);
        
        return result;
    }

    @JRubyMethod(name = "sort_by", frame = true)
    public static IRubyObject sort_by(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        if (self instanceof RubyArray) {
            RubyArray selfArray = (RubyArray) self;
            final IRubyObject[][] valuesAndCriteria = new IRubyObject[selfArray.size()][2];

            callEach(runtime, context, self, new BlockCallback() {
                int i = 0;

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    valuesAndCriteria[i][0] = largs[0];
                    valuesAndCriteria[i++][1] = block.yield(context, largs[0]);
                    return runtime.getNil();
                }
            });
            
            Arrays.sort(valuesAndCriteria, new Comparator() {
                public int compare(Object o1, Object o2) {
                    IRubyObject ro1 = ((IRubyObject[]) o1)[1];
                    IRubyObject ro2 = ((IRubyObject[]) o2)[1];
                    return RubyFixnum.fix2int(ro1.callMethod(context, MethodIndex.OP_SPACESHIP, "<=>", ro2));
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
            
            Arrays.sort(valuesAndCriteria, new Comparator() {
                public int compare(Object o1, Object o2) {
                    IRubyObject ro1 = ((IRubyObject[]) o1)[1];
                    IRubyObject ro2 = ((IRubyObject[]) o2)[1];
                    return RubyFixnum.fix2int(ro1.callMethod(context, MethodIndex.OP_SPACESHIP, "<=>", ro2));
                }
            });
            
            for (int i = 0; i < valuesAndCriteria.length; i++) {
                result.eltInternalSet(i, valuesAndCriteria[i][0]);
            }

            return result;
        }
    }

    @JRubyMethod(name = "grep", required = 1, frame = true)
    public static IRubyObject grep(IRubyObject self, final IRubyObject pattern, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyArray result = runtime.newArray();

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (pattern.callMethod(context, MethodIndex.OP_EQQ, "===", largs[0]).isTrue()) {
                        result.append(block.yield(context, largs[0]));
                    }
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (pattern.callMethod(context, MethodIndex.OP_EQQ, "===", largs[0]).isTrue()) {
                        result.append(largs[0]);
                    }
                    return runtime.getNil();
                }
            });
        }
        
        return result;
    }

    @JRubyMethod(name = "detect", name2 = "find", optional = 1, frame = true)
    public static IRubyObject detect(IRubyObject self, IRubyObject[] args, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject result[] = new IRubyObject[] { null };
        IRubyObject ifnone = null;

        if (Arity.checkArgumentCount(runtime, args, 0, 1) == 1) ifnone = args[0];

        try {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (block.yield(context, largs[0]).isTrue()) {
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

    @JRubyMethod(name = "select", name2 = "find_all", frame = true)
    public static IRubyObject select(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (block.yield(context, largs[0]).isTrue()) result.append(largs[0]);
                return runtime.getNil();
            }
        });

        return result;
    }

    @JRubyMethod(name = "reject", frame = true)
    public static IRubyObject reject(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyArray result = runtime.newArray();

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (!block.yield(context, largs[0]).isTrue()) result.append(largs[0]);
                return runtime.getNil();
            }
        });

        return result;
    }

    @JRubyMethod(name = "collect", name2 = "map", frame = true)
    public static IRubyObject collect(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyArray result = runtime.newArray();

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    result.append(block.yield(context, largs[0]));
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new AppendBlockCallback(runtime, result));
        }
        return result;
    }

    @JRubyMethod(name = "inject", optional = 1, frame = true)
    public static IRubyObject inject(IRubyObject self, IRubyObject[] args, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject result[] = new IRubyObject[] { null };

        if (Arity.checkArgumentCount(runtime, args, 0, 1) == 1) result[0] = args[0];

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                result[0] = result[0] == null ? 
                        largs[0] : block.yield(context, runtime.newArray(result[0], largs[0]));

                return runtime.getNil();
            }
        });

        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod(name = "partition", frame = true)
    public static IRubyObject partition(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyArray arr_true = runtime.newArray();
        final RubyArray arr_false = runtime.newArray();

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (block.yield(context, largs[0]).isTrue()) {
                    arr_true.append(largs[0]);
                } else {
                    arr_false.append(largs[0]);
                }

                return runtime.getNil();
            }
        });

        return runtime.newArray(arr_true, arr_false);
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
            this.block.yield(context, runtime.newArray(iargs[0], runtime.newFixnum(index++)));
            return runtime.getNil();            
        }
    }

    @JRubyMethod(name = "each_with_index", frame = true)
    public static IRubyObject each_with_index(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        self.callMethod(context, "each", new CallBlock(self, self.getRuntime().getEnumerable(), 
                Arity.noArguments(), new EachWithIndex(context, block), context));
        
        return self;
    }

    @JRubyMethod(name = "include?", name2 = "member?", required = 1)
    public static IRubyObject include_p(IRubyObject self, final IRubyObject arg) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        try {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (RubyObject.equalInternal(context, arg, largs[0]).isTrue()) {
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
    public static IRubyObject max(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject result[] = new IRubyObject[] { null };

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (result[0] == null || RubyComparable.cmpint(block.yield(context, 
                            runtime.newArray(largs[0], result[0])), largs[0], result[0]) > 0) {
                        result[0] = largs[0];
                    }
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (result[0] == null || RubyComparable.cmpint(largs[0].callMethod(context,
                            MethodIndex.OP_SPACESHIP, "<=>", result[0]), largs[0], result[0]) > 0) {
                        result[0] = largs[0];
                    }
                    return runtime.getNil();
                }
            });
        }
        
        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod(name = "min", frame = true)
    public static IRubyObject min(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject result[] = new IRubyObject[] { null };

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (result[0] == null || RubyComparable.cmpint(block.yield(context, 
                            runtime.newArray(largs[0], result[0])), largs[0], result[0]) < 0) {
                        result[0] = largs[0];
                    }
                    return runtime.getNil();
                }
            });
        } else {
            callEach(runtime, context, self, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (result[0] == null || RubyComparable.cmpint(largs[0].callMethod(context,
                            MethodIndex.OP_SPACESHIP, "<=>", result[0]), largs[0], result[0]) < 0) {
                        result[0] = largs[0];
                    }
                    return runtime.getNil();
                }
            });
        }
        
        return result[0] == null ? runtime.getNil() : result[0];
    }

    @JRubyMethod(name = "all?", frame = true)
    public static IRubyObject all_p(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (!block.yield(context, largs[0]).isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
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
    public static IRubyObject any_p(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        try {
            if (block.isGiven()) {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                        if (block.yield(context, largs[0]).isTrue()) {
                            throw JumpException.SPECIAL_JUMP;
                        }
                        return runtime.getNil();
                    }
                });
            } else {
                callEach(runtime, context, self, new BlockCallback() {
                    public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
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
    public static IRubyObject zip(IRubyObject self, final IRubyObject[] args, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].convertToType(runtime.getArray(), MethodIndex.TO_A, "to_a");
        }
        
        final int aLen = args.length + 1;

        if (block.isGiven()) {
            callEach(runtime, context, self, new BlockCallback() {
                int ix = 0;

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    RubyArray array = runtime.newArray(aLen);
                    array.append(largs[0]);
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.append(((RubyArray) args[i]).entry(ix));
                    }
                    block.yield(context, array);
                    ix++;
                    return runtime.getNil();
                }
            });
            return runtime.getNil();
        } else {
            final RubyArray zip = runtime.newArray();
            callEach(runtime, context, self, new BlockCallback() {
                int ix = 0;

                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    RubyArray array = runtime.newArray(aLen);
                    array.append(largs[0]);
                    for (int i = 0, j = args.length; i < j; i++) {
                        array.append(((RubyArray) args[i]).entry(ix));
                    }
                    zip.append(array);
                    ix++;
                    return runtime.getNil();
                }
            });
            return zip;
        }
    }

    @JRubyMethod(name = "group_by", frame = true)
    public static IRubyObject group_by(IRubyObject self, final Block block) {
        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyHash result = new RubyHash(runtime);

        callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                IRubyObject key = block.yield(context, largs[0]);
                IRubyObject curr = result.fastARef(key);

                if (curr == null) {
                    curr = runtime.newArray();
                    result.fastASet(key, curr);
                }
                curr.callMethod(context, MethodIndex.OP_LSHIFT, "<<", largs[0]);
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
            result.append(largs[0]);
            
            return runtime.getNil();
        }
    }
}
