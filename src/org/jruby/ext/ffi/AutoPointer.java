
package org.jruby.ext.ffi;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.WeakReferenceReaper;

import static org.jruby.runtime.Visibility.*;

@JRubyClass(name = "FFI::" + AutoPointer.AUTOPTR_CLASS_NAME, parent = "FFI::Pointer")
public final class AutoPointer extends Pointer {
    static final String AUTOPTR_CLASS_NAME = "AutoPointer";
    
    /** Keep strong references to the Reaper until cleanup */
    private static final ConcurrentMap<ReaperGroup, Boolean> referenceSet = new ConcurrentHashMap<ReaperGroup, Boolean>();
    private static final ThreadLocal<Reference<ReaperGroup>> currentReaper = new ThreadLocal<Reference<ReaperGroup>>();
    
    private Pointer pointer;
    private Object referent;
    private transient volatile Reaper reaper;
    
    public static RubyClass createAutoPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(AUTOPTR_CLASS_NAME,
                module.getClass("Pointer"),
                AutoPointerAllocator.INSTANCE);
        result.defineAnnotatedMethods(AutoPointer.class);
        result.defineAnnotatedConstants(AutoPointer.class);

        return result;
    }

    private static final class AutoPointerAllocator implements ObjectAllocator {
        static final ObjectAllocator INSTANCE = new AutoPointerAllocator();

        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new AutoPointer(runtime, klazz);
        }

    }

    private AutoPointer(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz, new NullMemoryIO(runtime));
    }
    
    private static final void checkPointer(Ruby runtime, IRubyObject ptr) {
        if (!(ptr instanceof Pointer)) {
            throw runtime.newTypeError(ptr, runtime.getModule("FFI").getClass("Pointer"));
        }
        if (ptr instanceof MemoryPointer || ptr instanceof AutoPointer) {
            throw runtime.newTypeError("Cannot use AutoPointer with MemoryPointer or AutoPointer instances");
        }
    }

    @JRubyMethod(name="from_native", meta = true)
    public static IRubyObject from_native(ThreadContext context, IRubyObject recv, IRubyObject value, IRubyObject ctx) {
        return ((RubyClass) recv).newInstance(context, new IRubyObject[] { value }, Block.NULL_BLOCK);
    }

    @Override
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject pointerArg) {

        Ruby runtime = context.getRuntime();

        checkPointer(runtime, pointerArg);

        // If no release method is defined, then memory leaks will result.
        if (!getMetaClass().respondsTo("release")) {
                throw runtime.newRuntimeError("No release method defined");
        }

        setMemoryIO(((Pointer) pointerArg).getMemoryIO());
        this.pointer = (Pointer) pointerArg;        
        setReaper(new Reaper(pointer, getMetaClass(), "release"));

        return this;
    }

    @Override
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject pointerArg, IRubyObject releaser) {

        checkPointer(context.getRuntime(), pointerArg);

        setMemoryIO(((Pointer) pointerArg).getMemoryIO());
        this.pointer = (Pointer) pointerArg;
        setReaper(new Reaper(pointer, releaser, "call"));

        return this;
    }

    @JRubyMethod(name = "free")
    public final IRubyObject free(ThreadContext context) {
        Reaper r = reaper;

        if (r == null || r.released) {
            throw context.getRuntime().newRuntimeError("pointer already freed");
        }

        r.release(context);
        reaper = null;
        referent = null;
        
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "autorelease=")
    public final IRubyObject autorelease(ThreadContext context, IRubyObject autorelease) {
        Reaper r = reaper;

        if (r == null || r.released) {
            throw context.getRuntime().newRuntimeError("pointer already freed");
        }

        r.autorelease(autorelease.isTrue());
        
        return context.getRuntime().getNil();
    }

    private void setReaper(Reaper reaper) {
        Reference<ReaperGroup> reaperGroupReference = currentReaper.get();
        ReaperGroup reaperGroup = reaperGroupReference != null ? reaperGroupReference.get() : null;
        Object referent = reaperGroup != null ? reaperGroup.get() : null;
        if (referent == null || !reaperGroup.canAccept()) {
            reaperGroup = new ReaperGroup(referent = new Object());
            currentReaper.set(new SoftReference<ReaperGroup>(reaperGroup));
            referenceSet.put(reaperGroup, Boolean.TRUE);
        }
        this.referent = referent;
        this.reaper = reaper;
        reaperGroup.add(reaper);
    }

    private static final class ReaperGroup extends WeakReferenceReaper<Object> implements Runnable {
        private static int MAX_REAPERS_PER_GROUP = 100;
        private int reaperCount;
        private volatile Reaper head;
        
        ReaperGroup(Object referent) {
            super(referent);
        }
        
        boolean canAccept() {
            return reaperCount < MAX_REAPERS_PER_GROUP;
        }
        
        void add(Reaper r) {
            ++reaperCount;
            r.next = head;
            head = r;
        }
        
        public void run() {
            referenceSet.remove(this);
            Ruby runtime = null;
            ThreadContext ctx = null;
            Reaper r = head;
            
            while (r != null) {
                if (!r.released && !r.unmanaged) {
                    if (r.getRuntime() != runtime) {
                        runtime = r.getRuntime();
                        ctx = runtime.getCurrentContext();
                    }
                    r.dispose(ctx);
                }
                r = r.next;
            }
        } 
    }

    private static final class Reaper {
        final Pointer pointer;
        final IRubyObject proc;
        final String methodName;
        volatile Reaper next;
        volatile boolean released;
        volatile boolean unmanaged;


        private Reaper(Pointer ptr, IRubyObject proc, String methodName) {
            this.pointer = ptr;
            this.proc = proc;
            this.methodName = methodName;
        }
        
        final Ruby getRuntime() {
            return proc.getRuntime();
        }
        
        void dispose(ThreadContext context) {
            proc.callMethod(context, methodName, pointer);
        }

        public final void release(ThreadContext context) {
            if (!released) {
                released = true;
                dispose(context);
            }
        }

        public final void autorelease(boolean autorelease) {
            this.unmanaged = !autorelease;
        }
    }
}
