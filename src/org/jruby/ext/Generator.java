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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.Visibility;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Generator", include="Enumerable")
public class Generator {
    public static class Service implements Library {
        public void load(final Ruby runtime, boolean wrap) throws IOException {
            createGenerator(runtime);
        }
    }

    public static void createGenerator(Ruby runtime) throws IOException {
        RubyClass cGen = runtime.defineClass("Generator",runtime.getObject(), runtime.getObject().getAllocator());
        cGen.includeModule(runtime.getEnumerable());
        cGen.defineAnnotatedMethods(Generator.class);
    }

    static class GeneratorData implements Runnable {
        private IRubyObject gen;
        private Object mutex = new Object();

        private IRubyObject enm;
        private RubyProc proc;

        private Thread t;
        private volatile boolean end;
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
            if (t != null) {
                // deal with previously started thread first
                t.interrupt();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }

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
                    if(ibc.haveValue() && proc == null) {
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
            private boolean shouldSkip = false;
            public IRubyObject call(ThreadContext context, IRubyObject[] iargs, Block block) {
                if (shouldSkip) {
                    // the thread was interrupted, this is a signal
                    // that we should not do any work, and exit the thread.
                    return gen.getRuntime().getNil();
                }
                boolean inter = true;
                synchronized(mutex) {
                    mutex.notifyAll();
                    while(inter) {
                        try {
                            mutex.wait();
                            inter = false;
                        } catch(InterruptedException e) {
                            shouldSkip = true;
                            return gen.getRuntime().getNil();
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
            ThreadContext context = gen.getRuntime().getCurrentContext();
            if(enm != null) {
                RuntimeHelpers.invoke(context, enm, "each", 
                        CallBlock.newCallClosure(enm,enm.getMetaClass().getRealClass(),Arity.noArguments(),ibc,context));
            } else {
                proc.call(context, new IRubyObject[]{gen});
            }
            end = true;
            synchronized(mutex) {
                mutex.notifyAll();
            }
        }
    }

    @JRubyMethod(name = "new", rest = true, frame = true, meta = true)
    public static IRubyObject new_instance(IRubyObject self, IRubyObject[] args, Block block) {
        // Generator#new
        IRubyObject result = new RubyObject(self.getRuntime(),(RubyClass)self);
        result.dataWrapStruct(new GeneratorData(result));
        result.callMethod(self.getRuntime().getCurrentContext(), "initialize", args, block);
        return result;
    }

    @JRubyMethod(optional = 1, frame = true, visibility = Visibility.PRIVATE)
    public static IRubyObject initialize(IRubyObject self, IRubyObject[] args, Block block) {
        // Generator#initialize
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        
        self.getInstanceVariables().setInstanceVariable("@queue",self.getRuntime().newArray());
        self.getInstanceVariables().setInstanceVariable("@index",self.getRuntime().newFixnum(0));
        
        if(Arity.checkArgumentCount(self.getRuntime(), args,0,1) == 1) {
            d.setEnum(args[0]);
        } else {
            d.setProc(self.getRuntime().newProc(Block.Type.PROC, block));
        }
        return self;
    }

    @JRubyMethod(frame = true)
    public static IRubyObject yield(IRubyObject self, IRubyObject value, Block block) {
        // Generator#yield
        self.getInstanceVariables().getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(),"<<",value);
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        d.doWait();
        return self;
    }

    @JRubyMethod(name = "end?")
    public static IRubyObject end_p(IRubyObject self) {
        // Generator#end_p
        GeneratorData d = (GeneratorData)self.dataGetStruct();
        
        boolean emptyQueue = self.getInstanceVariables().getInstanceVariable("@queue").callMethod(
                self.getRuntime().getCurrentContext(), "empty?").isTrue();
        
        return (d.isEnd() && emptyQueue) ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
    }

    @JRubyMethod(name = "next?")
    public static IRubyObject next_p(IRubyObject self) {
        // Generator#next_p        
        return RuntimeHelpers.negate(
                RuntimeHelpers.invoke(self.getRuntime().getCurrentContext(), self, "end?"),
                self.getRuntime());
    }

    @JRubyMethod(name = {"index", "pos"})
    public static IRubyObject index(IRubyObject self) {
        // Generator#index
        return self.getInstanceVariables().getInstanceVariable("@index");
    }

    @JRubyMethod(frame = true)
    public static IRubyObject next(IRubyObject self, Block block) {
        // Generator#next
        GeneratorData d = (GeneratorData)self.dataGetStruct();

        if(RuntimeHelpers.invoke(self.getRuntime().getCurrentContext(), self, "end?").isTrue()) {
            throw self.getRuntime().newEOFError("no more elements available");
        }

        d.generate();
        self.getInstanceVariables().setInstanceVariable("@index",self.getInstanceVariables().getInstanceVariable("@index").callMethod(self.getRuntime().getCurrentContext(), "+",self.getRuntime().newFixnum(1)));
        return self.getInstanceVariables().getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(),"shift");
    }

    @JRubyMethod(frame = true)
    public static IRubyObject current(IRubyObject self, Block block) {
        // Generator#current
        if(self.getInstanceVariables().getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(), "empty?").isTrue()) {
            throw self.getRuntime().newEOFError("no more elements available");
        }
        return self.getInstanceVariables().getInstanceVariable("@queue").callMethod(self.getRuntime().getCurrentContext(),"first");
    }

    @JRubyMethod(frame = true)
    public static IRubyObject rewind(IRubyObject self, Block block) {
        // Generator#rewind
        if(self.getInstanceVariables().getInstanceVariable("@index").callMethod(self.getRuntime().getCurrentContext(),"nonzero?").isTrue()) {
            GeneratorData d = (GeneratorData)self.dataGetStruct();

            self.getInstanceVariables().setInstanceVariable("@queue",self.getRuntime().newArray());
            self.getInstanceVariables().setInstanceVariable("@index",self.getRuntime().newFixnum(0));
            
            d.start();
        }

        return self;
    }

    @JRubyMethod(frame = true)
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
