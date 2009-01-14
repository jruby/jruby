
package org.jruby.ext.ffi.jffi;

import java.nio.channels.ByteChannel;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.FFIProvider;

public class Factory extends org.jruby.ext.ffi.Factory {

    public Factory() {
        if (!com.kenai.jffi.Platform.getPlatform().isSupported()) {
            throw new UnsatisfiedLinkError("JFFI backend not available");
        }
    }

    @Override
    public void init(Ruby runtime, RubyModule ffi) {
        super.init(runtime, ffi);
        //
        // Hook up the MemoryPointer class if its not already there
        //
        synchronized (ffi) {
            if (ffi.fastGetClass("DynamicLibrary") == null) {
                DynamicLibrary.createDynamicLibraryClass(runtime, ffi);
            }
            if (ffi.fastGetClass("Invoker") == null) {
                JFFIInvoker.createInvokerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("VariadicInvoker") == null) {
                VariadicInvoker.createVariadicInvokerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("Callback") == null) {
                CallbackManager.createCallbackClass(runtime, ffi);
            }
        }
    }
    @Override
    protected FFIProvider newProvider(Ruby runtime) {
        return new JFFIProvider(runtime);
    }

    @Override
    public <T> T loadLibrary(String libraryName, Class<T> libraryClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ByteChannel newByteChannel(int fd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param clear If the memory should be cleared.
     * @return A new <tt>MemoryIO</tt>.
     */
    public AllocatedDirectMemoryIO allocateDirectMemory(int size, boolean clear) {
        return AllocatedNativeMemoryIO.allocate(size, clear);
    }

    public DirectMemoryIO wrapDirectMemory(long address) {
        return address != 0 ? new NativeMemoryIO(address) : null;
    }
    @Override
    public CallbackManager getCallbackManager() {
        return CallbackManager.getInstance();
    }
}
