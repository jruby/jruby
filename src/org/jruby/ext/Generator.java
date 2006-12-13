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
package org.jruby.ext;

import java.io.IOException;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyProc;

import org.jruby.runtime.Arity;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.internal.runtime.methods.MultiStub;
import org.jruby.internal.runtime.methods.MultiStubMethod;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Generator {
    public static class Service implements Library {
        public void load(final IRuby runtime) throws IOException {
            createGenerator(runtime);
        }
    }

    public static void createGenerator(IRuby runtime) throws IOException {
        RubyClass cGen = runtime.defineClass("Generator",runtime.getObject());
        cGen.includeModule(runtime.getModule("Enumerable"));

        GenStub0 gstub = new GenStub0();
        gstub.gen_new = new MultiStubMethod(gstub,0,cGen,Arity.optional(),Visibility.PUBLIC);
        gstub.gen_initialize = new MultiStubMethod(gstub,1,cGen,Arity.optional(),Visibility.PUBLIC);
        gstub.gen_yield = new MultiStubMethod(gstub,2,cGen,Arity.singleArgument(),Visibility.PUBLIC);
        gstub.gen_end_p = new MultiStubMethod(gstub,3,cGen,Arity.noArguments(),Visibility.PUBLIC);
        gstub.gen_next_p = new MultiStubMethod(gstub,4,cGen,Arity.noArguments(),Visibility.PUBLIC);
        gstub.gen_index = new MultiStubMethod(gstub,5,cGen,Arity.noArguments(),Visibility.PUBLIC);
        gstub.gen_next = new MultiStubMethod(gstub,6,cGen,Arity.noArguments(),Visibility.PUBLIC);
        gstub.gen_current = new MultiStubMethod(gstub,7,cGen,Arity.noArguments(),Visibility.PUBLIC);
        gstub.gen_rewind = new MultiStubMethod(gstub,8,cGen,Arity.noArguments(),Visibility.PUBLIC);
        gstub.gen_each = new MultiStubMethod(gstub,9,cGen,Arity.noArguments(),Visibility.PUBLIC);
       
        cGen.addSingletonMethod("new",gstub.gen_new);
        cGen.addMethod("initialize",gstub.gen_initialize);
        cGen.addMethod("yield",gstub.gen_yield);
        cGen.addMethod("end?",gstub.gen_end_p);
        cGen.addMethod("next?",gstub.gen_next_p);
        cGen.addMethod("index",gstub.gen_index);
        cGen.defineAlias("pos","index");
        cGen.addMethod("next",gstub.gen_next);
        cGen.addMethod("current",gstub.gen_current);
        cGen.addMethod("rewind",gstub.gen_rewind);
        cGen.addMethod("each",gstub.gen_each);
    }

    static class GeneratorData implements Runnable {
        private IRubyObject gen;
        private Object mutex = new Object();

        private IRubyObject enm;
        private RubyProc proc;

        private Thread t;
        private boolean end;
        private IterBlockCallback ibc;

        public GeneratorData(IRubyObject gen) {
            this.gen = gen;
        }

        public void setEnum(IRubyObject enm) {
            this.proc = null;
            this.enm = enm;
            start();
        }

        public void setProc(RubyProc proc) {
            this.proc = proc;
            this.enm = null;
            start();
        }

        public void start() {
            end = false;
            ibc = new IterBlockCallback();
            t = new Thread(this);
            t.setDaemon(true);
            t.start();
            generate();
        }

        public boolean isEnd() {
            return end;
        }

        private boolean available = false;

        public void doWait() {
            available = true;
            if(proc != null) {
                boolean inter = true;
                synchronized(mutex) {
                    mutex.notifyAll();
                    while(inter) {
                        try {
                            mutex.wait();
                            inter = false;
                        } catch(InterruptedException e) {
                        }
                    }
                }
            }
        }

        public void generate() {
            if(proc == null) {
                boolean inter = true;
                synchronized(mutex) {
                    while(!ibc.haveValue() && !end) {
                        mutex.notifyAll();
                        inter = true;
                        while(inter) {
                            try {
                                mutex.wait();
                                inter = false;
                            } catch(InterruptedException e) {
                            }
                        }
                    }
                    if(!end && proc == null) {
                        gen.callMethod(gen.getRuntime().getCurrentContext(),"yield",ibc.pop());
                    }
                }
            } else {
                synchronized(mutex) {
                    while(!available && !end) {
                        boolean inter = true;
                        mutex.notifyAll();
                        while(inter) {
                            try {
                                mutex.wait(20);
                                inter = false;
                            } catch(InterruptedException e) {
                            }
                        }
                    }
                    available = false;
                }
            }

        }

        private class IterBlockCallback implements BlockCallback {
            private IRubyObject obj;
            public IRubyObject call(IRubyObject[] iargs, IRubyObject iself) {
                boolean inter = true;
                synchronized(mutex) {
                    mutex.notifyAll();
                    while(inter) {
                        try {
                            mutex.wait();
                            inter = false;
                        } catch(InterruptedException e) {
                        }
                    }
                    if(iargs.length > 1) {
                        obj = gen.getRuntime().newArray(iargs);
                    } else {
                        obj = iargs[0];
                    }
                    mutex.notifyAll();
                    return gen.getRuntime().getNil();
                }
            }
            public boolean haveValue() {
                return obj != null;
            }
            public IRubyObject pop() {
                IRubyObject a = obj;
                obj = null;
                return a;
            }
        }

        public void run() {
            if(enm != null) {
                ThreadContext context = gen.getRuntime().getCurrentContext();
                Iter bef = context.getFrameIter();
                context.preBlockPassEval(new CallBlock(enm,enm.getMetaClass().getRealClass(),Arity.noArguments(),ibc,context));
                enm.callMethod(context, "each");
                context.postBlockPassEval();
                context.setFrameIter(bef);
                end = true;
            } else {
                proc.call(new IRubyObject[]{gen});
                end = true;
            }
        }
    }

    public static class GenStub0 implements MultiStub {
        public MultiStubMethod gen_new;
        public MultiStubMethod gen_initialize;
        public MultiStubMethod gen_yield;
        public MultiStubMethod gen_end_p;
        public MultiStubMethod gen_next_p;
        public MultiStubMethod gen_index;
        public MultiStubMethod gen_next;
        public MultiStubMethod gen_current;
        public MultiStubMethod gen_rewind;
        public MultiStubMethod gen_each;

        public IRubyObject method0(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#new
            IRubyObject result = new RubyObject(self.getRuntime(),(RubyClass)self);
            result.dataWrapStruct(new GeneratorData(result));
            result.callInit(args);
            return result;
        }

        public IRubyObject method1(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#initialize
            GeneratorData d = (GeneratorData)self.dataGetStruct();

            self.setInstanceVariable("@queue",self.getRuntime().newArray());
            self.setInstanceVariable("@index",self.getRuntime().newFixnum(0));

            if(self.checkArgumentCount(args,0,1) == 1) {
                d.setEnum(args[0]);
            } else {
                d.setProc(RubyProc.newProc(self.getRuntime(),false));
            }
            return self;
        }

        public IRubyObject method2(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#yield
            self.getInstanceVariable("@queue").callMethod(context,"<<",args[0]);
            GeneratorData d = (GeneratorData)self.dataGetStruct();
            d.doWait();
            return self;
        }

        public IRubyObject method3(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#end_p
            GeneratorData d = (GeneratorData)self.dataGetStruct();
            return d.isEnd() ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
        }

        public IRubyObject method4(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#next_p
            GeneratorData d = (GeneratorData)self.dataGetStruct();
            return !d.isEnd() ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
        }

        public IRubyObject method5(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#index
            return self.getInstanceVariable("@index");
        }

        public IRubyObject method6(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#next
            GeneratorData d = (GeneratorData)self.dataGetStruct();
            if(d.isEnd()) {
                throw self.getRuntime().newEOFError();
            }
            d.generate();
            self.setInstanceVariable("@index",self.getInstanceVariable("@index").callMethod(context,"+",self.getRuntime().newFixnum(1)));
            return self.getInstanceVariable("@queue").callMethod(context,"shift");
        }

        public IRubyObject method7(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#current
            if(self.getInstanceVariable("@queue").callMethod(context,"empty?").isTrue()) {
                throw self.getRuntime().newEOFError();
            }
            return self.getInstanceVariable("@queue").callMethod(context,"first");
        }

        public IRubyObject method8(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#rewind
            if(self.getInstanceVariable("@index").callMethod(context,"nonzero?").isTrue()) {
                GeneratorData d = (GeneratorData)self.dataGetStruct();

                self.setInstanceVariable("@queue",self.getRuntime().newArray());
                self.setInstanceVariable("@index",self.getRuntime().newFixnum(0));
            
                d.start();
            }

            return self;
        }

        public IRubyObject method9(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            // Generator#each
            self.callMethod(context,"rewind");
            while(self.callMethod(context,"next?").isTrue()) {
                context.yield(self.callMethod(context,"next"));
            }
            return self;
        }
    }
}// Generator
