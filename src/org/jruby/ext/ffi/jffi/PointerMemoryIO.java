
package org.jruby.ext.ffi.jffi;

/**
 * A MemoryIO implementation that has a real memory address
 */
public interface PointerMemoryIO extends org.jruby.ext.ffi.MemoryIO {
    long getAddress();
    void putAddress(long offset, long value);
}
