
package org.jruby.ext.ffi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ReferenceReaper;

@JRubyClass(name = "FFI::" + AutoPointer.AUTOPTR_CLASS_NAME, parent = "FFI::Pointer")
public final class AutoPointer extends Pointer {
    static final String AUTOPTR_CLASS_NAME = "AutoPointer";
    
    /** Keep strong references to the Reaper until cleanup */
    private static final Map<Reaper, Boolean> referenceSet = new ConcurrentHashMap<Reaper, Boolean>();
    
    private Pointer pointer;
    
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
            throw runtime.newTypeError(ptr, runtime.fastGetModule("FFI").fastGetClass("Pointer"));
        }
        if (ptr instanceof MemoryPointer || ptr instanceof AutoPointer) {
            throw runtime.newTypeError("Cannot use AutoPointer with MemoryPointer or AutoPointer instances");
        }
    }

    @Override
    @JRubyMethod(name = "initialize")
    public final IRubyObject initialize(ThreadContext context, IRubyObject pointerArg) {

        Ruby runtime = context.getRuntime();

        checkPointer(runtime, pointerArg);

        // If no release method is defined, then memory leaks will result.
        if (!getMetaClass().respondsTo("release")) {
                throw runtime.newRuntimeError("No release method defined");
        }

        setMemoryIO(((Pointer) pointerArg).getMemoryIO());
        this.pointer = (Pointer) pointerArg;
        referenceSet.put(new Reaper(this, pointer, getMetaClass(), "release"), Boolean.TRUE);

        return this;
    }

    @Override
    @JRubyMethod(name = "initialize")
    public final IRubyObject initialize(ThreadContext context, IRubyObject pointerArg, IRubyObject releaser) {

        checkPointer(context.getRuntime(), pointerArg);

        setMemoryIO(((Pointer) pointerArg).getMemoryIO());
        this.pointer = (Pointer) pointerArg;
        referenceSet.put(new Reaper(this, pointer, releaser, "call"), Boolean.TRUE);

        return this;
    }

    private static final class Reaper extends ReferenceReaper.Phantom<AutoPointer> implements Runnable {
        private final Pointer pointer;
        private final IRubyObject proc;
        private final String methodName;

        private Reaper(AutoPointer pointer, Pointer ptr, IRubyObject proc, String methodName) {
            super(pointer);
            this.pointer = ptr;
            this.proc = proc;
            this.methodName = methodName;
        }

        public void run() {
            referenceSet.remove(this);
            proc.callMethod(pointer.getRuntime().getCurrentContext(), methodName, pointer);
        }
    }
}
