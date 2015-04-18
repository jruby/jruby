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
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

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

    /** Function object for lazily computing size (used for internally created enumerators) */
    private SizeFn sizeFn;
    
    private IRubyObject feedValue;

    public static void defineEnumerator(Ruby runtime) {
        RubyModule enm = runtime.getClassFromPath("Enumerable");
        
        final RubyClass enmr;
        enmr = runtime.defineClass("Enumerator", runtime.getObject(), ENUMERATOR_ALLOCATOR);

        enmr.includeModule(enm);
        enmr.defineAnnotatedMethods(RubyEnumerator.class);
        runtime.setEnumerator(enmr);

        RubyGenerator.createGeneratorClass(runtime);
        RubyYielder.createYielderClass(runtime);
    }

    private static ObjectAllocator ENUMERATOR_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyEnumerator(runtime, klass);
        }
    };

    private RubyEnumerator(Ruby runtime, RubyClass type) {
        super(runtime, type);
        object = runtime.getNil();
        initialize(runtime.getNil(), RubyString.newEmptyString(runtime), IRubyObject.NULL_ARRAY);
    }

    private RubyEnumerator(Ruby runtime, RubyClass type, IRubyObject object, IRubyObject method, IRubyObject[]args, IRubyObject size) {
        super(runtime, type);
        initialize20(object, method, args, size, null);
    }

    private RubyEnumerator(Ruby runtime, RubyClass type, IRubyObject object, IRubyObject method, IRubyObject[]args, SizeFn sizeFn) {
        super(runtime, type);
        initialize20(object, method, args, null, sizeFn);
    }

    private RubyEnumerator(Ruby runtime, RubyClass type, IRubyObject object, IRubyObject method, IRubyObject[]args) {
        super(runtime, type);
        initialize(object, method, args);
    }

    /**
     * Transform object into an Enumerator with the given size
     */
    public static IRubyObject enumeratorizeWithSize(ThreadContext context, final IRubyObject object, String method, IRubyObject[] args, SizeFn sizeFn) {
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), args, sizeFn);
    }

    public static IRubyObject enumeratorizeWithSize(ThreadContext context, IRubyObject object, String method, SizeFn sizeFn) {
        return enumeratorizeWithSize(context, object, method, NULL_ARRAY, sizeFn);
    }

    public static IRubyObject enumeratorizeWithSize(ThreadContext context, IRubyObject object, String method,IRubyObject arg, IRubyObject size) {
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), object, runtime.fastNewSymbol(method), new IRubyObject[] { arg }, size);
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

    @Override
    public IRubyObject initialize(ThreadContext context) {
        return initialize20(context, Block.NULL_BLOCK);
    }

    public IRubyObject initialize19(ThreadContext context, Block block) {
        return initialize20(context, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize20(ThreadContext context, Block block) {
        return initialize20(context, NULL_ARRAY, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize20(ThreadContext context, IRubyObject object, Block block) {
        return initialize20(context, new IRubyObject[]{ object }, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, rest = true)
    public IRubyObject initialize20(ThreadContext context, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject object;
        IRubyObject method = runtime.newSymbol("each");
        IRubyObject size = null;

        if (block.isGiven()) {
            Arity.checkArgumentCount(runtime, args, 0, 1);
            if (args.length > 0) {
                size = args[0];
                args = Arrays.copyOfRange(args, 1, args.length);

                if (!(size.isNil() || size.respondsTo("call")) &&
                        !(runtime.getFloat().isInstance(size) && ((RubyFloat)size).getDoubleValue() == Float.POSITIVE_INFINITY) &&
                        !(size instanceof RubyInteger)) {
                    throw runtime.newTypeError(size, runtime.getInteger());
                }
            }
            object = context.runtime.getGenerator().newInstance(context, IRubyObject.NULL_ARRAY, block);

        } else {
            Arity.checkArgumentCount(runtime, args, 1, -1);
            // TODO need a deprecation WARN here, but can't add it until ruby/jruby/kernel20/enumerable.rb is deleted or stops calling this without a block
            object = args[0];
            args = Arrays.copyOfRange(args, 1, args.length);
            if (args.length > 0) {
                method = args[0];
                args = Arrays.copyOfRange(args, 1, args.length);
            }
        }

        return initialize20(object, method, args, size, null);
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject object, IRubyObject method) {
        return initialize20(context, object, method, Block.NULL_BLOCK);
    }

    public IRubyObject initialize19(ThreadContext context, IRubyObject object, IRubyObject method, Block block) {
        return initialize20(context, object, method, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize20(ThreadContext context, IRubyObject object, IRubyObject method, Block block) {
        Ruby runtime = context.runtime;

        IRubyObject size = context.nil;

        if (block.isGiven()) {
            throw context.runtime.newArgumentError(2, 1);
        } else {
            return initialize(object, method, NULL_ARRAY);
        }
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg) {
        return initialize20(context, object, method, methodArg, Block.NULL_BLOCK);
    }

    public IRubyObject initialize19(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg, Block block) {
        return initialize20(context, object, method, methodArg, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize20(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg, Block block) {
        Ruby runtime = context.runtime;

        IRubyObject size = context.nil;

        if (block.isGiven()) {
            throw context.runtime.newArgumentError(3, 1);
        } else {
            return initialize(object, method, new IRubyObject[] { methodArg });
        }
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        return initialize20(context, args, Block.NULL_BLOCK);
    }

    public IRubyObject initialize19(ThreadContext context, IRubyObject[] args, Block block) {
        return initialize20(context, args, block);
    }

    private IRubyObject initialize(IRubyObject object, IRubyObject method, IRubyObject[] methodArgs) {
        return initialize20(object, method, methodArgs, null, null);
    }

    private IRubyObject initialize20(IRubyObject object, IRubyObject method, IRubyObject[] methodArgs, IRubyObject size, SizeFn sizeFn) {
        final Ruby runtime = getRuntime();
        this.object = object;
        this.method = method.asJavaString();
        this.methodArgs = methodArgs;
        this.size = size;
        this.sizeFn = sizeFn;
        this.feedValue = runtime.getNil();
        setInstanceVariable("@__object__", object);
        setInstanceVariable("@__method__", method);
        setInstanceVariable("@__args__", RubyArray.newArrayNoCopyLight(runtime, methodArgs));
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
        copy.size       = this.size;
        copy.sizeFn     = this.sizeFn;
        copy.feedValue  = getRuntime().getNil();
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

    @JRubyMethod(name = "inspect")
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
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), arg, runtime.newSymbol("each"), IRubyObject.NULL_ARRAY);
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), arg1, arg2, IRubyObject.NULL_ARRAY);
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        Ruby runtime = context.runtime;
        return new RubyEnumerator(runtime, runtime.getEnumerator(), arg1, arg2, new IRubyObject[]{arg3});
    }

    @JRubyMethod(required = 1)
    public IRubyObject each_with_object(final ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? RubyEnumerable.each_with_objectCommon19(context, this, block, arg) :
                enumeratorizeWithSize(context, this, "each_with_object", new IRubyObject[]{arg}, enumSizeFn(context));
    }

    @JRubyMethod
    public IRubyObject with_object(ThreadContext context, final IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_with_objectCommon19(context, this, block, arg) : enumeratorizeWithSize(context, this, "with_object", new IRubyObject[]{arg}, enumSizeFn(context));
    }

    @JRubyMethod(rest = true)
    public IRubyObject each_entry(ThreadContext context, final IRubyObject[] args, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_entryCommon(context, this, args, block) : enumeratorize(context.runtime, getType(), this, "each_entry", args);
    }

    @JRubyMethod(name = "each_slice")
    public IRubyObject each_slice19(ThreadContext context, IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_slice(context, this, arg, block) : enumeratorize(context.runtime, getType(), this, "each_slice", arg);
    }

    @JRubyMethod(name = "each_cons")
    public IRubyObject each_cons19(ThreadContext context, IRubyObject arg, final Block block) {
        return block.isGiven() ? RubyEnumerable.each_cons(context, this, arg, block) : enumeratorize(context.runtime, getType(), this, "each_cons", arg);
    }

    @JRubyMethod
    public IRubyObject size(ThreadContext context) {
        if (sizeFn != null) {
            return sizeFn.size(methodArgs);
        }

        if (size != null) {
            if (size.respondsTo("call")) {
                return size.callMethod(context, "call");
            }
            
            return size;
        }

        return context.nil;
    }

    private SizeFn enumSizeFn(final ThreadContext context) {
        final RubyEnumerator self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.size(context);
            }
        };
    }

    private IRubyObject with_index_common(ThreadContext context, final Block block, final String rubyMethodName, IRubyObject arg) {
        final Ruby runtime = context.runtime;
        int index = arg.isNil() ? 0 : RubyNumeric.num2int(arg);
        if (!block.isGiven()) {
            return arg.isNil() ? enumeratorizeWithSize(context, this, rubyMethodName, enumSizeFn(context)) :
                enumeratorizeWithSize(context, this, rubyMethodName, new IRubyObject[]{runtime.newFixnum(index)}, enumSizeFn(context));
        }

        return RubyEnumerable.callEach(runtime, context, this, new RubyEnumerable.EachWithIndex(context, block, index));
    }

    @JRubyMethod
    public IRubyObject each_with_index(ThreadContext context, final Block block) {
        return with_index_common(context, block, "each_with_index", context.runtime.getNil());
    }

    public IRubyObject with_index(ThreadContext context, final Block block) {
        return with_index19(context, block);
    }

    @JRubyMethod(name = "with_index")
    public IRubyObject with_index19(ThreadContext context, final Block block) {
        return with_index_common(context, block, "with_index", context.runtime.getNil());
    }

    @JRubyMethod(name = "with_index")
    public IRubyObject with_index19(ThreadContext context, IRubyObject arg, final Block block) {
        return with_index_common(context, block, "with_index", arg);
    }
    
    private volatile Nexter nexter = null;
    
    @JRubyMethod
    public synchronized IRubyObject next(ThreadContext context) {
        ensureNexter(context);
        if (!feedValue.isNil()) feedValue = context.nil;
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

    @JRubyMethod(name = "peek_values")
    public synchronized IRubyObject peekValues(ThreadContext context) {
        ensureNexter(context);

        return RubyArray.newArray(context.runtime, nexter.peek());
    }

    @JRubyMethod(name = "next_values")
    public synchronized IRubyObject nextValues(ThreadContext context) {
        ensureNexter(context);
        if (!feedValue.isNil()) feedValue = context.nil;
        return RubyArray.newArray(context.runtime, nexter.next());
    }

    @JRubyMethod
    public IRubyObject feed(ThreadContext context, IRubyObject val) {
        ensureNexter(context);
        if (!feedValue.isNil()) {
            throw context.runtime.newTypeError("feed value already set");
        }
        feedValue = val;
        nexter.setFeedValue(val);
        return context.nil;
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
    
    @Override
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

    /**
     * "Function" type for java-created enumerators with size.  Should be implemented so that calls to
     * SizeFn#size are kept in sync with the size of the created enum (i.e. if the object underlying an enumerator
     * changes, calls to SizeFn#size should reflect that change).
     */
    public interface SizeFn {
        IRubyObject size(IRubyObject[] args);
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

        private IRubyObject feedValue;
        
        public Nexter(Ruby runtime, IRubyObject object, String method, IRubyObject[] methodArgs) {
            this.object = object;
            this.method = method;
            this.methodArgs = methodArgs;
            this.runtime = runtime;
        }

        public void setFeedValue(IRubyObject feedValue) {
            this.feedValue = feedValue;
        }

        public IRubyObject getFeedValue() {
            return feedValue;
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

        /** Exception used for unrolling the iteration on terminate */
        private static class TerminateEnumeration extends RuntimeException implements Unrescuable {}
        
        public ThreadedNexter(Ruby runtime, IRubyObject object, String method, IRubyObject[] methodArgs) {
            super(runtime, object, method, methodArgs);
            setFeedValue(runtime.getNil());
        }

        @Override
        public synchronized IRubyObject next() {
            if (doneObject != null) {
                return returnValue(doneObject);
            }
            
            ensureStarted();
            
            return returnValue(take());
        }
        
        @Override
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
        
        @Override
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
            if (thread == null) future = runtime.getFiberExecutor().submit(this);
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
        
        @Override
        public void run() {
            if (die) return;
            
            thread = Thread.currentThread();
            ThreadContext context = runtime.getCurrentContext();
            
            if (DEBUG) System.out.println(Thread.currentThread().getName() + ": starting up nexter thread");
            
            IRubyObject finalObject = NEVER;
            
            try {
                IRubyObject oldExc = runtime.getGlobalVariables().get("$!"); // Save $!
                final TerminateEnumeration terminateEnumeration = new TerminateEnumeration();
                try {
                    object.callMethod(context, method, methodArgs, CallBlock.newCallClosure(object, object.getMetaClass(), Signature.OPTIONAL, new BlockCallback() {
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                            try {
                                if (DEBUG) System.out.println(Thread.currentThread().getName() + ": exchanging: " + Arrays.toString(args));
                                if (die) throw terminateEnumeration;
                                out.put(RubyEnumerable.packEnumValues(runtime, args));
                                if (die) throw terminateEnumeration;
                            } catch (InterruptedException ie) {
                                if (DEBUG) System.out.println(Thread.currentThread().getName() + ": interrupted");

                                throw terminateEnumeration;
                            }

                            IRubyObject feedValue = getFeedValue();
                            setFeedValue(context.nil);
                            return feedValue;
                        }
                    }, context));
                } catch (TerminateEnumeration te) {
                    if (te != terminateEnumeration) {
                        throw te;
                    }
                    // ignore, we're shutting down
                } catch (RaiseException re) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + ": exception at toplevel: " + re.getException());
                    finalObject = re.getException();
                    runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
                } catch (Throwable t) {
                    if (DEBUG) {
                        System.out.println(Thread.currentThread().getName() + ": exception at toplevel: " + t);
                        t.printStackTrace();
                    }
                    Helpers.throwException(t);
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
