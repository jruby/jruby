package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.util.ByteList;

/**
*
*/
final class NativeStringHandle {
    final ByteList bl;
    final DirectMemoryIO memory;

    NativeStringHandle(DirectMemoryIO memory, ByteList bl) {
        this.memory = memory;
        this.bl = bl;
    }
}
