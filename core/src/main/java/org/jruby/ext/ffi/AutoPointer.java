
package org.jruby.ext.ffi;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.PhantomReferenceReaper;

import static org.jruby.runtime.Visibility.*;

@JRubyClass(name = "FFI::" + AutoPointer.AUTOPTR_CLASS_NAME, parent = "FFI::Pointer")
public class AutoPointer extends Pointer {
    static final String AUTOPTR_CLASS_NAME = "AutoPointer";
    
    /** Keep strong references to the Reaper until cleanup */
    private static final ConcurrentMap<ReaperGroup, Boolean> referenceSet = new ConcurrentHashMap<ReaperGroup, Boolean>();
    private static final ThreadLocal<Reference<ReaperGroup>> currentReaper = new ThreadLocal<Reference<ReaperGroup>>();
    
    private Pointer pointer;
    private Object referent;
    private transient volatile Reaper reaper;
    
    public static RubyClass createAutoPointerClass(Ruby runtime, RubyModule module) {
        RubyClass autoptrClass = module.defineClassUnder(AUTOPTR_CLASS_NAME,
                module.getClass("Pointer"),
                RubyInstanceConfig.REIFY_RUBY_CLASSES ? new ReifyingAllocator(AutoPointer.class) : AutoPointerAllocator.INSTANCE);
        autoptrClass.defineAnnotatedMethods(AutoPointer.class);
        autoptrClass.defineAnnotatedConstants(AutoPointer.class);
        autoptrClass.setReifiedClass(AutoPointer.class);
        autoptrClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof AutoPointer && super.isKindOf(obj, type);
            }
        };

        return autoptrClass;
    }

    private static final class AutoPointerAllocator implements ObjectAllocator {
        static final ObjectAllocator INSTANCE = new AutoPointerAllocator();

        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new AutoPointer(runtime, klazz);
        }

    }

    public AutoPointer(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz, runtime.getFFI().getNullMemoryIO());
    }
    
    private static final void checkPointer(Ruby runtime, IRubyObject ptr) {
        if (!(ptr instanceof Pointer)) {
            throw runtime.newTypeError(ptr, runtime.getFFI().pointerClass);
        }
        if (ptr instanceof MemoryPointer || ptr instanceof AutoPointer) {
            throw runtime.newTypeError("Cannot use AutoPointer with MemoryPointer or AutoPointer instances");
        }
    }

    @JRubyMethod(name="from_native", meta = true)
    public static IRubyObject from_native(ThreadContext context, IRubyObject recv, IRubyObject value, IRubyObject ctx) {
        return ((RubyClass) recv).newInstance(context, value, Block.NULL_BLOCK);
    }

    @Override
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject pointerArg) {

        Ruby runtime = context.runtime;

        checkPointer(runtime, pointerArg);

        Object ffiHandle = getMetaClass().getFFIHandle();
        if (!(ffiHandle instanceof ClassData)) {
            getMetaClass().setFFIHandle(ffiHandle = new ClassData());
        }
        ClassData classData = (ClassData) ffiHandle;

        // If no release method is defined, then memory leaks will result.
        DynamicMethod releaseMethod = classData.releaseCallSite.retrieveCache(getMetaClass().getMetaClass(), classData.releaseCallSite.getMethodName()).method;
        if (releaseMethod.isUndefined()) {
            throw runtime.newRuntimeError("release method undefined");

        } else if ((releaseMethod.getArity().isFixed() && releaseMethod.getArity().required() != 1) || releaseMethod.getArity().required() > 1) {
            throw runtime.newRuntimeError("wrong number of arguments to release method ("
                    + 1 + " for " + releaseMethod.getArity().required()  + ")");
        }


        setMemoryIO(((Pointer) pointerArg).getMemoryIO());
        this.pointer = (Pointer) pointerArg;
        this.size = pointer.size;
        this.typeSize = pointer.typeSize;
        setReaper(new Reaper(pointer, getMetaClass(), classData.releaseCallSite));

        return this;
    }

    @Override
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject pointerArg, IRubyObject releaser) {

        checkPointer(context.runtime, pointerArg);

        setMemoryIO(((Pointer) pointerArg).getMemoryIO());
        this.pointer = (Pointer) pointerArg;
        this.size = pointer.size;
        this.typeSize = pointer.typeSize;

        Object ffiHandle = releaser.getMetaClass().getFFIHandleAccessorField().getVariableAccessorForRead().get(releaser);
        if (!(ffiHandle instanceof ReleaserData)) {
            getMetaClass().setFFIHandle(ffiHandle = new ReleaserData());
        }

        ReleaserData releaserData = (ReleaserData) ffiHandle;
        DynamicMethod releaseMethod = releaserData.releaseCallSite.retrieveCache(releaser.getMetaClass(), releaserData.releaseCallSite.getMethodName()).method;
        // If no release method is defined, then memory leaks will result.
        if (releaseMethod.isUndefined()) {
            throw context.runtime.newRuntimeError("call method undefined");

        } else if ((releaseMethod.getArity().isFixed() && releaseMethod.getArity().required() != 1) || releaseMethod.getArity().required() > 1) {
            throw context.runtime.newRuntimeError("wrong number of arguments to call method ("
                    + 1 + " for " + releaseMethod.getArity().required()  + ")");
        }

        setReaper(new Reaper(pointer, releaser, releaserData.releaseCallSite));

        return this;
    }

    @JRubyMethod(name = "free")
    public final IRubyObject free(ThreadContext context) {
        Reaper r = reaper;

        if (r == null || r.released) {
            throw context.runtime.newRuntimeError("pointer already freed");
        }

        r.release(context);
        reaper = null;
        referent = null;

        return context.runtime.getNil();
    }

    @JRubyMethod(name = "autorelease=")
    public final IRubyObject autorelease(ThreadContext context, IRubyObject autorelease) {
        Reaper r = reaper;

        if (r == null || r.released) {
            throw context.runtime.newRuntimeError("pointer already freed");
        }

        r.autorelease(autorelease.isTrue());

        return context.runtime.getNil();
    }

    @JRubyMethod(name = "autorelease?")
    public final IRubyObject autorelease_p(ThreadContext context) {
        return context.runtime.newBoolean(reaper != null ? !reaper.unmanaged : false);
    }

    private void setReaper(Reaper reaper) {
        Reference<ReaperGroup> reaperGroupReference = currentReaper.get();
        ReaperGroup reaperGroup = reaperGroupReference != null ? reaperGroupReference.get() : null;
        Object referent = reaperGroup != null ? reaperGroup.referent() : null;
        if (referent == null || !reaperGroup.canAccept()) {
            reaperGroup = new ReaperGroup(referent = new Object());
            currentReaper.set(new SoftReference<ReaperGroup>(reaperGroup));
            referenceSet.put(reaperGroup, Boolean.TRUE);
        }
        this.referent = referent;
        this.reaper = reaper;
        reaperGroup.add(reaper);
    }

    private static final class ReaperGroup extends PhantomReferenceReaper<Object> implements Runnable {
        private static int MAX_REAPERS_PER_GROUP = 100;
        private final WeakReference<Object> weakref;
        private int reaperCount;
        private volatile Reaper head;
        
        ReaperGroup(Object referent) {
            super(referent);
            this.weakref = new WeakReference<Object>(referent);
        }
        
        Object referent() {
            return weakref.get();
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
        final CachingCallSite callSite;
        volatile Reaper next;
        volatile boolean released;
        volatile boolean unmanaged;


        private Reaper(Pointer ptr, IRubyObject proc, CachingCallSite callSite) {
            this.pointer = ptr;
            this.proc = proc;
            this.callSite = callSite;
        }
        
        final Ruby getRuntime() {
            return proc.getRuntime();
        }
        
        void dispose(ThreadContext context) {
            callSite.call(context, proc, proc, pointer);
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

    private static final class ClassData {
        private final CachingCallSite releaseCallSite = new FunctionalCachingCallSite("release");
    }

    private static final class ReleaserData {
        private final CachingCallSite releaseCallSite = new FunctionalCachingCallSite("call");
    }
}
