
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

import static org.jruby.RubyModule.*;

@JRubyClass(name = "FFI::AutoPointer", parent = "FFI::Pointer")
public class AutoPointer extends Pointer {

    public static RubyClass createAutoPointerClass(ThreadContext context, RubyModule FFI, RubyClass Pointer) {
        ObjectAllocator allocator = Options.REIFY_FFI.load() ? new ReifyingAllocator(AutoPointer.class) : AutoPointer::new;
        return FFI.defineClassUnder(context, "AutoPointer", Pointer, allocator).
                reifiedClass(AutoPointer.class).
                kindOf(new KindOf() {
                    @Override
                    public boolean isKindOf(IRubyObject obj, RubyModule type) {
                        return obj instanceof AutoPointer && super.isKindOf(obj, type);
                    }
                });
    }

    public AutoPointer(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz, runtime.getFFI().getNullMemoryIO());
    }
}
