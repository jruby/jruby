package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;

abstract public class MemoryObject extends RubyObject {
    /** The Memory I/O object */
    private MemoryIO memory;

    protected MemoryObject(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    /**
     * Gets the memory I/O accessor to read/write to the memory area.
     *
     * @return A memory accessor.
     */
    public final MemoryIO getMemoryIO() {
        return memory != null ? memory : initMemoryIO();
    }

    /**
     * Replaces the native memory object backing this ruby memory object
     *
     * @param memory The new memory I/O object
     * @return The old memory I/O object
     */
    protected final MemoryIO setMemoryIO(MemoryIO memory) {
        MemoryIO old = this.memory;
        this.memory = memory;

        return old;
    }

    private MemoryIO initMemoryIO() {
        return this.memory = allocateMemoryIO();
    }

    protected abstract MemoryIO allocateMemoryIO();
}
