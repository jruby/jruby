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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyProc;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.runtime.MethodIndex;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Generator {
    public static class Service implements Library {
        public void load(final Ruby runtime) throws IOException {
            createGenerator(runtime);
        }
    }

    public static void createGenerator(Ruby runtime) throws IOException {
        RubyClass cGen = runtime.defineClass("Generator",runtime.getObject(), runtime.getObject().getAllocator());
        cGen.includeModule(runtime.getModule("Enumerable"));

        CallbackFactory callbackFactory = runtime.callbackFactory(Generator.class);

        cGen.getMetaClass().defineMethod("new",callbackFactory.getOptSingletonMethod("new_instance"));
        cGen.defineMethod("initialize",callbackFactory.getOptSingletonMethod("initialize"));
        cGen.defineMethod("yield",callbackFactory.getSingletonMethod("yield",IRubyObject.class));
        cGen.defineFastMethod("end?",callbackFactory.getFastSingletonMethod("end_p"));
        cGen.defineFastMethod("next?",callbackFactory.getFastSingletonMethod("next_p"));
        cGen.defineFastMethod("index",callbackFactory.getFastSingletonMethod("index"));
        cGen.defineAlias("pos","index");
        cGen.defineMethod("next",callbackFactory.getSingletonMethod("next"));
        cGen.defineMethod("current",callbackFactory.getSingletonMethod("current"));
        cGen.defineMethod("rewind",callbackFactory.getSingletonMethod("rewind"));
        cGen.defineMethod("each",callbackFactory.getSingletonMethod("each"));
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
            public IRubyObject call(ThreadContext context, IRubyObject[] iargs, Block block) {
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
                        obj = gen.getRuntime().newArrayNoCopy(iargs);
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
                enm.callMethod(context, "each", new CallBlock(enm,enm.getMetaClass().getRealClass(),Arity.noArguments(),ibc,context));
            } else {
                proc.call(new IRubyObject[]{gen});
            }
            end = true;
        }
    }

    public static IRubyObject new_instance(IRubyObject self, IRubyObject[] args, Block block) {
        // Generator#new
        IRubyObject result = new RubyObject(self.getRuntime(),(RubyClass)self);
        result.dataWrapStruct(new GeneratorData(result));
        result.callMethod(self.getRuntime().getCurrentContext(), "initialize", args, block);
        return result;
    }

    public static IRubyObject initialize(IRubyObject self, IRubyObject[] args, Block block) {
        // Generator#initialize
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        
        self.setInstanceVariable("@queue",self.getRuntime().newArray());
        self.setInstanceVariable("@index",self.getRuntime().newFixnum(0));
        
        if(Arity.checkArgumentCount(self.getRuntime(), args,0,1) == 1) {
            d.setEnum(args[0]);
        } else {
            d.setProc(self.getRuntime().newProc(false, Block.NULL_BLOCK));
        }
        return self;
    }

    public static IRubyObject yield(IRubyObject self, IRubyObject value, Block block) {
        // Generator#yield
        self.getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(),"<<",value);
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        d.doWait();
        return self;
    }

    public static IRubyObject end_p(IRubyObject self) {
        // Generator#end_p
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        return d.isEnd() ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
    }

    public static IRubyObject next_p(IRubyObject self) {
        // Generator#next_p
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        return !d.isEnd() ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
    }

    public static IRubyObject index(IRubyObject self) {
        // Generator#index
        return self.getInstanceVariable("@index");
    }

    public static IRubyObject next(IRubyObject self, Block block) {
        // Generator#next
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        if(d.isEnd()) {
            throw self.getRuntime().newEOFError();
        }
        d.generate();
        self.setInstanceVariable("@index",self.getInstanceVariable("@index").callMethod(self.getRuntime().getCurrentContext(),MethodIndex.OP_PLUS, "+",self.getRuntime().newFixnum(1)));
        return self.getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(),"shift");
    }

    public static IRubyObject current(IRubyObject self, Block block) {
            // Generator#current
        if(self.getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(),MethodIndex.EMPTY_P, "empty?").isTrue()) {
            throw self.getRuntime().newEOFError();
        }
        return self.getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(),"first");
    }

    public static IRubyObject rewind(IRubyObject self, Block block) {
        // Generator#rewind
        if(self.getInstanceVariable("@index").callMethod(self.getRuntime().getCurrentContext(),"nonzero?").isTrue()) {
            GeneratorData d = (GeneratorData)self.dataGetStruct();

            self.setInstanceVariable("@queue",self.getRuntime().newArray());
            self.setInstanceVariable("@index",self.getRuntime().newFixnum(0));
            
            d.start();
        }

        return self;
    }

    public static IRubyObject each(IRubyObject self, Block block) {
        // Generator#each
        rewind(self,Block.NULL_BLOCK);
        ThreadContext ctx = self.getRuntime().getCurrentContext();
        while(next_p(self).isTrue()) {
            block.yield(ctx, next(self, Block.NULL_BLOCK));
        }
        return self;
    }
}// Generator
