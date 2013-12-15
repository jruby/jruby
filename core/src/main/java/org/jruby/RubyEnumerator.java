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
 * Copyright (C) 2006 Michael Studman <me@michaelstudman.com>
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
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import static org.jruby.CompatVersion.RUBY1_8;
import static org.jruby.CompatVersion.RUBY1_9;
import static org.jruby.CompatVersion.RUBY2_0;
import static org.jruby.runtime.Visibility.PRIVATE;

/**
 * Implementation of Ruby's Enumerator module.
 */
@JRubyModule(name="Enumerable::Enumerator", include="Enumerable")
public class RubyEnumerator extends RubyObject {
    /** target for each operation */
    private IRubyObject object;

    /** method to invoke for each operation */
    private String method;

    /** args to each method */
    private IRubyObject[] methodArgs;
    
    /** A value or proc to provide the size of the Enumerator contents*/
    private IRubyObject size;

    public static void defineEnumerator(Ruby runtime) {
        RubyModule enm = runtime.getClassFromPath("Enumerable");
        
        final RubyClass enmr;
        if (runtime.is1_9() || runtime.is2_0()) {
            enmr = runtime.defineClass("Enumerator", runtime.getObject(), ENUMERATOR_ALLOCATOR);
        } else {
            enmr = enm.defineClassUnder("Enumerator", runtime.getObject(), ENUMERATOR_ALLOCATOR);
        }

        enmr.includeModule(enm);
        enmr.defineAnnotatedMethods(RubyEnumerator.class);
        runtime.setEnumerator(enmr);

        RubyYielder.createYielderClass(runtime);
    }

    private static ObjectAllocator ENUMERATOR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyEnumerator(runtime, klass);
        }
    };

    private RubyEnumerator(Ruby runtime, RubyClass type) {
        super(runtime, type);
        object = runtime.getNil();
        initialize(runtime.getNil(), RubyString.newEmptyString(runtime), IRubyObject.NULL_ARRAY);
    }

    private RubyEnumerator(Ruby runtime, RubyClass type, IRubyObject object, IRubyObject method, IRubyObject[]args) {
        super(runtime, type);
        initialize(object, method, args);
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method) {
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), IRubyObject.NULL_ARRAY);
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method, IRubyObject arg) {
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), new IRubyObject[] {arg});
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method, IRubyObject[] args) {
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), args); // TODO: make sure it's really safe to not to copy it
    }

    public static IRubyObject enumeratorize(Ruby runtime, RubyClass type, IRubyObject object, String method) {
        return new RubyEnumerator(runtime, type, object, runtime.fastNewSymbol(method), IRubyObject.NULL_ARRAY);
    }

    public static IRubyObject enumeratorize(Ruby runtime, RubyClass type, IRubyObject object, String method, IRubyObject arg) {
        return new RubyEnumerator(runtime, type, object, runtime.fastNewSymbol(method), new IRubyObject[] {arg});
    }

    public static IRubyObject enumeratorize(Ruby runtime, RubyClass type, IRubyObject object, String method, IRubyObject[] args) {
        return new RubyEnumerator(runtime, type, object, runtime.fastNewSymbol(method), args); // TODO: make sure it's really safe to not to copy it
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context) {
        throw context.runtime.newArgumentError(0, 1);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, Block block) {
        if(!block.isGiven()) {
            throw context.runtime.newArgumentError(0, 1);
        }

        // TODO: avoid double lookup
        IRubyObject obj = context.runtime.getModule("JRuby").getClass("Generator").callMethod(context, "new", new IRubyObject[0], block);
        return initialize19(context, obj, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY2_0)
    public IRubyObject initialize20(ThreadContext context, Block block) {
        return initialize19(context, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject object) {
        return initialize(object, context.runtime.fastNewSymbol("each"), NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject object, Block block) {
        return initialize(object, context.runtime.fastNewSymbol("each"), NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY2_0)
    public IRubyObject initialize20(ThreadContext context, IRubyObject object, Block block) {
        Ruby runtime = context.runtime;
        RubySymbol each = runtime.newSymbol("each");
        
        // check for size
        if ((object.isNil() || runtime.getProc().isInstance(object)) ||
                runtime.getFloat().isInstance(object) && ((RubyFloat)object).getDoubleValue() == Float.POSITIVE_INFINITY) {
            // object is nil, a proc, or infinity; use it for size
            IRubyObject gen = context.runtime.getModule("JRuby").getClass("Generator").callMethod(context, "new", new IRubyObject[0], block);
            return initialize20(gen, each, NULL_ARRAY, object);
        }
        return initialize(object, each, NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject object, IRubyObject method) {
        return initialize(object, method, NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject object, IRubyObject method, Block block) {
        return initialize(object, method, NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY2_0)
    public IRubyObject initialize20(ThreadContext context, IRubyObject object, IRubyObject method, Block block) {
        return initialize(object, method, NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg) {
        return initialize(object, method, new IRubyObject[] { methodArg });
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg, Block block) {
        return initialize(object, method, new IRubyObject[] { methodArg });
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY2_0)
    public IRubyObject initialize20(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg, Block block) {
        return initialize(object, method, new IRubyObject[] { methodArg });
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0: return initialize(context);
            case 1: return initialize(context, args[0]);
            case 2: return initialize(context, args[0], args[1]);
        }

        IRubyObject[] methArgs = new IRubyObject[args.length - 2];
        System.arraycopy(args, 2, methArgs, 0, methArgs.length);
        return initialize(args[0], args[1], methArgs);
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return initialize19(context, block);
            case 1: return initialize19(context, args[0], block);
            case 2: return initialize19(context, args[0], args[1], block);
        }

        IRubyObject[] methArgs = new IRubyObject[args.length - 2];
        System.arraycopy(args, 2, methArgs, 0, methArgs.length);
        return initialize(args[0], args[1], methArgs);
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY2_0)
    public IRubyObject initialize20(ThreadContext context, IRubyObject[] args, Block block) {
        return initialize19(context, args, block);
    }

    private IRubyObject initialize(IRubyObject object, IRubyObject method, IRubyObject[] methodArgs) {
        this.object = object;
        this.method = method.asJavaString();
        this.methodArgs = methodArgs;
        setInstanceVariable("@__object__", object);
        setInstanceVariable("@__method__", method);
        setInstanceVariable("@__args__", RubyArray.newArrayNoCopyLight(getRuntime(), methodArgs));
        return this;
    }

    private IRubyObject initialize20(IRubyObject object, IRubyObject method, IRubyObject[] methodArgs, IRubyObject size) {
        this.object = object;
        this.method = method.asJavaString();
        this.methodArgs = methodArgs;
        this.size = size;
        setInstanceVariable("@__object__", object);
        setInstanceVariable("@__method__", method);
        setInstanceVariable("@__args__", RubyArray.newArrayNoCopyLight(getRuntime(), methodArgs));
        return this;
    }

    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        // JRUBY-5013: Enumerator needs to copy private fields in order to have a valid structure
        RubyEnumerator copy = (RubyEnumerator) super.dup();
        copy.object     = this.object;
        copy.method     = this.method;
        copy.methodArgs = this.methodArgs;
        return copy;
    }

    /**
     * Send current block and supplied args to method on target. According to MRI
     * Block may not be given and "each" should just ignore it and call on through to
     * underlying method.
     */
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        return object.callMethod(context, method, methodArgs, block);
    }
    
    @JRubyMethod(rest = true)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        if (args.length == 0) {
            return each(context, block);
        }
        
        IRubyObject[] newArgs = new IRubyObject[methodArgs.length + args.length];
        System.arraycopy(methodArgs, 0, newArgs, 0, methodArgs.length);
        System.arraycopy(args, 0, newArgs, methodArgs.length, args.length);
        
        return new RubyEnumerator(context.runtime, getType(), object, context.runtime.newSymbol("each"), newArgs);
    }

    @JRubyMethod(name = "inspect", compat = RUBY1_9)
    public IRubyObject inspect19(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isInspecting(this)) return inspect(context, true);

        try {
            runtime.registerInspecting(this);
            return inspect(context, false);
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    private IRubyObject inspect(ThreadContext context, boolean recurse) {
        Ruby runtime = context.runtime;
        ByteList bytes = new ByteList();
        bytes.append((byte)'#').append((byte)'<');
        bytes.append(getMetaClass().getName().getBytes());
        bytes.append((byte)':').append((byte)' ');

        if (recurse) {
            bytes.append("...>".getBytes());
            return RubyString.newStringNoCopy(runtime, bytes).taint(context);
        } else {
            boolean tainted = isTaint();
            bytes.append(RubyObject.inspect(context, object).getByteList());
            bytes.append((byte)':');
            bytes.append(method.getBytes());
            if (methodArgs.length > 0) {
                bytes.append((byte)'(');
                for (int i= 0; i < methodArgs.length; i++) {
                    bytes.append(RubyObject.inspect(context, methodArgs[i]).getByteList());
                    if (i < methodArgs.length - 1) {
                        bytes.append((byte)',').append((byte)' ');
                    } else {
                        bytes.append((byte)')');
                    }
                    if (methodArgs[i].isTaint()) tainted = true;
                }
            }
            bytes.append((byte)'>');
            RubyString result = RubyString.newStringNoCopy(runtime, bytes);
            if (tainted) result.setTaint(true);
            return result;
        }
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg) {
        return context.runtime.getEnumerator().callMethod(context, "new", arg);
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return Helpers.invoke(context, context.runtime.getEnumerator(), "new", arg1, arg2);
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return Helpers.invoke(context, context.runtime.getEnumerator(), "new", arg1, arg2, arg3);
    }

    @JRubyMethod(required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject each_with_object(ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? RubyEnumerable.each_with_objectCommon19(context, this, block, arg) : enumeratorize(context.runtime, getType(), this, "each_with_object", arg);
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject with_object(ThreadContext context, final IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_with_objectCommon19(context, this, block, arg) : enumeratorize(context.runtime, getType(), this, "with_object", arg);
    }

    @JRubyMethod(rest = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject each_entry(ThreadContext context, final IRubyObject[] args, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_entryCommon(context, this, args, block) : enumeratorize(context.runtime, getType(), this, "each_entry", args);
    }

    @JRubyMethod(name = "each_slice")
    public IRubyObject each_slice19(ThreadContext context, IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_slice(context, this, arg, block) : enumeratorize(context.runtime, getType(), this, "each_slice", arg);
    }

    @JRubyMethod(name = "enum_slice", compat = RUBY1_8)
    public IRubyObject enum_slice(ThreadContext context, IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_slice(context, this, arg, block) : enumeratorize(context.runtime, getType(), this, "enum_slice", arg);
    }

    @JRubyMethod(name = "each_cons")
    public IRubyObject each_cons19(ThreadContext context, IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_cons(context, this, arg, block) : enumeratorize(context.runtime, getType(), this, "each_cons", arg);
    }

    @JRubyMethod(name = "enum_cons", compat = RUBY1_8)
    public IRubyObject enum_cons(ThreadContext context, IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_cons(context, this, arg, block) : enumeratorize(context.runtime, getType(), this, "enum_cons", arg);
    }
    
    @JRubyMethod(compat = RUBY2_0)
    public IRubyObject size(ThreadContext context) {
        if (size != null) {
            if (context.runtime.getProc().isInstance(size)) {
                return ((RubyProc)size).call(context, NULL_ARRAY);
            }
            
            return size;
        }
        
        return context.nil;
    }

    private static IRubyObject with_index_common(ThreadContext context, IRubyObject self, 
            final Block block, final String rubyMethodName, IRubyObject arg) {
        final Ruby runtime = context.runtime;
        int index = arg.isNil() ? 0 : RubyNumeric.num2int(arg);
        if (!block.isGiven()) {
            return arg.isNil() ? enumeratorize(runtime, self.getType(), self, rubyMethodName) :
                enumeratorize(runtime, self.getType(), self , rubyMethodName, runtime.newFixnum(index));
        }

        return RubyEnumerable.callEach(runtime, context, self, new RubyEnumerable.EachWithIndex(context, block, index));
    }

    @JRubyMethod
    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, final Block block) {
        return with_index_common(context, self, block, "each_with_index", context.runtime.getNil());
    }

    @JRubyMethod(compat = RUBY1_8)
    public static IRubyObject with_index(ThreadContext context, IRubyObject self, final Block block) {
        return with_index_common(context, self, block, "with_index", context.runtime.getNil());
    }

    @JRubyMethod(name = "with_index", compat = RUBY1_9)
    public static IRubyObject with_index19(ThreadContext context, IRubyObject self, final Block block) {
        return with_index_common(context, self, block, "with_index", context.runtime.getNil());
    }

    @JRubyMethod(name = "with_index", compat = RUBY1_9)
    public static IRubyObject with_index19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return with_index_common(context, self, block, "with_index", arg);
    }
    
    private volatile Nexter nexter = null;
    
    @JRubyMethod
    public synchronized IRubyObject next(ThreadContext context) {
        ensureNexter(context);
        
        return nexter.next();
    }
    
    @JRubyMethod
    public synchronized IRubyObject rewind(ThreadContext context) {
        if (object.respondsTo("rewind")) object.callMethod(context, "rewind");
        
        if (nexter != null) {
            nexter.shutdown();
            nexter = null;
        }
        
        return this;
    }
    
    @JRubyMethod
    public synchronized IRubyObject peek(ThreadContext context) {
        ensureNexter(context);
        
        return nexter.peek();
    }
    
    private void ensureNexter(ThreadContext context) {
        if (nexter == null) {
            if (Options.ENUMERATOR_LIGHTWEIGHT.load()) {
                if (object instanceof RubyArray && method.equals("each") && methodArgs.length == 0) {
                    nexter = new ArrayNexter(context.runtime, object, method, methodArgs);
                } else {
                    nexter = new ThreadedNexter(context.runtime, object, method, methodArgs);
                }
            } else {
                nexter = new ThreadedNexter(context.runtime, object, method, methodArgs);
            }
        }
    }
    
    protected void finalize() throws Throwable {
        try {
            Nexter nexter = this.nexter;
            if (nexter != null) {
                nexter.shutdown();
                nexter = null;
            }
        } finally {
            super.finalize();
        }
    }
    
    private static abstract class Nexter {
        /** the runtime associated with all objects */
        protected final Ruby runtime;
        
        /** target for each operation */
        protected final IRubyObject object;

        /** method to invoke for each operation */
        protected final String method;

        /** args to each method */
        protected final IRubyObject[] methodArgs;
        
        public Nexter(Ruby runtime, IRubyObject object, String method, IRubyObject[] methodArgs) {
            this.object = object;
            this.method = method;
            this.methodArgs = methodArgs;
            this.runtime = runtime;
        }
        
        public abstract IRubyObject next();
        
        public abstract void shutdown();
        
        public abstract IRubyObject peek();
    }
    
    private static class ArrayNexter extends Nexter {
        private final RubyArray array;
        private int index = 0;
        
        public ArrayNexter(Ruby runtime, IRubyObject object, String method, IRubyObject[] methodArgs) {
            super(runtime, object, method, methodArgs);
            array = (RubyArray)object;
        }

        @Override
        public IRubyObject next() {
            IRubyObject obj = peek();
            index += 1;
            return obj;
        }

        @Override
        public void shutdown() {
            // not really anything to do
            index = 0;
        }

        @Override
        public IRubyObject peek() {
            checkIndex();
            
            return get();
        }
        
        protected IRubyObject get() {
            return array.eltOk(index);
        }

        private void checkIndex() throws RaiseException {
            if (index >= array.size()) throw runtime.newLightweightStopIterationError("stop iteration");
        }
    }
    
    private static class ThreadedNexter extends Nexter implements Runnable {
        private static final boolean DEBUG = false;
        
        /** sync queue to wait for values */
        private SynchronousQueue<IRubyObject> out = new SynchronousQueue<IRubyObject>();
        
        /** thread that's executing this Nexter */
        private volatile Thread thread;
        
        /** whether we're done iterating */
        private IRubyObject doneObject;
        
        /** future to cancel job if it has not started */
        private Future future;
        
        /** death mark */
        protected volatile boolean die = false;

        /** the last value we got, used for peek */
        private IRubyObject lastValue;
        
        public ThreadedNexter(Ruby runtime, IRubyObject object, String method, IRubyObject[] methodArgs) {
            super(runtime, object, method, methodArgs);
        }
        
        public synchronized IRubyObject next() {
            if (doneObject != null) {
                return returnValue(doneObject);
            }
            
            ensureStarted();
            
            return returnValue(take());
        }
        
        public synchronized void shutdown() {
            // cancel future in case we have not been started
            future.cancel(true);
                
            // mark for death
            die = true;
            
            Thread myThread = thread;
            if (myThread != null) {
                if (DEBUG) System.out.println("clearing for shutdown");
                
                // we interrupt twice, to break out of iteration and
                // (potentially) break out of final exchange
                myThread.interrupt();
                myThread.interrupt();
                
                // release references
                thread = null;
                doneObject = null;
            }
        }
        
        public synchronized IRubyObject peek() {
            if (doneObject != null) {
                return returnValue(doneObject);
            }
            
            ensureStarted();
            
            if (lastValue != null) {
                return lastValue;
            }
            
            peekTake();
            
            return returnValue(lastValue);
        }
        
        private void ensureStarted() {
            if (thread == null) future = runtime.getExecutor().submit(this);
        }
        
        private IRubyObject peekTake() {
            try {
                return lastValue = out.take();
            } catch (InterruptedException ie) {
                throw runtime.newThreadError("interrupted during iteration");
            }
        }
        
        private IRubyObject take() {
            try {
                if (lastValue != null) {
                    return lastValue;
                }
                
                return out.take();
            } catch (InterruptedException ie) {
                throw runtime.newThreadError("interrupted during iteration");
            } finally {
                lastValue = null;
            }
        }
        
        private IRubyObject returnValue(IRubyObject value) {
            // if it's the NEVER object, raise StopIteration
            if (value == NEVER) {
                doneObject = value;
                throw runtime.newLightweightStopIterationError("stop iteration");
            }
            
            // if it's an exception, raise it
            if (value instanceof RubyException) {
                doneObject = value;
                throw new RaiseException((RubyException)value);
            }
            
            // otherwise, just return it
            return value;
        }
        
        public void run() {
            if (die) return;
            
            thread = Thread.currentThread();
            ThreadContext context = runtime.getCurrentContext();
            
            if (DEBUG) System.out.println(Thread.currentThread().getName() + ": starting up nexter thread");
            
            IRubyObject finalObject = NEVER;
            
            try {
                try {
                    object.callMethod(context, method, methodArgs, CallBlock.newCallClosure(object, object.getMetaClass(), Arity.OPTIONAL, new BlockCallback() {
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                            try {
                                if (DEBUG) System.out.println(Thread.currentThread().getName() + ": exchanging: " + args[0]);
                                if (die) throw new JumpException.BreakJump(-1, NEVER);
                                out.put(RubyEnumerable.packEnumValues(runtime, args));
                                if (die) throw new JumpException.BreakJump(-1, NEVER);
                            } catch (InterruptedException ie) {
                                if (DEBUG) System.out.println(Thread.currentThread().getName() + ": interrupted");

                                throw new JumpException.BreakJump(-1, NEVER);
                            }

                            return context.nil;
                        }
                    }, context));
                } catch (JumpException.BreakJump bj) {
                    // ignore, we're shutting down
                } catch (RaiseException re) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + ": exception at toplevel: " + re.getException());
                    finalObject = re.getException();
                }

                try {
                    if (!die) out.put(finalObject);
                } catch (InterruptedException ie) {
                    // ignore
                }
            } finally {
                // disassociate this Nexter with the thread running it
                thread = null;
            }
        }
    }
}
