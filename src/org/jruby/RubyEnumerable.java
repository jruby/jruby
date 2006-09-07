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
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.internal.runtime.methods.MultiStub;
import org.jruby.internal.runtime.methods.MultiStubMethod;

/**
 * The implementation of Ruby's Enumerable module.
 */
public class RubyEnumerable {
    private static class ListAddBlockCallback implements BlockCallback {
        private final List arr = new ArrayList();
        private final IRuby runtime;
        public ListAddBlockCallback(IRuby runtime) {
            this.runtime = runtime;
        }
        public IRubyObject call(IRubyObject[] iargs, IRubyObject iself) {
            if(iargs.length > 1) {
                arr.add(runtime.newArray(iargs));
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
            return RubyFixnum.fix2int((((IRubyObject[])o1)[0].callMethod("<=>",((IRubyObject[])o2)[0])));
        }
    }
    private static class RubyYieldComparator implements Comparator {
        private final ThreadContext tc;
        private final IRuby runtime;
        public RubyYieldComparator(ThreadContext tc) {
            this.tc = tc;
            this.runtime = tc.getRuntime();
        }
        public int compare(Object o1, Object o2) {
            return RubyFixnum.fix2int(tc.yield(runtime.newArray((IRubyObject)o1,(IRubyObject)o2)));
        }
    }

    public static IRubyObject callEach(ThreadContext tc, IRubyObject self, RubyModule module, BlockCallback bc) {
        Iter bef = tc.getFrameIter();
        tc.preBlockPassEval(new CallBlock(self,module,Arity.noArguments(),bc,tc));
        IRubyObject ret = self.callMethod("each");
        tc.postBlockPassEval();
        tc.setFrameIter(bef);
        return ret;
    }

    public static List eachToList(ThreadContext tc, IRubyObject self, RubyModule module) {
        ListAddBlockCallback ladc = new ListAddBlockCallback(tc.getRuntime());
        callEach(tc,self,module,ladc);
        return ladc.getList();
    }



    public static class RubyEnumerableStub0 implements MultiStub {
        public static RubyEnumerableStub0 createStub(final RubyModule recv) {
            return new RubyEnumerableStub0(recv);
        }
        public final MultiStubMethod to_a;
        public final MultiStubMethod sort;
        public final MultiStubMethod sort_by;
        public final MultiStubMethod grep;
        public final MultiStubMethod detect;
        public final MultiStubMethod select;
        public final MultiStubMethod reject;
        public final MultiStubMethod collect;
        public final MultiStubMethod inject;
        public final MultiStubMethod partition;

        private final RubyModule module;

        private RubyEnumerableStub0(final RubyModule recv) {
            this.module = recv;
            this.to_a = new MultiStubMethod(RubyEnumerableStub0.this,0,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.sort = new MultiStubMethod(RubyEnumerableStub0.this,1,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.sort_by = new MultiStubMethod(RubyEnumerableStub0.this,2,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.grep = new MultiStubMethod(RubyEnumerableStub0.this,3,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.detect = new MultiStubMethod(RubyEnumerableStub0.this,4,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.select = new MultiStubMethod(RubyEnumerableStub0.this,5,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.reject = new MultiStubMethod(RubyEnumerableStub0.this,6,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.collect = new MultiStubMethod(RubyEnumerableStub0.this,7,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.inject = new MultiStubMethod(RubyEnumerableStub0.this,8,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.partition = new MultiStubMethod(RubyEnumerableStub0.this,9,recv,Arity.noArguments(), Visibility.PUBLIC);
        }

        public IRubyObject method0(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //TO_A
            return tc.getRuntime().newArray(eachToList(tc,self,module));
        }
        public IRubyObject method1(final ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //SORT
            if(!tc.isBlockGiven()) {
                IRubyObject res = self.callMethod("to_a");
                return res.callMethod("sort");
            } else {
                final List arr = eachToList(tc,self,module);
                Collections.sort(arr, new RubyYieldComparator(tc));
                return tc.getRuntime().newArray(arr);
            }
        }
        public IRubyObject method2(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //SORT_BY
            List result = eachToList(tc,self,module);
            IRubyObject[][] secResult = new IRubyObject[result.size()][2];
            int ix = 0;
            for(Iterator iter = result.iterator();iter.hasNext();ix++) {
                IRubyObject iro = (IRubyObject)iter.next();
                secResult[ix][0] = tc.yield(iro);
                secResult[ix][1] = iro;
            }
            Arrays.sort(secResult, new RubyFirstArrayComparator());
            IRubyObject[] result2 = new IRubyObject[secResult.length];
            for(int i=0,j=result2.length;i<j;i++) {
                result2[i] = secResult[i][1];
            }
            return tc.getRuntime().newArray(result2);
        }
        public IRubyObject method3(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //GREP
            List arr = eachToList(tc,self,module);
            List result = new ArrayList();
            IRubyObject pattern = args[0];
            if(!tc.isBlockGiven()) {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject item = (IRubyObject)iter.next();
                    if(pattern.callMethod("===",item).isTrue()) {
                        result.add(item);
                    }
                }                
            } else {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject item = (IRubyObject)iter.next();
                    if(pattern.callMethod("===",item).isTrue()) {
                        result.add(tc.yield(item));
                    }
                }                
            }
            return tc.getRuntime().newArray(result);
        }
        public IRubyObject method4(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //DETECT
            List arr = eachToList(tc,self,module);
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject element = (IRubyObject)iter.next();
                if(tc.yield(element).isTrue()) {
                    return element;
                }                
            }
            if(args.length > 0 && args[0] instanceof RubyProc) {
                return ((RubyProc)args[0]).call(new IRubyObject[0]);
            }
            return tc.getRuntime().getNil();
        }
        public IRubyObject method5(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //SELECT
            List arr = eachToList(tc,self,module);
            List result = new ArrayList(arr.size());
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject element = (IRubyObject)iter.next();
                if(tc.yield(element).isTrue()) {
                    result.add(element);
                }                
            }
            return tc.getRuntime().newArray(result);
        }
        public IRubyObject method6(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //REJECT
            List arr = eachToList(tc,self,module);
            List result = new ArrayList(arr.size());
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject element = (IRubyObject)iter.next();
                if(!tc.yield(element).isTrue()) {
                    result.add(element);
                }                
            }
            return tc.getRuntime().newArray(result);
        }
        public IRubyObject method7(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //COLLECT
            List arr = eachToList(tc,self,module);
            IRubyObject[] result = new IRubyObject[arr.size()];
            if(tc.isBlockGiven()) {
                int i=0;
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    result[i++] = tc.yield((IRubyObject)iter.next());
                }
            } else {
                int i=0;
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    result[i++] = (IRubyObject)iter.next();
                }
            }
            return tc.getRuntime().newArray(result);
        }
        public IRubyObject method8(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //INJECT
            IRubyObject result = null;
            if(args.length > 0) {
                result = args[0];
            }
            List arr = eachToList(tc,self,module);
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if(result == null) {
                    result = item;
                } else {
                    result = tc.yield(tc.getRuntime().newArray(result,item));
                }
            }
            if(null == result) {
                return tc.getRuntime().getNil();
            }
            return result;
        }
        public IRubyObject method9(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //PARTITION
            List arr = eachToList(tc,self,module);
            List arr_true = new ArrayList(arr.size()/2);
            List arr_false = new ArrayList(arr.size()/2);
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                if(tc.yield(item).isTrue()) {
                    arr_true.add(item);
                } else {
                    arr_false.add(item);
                }
            }
            return tc.getRuntime().newArray(tc.getRuntime().newArray(arr_true),tc.getRuntime().newArray(arr_false));
        }
    }

    public static class RubyEnumerableStub1 implements MultiStub {
        public static RubyEnumerableStub1 createStub(final RubyModule recv) {
            return new RubyEnumerableStub1(recv);
        }
        public final MultiStubMethod each_with_index;
        public final MultiStubMethod include_p;
        public final MultiStubMethod max;
        public final MultiStubMethod min;
        public final MultiStubMethod all_p;
        public final MultiStubMethod any_p;
        public final MultiStubMethod zip;
        public final MultiStubMethod group_by;

        private final RubyModule module;

        private RubyEnumerableStub1(final RubyModule recv) {
            this.module = recv;
            this.each_with_index = new MultiStubMethod(RubyEnumerableStub1.this,0,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.include_p = new MultiStubMethod(RubyEnumerableStub1.this,1,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.max = new MultiStubMethod(RubyEnumerableStub1.this,2,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.min = new MultiStubMethod(RubyEnumerableStub1.this,3,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.all_p = new MultiStubMethod(RubyEnumerableStub1.this,4,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.any_p = new MultiStubMethod(RubyEnumerableStub1.this,5,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.zip = new MultiStubMethod(RubyEnumerableStub1.this,6,recv,Arity.noArguments(), Visibility.PUBLIC);
            this.group_by = new MultiStubMethod(RubyEnumerableStub1.this,7,recv,Arity.noArguments(), Visibility.PUBLIC);
        }

        public IRubyObject method0(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //EACH_WITH_INDEX
            int index = 0;
            List arr = eachToList(tc,self,module);
            IRuby rt = tc.getRuntime();
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                tc.yield(rt.newArray((IRubyObject)iter.next(),rt.newFixnum(index++)));
            }
            return self;
        }
        public IRubyObject method1(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //INCLUDE?
            List arr = eachToList(tc,self,module);
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                if(args[0].callMethod("==",(IRubyObject)iter.next()).isTrue()) {
                    return tc.getRuntime().getTrue();
                }
            }
            return tc.getRuntime().getFalse();
        }
        public IRubyObject method2(final ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //MAX
            IRubyObject result = null;
            List arr = eachToList(tc,self,module);

            if(tc.isBlockGiven()) {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject item = (IRubyObject)iter.next();
                    if(result == null || (tc.yield(tc.getRuntime().newArray(item,result)).callMethod(">", RubyFixnum.zero(tc.getRuntime()))).isTrue()) {
                        result = item;
                    }
                }
            } else {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject item = (IRubyObject)iter.next();
                    if(result == null || item.callMethod("<=>",result).callMethod(">", RubyFixnum.zero(tc.getRuntime())).isTrue()) {
                        result = item;
                    }
                }
            }
            if(null == result) {
                return tc.getRuntime().getNil();
            }
            return result;
        }
        public IRubyObject method3(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //MIN
            IRubyObject result = null;
            List arr = eachToList(tc,self,module);

            if(tc.isBlockGiven()) {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject item = (IRubyObject)iter.next();
                    if(result == null || (tc.yield(tc.getRuntime().newArray(item,result)).callMethod("<", RubyFixnum.zero(tc.getRuntime()))).isTrue()) {
                        result = item;
                    }
                }
            } else {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject item = (IRubyObject)iter.next();
                    if(result == null || item.callMethod("<=>",result).callMethod("<", RubyFixnum.zero(tc.getRuntime())).isTrue()) {
                        result = item;
                    }
                }
            }
            if(null == result) {
                return tc.getRuntime().getNil();
            }
            return result;
        }
        public IRubyObject method4(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //ALL?
            boolean all = true;
            List arr = eachToList(tc,self,module);

            if(tc.isBlockGiven()) {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    if(!tc.yield((IRubyObject)iter.next()).isTrue()) {
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
            return all ? tc.getRuntime().getTrue() : tc.getRuntime().getFalse();
        }
        public IRubyObject method5(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //ANY?
            boolean any = false;
            List arr = eachToList(tc,self,module);

            if(tc.isBlockGiven()) {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    if(tc.yield((IRubyObject)iter.next()).isTrue()) {
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
            return any ? tc.getRuntime().getTrue() : tc.getRuntime().getFalse();
        }
        public IRubyObject method6(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //ZIP
            List arr = eachToList(tc,self,module);
            int ix = 0;
            List zip = new ArrayList(arr.size());
            int aLen = args.length+1;
            if(tc.isBlockGiven()) {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject elem = (IRubyObject)iter.next();
                    List array = new ArrayList(aLen);
                    array.add(elem);
                    for(int i=0,j=args.length;i<j;i++) {
                        array.add(args[i].callMethod("[]",tc.getRuntime().newFixnum(ix)));
                    }
                    tc.yield(tc.getRuntime().newArray(array));
                    ix++;
                }
                return tc.getRuntime().getNil();
            } else {
                for(Iterator iter = arr.iterator();iter.hasNext();) {
                    IRubyObject elem = (IRubyObject)iter.next();
                    List array = new ArrayList(aLen);
                    array.add(elem);
                    for(int i=0,j=args.length;i<j;i++) {
                        array.add(args[i].callMethod("[]",tc.getRuntime().newFixnum(ix)));
                    }
                    zip.add(tc.getRuntime().newArray(array));
                    ix++;
                }
            }
            return tc.getRuntime().newArray(zip);
        }
        public IRubyObject method7(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            //GROUP_BY
            List arr = eachToList(tc,self,module);
            Map results = new HashMap(arr.size());
            for(Iterator iter = arr.iterator();iter.hasNext();) {
                IRubyObject item = (IRubyObject)iter.next();
                IRubyObject key = tc.yield(item);
                IRubyObject curr = (IRubyObject)results.get(key);
                if(curr == null) {
                    curr = tc.getRuntime().newArray();
                    results.put(key,curr);
                }
                curr.callMethod("<<",item);
            }
            return new RubyHash(tc.getRuntime(),results,tc.getRuntime().getNil());
        }
        public IRubyObject method8(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            return null;
        }
        public IRubyObject method9(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            return null;
        }
    }

    public static RubyModule createEnumerableModule(IRuby runtime) {
        RubyModule enm = runtime.defineModule("Enumerable");
        RubyEnumerableStub0 stub0 = RubyEnumerableStub0.createStub(enm);
        RubyEnumerableStub1 stub1 = RubyEnumerableStub1.createStub(enm);

        enm.addModuleFunction("to_a", stub0.to_a);
        enm.addModuleFunction("entries", stub0.to_a);
        enm.addModuleFunction("sort", stub0.sort);
        enm.addModuleFunction("sort_by", stub0.sort_by);
        enm.addModuleFunction("grep", stub0.grep);
        enm.addModuleFunction("detect", stub0.detect);
        enm.addModuleFunction("find", stub0.detect);
        enm.addModuleFunction("select", stub0.select);
        enm.addModuleFunction("find_all", stub0.select);
        enm.addModuleFunction("reject", stub0.reject);
        enm.addModuleFunction("collect", stub0.collect);
        enm.addModuleFunction("map", stub0.collect);
        enm.addModuleFunction("inject", stub0.inject);
        enm.addModuleFunction("partition", stub0.partition);

        enm.addModuleFunction("each_with_index", stub1.each_with_index);
        enm.addModuleFunction("include?", stub1.include_p);
        enm.addModuleFunction("member?", stub1.include_p);
        enm.addModuleFunction("max", stub1.max);
        enm.addModuleFunction("min", stub1.min);
        enm.addModuleFunction("all?", stub1.all_p);
        enm.addModuleFunction("any?", stub1.any_p);
        enm.addModuleFunction("zip", stub1.zip);
        enm.addModuleFunction("group_by", stub1.group_by);

        return enm;
    }
}