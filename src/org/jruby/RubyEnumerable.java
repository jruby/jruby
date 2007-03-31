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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The implementation of Ruby's Enumerable module.
 */
public class RubyEnumerable {
    private static class ListAddBlockCallback implements BlockCallback {
        private final List arr = new ArrayList();
        private final Ruby runtime;
        public ListAddBlockCallback(Ruby runtime) {
            this.runtime = runtime;
        }
        public IRubyObject call(ThreadContext context, IRubyObject[] iargs, IRubyObject iself, Block block) {
            if(iargs.length > 1) {
                arr.add(runtime.newArrayNoCopy(iargs));
            } else {
                arr.add(iargs[0]);
            }

            return runtime.getNil();
        }
        public List getList() {
            return arr;
        }
    }

    private static class RubyFirstArrayComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            IRubyObject obj1 = ((IRubyObject[])o1)[0];
            return RubyFixnum.fix2int((((IRubyObject[])o1)[0].callMethod(obj1.getRuntime().getCurrentContext(),"<=>", ((IRubyObject[])o2)[0])));
        }
    }
    private static class RubyYieldComparator implements Comparator {
        private final ThreadContext context;
        private final Ruby runtime;
        private final Block block;
        public RubyYieldComparator(ThreadContext context, Block block) {
            this.context = context;
            this.runtime = context.getRuntime();
            this.block = block;
        }
        public int compare(Object o1, Object o2) {
            return RubyFixnum.fix2int(block.yield(context, runtime.newArray((IRubyObject)o1,(IRubyObject)o2)));
        }
    }

    public static IRubyObject callEach(ThreadContext context, IRubyObject self, RubyModule module, BlockCallback bc) {
        return self.callMethod(context, "each", new CallBlock(self,module,Arity.noArguments(),bc,context));
    }

    public static List eachToList(IRubyObject self) {
        ListAddBlockCallback ladc = new ListAddBlockCallback(self.getRuntime());
        callEach(self.getRuntime().getCurrentContext(),self,self.getRuntime().getModule("Enumerable"),ladc);
        return ladc.getList();
    }

    public static RubyModule createEnumerableModule(Ruby runtime) {
        RubyModule enm = runtime.defineModule("Enumerable");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyEnumerable.class);

        enm.defineFastMethod("to_a", callbackFactory.getFastSingletonMethod("to_a"));
        enm.defineFastMethod("entries", callbackFactory.getFastSingletonMethod("to_a"));
        enm.defineFastMethod("entries", callbackFactory.getFastSingletonMethod("to_a"));
        enm.defineMethod("sort", callbackFactory.getSingletonMethod("sort"));
        enm.defineMethod("sort_by", callbackFactory.getSingletonMethod("sort_by"));
        enm.defineMethod("grep", callbackFactory.getSingletonMethod("grep",IRubyObject.class));
        enm.defineMethod("detect", callbackFactory.getOptSingletonMethod("detect"));
        enm.defineMethod("find", callbackFactory.getOptSingletonMethod("detect"));
        enm.defineMethod("select", callbackFactory.getSingletonMethod("select"));
        enm.defineMethod("find_all", callbackFactory.getSingletonMethod("select"));
        enm.defineMethod("reject", callbackFactory.getSingletonMethod("reject"));
        enm.defineMethod("collect", callbackFactory.getSingletonMethod("collect"));
        enm.defineMethod("map", callbackFactory.getSingletonMethod("collect"));
        enm.defineMethod("inject", callbackFactory.getOptSingletonMethod("inject"));
        enm.defineMethod("partition", callbackFactory.getSingletonMethod("partition"));
        enm.defineMethod("each_with_index", callbackFactory.getSingletonMethod("each_with_index"));
        enm.defineFastMethod("include?", callbackFactory.getFastSingletonMethod("include_p",IRubyObject.class));
        enm.defineFastMethod("member?", callbackFactory.getFastSingletonMethod("include_p",IRubyObject.class));
        enm.defineMethod("max", callbackFactory.getSingletonMethod("max"));
        enm.defineMethod("min", callbackFactory.getSingletonMethod("min"));
        enm.defineMethod("all?", callbackFactory.getSingletonMethod("all_p"));
        enm.defineMethod("any?", callbackFactory.getSingletonMethod("any_p"));
        enm.defineMethod("zip", callbackFactory.getOptSingletonMethod("zip"));
        enm.defineMethod("group_by", callbackFactory.getSingletonMethod("group_by"));

        return enm;
    }

    public static IRubyObject to_a(IRubyObject recv) {
        return recv.getRuntime().newArray(eachToList(recv));
    }

    public static IRubyObject sort(IRubyObject recv, Block block) {
        ThreadContext ctx = recv.getRuntime().getCurrentContext();
        if (!block.isGiven()) return recv.callMethod(ctx, "to_a").callMethod(ctx, "sort"); 
        final List arr = eachToList(recv);
        Collections.sort(arr, new RubyYieldComparator(ctx, block));
        return recv.getRuntime().newArray(arr);
    }

    public static IRubyObject sort_by(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List result = eachToList(self);
        IRubyObject[][] secResult = new IRubyObject[result.size()][2];
        int ix = 0;
        for(Iterator iter = result.iterator();iter.hasNext();ix++) {
            IRubyObject iro = (IRubyObject)iter.next();
            secResult[ix][0] = block.yield(context, iro);
            secResult[ix][1] = iro;
        }
        Arrays.sort(secResult, new RubyFirstArrayComparator());
        IRubyObject[] result2 = new IRubyObject[secResult.length];
        for(int i=0,j=result2.length;i<j;i++) {
            result2[i] = secResult[i][1];
        }
        return context.getRuntime().newArrayNoCopy(result2);
    }

    public static IRubyObject grep(IRubyObject self, IRubyObject pattern, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        List result = new ArrayList();
        if (!block.isGiven()) {
            for (Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if (pattern.callMethod(context,"===", item).isTrue()) {
                    result.add(item);
                }
            }                
        } else {
            for (Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if (pattern.callMethod(context,"===", item).isTrue()) {
                    result.add(block.yield(context, item));
                }
            }                
        }
        return context.getRuntime().newArray(result);
    }

    public static IRubyObject detect(IRubyObject self, IRubyObject[] args, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        for(Iterator iter = arr.iterator();iter.hasNext();) {
            IRubyObject element = (IRubyObject)iter.next();
            if(block.yield(context, element).isTrue()) {
                return element;
            }                
        }
        if(args.length > 0 && args[0] instanceof RubyProc) {
            return ((RubyProc)args[0]).call(new IRubyObject[0]);
        }
        return context.getRuntime().getNil();
    }

    public static IRubyObject select(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        List result = new ArrayList(arr.size());
        for(Iterator iter = arr.iterator();iter.hasNext();) {
            IRubyObject element = (IRubyObject)iter.next();
            if(block.yield(context, element).isTrue()) {
                result.add(element);
            }                
        }
        return context.getRuntime().newArray(result);
    }

    public static IRubyObject reject(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        List result = new ArrayList(arr.size());
        for(Iterator iter = arr.iterator();iter.hasNext();) {
            IRubyObject element = (IRubyObject)iter.next();
            if(!block.yield(context, element).isTrue()) {
                result.add(element);
            }                
        }
        return context.getRuntime().newArray(result);
    }

    public static IRubyObject collect(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        IRubyObject[] result = new IRubyObject[arr.size()];
        if (block.isGiven()) {
            int i=0;
            for (Iterator iter = arr.iterator();iter.hasNext();) {
                result[i++] = block.yield(context, (IRubyObject)iter.next());
            }
        } else {
            int i=0;
            for (Iterator iter = arr.iterator();iter.hasNext();) {
                result[i++] = (IRubyObject)iter.next();
            }
        }
        return context.getRuntime().newArrayNoCopy(result);
    }

    public static IRubyObject inject(IRubyObject self, IRubyObject[] args, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject result = null;
        if(args.length > 0) {
            result = args[0];
        }
        List arr = eachToList(self);
        for(Iterator iter = arr.iterator();iter.hasNext();) {
            IRubyObject item = (IRubyObject)iter.next();
            if(result == null) {
                result = item;
            } else {
                result = block.yield(context, context.getRuntime().newArray(result,item));
            }
        }
        if(null == result) {
            return context.getRuntime().getNil();
        }
        return result;
    }

    public static IRubyObject partition(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        List arr_true = new ArrayList(arr.size()/2);
        List arr_false = new ArrayList(arr.size()/2);
        for(Iterator iter = arr.iterator();iter.hasNext();) {
            IRubyObject item = (IRubyObject)iter.next();
            if(block.yield(context, item).isTrue()) {
                arr_true.add(item);
            } else {
                arr_false.add(item);
            }
        }
        return context.getRuntime().newArray(context.getRuntime().newArray(arr_true),context.getRuntime().newArray(arr_false));
    }


    private static class EachWithIndex implements BlockCallback {
        private int index = 0;
        private Block block;
        private Ruby runtime;
        public EachWithIndex(ThreadContext ctx, Block block) {
            this.block = block;
            this.runtime = ctx.getRuntime();
        }
        public IRubyObject call(ThreadContext context, IRubyObject[] iargs, IRubyObject iself, Block block) {
            IRubyObject val;
            if(iargs.length > 1) {
                val = runtime.newArray(iargs);
            } else {
                val = iargs[0];
            }
            this.block.yield(context, runtime.newArray(val, runtime.newFixnum(index++)));
            return runtime.getNil();
        }
    }

    public static IRubyObject each_with_index(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        self.callMethod(context, "each", new CallBlock(self,self.getRuntime().getModule("Enumerable"),Arity.noArguments(),new EachWithIndex(context,block),context));
        return self;
    }

    public static IRubyObject include_p(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        for(Iterator iter = arr.iterator();iter.hasNext();) {
            if(arg.callMethod(context,"==", (IRubyObject)iter.next()).isTrue()) {
                return context.getRuntime().getTrue();
            }
        }
        return context.getRuntime().getFalse();
    }

    public static IRubyObject max(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject result = null;
        List arr = eachToList(self);

        if(block.isGiven()) {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if(result == null || (block.yield(context, context.getRuntime().newArray(item,result)).callMethod(context, ">", RubyFixnum.zero(context.getRuntime()))).isTrue()) {
                    result = item;
                }
            }
        } else {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if(result == null || item.callMethod(context,"<=>", result).callMethod(context, ">", RubyFixnum.zero(context.getRuntime())).isTrue()) {
                    result = item;
                }
            }
        }
        if(null == result) {
            return context.getRuntime().getNil();
        }
        return result;
    }

    public static IRubyObject min(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject result = null;
        List arr = eachToList(self);

        if(block.isGiven()) {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if(result == null || (block.yield(context, context.getRuntime().newArray(item,result)).callMethod(context, "<", RubyFixnum.zero(context.getRuntime()))).isTrue()) {
                    result = item;
                }
            }
        } else {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if(result == null || item.callMethod(context,"<=>", result).callMethod(context, "<", RubyFixnum.zero(context.getRuntime())).isTrue()) {
                    result = item;
                }
            }
        }
        if(null == result) {
            return context.getRuntime().getNil();
        }
        return result;
    }

    public static IRubyObject all_p(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        boolean all = true;
        List arr = eachToList(self);

        if (block.isGiven()) {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                if(!block.yield(context, (IRubyObject)iter.next()).isTrue()) {
                    all = false;
                    break;
                }
            }
        } else {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                if(!((IRubyObject)iter.next()).isTrue()) {
                    all = false;
                    break;
                }
            }
        }
        return all ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    public static IRubyObject any_p(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        boolean any = false;
        List arr = eachToList(self);

        if (block.isGiven()) {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                if(block.yield(context, (IRubyObject)iter.next()).isTrue()) {
                    any = true;
                    break;
                }
            }
        } else {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                if(((IRubyObject)iter.next()).isTrue()) {
                    any = true;
                    break;
                }
            }
        }
        return any ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    public static IRubyObject zip(IRubyObject self, IRubyObject[] args, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        int ix = 0;
        List zip = new ArrayList(arr.size());
        int aLen = args.length+1;
        if(block.isGiven()) {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject elem = (IRubyObject)iter.next();
                List array = new ArrayList(aLen);
                array.add(elem);
                for(int i=0,j=args.length;i<j;i++) {
                    array.add(args[i].callMethod(context,"[]", context.getRuntime().newFixnum(ix)));
                }
                block.yield(context, context.getRuntime().newArray(array));
                ix++;
            }
            return context.getRuntime().getNil();
        } else {
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject elem = (IRubyObject)iter.next();
                List array = new ArrayList(aLen);
                array.add(elem);
                for(int i=0,j=args.length;i<j;i++) {
                    array.add(args[i].callMethod(context,"[]", context.getRuntime().newFixnum(ix)));
                }
                zip.add(context.getRuntime().newArray(array));
                ix++;
            }
        }
        return context.getRuntime().newArray(zip);
    }

    public static IRubyObject group_by(IRubyObject self, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        List arr = eachToList(self);
        Map results = new HashMap(arr.size());
        for(Iterator iter = arr.iterator();iter.hasNext();) {
            IRubyObject item = (IRubyObject)iter.next();
            IRubyObject key = block.yield(context, item);
            IRubyObject curr = (IRubyObject)results.get(key);
            if(curr == null) {
                curr = context.getRuntime().newArray();
                results.put(key,curr);
            }
            curr.callMethod(context,"<<", item);
        }
        return new RubyHash(context.getRuntime(),results,context.getRuntime().getNil());
    }
}
