
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyModule;

/**
 * An instance of Factory that is used when no FFI implementation can be found.
 */
public class NoImplFactory extends Factory {
    private final String msg;

    public NoImplFactory(String msg) {
        this.msg = new StringBuilder("FFI not available: ").append(msg).toString();
    }

    @Override
    public void init(Ruby runtime, RubyModule ffi) {
        throw runtime.newNotImplementedError(msg);
    }


    @Override
    public MemoryIO allocateDirectMemory(Ruby runtime, int size, boolean clear) {
        throw runtime.newNotImplementedError(msg);
    }

    @Override
    public MemoryIO allocateDirectMemory(Ruby runtime, int size, int align, boolean clear) {
        throw runtime.newNotImplementedError(msg);
    }

    @Override
    public MemoryIO allocateTransientDirectMemory(Ruby runtime, int size, int align, boolean clear) {
        throw runtime.newNotImplementedError(msg);
    }

    @Override
    public MemoryIO wrapDirectMemory(Ruby runtime, long address) {
        throw runtime.newNotImplementedError(msg);
    }

    @Override
    public CallbackManager getCallbackManager() {
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public AbstractInvoker newFunction(Ruby runtime, Pointer address, CallbackInfo cbInfo) {
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public int sizeOf(NativeType type) {
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public int alignmentOf(NativeType type) {
        throw new UnsupportedOperationException(msg);
    }

}
