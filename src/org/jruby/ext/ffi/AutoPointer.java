
package org.jruby.ext.ffi;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.threading.DaemonThreadFactory;

@JRubyClass(name = "FFI::" + AutoPointer.CLASS_NAME, parent = "JRuby::FFI::AbstractMemoryPointer")
public class AutoPointer extends Pointer {
    public static final String CLASS_NAME = "AutoPointer";
    private final Pointer pointer;
    private final PointerHolder holder;

    public static RubyClass createAutoPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(CLASS_NAME,
                module.getClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(AutoPointer.class);
        result.defineAnnotatedConstants(AutoPointer.class);

        return result;
    }

    /**
     * Creates a new <tt>AutoPointer</tt> instance.
     * @param pointer - the pointer to free when this AutoPointer is garbage collected
     */
    private AutoPointer(Ruby runtime, Pointer pointer, IRubyObject proc) {
        super(runtime, runtime.fastGetModule("FFI").fastGetClass(CLASS_NAME),
                pointer.getMemoryIO(), pointer.getSize());
        this.pointer = pointer;
        holder = new PointerHolder(pointer, proc);
    }
    @JRubyMethod(name = "__alloc", meta = true)
    public static IRubyObject newAutoPointer(ThreadContext context, IRubyObject self, IRubyObject pointerArg, IRubyObject proc) {
        return new AutoPointer(context.getRuntime(), (Pointer) pointerArg, proc);
    }
    @Override
    protected AbstractMemory slice(Ruby runtime, long offset) {
        return pointer.slice(runtime, offset);
    }
    @Override
    protected Pointer getPointer(Ruby runtime, long offset) {
        return pointer.getPointer(runtime, offset);
    }
    
    private static final class PointerHolder {
        private static final Executor executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
        private final Pointer pointer;
        private final IRubyObject proc;
        private PointerHolder(Pointer pointer, IRubyObject proc) {
            this.pointer = pointer;
            this.proc = proc;
        }
        @Override
        protected void finalize() throws Exception {
            executor.execute(new Reaper(pointer, proc));
        }
    }
    private static final class Reaper implements Runnable {
        private final Pointer pointer;
        private final IRubyObject proc;
        private Reaper(Pointer pointer, IRubyObject proc) {
            this.pointer = pointer;
            this.proc = proc;
        }
        public void run() {
            try {
                proc.callMethod(pointer.getRuntime().getCurrentContext(), "call", pointer);
            } catch (Exception ex) {}
        }
    }
}
