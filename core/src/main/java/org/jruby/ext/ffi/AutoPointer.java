
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

@JRubyClass(name = "FFI::AutoPointer", parent = "FFI::Pointer")
public class AutoPointer extends Pointer {

    public static RubyClass createAutoPointerClass(Ruby runtime, RubyModule module) {
        RubyClass autoptrClass = module.defineClassUnder("AutoPointer",
                module.getClass("Pointer"),
                Options.REIFY_FFI.load() ? new ReifyingAllocator(AutoPointer.class) : AutoPointer::new);
        autoptrClass.setReifiedClass(AutoPointer.class);
        autoptrClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof AutoPointer && super.isKindOf(obj, type);
            }
        };

        return autoptrClass;
    }

    public AutoPointer(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz, runtime.getFFI().getNullMemoryIO());
    }
}
